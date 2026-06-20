package com.notdigest.app.domain.model

import java.time.LocalTime

/**
 * A scheduled digest delivery time. Stored as [minuteOfDay] (0..1439) so it is timezone- and
 * locale-agnostic; the UI renders it with the device's clock format.
 */
data class Schedule(
    val id: Long = 0L,
    val label: String,
    val minuteOfDay: Int,
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
) {
    val hour: Int get() = minuteOfDay / 60
    val minute: Int get() = minuteOfDay % 60
    val localTime: LocalTime get() = LocalTime.of(hour, minute)

    companion object {
        fun fromTime(hour: Int, minute: Int): Int = (hour * 60 + minute).coerceIn(0, 1439)
    }
}
