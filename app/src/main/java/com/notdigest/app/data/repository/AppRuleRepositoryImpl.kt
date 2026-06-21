package com.notdigest.app.data.repository

import com.notdigest.app.core.util.CriticalDefaults
import com.notdigest.app.core.util.TimeProvider
import com.notdigest.app.data.local.dao.AppRuleDao
import com.notdigest.app.data.local.entity.AppRuleEntity
import com.notdigest.app.data.local.mapper.toDomain
import com.notdigest.app.data.local.mapper.toEntity
import com.notdigest.app.domain.model.AppRule
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.repository.AppRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRuleRepositoryImpl @Inject constructor(
    private val dao: AppRuleDao,
    private val time: TimeProvider,
) : AppRuleRepository {

    override fun observeRules(): Flow<List<AppRule>> =
        dao.observeAll().map { it.map(AppRuleEntity::toDomain) }

    override fun observeRecentlyChanged(limit: Int): Flow<List<AppRule>> =
        dao.observeRecentlyChanged(limit).map { it.map(AppRuleEntity::toDomain) }

    override fun observeRealtimeCount(): Flow<Int> =
        dao.observeCountByMode(DigestMode.REALTIME.name)

    override suspend fun getMode(packageName: String): DigestMode {
        dao.getByPackage(packageName)?.let {
            // An explicit rule always wins (incl. a user deliberately moving a critical app to Digest).
            return runCatching { DigestMode.valueOf(it.mode) }.getOrDefault(DigestMode.DIGEST)
        }
        // No rule yet (cold start / brand-new app, before seeding finishes): never suppress a critical
        // app (SMS/OTP/dialer/…) by blindly defaulting to Digest — that could swallow a live 2FA code.
        return if (CriticalDefaults.isCritical(packageName, packageName)) DigestMode.REALTIME else DigestMode.DIGEST
    }

    override suspend fun setMode(packageName: String, appName: String, mode: DigestMode) {
        val existing = dao.getByPackage(packageName)
        dao.upsert(
            AppRuleEntity(
                packageName = packageName,
                appName = appName,
                mode = mode.name,
                isSystemApp = existing?.isSystemApp ?: false,
                updatedAt = time.now(),
            ),
        )
    }

    override suspend fun setModeForAll(packages: List<Pair<String, String>>, mode: DigestMode) {
        if (packages.isEmpty()) return
        val now = time.now()
        val existing = dao.snapshot().associateBy { it.packageName }
        dao.upsertAll(
            packages.map { (pkg, name) ->
                AppRuleEntity(
                    packageName = pkg,
                    appName = name,
                    mode = mode.name,
                    isSystemApp = existing[pkg]?.isSystemApp ?: false,
                    updatedAt = now,
                )
            },
        )
    }

    override suspend fun seedDefaults(apps: List<AppRule>) {
        if (apps.isEmpty()) return
        dao.insertIgnore(apps.map(AppRule::toEntity))
    }

    override suspend fun snapshot(): List<AppRule> = dao.snapshot().map(AppRuleEntity::toDomain)
}
