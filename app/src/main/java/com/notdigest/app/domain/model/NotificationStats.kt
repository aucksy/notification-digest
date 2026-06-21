package com.notdigest.app.domain.model

/**
 * Lightweight, non-gamified metrics shown on the dashboard to reinforce the app's value.
 * "Interruptions avoided" == notifications that were batched instead of buzzing the user.
 */
data class NotificationStats(
    val waitingCount: Int = 0,
    val batchedToday: Int = 0,
    val avoidedLast7Days: Int = 0,
    val totalAvoided: Long = 0L,
    val deliveredToday: Int = 0,
    val realtimeAppCount: Int = 0,
    /** Monotonic lifetime count of interruptions avoided — survives retention purges and reinstalls. */
    val lifetimeAvoided: Long = 0L,
)
