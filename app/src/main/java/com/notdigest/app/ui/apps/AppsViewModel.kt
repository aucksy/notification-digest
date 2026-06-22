package com.notdigest.app.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.model.InstalledApp
import com.notdigest.app.domain.repository.AppRuleRepository
import com.notdigest.app.domain.repository.InstalledAppsRepository
import com.notdigest.app.domain.usecase.SyncInstalledAppRulesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppsFilter { ALL, DIGEST, REALTIME }

data class AppRowItem(
    val packageName: String,
    val appName: String,
    val mode: DigestMode,
    val isSystem: Boolean,
    // When the user toggles an app out of the current filter, it stays rendered (showing its new mode)
    // and slides away before the rule actually changes — Digest exits right, Real-Time exits left.
    val exiting: Boolean = false,
    val exitToRight: Boolean = false,
)

data class AppsUiState(
    val apps: List<AppRowItem> = emptyList(),
    val recentlyChanged: List<AppRowItem> = emptyList(),
    val query: String = "",
    val filter: AppsFilter = AppsFilter.ALL,
    val selected: Set<String> = emptySet(),
    val loading: Boolean = true,
    val digestCount: Int = 0,
    val realtimeCount: Int = 0,
) {
    val selectionMode: Boolean get() = selected.isNotEmpty()
}

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val installedAppsRepository: InstalledAppsRepository,
    private val appRuleRepository: AppRuleRepository,
    private val syncRules: SyncInstalledAppRulesUseCase,
    private val filterRequest: AppsFilterRequest,
) : ViewModel() {

    private val installed = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(AppsFilter.ALL)
    private val selected = MutableStateFlow<Set<String>>(emptySet())
    private val loading = MutableStateFlow(true)

    /** Apps mid-exit-animation: package -> the change to commit once the slide-out finishes. */
    private data class ExitAnim(val appName: String, val targetMode: DigestMode, val toRight: Boolean)
    private val exiting = MutableStateFlow<Map<String, ExitAnim>>(emptyMap())
    private val exitJobs = mutableMapOf<String, Job>()

    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private val core = combine(
        installed,
        query,
        filter,
        selected,
        appRuleRepository.observeRules(),
    ) { apps, q, f, sel, rules ->
        val modeByPkg = rules.associate { it.packageName to it.mode }
        val merged = apps.map {
            AppRowItem(it.packageName, it.appName, modeByPkg[it.packageName] ?: it.mode, it.isSystemApp)
        }
        val filtered = merged.filter { item ->
            val matchesQuery = q.isBlank() ||
                item.appName.contains(q, ignoreCase = true) ||
                item.packageName.contains(q, ignoreCase = true)
            val matchesFilter = when (f) {
                AppsFilter.ALL -> true
                AppsFilter.DIGEST -> item.mode == DigestMode.DIGEST
                AppsFilter.REALTIME -> item.mode == DigestMode.REALTIME
            }
            matchesQuery && matchesFilter
        }
        val recent = rules
            .filter { it.updatedAt > 0 }
            .sortedByDescending { it.updatedAt }
            .take(6)
            .mapNotNull { rule -> merged.firstOrNull { it.packageName == rule.packageName } }
        AppsUiState(
            apps = filtered,
            recentlyChanged = recent,
            query = q,
            filter = f,
            selected = sel.intersect(filtered.map { it.packageName }.toSet()),
            digestCount = merged.count { it.mode == DigestMode.DIGEST },
            realtimeCount = merged.count { it.mode == DigestMode.REALTIME },
        )
    }

    val uiState = combine(core, loading, exiting) { state, isLoading, exit ->
        // Overlay exiting apps: show their new mode (so the toggle animates) and keep them in the list
        // until the slide-out finishes. They still match the filter here because the rule hasn't changed yet.
        val apps = if (exit.isEmpty()) {
            state.apps
        } else {
            state.apps.map { item ->
                exit[item.packageName]?.let { item.copy(mode = it.targetMode, exiting = true, exitToRight = it.toRight) } ?: item
            }
        }
        state.copy(apps = apps, loading = isLoading && state.apps.isEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())

    init {
        refresh()
        // Honor a pre-filter requested by another screen (e.g. the Home "Real-Time apps" tile).
        viewModelScope.launch {
            filterRequest.pending.collect { requested ->
                if (requested != null) {
                    filter.value = requested
                    filterRequest.consume()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            runCatching { syncRules() }
            installed.value = runCatching { installedAppsRepository.getInstalledApps() }.getOrDefault(emptyList())
            loading.value = false
        }
    }

    fun onQueryChange(value: String) { query.value = value }

    fun setFilter(value: AppsFilter) { filter.value = value }

    fun setMode(item: AppRowItem, mode: DigestMode) {
        if (item.mode == mode) return
        // Any prior in-flight exit for this app is now superseded — drop it (and its armed fallback) so
        // a stale deferred commit can never overwrite this newer action.
        cancelPendingExit(item.packageName)
        val activeFilterMode = filterModeOf(filter.value)
        if (activeFilterMode != null && activeFilterMode != mode) {
            // The app will drop out of the current filter — animate it out first, then commit.
            exiting.value = exiting.value +
                (item.packageName to ExitAnim(item.appName, mode, toRight = item.mode == DigestMode.DIGEST))
            // Fallback: if the slide-out never reports back (e.g. the user leaves the screen mid-swipe),
            // still commit shortly after so the change is never lost.
            exitJobs[item.packageName] = viewModelScope.launch {
                delay(EXIT_FALLBACK_MS)
                commitExit(item.packageName)
            }
        } else {
            viewModelScope.launch { appRuleRepository.setMode(item.packageName, item.appName, mode) }
        }
    }

    /** Called when an app's slide-out animation finishes: apply the deferred mode change. */
    fun onExitFinished(packageName: String) = commitExit(packageName)

    private fun commitExit(packageName: String) {
        val anim = exiting.value[packageName] ?: return
        exitJobs.remove(packageName)?.cancel()
        exiting.value = exiting.value - packageName
        viewModelScope.launch { appRuleRepository.setMode(packageName, anim.appName, anim.targetMode) }
    }

    /** Forget a deferred exit without committing it (superseded by a newer authoritative change). */
    private fun cancelPendingExit(packageName: String) {
        exitJobs.remove(packageName)?.cancel()
        if (exiting.value.containsKey(packageName)) exiting.value = exiting.value - packageName
    }

    private fun filterModeOf(f: AppsFilter): DigestMode? = when (f) {
        AppsFilter.DIGEST -> DigestMode.DIGEST
        AppsFilter.REALTIME -> DigestMode.REALTIME
        AppsFilter.ALL -> null
    }

    fun toggleSelection(packageName: String) {
        selected.value = selected.value.toMutableSet().apply { if (!add(packageName)) remove(packageName) }
    }

    fun startSelection(packageName: String) { selected.value = setOf(packageName) }

    fun clearSelection() { selected.value = emptySet() }

    fun selectAllVisible() { selected.value = uiState.value.apps.map { it.packageName }.toSet() }

    fun bulkSetMode(mode: DigestMode) {
        viewModelScope.launch {
            val byPkg = uiState.value.apps.associateBy { it.packageName }
            val targets = selected.value.mapNotNull { byPkg[it]?.let { app -> app.packageName to app.appName } }
            // This bulk write is authoritative — drop any deferred per-app exits it would otherwise race.
            targets.forEach { cancelPendingExit(it.first) }
            appRuleRepository.setModeForAll(targets, mode)
            clearSelection()
            eventChannel.send("${targets.size} apps set to ${if (mode == DigestMode.DIGEST) "Digest" else "Real-Time"}")
        }
    }

    private companion object {
        // Slightly longer than the row's slide-out so the animation normally drives the commit.
        const val EXIT_FALLBACK_MS = 450L
    }
}
