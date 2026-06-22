package com.notdigest.app.service

import com.notdigest.app.di.ApplicationScope
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.repository.AppRuleRepository
import com.notdigest.app.domain.repository.InstalledAppsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An always-current, in-memory snapshot of every app's [DigestMode], kept in sync with Room.
 *
 * The listener consults this **synchronously** on the binder thread so it can cancel a Digest-mode
 * notification immediately, instead of waiting for a database round-trip — which is what made
 * suppression visibly lag (the notification flashed before it vanished). Unknown packages return
 * null so the listener can fall back to the authoritative async lookup.
 */
@Singleton
class ModeCache @Inject constructor(
    appRuleRepository: AppRuleRepository,
    private val installedApps: InstalledAppsRepository,
    @ApplicationScope scope: CoroutineScope,
) {
    @Volatile
    private var modes: Map<String, DigestMode> = emptyMap()

    // Packages the user can actually see/configure in the Apps list (have a launcher activity). Used to
    // leave system-service notifications (which never appear there) alone. Refreshed alongside rule
    // changes — a newly installed app gets a rule via the sync, which re-emits here.
    @Volatile
    private var launchable: Set<String> = emptySet()

    init {
        appRuleRepository.observeRules()
            .onEach { rules ->
                modes = rules.associate { it.packageName to it.mode }
                runCatching { launchable = installedApps.launchablePackageNames() }
            }
            .launchIn(scope)
    }

    /** The cached mode for [packageName], or null if not yet known (cold start / brand-new app). */
    fun cachedMode(packageName: String): DigestMode? = modes[packageName]

    /** False until the launchable set has been loaded at least once (avoids false negatives at cold start). */
    fun launchableKnown(): Boolean = launchable.isNotEmpty()

    /** Whether [packageName] has a launcher activity (i.e. appears in the user-facing Apps list). */
    fun isLaunchable(packageName: String): Boolean = packageName in launchable
}
