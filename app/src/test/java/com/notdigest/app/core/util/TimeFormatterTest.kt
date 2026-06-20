package com.notdigest.app.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TimeFormatterTest {

    private val zone = ZoneId.of("UTC")

    private fun epoch(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDate.of(year, month, day).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `relative buckets render compactly`() {
        val now = 10_000_000L
        assertThat(TimeFormatter.relative(now - 30_000, now)).isEqualTo("Just now")
        assertThat(TimeFormatter.relative(now - 5 * 60_000, now)).isEqualTo("5m")
        assertThat(TimeFormatter.relative(now - 3 * 60 * 60_000, now)).isEqualTo("3h")
        assertThat(TimeFormatter.relative(now - 2L * 24 * 60 * 60_000, now)).isEqualTo("2d")
    }

    @Test
    fun `clock formats respect 24-hour preference`() {
        assertThat(TimeFormatter.clockOf(15, 0, is24Hour = true)).isEqualTo("15:00")
        // 12-hour AM/PM marker is locale-dependent; assert the clock part only.
        assertThat(TimeFormatter.clockOf(15, 0, is24Hour = false)).contains("3:00")
    }

    @Test
    fun `whenLabel shows time today and Tomorrow prefix the next day`() {
        val now = epoch(2021, 1, 1, 10, 0)
        val laterToday = epoch(2021, 1, 1, 15, 0)
        val tomorrow = epoch(2021, 1, 2, 9, 0)

        assertThat(TimeFormatter.whenLabel(laterToday, now, is24Hour = true, zone)).isEqualTo("15:00")
        assertThat(TimeFormatter.whenLabel(tomorrow, now, is24Hour = true, zone)).isEqualTo("Tomorrow, 09:00")
    }
}
