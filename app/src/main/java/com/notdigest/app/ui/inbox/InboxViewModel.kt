package com.notdigest.app.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.domain.model.AppNotification
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.model.DigestType
import com.notdigest.app.domain.repository.AppRuleRepository
import com.notdigest.app.domain.repository.DigestRepository
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AppFilterOption(val packageName: String, val appName: String, val count: Int)

/** A transient message for the snackbar. When [undo] is non-null, an Undo action is shown. */
data class InboxMessage(val text: String, val undo: (suspend () -> Unit)? = null)

/** One delivery batch — a scheduled or manual "See All Notifications Now" — shown as a collapsible group. */
data class DeliveryGroup(
    val digestId: Long,
    val createdAt: Long,
    val type: DigestType,
    val isLatest: Boolean,
    val expanded: Boolean,
    val notifications: List<AppNotification>,
) {
    val count: Int get() = notifications.size
    val isManual: Boolean get() = type == DigestType.MANUAL
}

data class InboxUiState(
    val groups: List<DeliveryGroup> = emptyList(),
    val apps: List<AppFilterOption> = emptyList(),
    val availableDates: List<LocalDate> = emptyList(),
    val query: String = "",
    val appFilter: String? = null,
    val selectedDate: LocalDate? = null,
    val selectedIds: Set<Long> = emptySet(),
    val isDelivering: Boolean = false,
    val archivedCount: Int = 0,
    val totalDelivered: Int = 0,
) {
    val selectionMode: Boolean get() = selectedIds.isNotEmpty()
    val isEmpty: Boolean get() = groups.isEmpty()
}

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val appRuleRepository: AppRuleRepository,
    private val digestRepository: DigestRepository,
    private val deliverDigestUseCase: DeliverDigestUseCase,
    private val openNotificationUseCase: OpenNotificationUseCase,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    private val query = MutableStateFlow("")
    private val appFilter = MutableStateFlow<String?>(null)
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val isDelivering = MutableStateFlow(false)
    private val expandOverrides = MutableStateFlow<Map<Long, Boolean>>(emptyMap())

    private val messageChannel = Channel<InboxMessage>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    private val delivered = query.flatMapLatest { q ->
        if (q.isBlank()) notificationRepository.observeDelivered()
        else notificationRepository.searchDelivered(q)
    }

    private data class Core(
        val groups: List<DeliveryGroup>,
        val apps: List<AppFilterOption>,
        val dates: List<LocalDate>,
        val query: String,
        val appFilter: String?,
        val selectedDate: LocalDate?,
        val totalDelivered: Int,
    )

    private val core = combine(
        delivered,
        digestRepository.observeDigests(),
        query,
        appFilter,
        selectedDate,
    ) { list, digests, q, filter, date ->
        val digestById = digests.associateBy { it.id }
        val byApp = if (filter == null) list else list.filter { it.packageName == filter }

        var groups = byApp
            .filter { it.digestId != null }
            .groupBy { it.digestId!! }
            .map { (digestId, notifs) ->
                val digest = digestById[digestId]
                DeliveryGroup(
                    digestId = digestId,
                    createdAt = digest?.createdAt ?: notifs.maxOf { it.postedAt },
                    type = digest?.type ?: DigestType.SCHEDULED,
                    isLatest = false,
                    expanded = false,
                    notifications = notifs.sortedByDescending { it.postedAt },
                )
            }
            .sortedByDescending { it.createdAt }

        if (date != null) groups = groups.filter { localDateOf(it.createdAt) == date }
        groups = groups.mapIndexed { index, g -> g.copy(isLatest = index == 0 && date == null) }

        val apps = list
            .groupBy { it.packageName }
            .map { (pkg, items) -> AppFilterOption(pkg, items.first().appName, items.size) }
            .sortedByDescending { it.count }

        val dates = list
            .mapNotNull { it.digestId }
            .toSet()
            .mapNotNull { digestById[it]?.createdAt }
            .map { localDateOf(it) }
            .distinct()
            .sortedDescending()

        Core(groups, apps, dates, q, filter, date, list.size)
    }

    val uiState = combine(
        core,
        selectedIds,
        isDelivering,
        expandOverrides,
        notificationRepository.observePendingCount(),
    ) { c, sel, delivering, overrides, archived ->
        val groups = c.groups.map { g -> g.copy(expanded = overrides[g.digestId] ?: g.isLatest) }
        val visibleIds = c.groups.flatMap { it.notifications.map { n -> n.id } }.toSet()
        InboxUiState(
            groups = groups,
            apps = c.apps,
            availableDates = c.dates,
            query = c.query,
            appFilter = c.appFilter,
            selectedDate = c.selectedDate,
            selectedIds = sel.intersect(visibleIds),
            isDelivering = delivering,
            archivedCount = archived,
            totalDelivered = c.totalDelivered,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState())

    private fun localDateOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun onQueryChange(value: String) { query.value = value }

    fun setAppFilter(packageName: String?) {
        appFilter.value = if (appFilter.value == packageName) null else packageName
    }

    fun setSelectedDate(date: LocalDate?) { selectedDate.value = date }

    fun setGroupExpanded(digestId: Long, expanded: Boolean) {
        expandOverrides.value = expandOverrides.value + (digestId to expanded)
    }

    fun toggleSelection(id: Long) {
        selectedIds.value = selectedIds.value.toMutableSet().apply { if (!add(id)) remove(id) }
    }

    fun startSelection(id: Long) { selectedIds.value = setOf(id) }

    fun clearSelection() { selectedIds.value = emptySet() }

    fun selectAll() {
        selectedIds.value = uiState.value.groups.flatMap { it.notifications.map { n -> n.id } }.toSet()
    }

    fun open(notification: AppNotification) {
        viewModelScope.launch {
            val result = openNotificationUseCase(notification)
            messageChannel.send(InboxMessage(messageFor(result, notification.appName)))
        }
    }

    fun delete(ids: List<Long>) {
        val removed = uiState.value.groups.flatMap { it.notifications }.filter { it.id in ids }
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
            val result = runCatching { deliverDigestUseCase(DigestType.MANUAL, postNotification = false) }.getOrNull()
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
