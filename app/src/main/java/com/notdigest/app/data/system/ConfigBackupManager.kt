package com.notdigest.app.data.system

import android.app.backup.BackupManager
import android.content.Context
import android.net.Uri
import com.notdigest.app.core.Constants
import com.notdigest.app.di.IoDispatcher
import com.notdigest.app.domain.model.AppRule
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.model.Schedule
import com.notdigest.app.domain.model.ThemeMode
import com.notdigest.app.domain.model.UserPreferences
import com.notdigest.app.domain.repository.AppRuleRepository
import com.notdigest.app.domain.repository.PreferencesRepository
import com.notdigest.app.domain.repository.ScheduleRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backs up the user's *configuration* — app classifications (Digest vs Real-Time), schedules and
 * settings — so a reinstall (or a new device on the same Google account) auto-remembers them.
 *
 * It writes a tiny JSON snapshot to internal storage; Android **Auto Backup** then syncs that single
 * file to the user's Google Drive. We deliberately do NOT use the Drive REST API here: that needs an
 * OAuth client + SHA-1 registration (the exact thing that gated the ColorCloset backup). Auto Backup
 * needs none of that. Notification *content* is never included — only classifications/settings.
 */
@Singleton
class ConfigBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRuleRepository: AppRuleRepository,
    private val scheduleRepository: ScheduleRepository,
    private val preferencesRepository: PreferencesRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val file: File get() = File(context.filesDir, FILE_NAME)
    private val backupManager = BackupManager(context)

    /**
     * Begin keeping the backup snapshot current. Start this ONLY after [restoreIfPresent] has run,
     * so we never overwrite a freshly restored file before reading it.
     */
    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        combine(
            appRuleRepository.observeRules(),
            scheduleRepository.observeSchedules(),
            preferencesRepository.preferences,
        ) { rules, schedules, prefs -> Snapshot(rules, schedules, prefs) }
            .debounce(BACKUP_DEBOUNCE_MS)
            .onEach { runCatching { write(it) } }
            .flowOn(io)
            .launchIn(scope)
    }

    private fun write(snapshot: Snapshot) {
        file.writeText(buildJson(snapshot).toString())
        // Hint the framework to back up sooner than its usual ~daily cadence.
        backupManager.dataChanged()
    }

    private fun buildJson(snapshot: Snapshot): JSONObject = JSONObject().apply {
        put("version", SNAPSHOT_VERSION)
        put("rules", JSONArray().apply {
            snapshot.rules.forEach { rule ->
                put(
                    JSONObject()
                        .put("pkg", rule.packageName)
                        .put("name", rule.appName)
                        .put("mode", rule.mode.name)
                        .put("system", rule.isSystemApp),
                )
            }
        })
        put("schedules", JSONArray().apply {
            snapshot.schedules.forEach { s ->
                put(
                    JSONObject()
                        .put("label", s.label)
                        .put("minute", s.minuteOfDay)
                        .put("enabled", s.enabled)
                        .put("order", s.sortOrder),
                )
            }
        })
        put("prefs", snapshot.prefs.toJson())
    }

    /** Serialize the *current* configuration to a pretty JSON string for a user-chosen file backup. */
    suspend fun exportJson(): String = withContext(io) {
        val snapshot = Snapshot(
            rules = appRuleRepository.observeRules().first(),
            schedules = scheduleRepository.snapshot(),
            prefs = preferencesRepository.preferences.first(),
        )
        buildJson(snapshot).toString(2)
    }

    /** Write the current configuration to a user-picked document (SAF Uri). */
    suspend fun exportToUri(uri: Uri): Boolean = withContext(io) {
        runCatching {
            val json = exportJson()
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } ?: return@runCatching false
            true
        }.getOrDefault(false)
    }

    /** Read a configuration from a user-picked document (SAF Uri) and apply it. */
    suspend fun importFromUri(uri: Uri): Boolean = withContext(io) {
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: return@withContext false
        importJson(text)
    }

    /** Apply a configuration JSON the user imported from a file. Returns false if it isn't a valid backup. */
    suspend fun importJson(text: String): Boolean = withContext(io) {
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return@withContext false
        if (!json.has("rules") && !json.has("prefs")) return@withContext false
        applyJson(json)
        // Keep the internal snapshot (and therefore Auto Backup) in sync with the imported config.
        runCatching {
            file.writeText(json.toString())
            backupManager.dataChanged()
        }
        true
    }

    /**
     * If a restored snapshot exists, import it. Returns true if a snapshot was applied. Rules are set
     * exactly as backed up; schedules are only restored when none exist yet (so we don't duplicate
     * seeded/onboarding schedules).
     */
    suspend fun restoreIfPresent(): Boolean = withContext(io) {
        val snapshotFile = file
        if (!snapshotFile.exists()) return@withContext false
        val json = runCatching { JSONObject(snapshotFile.readText()) }.getOrNull()
            ?: return@withContext false
        applyJson(json)
        true
    }

    private suspend fun applyJson(json: JSONObject) {
        json.optJSONArray("rules")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val pkg = o.optString("pkg").takeIf { it.isNotBlank() } ?: continue
                val mode = runCatching { DigestMode.valueOf(o.optString("mode")) }
                    .getOrDefault(DigestMode.DIGEST)
                appRuleRepository.setMode(pkg, o.optString("name", pkg), mode)
            }
        }

        if (scheduleRepository.snapshot().isEmpty()) {
            json.optJSONArray("schedules")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    scheduleRepository.upsert(
                        Schedule(
                            label = o.optString("label", "Digest"),
                            minuteOfDay = o.optInt("minute", 12 * 60),
                            enabled = o.optBoolean("enabled", true),
                            sortOrder = o.optInt("order", i),
                        ),
                    )
                }
            }
        }

        json.optJSONObject("prefs")?.let { p ->
            runCatching { preferencesRepository.setThemeMode(ThemeMode.valueOf(p.optString("theme"))) }
            preferencesRepository.setDynamicColor(p.optBoolean("dynamic", true))
            preferencesRepository.setRetentionDays(p.optInt("retention", Constants.DEFAULT_RETENTION_DAYS))
            preferencesRepository.setHapticsEnabled(p.optBoolean("haptics", true))
            preferencesRepository.setRecommendationsEnabled(p.optBoolean("recommendations", true))
            preferencesRepository.setStatusNotificationEnabled(p.optBoolean("status", true))
            preferencesRepository.setOnboardingComplete(p.optBoolean("onboarding", false))
        }
    }

    private fun UserPreferences.toJson(): JSONObject = JSONObject()
        .put("theme", themeMode.name)
        .put("dynamic", dynamicColor)
        .put("retention", retentionDays)
        .put("haptics", hapticsEnabled)
        .put("recommendations", recommendationsEnabled)
        .put("status", statusNotificationEnabled)
        .put("onboarding", onboardingComplete)

    private data class Snapshot(
        val rules: List<AppRule>,
        val schedules: List<Schedule>,
        val prefs: UserPreferences,
    )

    companion object {
        const val FILE_NAME = "config_backup.json"
        private const val SNAPSHOT_VERSION = 1
        private const val BACKUP_DEBOUNCE_MS = 2_000L
    }
}
