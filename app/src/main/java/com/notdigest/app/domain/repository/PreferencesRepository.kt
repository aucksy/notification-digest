package com.notdigest.app.domain.repository

import com.notdigest.app.domain.model.ThemeMode
import com.notdigest.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/** Persistent user settings, backed by DataStore. */
interface PreferencesRepository {

    val preferences: Flow<UserPreferences>

    suspend fun snapshot(): UserPreferences

    suspend fun setThemeMode(mode: ThemeMode)

    /** Scheduled-theme window bounds, as minute-of-day (0..1439). */
    suspend fun setDarkModeStartTime(minuteOfDay: Int)
    suspend fun setDarkModeEndTime(minuteOfDay: Int)

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

    /** Whether the user has been through the "allow background running" setup (self-attested on return). */
    val backgroundSetupDone: Flow<Boolean>
    suspend fun setBackgroundSetupDone(done: Boolean)

    /** Epoch millis of the last time the Inbox was viewed — drives "new since your last visit" dots. */
    val inboxSeenAt: Flow<Long>
    suspend fun setInboxSeenAt(millis: Long)

    /**
     * Whether the one-time "swipe right → Real-Time" hint has already been shown. It nudges exactly
     * once, ever — on the single top-most not-yet-handled app — then never again for any app.
     */
    val swipeHintShown: Flow<Boolean>
    suspend fun setSwipeHintShown()

    /**
     * Lifetime count of interruptions avoided (every genuinely-new Digest notification suppressed).
     * Monotonic — never decremented by retention purges — and included in the backup so the headline
     * "avoided" figure survives a reinstall.
     */
    val lifetimeAvoided: Flow<Long>
    suspend fun addLifetimeAvoided(count: Int)
    suspend fun setLifetimeAvoided(value: Long)

    // --- Internal one-time flags (not part of the user-facing settings) ---

    /** Whether the cloud-backup config snapshot has already been imported on this install. */
    suspend fun isConfigRestored(): Boolean
    suspend fun setConfigRestored(restored: Boolean)

    /** Version of the critical-defaults migration already applied on this install. */
    suspend fun criticalDefaultsVersion(): Int
    suspend fun setCriticalDefaultsVersion(version: Int)
}
