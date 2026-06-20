package com.notdigest.app.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.model.InstalledApp
import com.notdigest.app.domain.repository.AppRuleRepository
import com.notdigest.app.domain.repository.InstalledAppsRepository
import com.notdigest.app.domain.usecase.SyncInstalledAppRulesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
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
) : ViewModel() {

    private val installed = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(AppsFilter.ALL)
    private val selected = MutableStateFlow<Set<String>>(emptySet())
    private val loading = MutableStateFlow(true)

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

    val uiState = combine(core, loading) { state, isLoading ->
        state.copy(loading = isLoading && state.apps.isEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())

    init { refresh() }

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
        viewModelScope.launch { appRuleRepository.setMode(item.packageName, item.appName, mode) }
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
            appRuleRepository.setModeForAll(targets, mode)
            clearSelection()
            eventChannel.send("${targets.size} apps set to ${if (mode == DigestMode.DIGEST) "Digest" else "Real-Time"}")
        }
    }
}
