package com.notdigest.app.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.domain.model.AppNotification
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.model.DigestType
import com.notdigest.app.domain.repository.AppRuleRepository
import com.notdigest.app.domain.repository.NotificationRepository
import com.notdigest.app.domain.system.LaunchResult
import com.notdigest.app.domain.usecase.DeliverDigestUseCase
import com.notdigest.app.domain.usecase.DeliverResult
import com.notdigest.app.domain.usecase.OpenNotificationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppFilterOption(val packageName: String, val appName: String, val count: Int)

/** A transient message for the snackbar. When [undo] is non-null, an Undo action is shown (~2s). */
data class InboxMessage(val text: String, val undo: (suspend () -> Unit)? = null)

/**
 * Inbox state. Shows **delivered** notifications as one flat list (the single place notifications
 * live, for the retention duration). Collected-but-unseen ones stay hidden behind an "archived"
 * count + See Now.
 */
data class InboxUiState(
    val items: List<AppNotification> = emptyList(),
    val apps: List<AppFilterOption> = emptyList(),
    val query: String = "",
    val appFilter: String? = null,
    val selectedIds: Set<Long> = emptySet(),
    val isDelivering: Boolean = false,
    val archivedCount: Int = 0,
    val totalDelivered: Int = 0,
) {
    val selectionMode: Boolean get() = selectedIds.isNotEmpty()
}

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val appRuleRepository: AppRuleRepository,
    private val deliverDigestUseCase: DeliverDigestUseCase,
    private val openNotificationUseCase: OpenNotificationUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val appFilter = MutableStateFlow<String?>(null)
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val isDelivering = MutableStateFlow(false)

    private val messageChannel = Channel<InboxMessage>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    private val delivered = query.flatMapLatest { q ->
        if (q.isBlank()) notificationRepository.observeDelivered()
        else notificationRepository.searchDelivered(q)
    }

    private val base = combine(delivered, query, appFilter, selectedIds, isDelivering) { list, q, filter, selected, delivering ->
        val apps = list
            .groupBy { it.packageName }
            .map { (pkg, items) -> AppFilterOption(pkg, items.first().appName, items.size) }
            .sortedByDescending { it.count }
        val visible = if (filter == null) list else list.filter { it.packageName == filter }
        val prunedSelection = selected.intersect(visible.map { it.id }.toSet())
        InboxUiState(
            items = visible,
            apps = apps,
            query = q,
            appFilter = filter,
            selectedIds = prunedSelection,
            isDelivering = delivering,
            totalDelivered = list.size,
        )
    }

    val uiState = combine(base, notificationRepository.observePendingCount()) { state, archived ->
        state.copy(archivedCount = archived)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState())

    fun onQueryChange(value: String) { query.value = value }

    fun setAppFilter(packageName: String?) {
        appFilter.value = if (appFilter.value == packageName) null else packageName
    }

    fun toggleSelection(id: Long) {
        selectedIds.value = selectedIds.value.toMutableSet().apply { if (!add(id)) remove(id) }
    }

    fun startSelection(id: Long) { selectedIds.value = setOf(id) }

    fun clearSelection() { selectedIds.value = emptySet() }

    fun selectAll() { selectedIds.value = uiState.value.items.map { it.id }.toSet() }

    fun open(notification: AppNotification) {
        viewModelScope.launch {
            val result = openNotificationUseCase(notification)
            messageChannel.send(InboxMessage(messageFor(result, notification.appName)))
        }
    }

    /** Swipe-left: delete with an Undo that re-inserts the removed notifications. */
    fun delete(ids: List<Long>) {
        val removed = uiState.value.items.filter { it.id in ids }
        viewModelScope.launch {
            notificationRepository.delete(ids)
            selectedIds.value = selectedIds.value - ids.toSet()
            messageChannel.send(
                InboxMessage(
                    text = if (ids.size == 1) "Notification deleted" else "${ids.size} deleted",
                    undo = { removed.forEach { notificationRepository.insert(it) } },
                ),
            )
        }
    }

    fun markRead(ids: List<Long>) {
        viewModelScope.launch {
            notificationRepository.markRead(ids)
            clearSelection()
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            notificationRepository.markAllDeliveredRead()
            messageChannel.send(InboxMessage("Marked all as read"))
        }
    }

    /** Swipe-right: move the app out of Digest into Real-Time, with an Undo that restores it. */
    fun makeAppRealtime(packageName: String, appName: String) {
        viewModelScope.launch {
            val previous = appRuleRepository.getMode(packageName)
            appRuleRepository.setMode(packageName, appName, DigestMode.REALTIME)
            if (appFilter.value == packageName) appFilter.value = null
            messageChannel.send(
                InboxMessage(
                    text = "$appName is now Real-Time",
                    undo = { appRuleRepository.setMode(packageName, appName, previous) },
                ),
            )
        }
    }

    fun seeNow() {
        viewModelScope.launch {
            isDelivering.value = true
            val result = runCatching { deliverDigestUseCase(DigestType.MANUAL) }.getOrNull()
            isDelivering.value = false
            messageChannel.send(
                InboxMessage(
                    when (result) {
                        is DeliverResult.Delivered -> "${result.notificationCount} notifications added to your inbox"
                        else -> "Nothing to show yet"
                    },
                ),
            )
        }
    }

    private fun messageFor(result: LaunchResult, appName: String): String = when (result) {
        LaunchResult.DEEP_LINKED -> "Opening in $appName…"
        LaunchResult.OPENED_APP -> "Opened $appName"
        LaunchResult.FAILED -> "Couldn't open this notification"
    }
}
