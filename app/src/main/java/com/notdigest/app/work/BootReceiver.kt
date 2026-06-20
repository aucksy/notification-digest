package com.notdigest.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notdigest.app.domain.system.DigestScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Re-arms the digest chain and cleanup job after a reboot or app update. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: DigestScheduler

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> {
                scheduler.reschedule()
                scheduler.ensureCleanupScheduled()
            }
        }
    }
}
