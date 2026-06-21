package com.notdigest.app.data.system

import android.app.Activity
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds a weak reference to the currently-resumed Activity so non-UI code (e.g. the deep-link
 * launcher) can start activities with an **Activity** context. Under Android 14/15 launch rules,
 * starting an activity from the application context can be blocked even when the app is visible;
 * launching from the foreground Activity is the reliable path.
 */
@Singleton
class CurrentActivityHolder @Inject constructor() {
    @Volatile
    private var ref: WeakReference<Activity>? = null

    fun set(activity: Activity) { ref = WeakReference(activity) }

    fun clear(activity: Activity) { if (ref?.get() === activity) ref = null }

    fun current(): Activity? = ref?.get()
}
