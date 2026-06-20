package com.notdigest.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {

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
