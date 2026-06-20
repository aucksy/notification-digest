package com.notdigest.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.notdigest.app.domain.model.Schedule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ComputeNextDigestTimeUseCaseTest {

    private val useCase = ComputeNextDigestTimeUseCase()
    private val zone = ZoneId.of("UTC")

    private fun epoch(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDate.of(year, month, day).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun schedule(minuteOfDay: Int, enabled: Boolean = true) =
        Schedule(label = "t", minuteOfDay = minuteOfDay, enabled = enabled)

    @Test
    fun `picks the next time later today`() {
        val now = epoch(2021, 1, 1, 10, 0)
        val result = useCase(listOf(schedule(9 * 60), schedule(12 * 60)), now, zone)
        assertThat(result).isEqualTo(epoch(2021, 1, 1, 12, 0))
    }

    @Test
    fun `rolls over to tomorrow's earliest when all times have passed`() {
        val now = epoch(2021, 1, 1, 13, 0)
        val result = useCase(listOf(schedule(9 * 60), schedule(12 * 60)), now, zone)
        assertThat(result).isEqualTo(epoch(2021, 1, 2, 9, 0))
    }

    @Test
    fun `ignores disabled schedules`() {
        val now = epoch(2021, 1, 1, 10, 0)
        val result = useCase(
            listOf(schedule(11 * 60, enabled = false), schedule(15 * 60)),
            now,
            zone,
        )
        assertThat(result).isEqualTo(epoch(2021, 1, 1, 15, 0))
    }

    @Test
    fun `returns null when nothing is enabled`() {
        val now = epoch(2021, 1, 1, 10, 0)
        assertThat(useCase(emptyList(), now, zone)).isNull()
        assertThat(useCase(listOf(schedule(9 * 60, enabled = false)), now, zone)).isNull()
    }

    @Test
    fun `a time exactly equal to now rolls to the next day`() {
        val now = epoch(2021, 1, 1, 12, 0)
        val result = useCase(listOf(schedule(12 * 60)), now, zone)
        assertThat(result).isEqualTo(epoch(2021, 1, 2, 12, 0))
    }
}
