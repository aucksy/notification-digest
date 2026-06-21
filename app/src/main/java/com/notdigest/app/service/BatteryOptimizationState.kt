package com.notdigest.app.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Battery / background-running helpers. On aggressive OEMs (ColorOS/OxygenOS, etc.) the system kills
 * background processes — including the notification listener — which lets notifications slip past
 * suppression and discards the in-memory deep-link targets.
 *
 * Two independent layers must both be permissive:
 *  1. **Standard battery optimization** (the Doze whitelist) — [isIgnoring], toggled by [requestIgnore].
 *  2. **The OEM's own background control** ("Allow background activity") — NOT exposed by any public
 *     API, so it can't be read or set by the app; we can only guide the user to it ([openAppBatterySettings]).
 *     The one thing we can detect is the *worst* choice, "Restrict" ([isBackgroundRestricted]).
 */
object BatteryOptimizationState {

    /** Layer 1: whether the app is on the standard battery-optimization (Doze) whitelist. */
    fun isIgnoring(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return runCatching { pm.isIgnoringBatteryOptimizations(context.packageName) }.getOrDefault(true)
    }

    /** True only when the user picked "Restrict background activity" — the one OEM state we can read. */
    fun isBackgroundRestricted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        return runCatching { am.isBackgroundRestricted }.getOrDefault(false)
    }

    /** Layer 1 action: the one-tap system dialog to allow ignoring battery optimization. */
    @SuppressLint("BatteryLife")
    fun requestIgnore(context: Context) {
        val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(request) }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    /**
     * Layer 2 guidance: open the app's system details page — one tap from the OEM "App battery
     * management" screen where the user chooses "Allow background activity". There's no public deep
     * link to a manufacturer's own control, so this is the closest reliable landing spot.
     */
    fun openAppBatterySettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
