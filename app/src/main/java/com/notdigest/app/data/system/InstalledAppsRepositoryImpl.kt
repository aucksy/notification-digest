package com.notdigest.app.data.system

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.notdigest.app.core.util.CriticalDefaults
import com.notdigest.app.data.local.dao.AppRuleDao
import com.notdigest.app.di.IoDispatcher
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.model.InstalledApp
import com.notdigest.app.domain.repository.InstalledAppsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledAppsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRuleDao: AppRuleDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : InstalledAppsRepository {

    override suspend fun getInstalledApps(): List<InstalledApp> = withContext(io) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        @Suppress("DEPRECATION", "QueryPermissionsNeeded")
        val resolved = pm.queryIntentActivities(launcherIntent, 0)

        val rules = appRuleDao.snapshot().associateBy { it.packageName }

        resolved.asSequence()
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { info ->
                val pkg = info.packageName
                val label = pm.getApplicationLabel(info).toString()
                val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val mode = rules[pkg]
                    ?.let { runCatching { DigestMode.valueOf(it.mode) }.getOrNull() }
                    ?: defaultMode(pkg, label)
                InstalledApp(pkg, label, isSystem, mode)
            }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }

    override suspend fun appLabel(packageName: String): String = withContext(io) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }

    private fun defaultMode(packageName: String, appName: String): DigestMode =
        if (CriticalDefaults.isCritical(packageName, appName)) DigestMode.REALTIME else DigestMode.DIGEST
}
