package com.notdigest.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.data.system.ConfigBackupManager
import com.notdigest.app.domain.model.ThemeMode
import com.notdigest.app.domain.model.UserPreferences
import com.notdigest.app.domain.repository.DigestRepository
import com.notdigest.app.domain.repository.NotificationRepository
import com.notdigest.app.domain.repository.PreferencesRepository
import com.notdigest.app.domain.system.DigestNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val notificationRepository: NotificationRepository,
    private val digestRepository: DigestRepository,
    private val digestNotifier: DigestNotifier,
    private val configBackupManager: ConfigBackupManager,
) : ViewModel() {

    /** Suggested filename when the user saves a manual backup. */
    val backupFileName: String get() = "notification-digest-backup.json"

    val preferences = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    private val exportChannel = Channel<String>(Channel.BUFFERED)
    val exportData = exportChannel.receiveAsFlow()

    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun setThemeMode(mode: ThemeMode) = launch { preferencesRepository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = launch { preferencesRepository.setDynamicColor(enabled) }
    fun setRetentionDays(days: Int) = launch { preferencesRepository.setRetentionDays(days) }
    fun setHaptics(enabled: Boolean) = launch { preferencesRepository.setHapticsEnabled(enabled) }
    fun setRecommendations(enabled: Boolean) = launch { preferencesRepository.setRecommendationsEnabled(enabled) }
    fun setStatusNotification(enabled: Boolean) = launch {
        preferencesRepository.setStatusNotificationEnabled(enabled)
        if (!enabled) digestNotifier.clearCollectingStatus()
    }

    /** Save app classifications, schedules and settings (no notification content) to a chosen file. */
    fun backupToFile(uri: Uri) {
        launch {
            val ok = configBackupManager.exportToUri(uri)
            eventChannel.send(if (ok) "Backup saved to file" else "Couldn't save the backup file")
        }
    }

    /** Restore app classifications, schedules and settings from a chosen backup file. */
    fun restoreFromFile(uri: Uri) {
        launch {
            val ok = configBackupManager.importFromUri(uri)
            eventChannel.send(if (ok) "App classifications & settings restored" else "That file isn't a valid backup")
        }
    }

    fun clearAllData() {
        launch {
            notificationRepository.purgeOlderThan(Long.MAX_VALUE)
            digestRepository.deleteOlderThan(Long.MAX_VALUE)
            eventChannel.send("All notification data cleared")
        }
    }

    fun requestExport() {
        launch {
            val items = notificationRepository.pendingSnapshot()
            val text = buildString {
                appendLine("Notification Digest — local export")
                appendLine("All data stays on your device. Nothing was uploaded.")
                appendLine("Pending notifications: ${items.size}")
                appendLine()
                items.groupBy { it.appName }.toSortedMap().forEach { (app, list) ->
                    appendLine("$app (${list.size})")
                    list.forEach { appendLine("  • ${it.preview}") }
                    appendLine()
                }
            }
            exportChannel.send(text)
        }
    }

    private inline fun launch(crossinline block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
