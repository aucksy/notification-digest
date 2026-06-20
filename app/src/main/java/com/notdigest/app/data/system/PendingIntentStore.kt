package com.notdigest.app.data.system

import android.app.PendingIntent
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache of the live [PendingIntent]s attached to captured notifications.
 *
 * Android does not allow a PendingIntent to be serialized/persisted, so the strongest possible
 * deep-link restoration is to hold the original intents in memory while the process lives and
 * re-fire them on tap (identical to tapping the real notification). After the process dies we fall
 * back to launching the owning app — see [DeepLinkLauncherImpl]. Bounded to avoid unbounded growth.
 */
@Singleton
class PendingIntentStore @Inject constructor() {

    private data class Entry(
        val contentIntent: PendingIntent?,
        val actions: List<PendingIntent?>,
    )

    private val entries = ConcurrentHashMap<String, Entry>()
    private val insertionOrder = ArrayDeque<String>()
    private val maxEntries = 600

    @Synchronized
    fun put(key: String, contentIntent: PendingIntent?, actions: List<PendingIntent?>) {
        if (!entries.containsKey(key)) {
            insertionOrder.addLast(key)
            while (insertionOrder.size > maxEntries) {
                val evicted = insertionOrder.removeFirst()
                entries.remove(evicted)
            }
        }
        entries[key] = Entry(contentIntent, actions)
    }

    fun contentIntent(key: String?): PendingIntent? = key?.let { entries[it]?.contentIntent }

    fun actionIntent(key: String?, index: Int): PendingIntent? =
        key?.let { entries[it]?.actions?.getOrNull(index) }

    fun hasDeepLink(key: String?): Boolean = contentIntent(key) != null

    @Synchronized
    fun remove(key: String?) {
        key?.let {
            entries.remove(it)
            insertionOrder.remove(it)
        }
    }
}
