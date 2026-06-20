package com.notdigest.app.domain.model

/** Theme preference; defaults to following the system setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

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
)
