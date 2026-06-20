package com.notdigest.app.ui.navigation

/** Centralised navigation routes, shared by the NavHost and notification deep links. */
object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val INBOX = "inbox"
    const val HISTORY = "history"
    const val APPS = "apps"
    const val SCHEDULE = "schedule"
    const val SETTINGS = "settings"

    const val ARG_DIGEST_ID = "digestId"
    const val DIGEST_DETAIL = "digest_detail/{$ARG_DIGEST_ID}"
    fun digestDetail(id: Long): String = "digest_detail/$id"

    /** Top-level destinations shown in the bottom navigation bar. */
    val BOTTOM_BAR = listOf(HOME, INBOX, HISTORY, APPS, SETTINGS)
}
