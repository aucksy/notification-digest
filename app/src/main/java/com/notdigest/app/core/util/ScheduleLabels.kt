package com.notdigest.app.core.util

/** Suggests a friendly default label for a delivery time based on the hour of day. */
object ScheduleLabels {
    fun forMinute(minuteOfDay: Int): String = when (minuteOfDay / 60) {
        in 5..10 -> "Morning"
        in 11..13 -> "Midday"
        in 14..16 -> "Afternoon"
        in 17..20 -> "Evening"
        else -> "Night"
    }
}
