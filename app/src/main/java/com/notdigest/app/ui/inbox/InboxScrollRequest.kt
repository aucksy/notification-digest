package com.notdigest.app.ui.inbox

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A one-shot request to jump the Inbox list back to the top. Set when the app is opened from a digest
 * notification, so a tap on a fresh digest lands on the newest items instead of wherever the user had
 * previously scrolled to. The Inbox is a bottom-bar destination with state restoration (which is what
 * preserves scroll on a normal tab switch), so a shared request — not a nav arg — is the clean way to
 * override that just for the digest-open case. Mirrors [com.notdigest.app.ui.apps.AppsFilterRequest].
 */
@Singleton
class InboxScrollRequest @Inject constructor() {
    private val _scrollToTop = MutableStateFlow(false)
    val scrollToTop: StateFlow<Boolean> = _scrollToTop.asStateFlow()

    fun request() { _scrollToTop.value = true }

    fun consume() { _scrollToTop.value = false }
}
