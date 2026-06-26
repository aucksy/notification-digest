package com.notdigest.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.notdigest.app.core.Constants
import com.notdigest.app.domain.model.ThemeMode
import com.notdigest.app.domain.model.UserPreferences
import com.notdigest.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DARK_START = intPreferencesKey("dark_mode_start_time")
        val DARK_END = intPreferencesKey("dark_mode_end_time")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val RETENTION = intPreferencesKey("retention_days")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val RECOMMENDATIONS = booleanPreferencesKey("recommendations_enabled")
        val STATUS_NOTIF = booleanPreferencesKey("status_notification_enabled")
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val CONFIG_RESTORED = booleanPreferencesKey("config_restored")
        val CRITICAL_DEFAULTS_VERSION = intPreferencesKey("critical_defaults_version")
        val DRIVE_AUTO = booleanPreferencesKey("drive_auto_backup")
        val DRIVE_LAST_BACKUP = longPreferencesKey("drive_last_backup_at")
        val LIFETIME_AVOIDED = longPreferencesKey("lifetime_avoided")
        val BG_SETUP_DONE = booleanPreferencesKey("background_setup_done")
        val INBOX_SEEN_AT = longPreferencesKey("inbox_seen_at")
        val SWIPE_HINTED = stringSetPreferencesKey("swipe_hinted_packages")
    }

    override val preferences: Flow<UserPreferences> = dataStore.data.map { p ->
        UserPreferences(
            themeMode = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            darkModeStartTime = p[Keys.DARK_START] ?: (20 * 60),
            darkModeEndTime = p[Keys.DARK_END] ?: (6 * 60),
            dynamicColor = p[Keys.DYNAMIC] ?: true,
            retentionDays = p[Keys.RETENTION] ?: Constants.DEFAULT_RETENTION_DAYS,
            hapticsEnabled = p[Keys.HAPTICS] ?: true,
            recommendationsEnabled = p[Keys.RECOMMENDATIONS] ?: true,
            statusNotificationEnabled = p[Keys.STATUS_NOTIF] ?: false,
            onboardingComplete = p[Keys.ONBOARDING] ?: false,
        )
        // Dedupe so unrelated key writes (e.g. the frequent lifetime-avoided counter) don't re-emit
        // identical preferences and needlessly re-trigger the config backup flow.
    }.distinctUntilChanged()

    override suspend fun snapshot(): UserPreferences = preferences.first()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME] = mode.name }
    }

    override suspend fun setDarkModeStartTime(minuteOfDay: Int) {
        dataStore.edit { it[Keys.DARK_START] = minuteOfDay.coerceIn(0, 1439) }
    }

    override suspend fun setDarkModeEndTime(minuteOfDay: Int) {
        dataStore.edit { it[Keys.DARK_END] = minuteOfDay.coerceIn(0, 1439) }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC] = enabled }
    }

    override suspend fun setRetentionDays(days: Int) {
        dataStore.edit { it[Keys.RETENTION] = days }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.HAPTICS] = enabled }
    }

    override suspend fun setRecommendationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.RECOMMENDATIONS] = enabled }
    }

    override suspend fun setStatusNotificationEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.STATUS_NOTIF] = enabled }
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING] = complete }
    }

    override suspend fun isConfigRestored(): Boolean =
        dataStore.data.map { it[Keys.CONFIG_RESTORED] ?: false }.first()

    override suspend fun setConfigRestored(restored: Boolean) {
        dataStore.edit { it[Keys.CONFIG_RESTORED] = restored }
    }

    override suspend fun criticalDefaultsVersion(): Int =
        dataStore.data.map { it[Keys.CRITICAL_DEFAULTS_VERSION] ?: 0 }.first()

    override suspend fun setCriticalDefaultsVersion(version: Int) {
        dataStore.edit { it[Keys.CRITICAL_DEFAULTS_VERSION] = version }
    }

    override val driveAutoBackup: Flow<Boolean> =
        dataStore.data.map { it[Keys.DRIVE_AUTO] ?: false }.distinctUntilChanged()

    override suspend fun setDriveAutoBackup(enabled: Boolean) {
        dataStore.edit { it[Keys.DRIVE_AUTO] = enabled }
    }

    override val driveLastBackupAt: Flow<Long> =
        dataStore.data.map { it[Keys.DRIVE_LAST_BACKUP] ?: 0L }.distinctUntilChanged()

    override suspend fun setDriveLastBackupAt(millis: Long) {
        dataStore.edit { it[Keys.DRIVE_LAST_BACKUP] = millis }
    }

    override val backgroundSetupDone: Flow<Boolean> =
        dataStore.data.map { it[Keys.BG_SETUP_DONE] ?: false }.distinctUntilChanged()

    override suspend fun setBackgroundSetupDone(done: Boolean) {
        dataStore.edit { it[Keys.BG_SETUP_DONE] = done }
    }

    override val inboxSeenAt: Flow<Long> =
        dataStore.data.map { it[Keys.INBOX_SEEN_AT] ?: 0L }.distinctUntilChanged()

    override suspend fun setInboxSeenAt(millis: Long) {
        dataStore.edit { it[Keys.INBOX_SEEN_AT] = millis }
    }

    override val swipeHintedPackages: Flow<Set<String>> =
        dataStore.data.map { it[Keys.SWIPE_HINTED] ?: emptySet() }.distinctUntilChanged()

    override suspend fun addSwipeHintedPackage(packageName: String) {
        dataStore.edit { it[Keys.SWIPE_HINTED] = (it[Keys.SWIPE_HINTED] ?: emptySet()) + packageName }
    }

    override val lifetimeAvoided: Flow<Long> =
        dataStore.data.map { it[Keys.LIFETIME_AVOIDED] ?: 0L }.distinctUntilChanged()

    override suspend fun addLifetimeAvoided(count: Int) {
        if (count <= 0) return
        dataStore.edit { it[Keys.LIFETIME_AVOIDED] = (it[Keys.LIFETIME_AVOIDED] ?: 0L) + count }
    }

    override suspend fun setLifetimeAvoided(value: Long) {
        dataStore.edit { it[Keys.LIFETIME_AVOIDED] = value }
    }
}
