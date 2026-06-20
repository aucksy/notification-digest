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

/**
 * Inbox state. By design it shows **delivered** notifications only — pending ones stay hidden behind
 * a count + Deliver banner, so the user never peeks before delivering.
 */
data class InboxUiState(
    val items: List<AppNotification> = emptyList(),
    val apps: List<AppFilterOption> = emptyList(),
    val query: String = "",
    val appFilter: String? = null,
    val selectedIds: Set<Long> = emptySet(),
    val isDelivering: Boolean = false,
    val waitingCount: Int = 0,
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

    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

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

    val uiState = combine(base, notificationRepository.observePendingCount()) { state, waiting ->
        state.copy(waitingCount = waiting)
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
            eventChannel.send(messageFor(result, notification.appName))
        }
    }

    fun delete(ids: List<Long>) {
        viewModelScope.launch {
            notificationRepository.delete(ids)
            selectedIds.value = selectedIds.value - ids.toSet()
            eventChannel.send(if (ids.size == 1) "Notification deleted" else "${ids.size} deleted")
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
            eventChannel.send("Marked all as read")
        }
    }

    fun makeAppRealtime(packageName: String, appName: String) {
        viewModelScope.launch {
            appRuleRepository.setMode(packageName, appName, DigestMode.REALTIME)
            if (appFilter.value == packageName) appFilter.value = null
            eventChannel.send("$appName is now Real-Time")
        }
    }

    fun deliverNow() {
        viewModelScope.launch {
            isDelivering.value = true
            val result = runCatching { deliverDigestUseCase(DigestType.MANUAL) }.getOrNull()
            isDelivering.value = false
            eventChannel.send(
                when (result) {
                    is DeliverResult.Delivered -> "Delivered ${result.notificationCount} notifications"
                    else -> "Nothing waiting to deliver"
                },
            )
        }
    }

    private fun messageFor(result: LaunchResult, appName: String): String = when (result) {
        LaunchResult.DEEP_LINKED -> "Opening in $appName…"
        LaunchResult.OPENED_APP -> "Opened $appName"
        LaunchResult.FAILED -> "Couldn't open this notification"
    }
}
