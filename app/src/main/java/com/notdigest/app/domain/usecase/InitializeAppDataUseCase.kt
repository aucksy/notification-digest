package com.notdigest.app.domain.usecase

import com.notdigest.app.core.util.CriticalDefaults
import com.notdigest.app.core.util.TimeProvider
import com.notdigest.app.data.system.ConfigBackupManager
import com.notdigest.app.domain.model.AppRule
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.repository.AppRuleRepository
import com.notdigest.app.domain.repository.InstalledAppsRepository
import com.notdigest.app.domain.repository.PreferencesRepository
import javax.inject.Inject

/**
 * Startup orchestration, run once per process (from the Application and when the listener connects):
 *  1. **Restore** the cloud-backup config snapshot on a fresh install (classifications/schedules/
 *     settings), exactly once.
 *  2. **Seed** rules for any installed app that doesn't have one (insert-or-ignore).
 *  3. **Migrate** the critical-defaults set to Real-Time once — but only when we did NOT restore a
 *     backup, so a restored install keeps the user's real choices.
 *
 * Idempotent: safe to call from multiple entry points.
 */
class InitializeAppDataUseCase @Inject constructor(
    private val installedApps: InstalledAppsRepository,
    private val appRules: AppRuleRepository,
    private val preferencesRepository: PreferencesRepository,
    private val configBackup: ConfigBackupManager,
    private val time: TimeProvider,
) {
    suspend operator fun invoke() {
        val restored = if (!preferencesRepository.isConfigRestored()) {
            val applied = runCatching { configBackup.restoreIfPresent() }.getOrDefault(false)
            preferencesRepository.setConfigRestored(true)
            applied
        } else {
            false
        }

        val apps = installedApps.getInstalledApps()
        appRules.seedDefaults(
            apps.map { AppRule(it.packageName, it.appName, it.mode, it.isSystemApp, time.now()) },
        )

        if (!restored && preferencesRepository.criticalDefaultsVersion() < CRITICAL_DEFAULTS_VERSION) {
            val ruleByPackage = appRules.snapshot().associateBy { it.packageName }
            apps.asSequence()
                .filter { CriticalDefaults.isCritical(it.packageName, it.appName) }
                .filter { ruleByPackage[it.packageName]?.mode != DigestMode.REALTIME }
                .forEach { appRules.setMode(it.packageName, it.appName, DigestMode.REALTIME) }
            preferencesRepository.setCriticalDefaultsVersion(CRITICAL_DEFAULTS_VERSION)
        }
    }

    companion object {
        /** Bump when the critical-defaults set changes and should be re-applied once. */
        const val CRITICAL_DEFAULTS_VERSION = 1
    }
}
