package com.notdigest.app.domain.usecase

import com.notdigest.app.domain.model.Schedule
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * Pure scheduling logic: given the user's schedules and "now", compute the epoch-millis of the
 * next enabled delivery. Returns null when no schedule is enabled.
 *
 * A schedule whose time has already passed today (or is exactly now) rolls over to tomorrow.
 */
class ComputeNextDigestTimeUseCase @Inject constructor() {

    operator fun invoke(
        schedules: List<Schedule>,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        val enabled = schedules.filter { it.enabled }
        if (enabled.isEmpty()) return null

        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        return enabled.minOf { schedule ->
            val todayAtTime = now.toLocalDate().atTime(schedule.localTime).atZone(zone)
            val candidate =
                if (todayAtTime.toInstant().toEpochMilli() <= nowMillis) todayAtTime.plusDays(1)
                else todayAtTime
            candidate.toInstant().toEpochMilli()
        }
    }
}
