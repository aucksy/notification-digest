package com.notdigest.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.core.Constants
import com.notdigest.app.core.util.ScheduleLabels
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

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val preferencesRepository: PreferencesRepository,
    private val scheduler: DigestScheduler,
    private val syncRules: SyncInstalledAppRulesUseCase,
) : ViewModel() {

    private val _selectedPreset = MutableStateFlow(SchedulePreset.BALANCED)
    val selectedPreset = _selectedPreset.asStateFlow()

    fun selectPreset(preset: SchedulePreset) {
        _selectedPreset.value = preset
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
