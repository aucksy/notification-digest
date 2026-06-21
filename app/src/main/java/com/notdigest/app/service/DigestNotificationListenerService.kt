package com.notdigest.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.notdigest.app.core.util.TimeProvider
import com.notdigest.app.data.system.PendingIntentStore
import com.notdigest.app.di.ApplicationScope
import com.notdigest.app.domain.model.AppNotification
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.model.NotificationActionItem
import com.notdigest.app.domain.repository.AppRuleRepository
import com.notdigest.app.domain.repository.NotificationRepository
import com.notdigest.app.domain.repository.PreferencesRepository
import com.notdigest.app.domain.system.DigestNotifier
import com.notdigest.app.domain.usecase.InitializeAppDataUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The core of the product. The system binds this service and delivers every posted notification.
 *
 * For apps in **Digest** mode we persist the notification and cancel it from the shade (so the user
 * is not interrupted); for **Real-Time** apps and a fixed set of never-batch cases (ongoing media,
 * calls, foreground services, group summaries) we do nothing and let Android behave normally.
 */
@AndroidEntryPoint
class DigestNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var appRuleRepository: AppRuleRepository
    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var pendingIntentStore: PendingIntentStore
    @Inject lateinit var digestNotifier: DigestNotifier
    @Inject lateinit var modeCache: ModeCache
    @Inject lateinit var initializeAppData: InitializeAppDataUseCase
    @Inject lateinit var time: TimeProvider

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationAccessState.setConnected(true)
        scope.launch {
            runCatching { initializeAppData() }
            // Status notification is off by default; clear any lingering one (e.g. after an update).
            if (!preferencesRepository.snapshot().statusNotificationEnabled) {
                digestNotifier.clearCollectingStatus()
            }
            // Catch up on anything that slipped through while we were dead.
            runCatching { sweepActiveNotifications() }
        }
    }

    /**
     * Suppress any Digest-mode notifications that are currently showing. Aggressive OEM battery
     * management kills this listener; notifications posted while we're dead are never intercepted and
     * just sit in the shade (e.g. an Amazon promo in the "Silent" tray). On every (re)connect we
     * re-scan the live notifications and tuck away the ones that should have been suppressed.
     */
    private suspend fun sweepActiveNotifications() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        active.forEach { sbn ->
            if (shouldIgnore(sbn)) return@forEach
            val captured = capture(sbn) ?: return@forEach
            runCatching { processWithLookup(captured) }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationAccessState.setConnected(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        if (shouldIgnore(sbn)) return

        // Decide as fast as possible. For Real-Time apps, do nothing. For known Digest apps, cancel
        // synchronously on the binder thread (no DB wait) so the notification barely flashes.
        when (modeCache.cachedMode(sbn.packageName)) {
            DigestMode.REALTIME -> return
            DigestMode.DIGEST -> {
                val captured = capture(sbn) ?: return
                captured.contentIntent?.let {
                    pendingIntentStore.put(captured.key, it, captured.actionIntents)
                }
                cancelNotification(captured.key)
                scope.launch { persist(captured) }
            }
            null -> {
                // Cold start / never-seen app: fall back to the authoritative async lookup.
                val captured = capture(sbn) ?: return
                scope.launch { processWithLookup(captured) }
            }
        }
    }

    /** Fast path: the notification is already cancelled; just store it and update the status. */
    private suspend fun persist(captured: CapturedNotification) {
        try {
            notificationRepository.upsertPending(captured.toDomain())
            updateStatusNotification()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to persist notification from ${captured.packageName}", t)
        }
    }

    /** Slow path used until the mode cache is warm: confirm the mode against the DB, then suppress. */
    private suspend fun processWithLookup(captured: CapturedNotification) {
        try {
            if (appRuleRepository.getMode(captured.packageName) != DigestMode.DIGEST) return
            captured.contentIntent?.let {
                pendingIntentStore.put(captured.key, it, captured.actionIntents)
            }
            cancelNotification(captured.key)
            notificationRepository.upsertPending(captured.toDomain())
            updateStatusNotification()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to handle notification from ${captured.packageName}", t)
        }
    }

    private suspend fun updateStatusNotification() {
        if (preferencesRepository.snapshot().statusNotificationEnabled) {
            digestNotifier.showCollectingStatus(notificationRepository.pendingSnapshot().size)
        }
    }

    /** Cases we never batch, regardless of the app's mode. */
    private fun shouldIgnore(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName == packageName) return true
        if (!sbn.isClearable) return true // ongoing / non-dismissible
        val flags = sbn.notification.flags
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return true
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return true
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return true
        return when (sbn.notification.category) {
            Notification.CATEGORY_CALL,
            Notification.CATEGORY_TRANSPORT,
            Notification.CATEGORY_NAVIGATION,
            Notification.CATEGORY_SERVICE,
            -> true
            else -> false
        }
    }

    private fun capture(sbn: StatusBarNotification): CapturedNotification? {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = (
            extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            )?.toString()?.trim().orEmpty()
        if (title.isBlank() && text.isBlank()) return null

        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val actions = sbn.notification.actions
        val actionItems = actions?.mapIndexed { i, a ->
            NotificationActionItem(index = i, title = a.title?.toString().orEmpty())
        } ?: emptyList()

        return CapturedNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = loadAppLabel(sbn.packageName),
            title = title,
            text = text,
            subText = subText,
            category = sbn.notification.category,
            postedAt = sbn.postTime,
            contentIntent = sbn.notification.contentIntent,
            actionIntents = actions?.map { it.actionIntent } ?: emptyList(),
            actionItems = actionItems,
        )
    }

    private fun loadAppLabel(pkg: String): String = runCatching {
        val pm = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    private companion object {
        const val TAG = "DigestListener"
    }
}

/** Immutable snapshot of the fields we read from a StatusBarNotification on the binder thread. */
private data class CapturedNotification(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val subText: String?,
    val category: String?,
    val postedAt: Long,
    val contentIntent: android.app.PendingIntent?,
    val actionIntents: List<android.app.PendingIntent?>,
    val actionItems: List<NotificationActionItem>,
) {
    fun toDomain(): AppNotification = AppNotification(
        sbnKey = key,
        packageName = packageName,
        appName = appName,
        title = title,
        text = text,
        subText = subText,
        category = category,
        postedAt = postedAt,
        hasDeepLink = contentIntent != null,
        actions = actionItems,
    )
}
