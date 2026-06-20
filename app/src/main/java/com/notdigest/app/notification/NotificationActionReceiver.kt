package com.notdigest.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notdigest.app.core.Constants
import com.notdigest.app.work.DigestDeliveryWorker

/** Handles action buttons fired from notifications (currently "Deliver now"). */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Constants.ACTION_DELIVER_NOW -> DigestDeliveryWorker.enqueueNow(context.applicationContext)
        }
    }
}
