package com.notdigest.app.ui.apps

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A tiny one-shot channel so other screens (e.g. the Home "Real-Time apps" stat) can open the Apps
 * tab pre-filtered. The Apps tab is a bottom-bar destination, so threading a nav argument through it
 * would fight the single-top/state-restoration semantics; a shared request is simpler and reliable.
 */
@Singleton
class AppsFilterRequest @Inject constructor() {
    private val _pending = MutableStateFlow<AppsFilter?>(null)
    val pending: StateFlow<AppsFilter?> = _pending.asStateFlow()

    fun request(filter: AppsFilter) { _pending.value = filter }

    fun consume() { _pending.value = null }
}
