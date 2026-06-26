package com.notdigest.app.core.util

import java.time.Instant

/** Pure logic for the SCHEDULED theme: is dark mode active right now, given the user's window? */
object ThemeSchedule {

    /**
     * Whether dark mode is active at [nowMinuteOfDay] for a window of [startMinute] → [endMinute]
     * (both minute-of-day, 0..1439). Handles a window that wraps past midnight (e.g. 1200 → 360 =
     * 8pm → 6am). A zero-length window (start == end) is treated as "never dark".
     *
     * Boundaries: dark is active at exactly [startMinute] and inactive at exactly [endMinute]
     * (`[start, end)`), so flipping the clock to the end time turns dark off, matching user intent.
     */
    fun isDark(nowMinuteOfDay: Int, startMinute: Int, endMinute: Int): Boolean = when {
        startMinute == endMinute -> false
        startMinute < endMinute -> nowMinuteOfDay in startMinute until endMinute
        else -> nowMinuteOfDay >= startMinute || nowMinuteOfDay < endMinute
    }

    /** [isDark] evaluated against a wall-clock instant in the given zone. */
    fun isDarkAt(nowEpochMillis: Long, zone: java.time.ZoneId, startMinute: Int, endMinute: Int): Boolean {
        val local = Instant.ofEpochMilli(nowEpochMillis).atZone(zone).toLocalTime()
        return isDark(local.hour * 60 + local.minute, startMinute, endMinute)
    }
}
