package com.notdigest.app.domain.usecase

import com.notdigest.app.core.util.TimeProvider
import com.notdigest.app.domain.model.AppRule
import com.notdigest.app.domain.repository.AppRuleRepository
import com.notdigest.app.domain.repository.InstalledAppsRepository
import javax.inject.Inject

/**
 * Ensures every installed app has a rule, defaulting new apps to Digest (except the small critical
 * set). Never overwrites an existing user choice — `seedDefaults` inserts-or-ignores. Safe to call
 * on every app open and when the listener connects.
 */
class SyncInstalledAppRulesUseCase @Inject constructor(
    private val installedApps: InstalledAppsRepository,
    private val appRules: AppRuleRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke() {
        val now = time.now()
        val apps = installedApps.getInstalledApps()
        appRules.seedDefaults(
            apps.map { AppRule(it.packageName, it.appName, it.mode, it.isSystemApp, now) },
        )
    }
}
