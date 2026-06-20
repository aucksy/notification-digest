package com.notdigest.app.domain.model

/** Direction of a smart suggestion. */
enum class RecommendationType { MOVE_TO_REALTIME, KEEP_IN_DIGEST }

/**
 * A subtle, dismissible suggestion derived purely from notification volume statistics —
 * no AI, no profiling. e.g. "Instagram sent 72 notifications this week. Move to Real-Time?"
 */
data class AppRecommendation(
    val packageName: String,
    val appName: String,
    val weeklyCount: Int,
    val type: RecommendationType,
    val currentMode: DigestMode,
)
