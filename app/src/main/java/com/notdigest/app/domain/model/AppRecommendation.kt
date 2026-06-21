package com.notdigest.app.domain.model

/** Direction of a smart suggestion. Only one kind now: quieting a noisy un-batched app. */
enum class RecommendationType { MOVE_TO_DIGEST }

/**
 * A subtle, dismissible suggestion derived purely from notification volume statistics —
 * no AI, no profiling. e.g. "Instagram sent 72 notifications this week. Move it to Digest?"
 */
data class AppRecommendation(
    val packageName: String,
    val appName: String,
    val weeklyCount: Int,
    val type: RecommendationType,
    val currentMode: DigestMode,
)
