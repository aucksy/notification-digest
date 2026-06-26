package com.notdigest.app.domain.model

/**
 * Theme preference; defaults to following the system setting. [SCHEDULED] switches between light and
 * dark automatically based on a user-chosen time window (see [UserPreferences.darkModeStartTime]).
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK, SCHEDULED }

/** All user-controllable settings, surfaced as a single immutable snapshot. */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val retentionDays: Int = 30,
    val hapticsEnabled: Boolean = true,
    val recommendationsEnabled: Boolean = true,
    // Off by default: users shouldn't be reminded of collected notifications until delivery time.
    val statusNotificationEnabled: Boolean = false,
    val onboardingComplete: Boolean = false,
    // Scheduled-theme window, as minute-of-day (0..1439). Defaults to dark 8pm → 6am. Only consulted
    // when [themeMode] == SCHEDULED. The window may wrap past midnight (start > end), which is the norm.
    val darkModeStartTime: Int = 20 * 60,
    val darkModeEndTime: Int = 6 * 60,
)
