package com.notdigest.app.domain.repository

import com.notdigest.app.domain.model.ThemeMode
import com.notdigest.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/** Persistent user settings, backed by DataStore. */
interface PreferencesRepository {

    val preferences: Flow<UserPreferences>

    suspend fun snapshot(): UserPreferences

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setRetentionDays(days: Int)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setRecommendationsEnabled(enabled: Boolean)
    suspend fun setStatusNotificationEnabled(enabled: Boolean)
    suspend fun setOnboardingComplete(complete: Boolean)

    // --- Google Drive backup (device/account-local — deliberately NOT part of UserPreferences, so
    //     updating the last-backup time can't re-trigger the config backup that observes prefs). ---

    val driveAutoBackup: Flow<Boolean>
    suspend fun setDriveAutoBackup(enabled: Boolean)

    val driveLastBackupAt: Flow<Long>
    suspend fun setDriveLastBackupAt(millis: Long)

    // --- Internal one-time flags (not part of the user-facing settings) ---

    /** Whether the cloud-backup config snapshot has already been imported on this install. */
    suspend fun isConfigRestored(): Boolean
    suspend fun setConfigRestored(restored: Boolean)

    /** Version of the critical-defaults migration already applied on this install. */
    suspend fun criticalDefaultsVersion(): Int
    suspend fun setCriticalDefaultsVersion(version: Int)
}
