package com.notdigest.app.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Pure, side-effect-free time formatting helpers (kept Android-free for testability). */
object TimeFormatter {

    private val time12 = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val time24 = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    fun clock(minuteOfDay: Int, is24Hour: Boolean, zone: ZoneId = ZoneId.systemDefault()): String {
        val t = Instant.ofEpochSecond((minuteOfDay * 60).toLong())
            .atZone(ZoneId.of("UTC"))
            .toLocalTime()
        return t.format(if (is24Hour) time24 else time12)
    }

    fun clockOf(hour: Int, minute: Int, is24Hour: Boolean): String =
        clock(hour * 60 + minute, is24Hour)

    /** Short relative label: "Just now", "5m", "3h", "2d", or an absolute date beyond a week. */
    fun relative(thenMillis: Long, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val deltaMs = nowMillis - thenMillis
        val minutes = abs(deltaMs) / 60_000
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m"
            minutes < 60 * 24 -> "${minutes / 60}h"
            minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}d"
            else -> {
                val then = Instant.ofEpochMilli(thenMillis).atZone(zone)
                val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
                val fmt = if (then.year == now.year) dayMonth else dayMonthYear
                then.format(fmt)
            }
        }
    }

    fun absolute(millis: Long, is24Hour: Boolean, zone: ZoneId = ZoneId.systemDefault()): String {
        val z = Instant.ofEpochMilli(millis).atZone(zone)
        val timeFmt = if (is24Hour) time24 else time12
        return "${z.format(dayMonth)}, ${z.toLocalTime().format(timeFmt)}"
    }

    /** Delivery-group label, e.g. "Today, 9:00 AM", "Yesterday, 6:00 PM", or "5 Jun, 12:00 PM". */
    fun deliveredLabel(
        millis: Long,
        nowMillis: Long,
        is24Hour: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val d = Instant.ofEpochMilli(millis).atZone(zone)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val timeStr = d.toLocalTime().format(if (is24Hour) time24 else time12)
        return when (d.toLocalDate()) {
            today -> "Today, $timeStr"
            today.minusDays(1) -> "Yesterday, $timeStr"
            else -> "${d.format(dayMonth)}, $timeStr"
        }
    }

    /** Short date label for the inbox date selector: "Today", "Yesterday", or "5 Jun". */
    fun dateChip(date: java.time.LocalDate, today: java.time.LocalDate): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(if (date.year == today.year) dayMonth else dayMonthYear)
    }

    /** Forward-looking label, e.g. "3:00 PM", "Tomorrow, 9:00 AM", or "5 Jun, 9:00 AM". */
    fun whenLabel(
        targetMillis: Long,
        nowMillis: Long,
        is24Hour: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val target = Instant.ofEpochMilli(targetMillis).atZone(zone)
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val timeFmt = if (is24Hour) time24 else time12
        val timeStr = target.toLocalTime().format(timeFmt)
        return when (target.toLocalDate()) {
            now.toLocalDate() -> timeStr
            now.toLocalDate().plusDays(1) -> "Tomorrow, $timeStr"
            else -> "${target.format(dayMonth)}, $timeStr"
        }
    }
}
