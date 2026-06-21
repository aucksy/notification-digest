package com.notdigest.app.data.system

/**
 * Google Drive backup configuration.
 *
 * Reuses the owner's existing Google Cloud project (the same one ColorCloset uses), so no new Web
 * client is needed — only an **Android OAuth client** for `com.notdigest.app` + the release SHA-1 has
 * to be added to that project for sign-in to succeed. We request the narrow `drive.file` scope, so the
 * app can only ever see the single backup file it creates — nothing else in the user's Drive.
 */
object DriveConfig {
    /** Web OAuth client id (project-level — shared with ColorCloset's project gmailapi-491903). */
    const val WEB_CLIENT_ID = "240978491498-ddd3k6hv2bgqv6ovkl662mguv5tt8nr2.apps.googleusercontent.com"

    const val FOLDER_NAME = "Notification Digest"
    const val FILE_NAME = "notification-digest-backup.json"
    const val FOLDER_MIME = "application/vnd.google-apps.folder"
    const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

    /** True once a real Web client id is present (lets the UI hide Drive cleanly if it's ever blanked). */
    val isConfigured: Boolean get() = WEB_CLIENT_ID.isNotBlank() && !WEB_CLIENT_ID.startsWith("REPLACE")
}
