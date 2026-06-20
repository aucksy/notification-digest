package com.notdigest.app.core

/** App-wide constants. Centralised so IDs and tuning values live in one place. */
object Constants {

    const val DATABASE_NAME = "notdigest.db"
    const val DATASTORE_NAME = "notdigest_prefs"

    // --- Notification channels ---
    const val CHANNEL_DIGEST = "digest_deliveries"
    const val CHANNEL_RECOMMENDATION = "smart_suggestions"
    const val CHANNEL_STATUS = "collection_status"

    // --- Notification ids ---
    const val NOTIF_ID_STATUS = 1
    const val NOTIF_ID_DIGEST_BASE = 1000
    const val NOTIF_ID_RECOMMENDATION_BASE = 2000

    // --- WorkManager unique work ---
    const val WORK_DIGEST_DELIVERY = "work_digest_delivery"
    const val WORK_RETENTION_CLEANUP = "work_retention_cleanup"

    // --- Intent actions / extras (digest notification + deep links) ---
    const val ACTION_OPEN_ROUTE = "com.notdigest.app.action.OPEN_ROUTE"
    const val ACTION_DELIVER_NOW = "com.notdigest.app.action.DELIVER_NOW"
    const val ACTION_OPEN_NOTIFICATION = "com.notdigest.app.action.OPEN_NOTIFICATION"
    const val EXTRA_ROUTE = "extra_route"
    const val EXTRA_DIGEST_ID = "extra_digest_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    // --- Recommendations (pure volume heuristics, no AI) ---
    const val RECOMMEND_MODERATE_WEEKLY = 20
    const val RECOMMEND_HIGH_WEEKLY = 50
    const val MAX_RECOMMENDATIONS = 3

    // --- Retention ---
    const val DEFAULT_RETENTION_DAYS = 30
    val RETENTION_OPTIONS = listOf(7, 14, 30, 60, 90)

    /** Curated starter schedules offered during onboarding. */
    object SchedulePresets {
        val WORKDAY = listOf(9 * 60, 12 * 60, 15 * 60, 18 * 60)
        val BALANCED = listOf(12 * 60, 18 * 60)
        val EVENING = listOf(18 * 60, 21 * 60)
    }
}
