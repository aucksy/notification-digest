package com.notdigest.app.domain.model

/** The user's chosen handling for a specific app. */
data class AppRule(
    val packageName: String,
    val appName: String,
    val mode: DigestMode,
    val isSystemApp: Boolean = false,
    val updatedAt: Long = 0L,
)

/** An installed app combined with its current rule, for the management screen. */
data class InstalledApp(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val mode: DigestMode,
)
