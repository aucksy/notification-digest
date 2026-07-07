package com.notdigest.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notdigest.app.domain.repository.PreferencesRepository
import com.notdigest.app.domain.system.DigestScheduler
import com.notdigest.app.service.ListenerKeepAliveService
import com.notdigest.app.service.NotificationAccessState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Re-arms the digest chain and cleanup job after a reboot or app update. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: DigestScheduler
    @Inject lateinit var preferencesRepository: PreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> {
                // Keep the receiver alive until the re-arm actually persists (it's a suspend DB +
                // WorkManager write that must finish inside the receiver's window).
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    try {
                        scheduler.rescheduleNow()
                        scheduler.ensureCleanupScheduled()
                        // Bring the keep-alive service back up after a reboot / update (boot is an
                        // allowed foreground-service start context; sync() is guarded either way).
                        val keepAlive = preferencesRepository.keepAliveEnabled.first() &&
                            NotificationAccessState.isGranted(context)
                        ListenerKeepAliveService.sync(context, keepAlive)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
