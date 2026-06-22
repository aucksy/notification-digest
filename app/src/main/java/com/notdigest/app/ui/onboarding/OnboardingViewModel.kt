package com.notdigest.app.ui.onboarding

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.core.Constants
import com.notdigest.app.core.util.ScheduleLabels
import com.notdigest.app.data.system.DriveBackupManager
import com.notdigest.app.domain.model.Schedule
import com.notdigest.app.domain.repository.PreferencesRepository
import com.notdigest.app.domain.repository.ScheduleRepository
import com.notdigest.app.domain.system.DigestScheduler
import com.notdigest.app.domain.usecase.SyncInstalledAppRulesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SchedulePreset(val label: String, val description: String, val times: List<Int>) {
    WORKDAY("Workday", "9 AM · 12 PM · 3 PM · 6 PM", Constants.SchedulePresets.WORKDAY),
    BALANCED("Balanced", "12 PM · 6 PM", Constants.SchedulePresets.BALANCED),
    EVENING("Evening", "6 PM · 9 PM", Constants.SchedulePresets.EVENING),
}

/** Drive sign-in / restore state for the onboarding "restore your data" step. */
data class DriveRestoreUi(
    val email: String? = null,
    val backupFound: Boolean = false,
    val busy: Boolean = false,
    val checked: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val preferencesRepository: PreferencesRepository,
    private val scheduler: DigestScheduler,
    private val syncRules: SyncInstalledAppRulesUseCase,
    private val driveBackupManager: DriveBackupManager,
) : ViewModel() {

    private val _selectedPreset = MutableStateFlow(SchedulePreset.BALANCED)
    val selectedPreset = _selectedPreset.asStateFlow()

    private val _driveRestore = MutableStateFlow(DriveRestoreUi())
    val driveRestore = _driveRestore.asStateFlow()

    fun selectPreset(preset: SchedulePreset) {
        _selectedPreset.value = preset
    }

    fun driveSignInIntent(): Intent = driveBackupManager.signInClient().signInIntent

    /** After Google sign-in, check Drive for an existing backup (never overwrites). */
    fun onDriveSignInResult(data: Intent?) {
        viewModelScope.launch {
            _driveRestore.value = _driveRestore.value.copy(busy = true)
            val email = driveBackupManager.handleSignInResult(data)
            if (email == null) {
                _driveRestore.value = DriveRestoreUi()
                return@launch
            }
            // Signing in during onboarding implies the user wants their setup backed up — turn auto-backup
            // on by default (they can still toggle it off in Settings).
            preferencesRepository.setDriveAutoBackup(true)
            val found = runCatching { driveBackupManager.backupExists() }.getOrDefault(false)
            _driveRestore.value = DriveRestoreUi(email = email, backupFound = found, busy = false, checked = true)
        }
    }

    /** Restore the Drive backup, then finish onboarding WITHOUT applying a preset (keeps restored schedules). */
    fun restoreAndFinish(onDone: () -> Unit) {
        viewModelScope.launch {
            _driveRestore.value = _driveRestore.value.copy(busy = true)
            val ok = runCatching { driveBackupManager.restoreFromDrive() }.getOrDefault(false)
            if (!ok) {
                _driveRestore.value = _driveRestore.value.copy(busy = false, backupFound = false)
                return@launch
            }
            runCatching { syncRules() }
            preferencesRepository.setOnboardingComplete(true)
            scheduler.reschedule()
            scheduler.ensureCleanupScheduled()
            onDone()
        }
    }

    /** Applies the chosen schedule, seeds rules and marks onboarding complete. */
    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            applyPreset(_selectedPreset.value)
            runCatching { syncRules() }
            preferencesRepository.setOnboardingComplete(true)
            scheduler.reschedule()
            scheduler.ensureCleanupScheduled()
            onDone()
        }
    }

    private suspend fun applyPreset(preset: SchedulePreset) {
        scheduleRepository.snapshot().forEach { scheduleRepository.delete(it.id) }
        preset.times.forEachIndexed { index, minute ->
            scheduleRepository.upsert(
                Schedule(
                    label = ScheduleLabels.forMinute(minute),
                    minuteOfDay = minute,
                    enabled = true,
                    sortOrder = index,
                ),
            )
        }
    }
}
