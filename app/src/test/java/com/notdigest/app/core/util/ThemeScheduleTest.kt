package com.notdigest.app.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThemeScheduleTest {

    private val eightPm = 20 * 60 // 1200
    private val sixAm = 6 * 60 // 360

    // --- Default overnight window (8pm → 6am, wraps past midnight) ---

    @Test
    fun `night is dark across the wrap`() {
        assertThat(ThemeSchedule.isDark(22 * 60, eightPm, sixAm)).isTrue() // 10pm
        assertThat(ThemeSchedule.isDark(0, eightPm, sixAm)).isTrue() // midnight
        assertThat(ThemeSchedule.isDark(2 * 60, eightPm, sixAm)).isTrue() // 2am
    }

    @Test
    fun `daytime is light`() {
        assertThat(ThemeSchedule.isDark(9 * 60, eightPm, sixAm)).isFalse() // 9am
        assertThat(ThemeSchedule.isDark(13 * 60, eightPm, sixAm)).isFalse() // 1pm
        assertThat(ThemeSchedule.isDark(19 * 60 + 59, eightPm, sixAm)).isFalse() // 7:59pm
    }

    @Test
    fun `start boundary turns dark on, end boundary turns dark off`() {
        // Half-open [start, end): dark begins exactly at start, ends exactly at end.
        assertThat(ThemeSchedule.isDark(eightPm, eightPm, sixAm)).isTrue() // exactly 8:00pm → dark
        assertThat(ThemeSchedule.isDark(sixAm, eightPm, sixAm)).isFalse() // exactly 6:00am → light
        assertThat(ThemeSchedule.isDark(sixAm - 1, eightPm, sixAm)).isTrue() // 5:59am → still dark
    }

    // --- Non-wrapping window (e.g. a daytime dark window 9am → 5pm) ---

    @Test
    fun `non-wrapping window is dark only inside it`() {
        val nineAm = 9 * 60
        val fivePm = 17 * 60
        assertThat(ThemeSchedule.isDark(12 * 60, nineAm, fivePm)).isTrue() // noon, inside
        assertThat(ThemeSchedule.isDark(8 * 60, nineAm, fivePm)).isFalse() // 8am, before
        assertThat(ThemeSchedule.isDark(18 * 60, nineAm, fivePm)).isFalse() // 6pm, after
    }

    // --- Degenerate window ---

    @Test
    fun `zero-length window is never dark`() {
        assertThat(ThemeSchedule.isDark(eightPm, eightPm, eightPm)).isFalse()
        assertThat(ThemeSchedule.isDark(0, eightPm, eightPm)).isFalse()
    }

    // --- isDarkAt against a real instant/zone ---

    @Test
    fun `isDarkAt resolves wall-clock in the given zone`() {
        val zone = java.time.ZoneId.of("UTC")
        // 2026-01-01T23:00:00Z → 11pm → inside the 8pm-6am window → dark
        val elevenPm = java.time.ZonedDateTime.of(2026, 1, 1, 23, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertThat(ThemeSchedule.isDarkAt(elevenPm, zone, eightPm, sixAm)).isTrue()
        // 2026-01-01T12:00:00Z → noon → light
        val noon = java.time.ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertThat(ThemeSchedule.isDarkAt(noon, zone, eightPm, sixAm)).isFalse()
    }
}
