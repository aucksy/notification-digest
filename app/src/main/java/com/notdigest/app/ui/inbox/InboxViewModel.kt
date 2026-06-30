package com.notdigest.app.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.core.util.TimeFormatter
import com.notdigest.app.core.util.localDayFlow
import com.notdigest.app.domain.model.AppNotification
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.model.DigestType
import com.notdigest.app.domain.repository.AppRuleRepository
import com.notdigest.app.domain.repository.NotificationRepository
import com.notdigest.app.domain.repository.PreferencesRepository
import com.notdigest.app.domain.system.LaunchResult
import com.notdigest.app.domain.usecase.DeliverDigestUseCase
import com.notdigest.app.domain.usecase.DeliverResult
import com.notdigest.app.domain.usecase.OpenNotificationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AppFilterOption(val packageName: String, val appName: String, val count: Int)

/** A transient message for the snackbar. When [undo] is non-null, an Undo action is shown. */
data class InboxMessage(val text: String, val undo: (suspend () -> Unit)? = null)

/** One day's worth of delivered notifications. */
data class DaySection(
    val date: LocalDate,
    val label: String,
    val notifications: List<AppNotification>,
) {
    val count: Int get() = notifications.size
    val key: Long get() = date.toEpochDay()
}

/** The collapsible "Older" group: every day before yesterday, split into per-date subgroups. */
data class OlderGroup(
    val count: Int,
    val expanded: Boolean,
    val dates: List<DaySection>,
)

