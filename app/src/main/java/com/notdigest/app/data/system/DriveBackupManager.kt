package com.notdigest.app.data.system

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.notdigest.app.di.IoDispatcher
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
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/** A user-facing failure talking to Google Drive (carries a short message for the UI). */
class DriveException(message: String) : Exception(message)

/**
 * Google Drive backup/restore. Mirrors the ColorCloset approach: Google Sign-In with the narrow
 * `drive.file` scope, one canonical backup file inside a dedicated "Notification Digest" folder, and
 * the Drive v3 REST API hit directly with the OAuth access token. Backs up the SAME config snapshot
 * as [ConfigBackupManager] (classifications, schedules, settings — never notification content).
 *
 * One-way dependency on [ConfigBackupManager] (for the JSON) — Config never references Drive, so the
 * auto-backup observer here can't create a cycle.
 */
@Singleton
class DriveBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configBackupManager: ConfigBackupManager,
    private val preferencesRepository: PreferencesRepository,
    private val appRuleRepository: AppRuleRepository,
    private val scheduleRepository: ScheduleRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val signInOptions: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveConfig.DRIVE_FILE_SCOPE))
            .requestIdToken(DriveConfig.WEB_CLIENT_ID)
            .build()
    }

    val isConfigured: Boolean get() = DriveConfig.isConfigured

    fun signInClient(): GoogleSignInClient = GoogleSignIn.getClient(context, signInOptions)

    /** The signed-in Google account email, or null if not connected. */
    fun currentEmail(): String? = GoogleSignIn.getLastSignedInAccount(context)?.email

    /** Parse the sign-in Activity result; returns the account email, or null if it failed/cancelled. */
    fun handleSignInResult(data: Intent?): String? {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return runCatching { task.getResult(ApiException::class.java) }.getOrNull()?.email
    }

    suspend fun signOut() {
        withContext(io) { runCatching { Tasks.await(signInClient().signOut()) } }
    }

    /**
     * Observe config-affecting changes and, when auto-backup is on and signed in, push a fresh Drive
     * backup (debounced, silent). Start once from the Application, AFTER any restore has run.
     */
    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        if (!DriveConfig.isConfigured) return
        var primed = false
        combine(
            appRuleRepository.observeRules(),
            scheduleRepository.observeSchedules(),
            preferencesRepository.preferences,
        ) { _, _, _ -> }
            .debounce(DEBOUNCE_MS)
            .onEach {
                if (!primed) { primed = true; return@onEach } // skip the initial hydration emission
                if (!preferencesRepository.driveAutoBackup.first()) return@onEach
                if (currentEmail() == null) return@onEach
                runCatching { backupNow() } // silent — manual backup stays available if this fails
            }
            .flowOn(io)
            .launchIn(scope)
    }

    /** Upload the current config snapshot to Drive, creating the folder/file as needed. Returns the time. */
    suspend fun backupNow(): Long = withContext(io) {
        val json = configBackupManager.exportJson()
        val token = accessToken()
        val folderId = ensureFolderId(token)
        val existing = findBackupId(token, folderId)
        // Create the new backup FIRST; only then remove the old one. If create fails, the old backup
        // survives; if delete fails, the worst case is a harmless duplicate (findBackupId takes the
        // newest). Never delete-before-create — that once destroyed a real backup.
        createFile(token, folderId, json)
        if (existing != null) runCatching { deleteFile(token, existing) }
        val now = System.currentTimeMillis()
        preferencesRepository.setDriveLastBackupAt(now)
        now
    }

    /** Restore the config from the Drive backup. Returns false if there's no backup yet. */
    suspend fun restoreFromDrive(): Boolean = withContext(io) {
        val token = accessToken()
        val folderId = findFolderId(token) ?: return@withContext false
        val id = findBackupId(token, folderId) ?: return@withContext false
        configBackupManager.importJson(downloadFile(token, id))
    }

    /**
     * Whether a backup already exists on Drive — WITHOUT creating the folder or touching anything.
     * Used on connect so we never blindly overwrite a real backup with a fresh/empty state.
     */
    suspend fun backupExists(): Boolean = withContext(io) {
        runCatching {
            val token = accessToken()
            val folderId = findFolderId(token) ?: return@runCatching false
            findBackupId(token, folderId) != null
        }.getOrDefault(false)
    }

    // --- Drive v3 REST (raw HTTP with the access token) ---

    private fun accessToken(): String {
        val account: Account = GoogleSignIn.getLastSignedInAccount(context)?.account
            ?: throw DriveException("Not signed in to Google.")
        return GoogleAuthUtil.getToken(context, account, "oauth2:${DriveConfig.DRIVE_FILE_SCOPE}")
    }

    /** The app's Drive folder id, or null if it doesn't exist yet (does NOT create it). */
    private fun findFolderId(token: String): String? {
        val q = URLEncoder.encode(
            "name = '${DriveConfig.FOLDER_NAME}' and mimeType = '${DriveConfig.FOLDER_MIME}' and trashed = false",
            "UTF-8",
        )
        val list = request("GET", "$DRIVE/files?q=$q&spaces=drive&pageSize=1&fields=files(id)", token)
        return JSONObject(list).optJSONArray("files")?.takeIf { it.length() > 0 }
            ?.getJSONObject(0)?.getString("id")
    }

    private fun ensureFolderId(token: String): String {
        findFolderId(token)?.let { return it }
        val body = JSONObject()
            .put("name", DriveConfig.FOLDER_NAME)
            .put("mimeType", DriveConfig.FOLDER_MIME)
            .toString()
        val created = request("POST", "$DRIVE/files?fields=id", token, "application/json", body)
        return JSONObject(created).getString("id")
    }

    private fun findBackupId(token: String, folderId: String): String? {
        val q = URLEncoder.encode(
            "name = '${DriveConfig.FILE_NAME}' and '$folderId' in parents and trashed = false",
            "UTF-8",
        )
        val res = request(
            "GET",
            "$DRIVE/files?q=$q&spaces=drive&orderBy=modifiedTime desc&pageSize=1&fields=files(id)",
            token,
        )
        return JSONObject(res).optJSONArray("files")?.takeIf { it.length() > 0 }
            ?.getJSONObject(0)?.getString("id")
    }

    private fun createFile(token: String, folderId: String, data: String) {
        val boundary = "ndbackupboundary"
        val metadata = JSONObject()
            .put("name", DriveConfig.FILE_NAME)
            .put("mimeType", "application/json")
            .put("parents", JSONArray().put(folderId))
            .toString()
        val body = buildString {
            append("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n")
            append("--$boundary\r\nContent-Type: application/json\r\n\r\n$data\r\n")
            append("--$boundary--")
        }
        request("POST", "$UPLOAD/files?uploadType=multipart&fields=id", token, "multipart/related; boundary=$boundary", body)
    }

    private fun deleteFile(token: String, id: String) {
        request("DELETE", "$DRIVE/files/$id", token)
    }

    private fun downloadFile(token: String, id: String): String =
        request("GET", "$DRIVE/files/$id?alt=media", token)

    private fun request(
        method: String,
        url: String,
        token: String,
        contentType: String? = null,
        body: String? = null,
    ): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer $token")
            if (contentType != null) setRequestProperty("Content-Type", contentType)
            if (body != null) {
                doOutput = true
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw DriveException("Google Drive request failed ($code). ${text.take(140)}")
            text
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val DRIVE = "https://www.googleapis.com/drive/v3"
        const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        const val DEBOUNCE_MS = 5_000L
    }
}
