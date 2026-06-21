package com.notdigest.app.core.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneId

/**
 * Emits the current local day immediately, then re-emits at each local midnight. Lets day-bounded
 * UI and stats (e.g. "today" counters, "Today/Yesterday" sections) roll over while the app stays
 * open, instead of freezing on the day they were first computed.
 */
fun localDayFlow(zone: ZoneId): Flow<LocalDate> = flow {
    while (true) {
        val today = LocalDate.now(zone)
        emit(today)
        val nextMidnightMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        delay((nextMidnightMs - System.currentTimeMillis()).coerceAtLeast(1_000L))
    }
}
