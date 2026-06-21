package com.notdigest.app.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.ui.components.AppIcon
import com.notdigest.app.ui.components.EmptyState
import com.notdigest.app.ui.components.ModeToggle
import com.notdigest.app.ui.theme.NotDigestTheme
import com.notdigest.app.ui.theme.Spacing

@Composable
fun AppsScreen(
    contentPadding: PaddingValues,
    viewModel: AppsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbarHostState.showSnackbar(it) } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
            if (state.selectionMode) {
                BulkBar(
                    count = state.selected.size,
                    onClose = viewModel::clearSelection,
                    onSelectAll = viewModel::selectAllVisible,
                    onDigest = { viewModel.bulkSetMode(DigestMode.DIGEST) },
                    onRealtime = { viewModel.bulkSetMode(DigestMode.REALTIME) },
                )
            } else {
                Column(Modifier.padding(horizontal = Spacing.screen)) {
                    Text(
                        "Apps",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = Spacing.sm),
                    )
                    Text(
                        "${state.digestCount} in Digest · ${state.realtimeCount} Real-Time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.sm),
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) { Icon(Icons.Filled.Close, "Clear") }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    // Declare it as a plain search box so the IME treats it consistently (matches
                    // the Inbox search) instead of guessing at autofill/suggestion toolbars.
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search,
                    ),
                )
                FilterRow(selected = state.filter, onSelect = viewModel::setFilter)
                if (state.recentlyChanged.isNotEmpty() && state.filter == AppsFilter.ALL && state.query.isBlank()) {
                    RecentlyChanged(
                        items = state.recentlyChanged,
                        onTap = { viewModel.onQueryChange(it.appName) },
                    )
                }
            }

            when {
                state.loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.apps.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Filled.Apps,
                        title = "No apps found",
                        subtitle = "Try a different search or filter.",
                        modifier = Modifier.padding(top = Spacing.xxxl),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Spacing.screen,
                            end = Spacing.screen,
                            top = Spacing.sm,
                            bottom = contentPadding.calculateBottomPadding() + Spacing.xxxl,
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        items(state.apps, key = { it.packageName }) { app ->
                            AppRow(
                                item = app,
                                selectionMode = state.selectionMode,
                                selected = app.packageName in state.selected,
                                onModeChange = { viewModel.setMode(app, it) },
                                onToggleSelect = { viewModel.toggleSelection(app.packageName) },
                                onLongPress = { viewModel.startSelection(app.packageName) },
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding() + Spacing.sm),
        )
    }
}

@Composable
private fun FilterRow(selected: AppsFilter, onSelect: (AppsFilter) -> Unit) {
    val options = listOf(AppsFilter.ALL to "All", AppsFilter.DIGEST to "Digest", AppsFilter.REALTIME to "Real-Time")
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.xs),
    ) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(label) }
        }
    }
}

@Composable
private fun RecentlyChanged(items: List<AppRowItem>, onTap: (AppRowItem) -> Unit) {
    Column {
        Text(
            "Recently changed",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.xs),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items.forEach { app ->
                Row(
                    Modifier.clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onTap(app) }
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    AppIcon(packageName = app.packageName, fallbackLabel = app.appName, size = 22.dp)
                    Text(app.appName, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun BulkBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDigest: () -> Unit,
    onRealtime: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, "Clear", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text("$count selected", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            TextButton(onClick = onSelectAll) { Text("Select all") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.padding(start = Spacing.sm, top = Spacing.xs)) {
            TextButton(onClick = onDigest) { Text("Move to Digest") }
            TextButton(onClick = onRealtime) { Text("Make Real-Time") }
        }
    }
}

@Composable
private fun AppRow(
    item: AppRowItem,
    selectionMode: Boolean,
    selected: Boolean,
    onModeChange: (DigestMode) -> Unit,
    onToggleSelect: () -> Unit,
    onLongPress: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else NotDigestTheme.brand.surfaceElevated)
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelect() },
                onLongClick = onLongPress,
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (selectionMode) {
            Box(
                Modifier.size(24.dp).clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        AppIcon(packageName = item.packageName, fallbackLabel = item.appName, size = 40.dp)
        Text(
            item.appName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (!selectionMode) {
            ModeToggle(mode = item.mode, onChange = onModeChange)
        }
    }
}
