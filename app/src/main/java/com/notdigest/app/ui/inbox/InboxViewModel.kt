package com.notdigest.app.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.core.util.TimeFormatter
import com.notdigest.app.core.util.localDayFlow
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AppFilterOption(val packageName: String, val appName: String, val count: Int)

/** A transient message for the snackbar. When [undo] is non-null, an Undo action is shown. */
data class InboxMessage(val text: String, val undo: (suspend () -> Unit)? = null)

/** One day's worth of delivered notifications, shown as a collapsible section ("Today", "Yesterday", …). */
data class DaySection(
    val date: LocalDate,
    val label: String,
    val defaultExpanded: Boolean,
    val expanded: Boolean,
    val notifications: List<AppNotification>,
) {
    val count: Int get() = notifications.size
    /** Stable key for expand/collapse overrides. */
    val key: Long get() = date.toEpochDay()
}

data class InboxUiState(
    val groups: List<DaySection> = emptyList(),
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
        val sections: List<DaySection>,
        val apps: List<AppFilterOption>,
        val dates: List<LocalDate>,
        val query: String,
        val appFilter: String?,
        val selectedDate: LocalDate?,
        val totalDelivered: Int,
    )

    private val core = combine(
        delivered,
        query,
        appFilter,
        selectedDate,
        // Re-emits at each local midnight so "Today"/"Yesterday" labels and default-expansion roll over
        // even if the inbox stays open across midnight.
        localDayFlow(zone),
    ) { list, q, filter, date, today ->
        val yesterday = today.minusDays(1)

        // Collapse exact duplicates an app re-fired within a delivery (same app + title + text).
        val deduped = list.distinctBy { listOf(it.digestId, it.packageName, it.title, it.text) }
        val byApp = if (filter == null) deduped else deduped.filter { it.packageName == filter }

        // Group by each notification's OWN day, so "Yesterday" means yesterday's notifications —
        // not "whatever digest happened to run yesterday".
        var sections = byApp
            .groupBy { localDateOf(it.postedAt) }
            .map { (day, notifs) ->
                DaySection(
                    date = day,
                    label = TimeFormatter.dateChip(day, today),
                    // Today and Yesterday open by default; anything older starts collapsed.
                    defaultExpanded = day == today || day == yesterday,
                    expanded = false,
                    notifications = notifs.sortedByDescending { it.postedAt },
                )
            }
            .sortedByDescending { it.date }

        if (date != null) sections = sections.filter { it.date == date }

        val apps = deduped
            .groupBy { it.packageName }
            .map { (pkg, items) -> AppFilterOption(pkg, items.first().appName, items.size) }
            .sortedByDescending { it.count }

        val dates = deduped.map { localDateOf(it.postedAt) }.distinct().sortedDescending()

        Core(sections, apps, dates, q, filter, date, deduped.size)
    }

    val uiState = combine(
        core,
        selectedIds,
        isDelivering,
        expandOverrides,
        notificationRepository.observePendingCount(),
    ) { c, sel, delivering, overrides, archived ->
        val sections = c.sections.map { s -> s.copy(expanded = overrides[s.key] ?: s.defaultExpanded) }
        val visibleIds = c.sections.flatMap { it.notifications.map { n -> n.id } }.toSet()
        InboxUiState(
            groups = sections,
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

    fun setGroupExpanded(key: Long, expanded: Boolean) {
        expandOverrides.value = expandOverrides.value + (key to expanded)
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
        LaunchResult.OPENED_APP -> "Opened $appName — couldn't open the exact screen"
        LaunchResult.OPENED_SETTINGS -> "$appName has no screen to open — showing its app info"
        LaunchResult.FAILED -> "Couldn't open this notification"
    }
}
