package com.notdigest.app.service

import android.content.Context
import android.os.PowerManager

/**
 * Whether the app is exempt from battery optimization. On aggressive OEMs (ColorOS/OxygenOS, etc.)
 * the system kills background processes — including the notification listener — which lets
 * notifications slip past suppression and discards the in-memory deep-link targets. Exempting the
 * app keeps it running so both work reliably.
 */
object BatteryOptimizationState {
    fun isIgnoring(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return runCatching { pm.isIgnoringBatteryOptimizations(context.packageName) }.getOrDefault(true)
    }
}
