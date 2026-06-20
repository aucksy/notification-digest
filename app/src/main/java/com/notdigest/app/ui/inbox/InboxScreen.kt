package com.notdigest.app.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdigest.app.domain.model.AppNotification
import com.notdigest.app.ui.components.AppIcon
import com.notdigest.app.ui.components.CountPill
import com.notdigest.app.ui.components.EmptyState
import com.notdigest.app.ui.components.NotDigestCard
import com.notdigest.app.ui.components.NotificationListItem
import com.notdigest.app.ui.theme.NotDigestTheme
import com.notdigest.app.ui.theme.Spacing

@Composable
fun InboxScreen(
    contentPadding: PaddingValues,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding()),
        ) {
            if (state.selectionMode) {
                SelectionBar(
                    count = state.selectedIds.size,
                    onClose = viewModel::clearSelection,
                    onSelectAll = viewModel::selectAll,
                    onMarkRead = { viewModel.markRead(state.selectedIds.toList()) },
                    onDelete = { viewModel.delete(state.selectedIds.toList()) },
                )
            } else {
                InboxHeader(
                    deliveredCount = state.totalDelivered,
                    onMarkAllRead = viewModel::markAllRead,
                )
                if (state.waitingCount > 0) {
                    WaitingBanner(
                        waitingCount = state.waitingCount,
                        isDelivering = state.isDelivering,
                        onDeliver = viewModel::deliverNow,
                    )
                }
                SearchField(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                )
                if (state.apps.isNotEmpty()) {
                    AppFilterChips(
                        apps = state.apps,
                        selected = state.appFilter,
                        onSelect = viewModel::setAppFilter,
                    )
                }
            }

            if (state.items.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = when {
                        state.query.isNotBlank() -> "No matches"
                        state.waitingCount > 0 -> "Nothing delivered yet"
                        else -> "Inbox zero"
                    },
                    subtitle = when {
                        state.query.isNotBlank() -> "Try a different search."
                        state.waitingCount > 0 -> "Hit Deliver to reveal your waiting notifications here."
                        else -> "Delivered notifications live here to read any time. Pending ones stay hidden until you deliver."
                    },
                    modifier = Modifier.padding(top = Spacing.xl),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.screen,
                        end = Spacing.screen,
                        top = Spacing.sm,
                        bottom = contentPadding.calculateBottomPadding() + Spacing.xxxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    items(state.items, key = { it.id }) { notification ->
                        SwipeableNotificationRow(
                            notification = notification,
                            selectionMode = state.selectionMode,
                            selected = notification.id in state.selectedIds,
                            onOpen = { viewModel.open(notification) },
                            onDelete = { viewModel.delete(listOf(notification.id)) },
                            onToggleSelect = { viewModel.toggleSelection(notification.id) },
                            onLongPress = { viewModel.startSelection(notification.id) },
                            onMakeRealtime = {
                                viewModel.makeAppRealtime(notification.packageName, notification.appName)
                            },
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding() + Spacing.sm),
        )
    }
}

@Composable
private fun InboxHeader(
    deliveredCount: Int,
    onMarkAllRead: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("Inbox", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            if (deliveredCount > 0) CountPill(text = deliveredCount.toString())
        }
        if (deliveredCount > 0) {
            IconButton(onClick = onMarkAllRead) {
                Icon(Icons.Filled.DoneAll, contentDescription = "Mark all read")
            }
        }
    }
}

/** The only thing the user sees about pending notifications: a count and a way to deliver them. */
@Composable
private fun WaitingBanner(
    waitingCount: Int,
    isDelivering: Boolean,
    onDeliver: () -> Unit,
) {
    NotDigestCard(modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "$waitingCount waiting",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Kept out of sight until you deliver",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onDeliver, enabled = !isDelivering) {
                if (isDelivering) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Bolt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Spacing.sm))
                    Text("Deliver")
                }
            }
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, "Clear selection", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(
                "$count selected",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Row {
            TextButton(onClick = onSelectAll) { Text("All") }
            IconButton(onClick = onMarkRead) {
                Icon(Icons.Filled.DoneAll, "Mark read", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen, vertical = Spacing.xs),
        placeholder = { Text("Search notifications") },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Filled.Close, "Clear") }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun AppFilterChips(
    apps: List<AppFilterOption>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        apps.forEach { app ->
            FilterChip(
                selected = selected == app.packageName,
                onClick = { onSelect(app.packageName) },
                label = { Text("${app.appName} ${app.count}") },
                leadingIcon = { AppIcon(packageName = app.packageName, fallbackLabel = app.appName, size = 20.dp) },
                shape = FilterChipDefaults.shape.let { MaterialTheme.shapes.large },
            )
        }
    }
}

@Composable
private fun SwipeableNotificationRow(
    notification: AppNotification,
    selectionMode: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onToggleSelect: () -> Unit,
    onLongPress: () -> Unit,
    onMakeRealtime: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onOpen(); false }
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )

    val rowBackground = if (selected) MaterialTheme.colorScheme.primaryContainer else NotDigestTheme.brand.surfaceElevated

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !selectionMode,
        enableDismissFromEndToStart = !selectionMode,
        backgroundContent = { SwipeBackground(dismissState.dismissDirection) },
        modifier = Modifier.clip(MaterialTheme.shapes.large),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(rowBackground)
                .combinedClickable(
                    onClick = { if (selectionMode) onToggleSelect() else onOpen() },
                    onLongClick = onLongPress,
                )
                .padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .padding(start = Spacing.xs)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Icon(Icons.Filled.DoneAll, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            NotificationListItem(
                notification = notification,
                onClick = { if (selectionMode) onToggleSelect() else onOpen() },
                modifier = Modifier.weight(1f),
            )
            if (!selectionMode) {
                IconButton(onClick = onMakeRealtime) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = "Make ${notification.appName} Real-Time",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val (color, icon, alignment) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Triple(
            MaterialTheme.colorScheme.primary,
            Icons.AutoMirrored.Filled.OpenInNew,
            Alignment.CenterStart,
        )
        SwipeToDismissBoxValue.EndToStart -> Triple(
            MaterialTheme.colorScheme.error,
            Icons.Filled.Delete,
            Alignment.CenterEnd,
        )
        SwipeToDismissBoxValue.Settled -> Triple(Color.Transparent, Icons.Filled.Delete, Alignment.Center)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.large)
            .background(color)
            .padding(horizontal = Spacing.xl),
        contentAlignment = alignment,
    ) {
        if (direction != SwipeToDismissBoxValue.Settled) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
    }
}
