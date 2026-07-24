package com.notdigest.app.domain.repository

import com.notdigest.app.domain.model.AppRule
import com.notdigest.app.domain.model.DigestMode
import kotlinx.coroutines.flow.Flow

/** Per-app Digest vs Real-Time rules. */
interface AppRuleRepository {

    fun observeRules(): Flow<List<AppRule>>

    /** Apps the user has explicitly (or by default) moved to Real-Time, most recent first. */
    fun observeRecentlyChanged(limit: Int = 5): Flow<List<AppRule>>

    fun observeRealtimeCount(): Flow<Int>

    /**
     * The handling for a package. Apps with no explicit rule default to [DigestMode.DIGEST]
     * (the whole point of the product), except the small critical set seeded at first run.
     */
    suspend fun getMode(packageName: String): DigestMode

    /**
     * Resolve [packageName]'s mode, seeding an *untouched default* rule (`updatedAt = 0L`) the first
     * time we ever encounter the app. Unlike [getMode] (a pure read), this persists a row, so a
     * brand-new app — installed after the initial seed and never opened in the Apps list — appears in
     * the management list. Returns the existing mode unchanged if a rule already exists (never
     * overwrites a user's choice).
     */
    suspend fun ensureSeeded(packageName: String, appName: String, isSystemApp: Boolean): DigestMode

    suspend fun setMode(packageName: String, appName: String, mode: DigestMode)

    suspend fun setModeForAll(packages: List<Pair<String, String>>, mode: DigestMode)

    /** Seed rules for newly seen apps, keeping a tiny critical set Real-Time by default. */
    suspend fun seedDefaults(apps: List<AppRule>)

    suspend fun snapshot(): List<AppRule>
}
