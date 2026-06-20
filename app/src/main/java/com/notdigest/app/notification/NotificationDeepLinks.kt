package com.notdigest.app.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.notdigest.app.MainActivity
import com.notdigest.app.core.Constants

/** Builds the PendingIntents used by digest & status notifications. */
object NotificationDeepLinks {

    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    /** Opens the app and navigates to [route] (an in-app NavHost route string). */
    fun openRoute(context: Context, route: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Constants.ACTION_OPEN_ROUTE
            putExtra(Constants.EXTRA_ROUTE, route)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
        }
        return PendingIntent.getActivity(context, route.hashCode(), intent, FLAGS)
    }

    /** Triggers an immediate "Deliver Now" from the status notification action. */
    fun deliverNow(context: Context): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = Constants.ACTION_DELIVER_NOW
        }
        return PendingIntent.getBroadcast(context, 1001, intent, FLAGS)
    }
}
