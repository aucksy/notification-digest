package com.notdigest.app.service

import com.notdigest.app.di.ApplicationScope
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.repository.AppRuleRepository
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
    @ApplicationScope scope: CoroutineScope,
) {
    @Volatile
    private var modes: Map<String, DigestMode> = emptyMap()

    init {
        appRuleRepository.observeRules()
            .onEach { rules -> modes = rules.associate { it.packageName to it.mode } }
            .launchIn(scope)
    }

    /** The cached mode for [packageName], or null if not yet known (cold start / brand-new app). */
    fun cachedMode(packageName: String): DigestMode? = modes[packageName]
}
