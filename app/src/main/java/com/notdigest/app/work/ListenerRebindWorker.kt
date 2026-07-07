package com.notdigest.app.work

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notdigest.app.service.DigestNotificationListenerService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodically asks the system to (re)bind the notification listener.
 *
 * Aggressive OEM battery management silently unbinds/kills the listener in the background, so
 * notifications posted while it's disconnected are never intercepted and just linger in the shade until
 * the user opens the app (which is when the system finally rebinds us). Nudging a rebind here triggers
 * [DigestNotificationListenerService.onListenerConnected] → the active-notification sweep, which
 * suppresses those slipped-through Digest notifications on its own.
 *
 * Best-effort by nature: if the OS force-stopped the whole process, this worker can't run until the app
 * is next opened. But for the common "listener unbound while the process is still alive / process
 * reclaimed then WorkManager re-runs us" cases, it recovers the listener without any user action and
 * without a persistent foreground-service notification.
 */
@HiltWorker
class ListenerRebindWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // requestRebind is a no-op if we're already bound, and simply logs if access was revoked.
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(applicationContext, DigestNotificationListenerService::class.java),
            )
        }
        return Result.success()
    }
}
