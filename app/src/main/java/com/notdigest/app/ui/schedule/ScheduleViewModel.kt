package com.notdigest.app.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.core.util.ScheduleLabels
import com.notdigest.app.domain.model.Schedule
import com.notdigest.app.domain.repository.ScheduleRepository
import com.notdigest.app.domain.system.DigestScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val scheduler: DigestScheduler,
) : ViewModel() {

    val schedules = scheduleRepository.observeSchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addSchedule(hour: Int, minute: Int) {
        viewModelScope.launch {
            val minuteOfDay = Schedule.fromTime(hour, minute)
            val order = scheduleRepository.snapshot().size
            scheduleRepository.upsert(
                Schedule(
                    label = ScheduleLabels.forMinute(minuteOfDay),
                    minuteOfDay = minuteOfDay,
                    enabled = true,
                    sortOrder = order,
                ),
            )
            scheduler.reschedule()
        }
    }

    fun updateTime(schedule: Schedule, hour: Int, minute: Int) {
        viewModelScope.launch {
            val minuteOfDay = Schedule.fromTime(hour, minute)
            scheduleRepository.upsert(
                schedule.copy(minuteOfDay = minuteOfDay, label = ScheduleLabels.forMinute(minuteOfDay)),
            )
            scheduler.reschedule()
        }
    }

    fun setEnabled(schedule: Schedule, enabled: Boolean) {
        viewModelScope.launch {
            scheduleRepository.setEnabled(schedule.id, enabled)
            scheduler.reschedule()
        }
    }

    fun delete(schedule: Schedule) {
        viewModelScope.launch {
            scheduleRepository.delete(schedule.id)
            scheduler.reschedule()
        }
    }

    fun move(fromIndex: Int, toIndex: Int) {
        val current = schedules.value
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val reordered = current.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        viewModelScope.launch { scheduleRepository.reorder(reordered.map { it.id }) }
    }
}
