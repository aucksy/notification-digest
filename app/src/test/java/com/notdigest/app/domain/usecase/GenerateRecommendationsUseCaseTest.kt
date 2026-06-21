package com.notdigest.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.model.RecommendationType
import com.notdigest.app.domain.usecase.GenerateRecommendationsUseCase.AppVolume
import org.junit.Test

class GenerateRecommendationsUseCaseTest {

    private val useCase = GenerateRecommendationsUseCase()

    // Suggestions are now only for un-batched (Real-Time) apps, so default the mode accordingly.
    private fun volume(pkg: String, count: Int, mode: DigestMode = DigestMode.REALTIME) =
        AppVolume(packageName = pkg, appName = pkg, mode = mode, weeklyCount = count)

    @Test
    fun `noisy real-time app suggests moving to Digest`() {
        val recs = useCase(listOf(volume("noisy", 80)), dismissed = emptySet())
        assertThat(recs).hasSize(1)
        assertThat(recs.first().type).isEqualTo(RecommendationType.MOVE_TO_DIGEST)
    }

    @Test
    fun `apps already in Digest are never recommended`() {
        val recs = useCase(listOf(volume("steady", 80, DigestMode.DIGEST)), dismissed = emptySet())
        assertThat(recs).isEmpty()
    }

    @Test
    fun `volume below the high threshold produces no recommendation`() {
        assertThat(useCase(listOf(volume("light", 25)), dismissed = emptySet())).isEmpty()
    }

    @Test
    fun `critical apps are never suggested for quieting`() {
        // 'messages' matches a critical keyword hint, so it stays Real-Time even when very noisy.
        val recs = useCase(listOf(volume("com.android.messages", 200)), dismissed = emptySet())
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
        // Top 3 by weekly volume, highest first. app1..app4 (10..40) are below the 50 threshold.
        assertThat(recs.map { it.weeklyCount }).containsExactly(100, 90, 80).inOrder()
    }
}
