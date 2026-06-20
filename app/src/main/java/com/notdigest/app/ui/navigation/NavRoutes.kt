package com.notdigest.app.ui.navigation

/** Centralised navigation routes, shared by the NavHost and notification deep links. */
object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val INBOX = "inbox"
    const val APPS = "apps"
    const val SCHEDULE = "schedule"
    const val SETTINGS = "settings"

    /** Top-level destinations shown in the bottom navigation bar. */
    val BOTTOM_BAR = listOf(HOME, INBOX, APPS, SETTINGS)
}
