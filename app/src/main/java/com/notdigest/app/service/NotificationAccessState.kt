package com.notdigest.app.service

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-global signal of whether the system has the listener bound right now. The UI observes
 * this to reflect collection status live; permission *grant* is checked separately via [isGranted].
 */
object NotificationAccessState {
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    internal fun setConnected(value: Boolean) {
        _connected.value = value
    }

    /** Whether the user has enabled Notification Access for this app in system settings. */
    fun isGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
