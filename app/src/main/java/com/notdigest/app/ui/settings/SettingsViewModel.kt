package com.notdigest.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.data.system.ConfigBackupManager
import com.notdigest.app.data.system.DriveBackupManager
import com.notdigest.app.domain.model.ThemeMode
import com.notdigest.app.domain.model.UserPreferences
import com.notdigest.app.domain.repository.DigestRepository
import com.notdigest.app.domain.repository.NotificationRepository
import com.notdigest.app.domain.repository.PreferencesRepository
import com.notdigest.app.domain.system.DigestNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the Settings screen needs to render the Google Drive section. */
enum class DriveBusy { NONE, SIGN_IN, BACKUP, RESTORE }

data class DriveUiState(
    val configured: Boolean = false,
    val email: String? = null,
    val lastBackupAt: Long = 0L,
    val autoBackup: Boolean = false,
    val busy: DriveBusy = DriveBusy.NONE,
) {
    val connected: Boolean get() = email != null
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val notificationRepository: NotificationRepository,
    private val digestRepository: DigestRepository,
    private val digestNotifier: DigestNotifier,
    private val configBackupManager: ConfigBackupManager,
    private val driveBackupManager: DriveBackupManager,
) : ViewModel() {

    /** Suggested filename when the user saves a manual backup. */
    val backupFileName: String get() = "notification-digest-backup.json"

    val preferences = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    private val exportChannel = Channel<String>(Channel.BUFFERED)
    val exportData = exportChannel.receiveAsFlow()

    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    // --- Google Drive ---
    private val driveEmail = MutableStateFlow(driveBackupManager.currentEmail())
    private val driveBusy = MutableStateFlow(DriveBusy.NONE)

    val driveState = combine(
        driveEmail,
        driveBusy,
        preferencesRepository.driveAutoBackup,
        preferencesRepository.driveLastBackupAt,
    ) { email, busy, auto, last ->
        DriveUiState(
            configured = driveBackupManager.isConfigured,
            email = email,
            lastBackupAt = last,
            autoBackup = auto,
            busy = busy,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DriveUiState(configured = driveBackupManager.isConfigured, email = driveBackupManager.currentEmail()),
    )

    /** Intent to launch the Google sign-in flow (host it from the Activity result launcher). */
    fun driveSignInIntent(): Intent = driveBackupManager.signInClient().signInIntent

    fun onDriveSignInResult(data: Intent?) {
        launch {
            driveBusy.value = DriveBusy.SIGN_IN
            val email = driveBackupManager.handleSignInResult(data)
            if (email == null) {
                driveBusy.value = DriveBusy.NONE
                eventChannel.send("Google sign-in didn't complete")
                return@launch
            }
            driveEmail.value = email
            // CRITICAL: never overwrite an existing backup on connect (that once wiped real data with
            // a fresh state). If a backup already exists, leave it alone and prompt to restore; only
            // create a first backup when there's genuinely nothing on Drive yet.
            val exists = runCatching { driveBackupManager.backupExists() }.getOrDefault(false)
            if (exists) {
                eventChannel.send("Connected. A Drive backup was found — tap “Restore from Drive” to bring it back.")
            } else {
                runCatching { driveBackupManager.backupNow() }
                    .onSuccess { eventChannel.send("Connected — backed up to Drive") }
                    .onFailure { eventChannel.send(it.message ?: "Connected to Google Drive") }
            }
            driveBusy.value = DriveBusy.NONE
        }
    }

    fun driveBackupNow() {
        launch {
            driveBusy.value = DriveBusy.BACKUP
            runCatching { driveBackupManager.backupNow() }
                .onSuccess { eventChannel.send("Backed up to Google Drive") }
                .onFailure { eventChannel.send(it.message ?: "Backup didn't complete") }
            driveBusy.value = DriveBusy.NONE
        }
    }

    fun driveRestore() {
        launch {
            driveBusy.value = DriveBusy.RESTORE
            runCatching { driveBackupManager.restoreFromDrive() }
                .onSuccess { found -> eventChannel.send(if (found) "Restored from Drive" else "No Drive backup found yet") }
                .onFailure { eventChannel.send(it.message ?: "Restore didn't complete") }
            driveBusy.value = DriveBusy.NONE
        }
    }

    fun driveSignOut() {
        launch {
            driveBackupManager.signOut()
            driveEmail.value = null
            eventChannel.send("Signed out of Google Drive")
        }
    }

    fun setDriveAutoBackup(enabled: Boolean) {
        launch { preferencesRepository.setDriveAutoBackup(enabled) }
    }

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
