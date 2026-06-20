package com.notdigest.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.model.RecommendationType
import com.notdigest.app.domain.usecase.GenerateRecommendationsUseCase.AppVolume
import org.junit.Test

class GenerateRecommendationsUseCaseTest {

    private val useCase = GenerateRecommendationsUseCase()

    private fun volume(pkg: String, count: Int, mode: DigestMode = DigestMode.DIGEST) =
        AppVolume(packageName = pkg, appName = pkg, mode = mode, weeklyCount = count)

    @Test
    fun `very high volume suggests moving to Real-Time`() {
        val recs = useCase(listOf(volume("noisy", 80)), dismissed = emptySet())
        assertThat(recs).hasSize(1)
        assertThat(recs.first().type).isEqualTo(RecommendationType.MOVE_TO_REALTIME)
    }

    @Test
    fun `moderate volume affirms keeping in Digest`() {
        val recs = useCase(listOf(volume("steady", 25)), dismissed = emptySet())
        assertThat(recs.first().type).isEqualTo(RecommendationType.KEEP_IN_DIGEST)
    }

    @Test
    fun `low volume produces no recommendation`() {
        assertThat(useCase(listOf(volume("quiet", 5)), dismissed = emptySet())).isEmpty()
    }

    @Test
    fun `real-time apps are never recommended`() {
        val recs = useCase(listOf(volume("rt", 90, DigestMode.REALTIME)), dismissed = emptySet())
        assertThat(recs).isEmpty()
    }

    @Test
    fun `dismissed apps are excluded`() {
        val recs = useCase(listOf(volume("noisy", 80)), dismissed = setOf("noisy"))
        assertThat(recs).isEmpty()
    }

    @Test
    fun `results are capped and ordered by volume`() {
        val volumes = (1..10).map { volume("app$it", count = it * 10) }
        val recs = useCase(volumes, dismissed = emptySet(), maxResults = 3)
        assertThat(recs).hasSize(3)
        // Top 3 by weekly volume, highest first (app1's count of 10 is below the moderate threshold).
        assertThat(recs.map { it.weeklyCount }).containsExactly(100, 90, 80).inOrder()
    }
}