data class InboxUiState(
    val today: DaySection? = null,
    val yesterday: DaySection? = null,
    val older: OlderGroup? = null,
    val apps: List<AppFilterOption> = emptyList(),
    val query: String = "",
    val appFilter: String? = null,
    val selectedIds: Set<Long> = emptySet(),
    val isDelivering: Boolean = false,
    val archivedCount: Int = 0,
    val totalDelivered: Int = 0,
) {
    val selectionMode: Boolean get() = selectedIds.isNotEmpty()
    val isEmpty: Boolean get() = today == null && yesterday == null && older == null

    /** Every notification currently loaded (today + yesterday + all older dates), regardless of expand. */
    fun loaded(): List<AppNotification> = buildList {
        today?.let { addAll(it.notifications) }
        yesterday?.let { addAll(it.notifications) }
        older?.dates?.forEach { addAll(it.notifications) }
    }
}

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val appRuleRepository: AppRuleRepository,
    private val deliverDigestUseCase: DeliverDigestUseCase,
    private val openNotificationUseCase: OpenNotificationUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val inboxScrollRequest: InboxScrollRequest,
) : ViewModel() {

    /** Set when the app is opened from a digest notification — the inbox should jump to the top. */
    val scrollToTop: StateFlow<Boolean> = inboxScrollRequest.scrollToTop

    fun consumeScrollToTop() { inboxScrollRequest.consume() }

    private val zone: ZoneId = ZoneId.systemDefault()

    private val query = MutableStateFlow("")
    private val appFilter = MutableStateFlow<String?>(null)
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val isDelivering = MutableStateFlow(false)
    private val olderExpanded = MutableStateFlow(false)

    private val messageChannel = Channel<InboxMessage>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    // #1 — view-based unread dots. A notification is "new" if its delivery is more recent than the last
    // time the user left the Inbox. Captured on resume (so the dots persist for the whole visit) and
    // advanced on leave (so the same items aren't new next visit). Starts at MAX_VALUE so nothing is
    // drawn as unread until the real threshold loads — avoids an all-dots flash on first frame.
    private val _seenThreshold = MutableStateFlow(Long.MAX_VALUE)
    val seenThreshold: StateFlow<Long> = _seenThreshold.asStateFlow()

    fun onInboxResumed() {
        viewModelScope.launch { _seenThreshold.value = preferencesRepository.inboxSeenAt.first() }
    }

    fun onInboxLeft() {
        viewModelScope.launch { preferencesRepository.setInboxSeenAt(System.currentTimeMillis()) }
    }

    // #2 — apps eligible for the one-time "swipe right → Real-Time" hint: still on the default Digest
    // (updatedAt == 0, i.e. the user hasn't set them) and not yet hinted.
    val hintPackages: StateFlow<Set<String>> = combine(
        appRuleRepository.observeRules(),
        preferencesRepository.swipeHintedPackages,
    ) { rules, hinted ->
        rules.filter { it.mode == DigestMode.DIGEST && it.updatedAt == 0L }
            .map { it.packageName }
            .toSet() - hinted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun markHinted(packageName: String) {
        viewModelScope.launch { preferencesRepository.addSwipeHintedPackage(packageName) }
    }

    // Only DELIVERED notifications appear here. Waiting (suppressed) ones stay hidden until their digest
    // time, or until the user taps "see all now" — that suppression is the core of the app.
    private val delivered = query.flatMapLatest { q ->
        if (q.isBlank()) notificationRepository.observeDelivered()
        else notificationRepository.searchDelivered(q)
    }

    private data class Core(
        val today: DaySection?,
        val yesterday: DaySection?,
        val older: List<DaySection>,
        val apps: List<AppFilterOption>,
        val query: String,
        val appFilter: String?,
        val total: Int,
    )

    private val core = combine(
        delivered,
        query,
        appFilter,
        // Re-emits at each local midnight so Today/Yesterday/Older roll over even if the inbox stays open.
        localDayFlow(zone),
    ) { list, q, filter, today ->
        val yesterday = today.minusDays(1)

        // Collapse exact duplicates an app re-fired within a delivery (same app + title + text).
        val deduped = list.distinctBy { listOf(it.digestId, it.packageName, it.title, it.text) }
        val byApp = if (filter == null) deduped else deduped.filter { it.packageName == filter }

        // Group by each notification's OWN day, so "Yesterday" means yesterday's notifications.
        val byDay = byApp.groupBy { localDateOf(it.postedAt) }
        fun section(day: LocalDate): DaySection? = byDay[day]?.takeIf { it.isNotEmpty() }?.let { notifs ->
            DaySection(day, TimeFormatter.dateChip(day, today), notifs.sortedByDescending { it.postedAt })
        }

        val olderDates = byDay.keys
            .filter { it.isBefore(yesterday) }
            .sortedDescending()
            .mapNotNull { section(it) }

        val apps = deduped
            .groupBy { it.packageName }
            .map { (pkg, items) -> AppFilterOption(pkg, items.first().appName, items.size) }
            .sortedByDescending { it.count }

        Core(section(today), section(yesterday), olderDates, apps, q, filter, deduped.size)
    }

    val uiState = combine(
        core,
        selectedIds,
        isDelivering,
        olderExpanded,
        notificationRepository.observePendingCount(),
    ) { c, sel, delivering, expanded, archived ->
        val older = c.older.takeIf { it.isNotEmpty() }?.let { dates ->
            OlderGroup(count = dates.sumOf { it.count }, expanded = expanded, dates = dates)
        }
        val loadedIds = buildList {
            c.today?.let { addAll(it.notifications) }
            c.yesterday?.let { addAll(it.notifications) }
            c.older.forEach { addAll(it.notifications) }
        }.map { it.id }.toSet()
        InboxUiState(
            today = c.today,
            yesterday = c.yesterday,
            older = older,
            apps = c.apps,
            query = c.query,
            appFilter = c.appFilter,
            selectedIds = sel.intersect(loadedIds),
            isDelivering = delivering,
            archivedCount = archived,
            totalDelivered = c.total,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState())

    private fun localDateOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun onQueryChange(value: String) { query.value = value }

    fun setAppFilter(packageName: String?) {
        appFilter.value = if (appFilter.value == packageName) null else packageName
    }

    fun toggleOlder() { olderExpanded.value = !olderExpanded.value }

    fun toggleSelection(id: Long) {
        selectedIds.value = selectedIds.value.toMutableSet().apply { if (!add(id)) remove(id) }
    }

    fun startSelection(id: Long) { selectedIds.value = setOf(id) }

    fun clearSelection() { selectedIds.value = emptySet() }

    fun selectAll() {
        selectedIds.value = uiState.value.loaded().map { it.id }.toSet()
    }

    fun open(notification: AppNotification) {
        viewModelScope.launch {
            val result = openNotificationUseCase(notification)
            messageChannel.send(InboxMessage(messageFor(result, notification.appName)))
        }
    }

    fun delete(ids: List<Long>) {
        val removed = uiState.value.loaded().filter { it.id in ids }
        viewModelScope.launch {
            notificationRepository.delete(ids)
            selectedIds.value = selectedIds.value - ids.toSet()
            messageChannel.send(
                InboxMessage(
                    text = if (ids.size == 1) "Notification deleted" else "${ids.size} deleted",
                    undo = { removed.forEach { notificationRepository.insert(it) } },
                ),
            )
        }
    }

    fun markRead(ids: List<Long>) {
        viewModelScope.launch {
            notificationRepository.markRead(ids)
            clearSelection()
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            // Advance the "seen" line to now so every current dot clears immediately (view-based model),
            // and keep the per-notification read flag in sync for History.
            val now = System.currentTimeMillis()
            _seenThreshold.value = now
            preferencesRepository.setInboxSeenAt(now)
            notificationRepository.markAllDeliveredRead()
            messageChannel.send(InboxMessage("Marked all as read"))
        }
    }

    fun makeAppRealtime(packageName: String, appName: String) {
        viewModelScope.launch {
            val previous = appRuleRepository.getMode(packageName)
            appRuleRepository.setMode(packageName, appName, DigestMode.REALTIME)
            if (appFilter.value == packageName) appFilter.value = null
            messageChannel.send(
                InboxMessage(
                    text = "$appName is now Real-Time",
                    undo = { appRuleRepository.setMode(packageName, appName, previous) },
                ),
            )
        }
    }

    fun seeNow() {
        viewModelScope.launch {
            isDelivering.value = true
            val result = runCatching { deliverDigestUseCase(DigestType.MANUAL, postNotification = false) }.getOrNull()
            isDelivering.value = false
            messageChannel.send(
                InboxMessage(
                    when (result) {
                        is DeliverResult.Delivered -> "${result.notificationCount} notifications added to your inbox"
                        else -> "Nothing to show yet"
                    },
                ),
            )
        }
    }

    private fun messageFor(result: LaunchResult, appName: String): String = when (result) {
        // Keep these neutral: tapping opens the app, which is the success the user expects. The old
        // "couldn't open the exact screen" wording read like a failure on every older notification.
        LaunchResult.DEEP_LINKED -> "Opening in $appName…"
        LaunchResult.OPENED_APP -> "Opening $appName…"
        LaunchResult.OPENED_SETTINGS -> "Opening $appName…"
        LaunchResult.FAILED -> "Couldn't open $appName"
    }
}
