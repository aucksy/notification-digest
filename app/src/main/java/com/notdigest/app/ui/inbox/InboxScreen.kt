package com.notdigest.app.ui.inbox

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdigest.app.domain.model.AppNotification
import com.notdigest.app.ui.LocalHapticsEnabled
import com.notdigest.app.ui.components.AppIcon
import com.notdigest.app.ui.components.CountPill
import com.notdigest.app.ui.components.EmptyState
import com.notdigest.app.ui.components.NotDigestCard
import com.notdigest.app.ui.components.NotificationListItem
import com.notdigest.app.ui.theme.NotDigestTheme
import com.notdigest.app.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InboxScreen(
    contentPadding: PaddingValues,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val hapticsOn = LocalHapticsEnabled.current
    val buzz = { if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            val undo = msg.undo
            if (undo != null) {
                val autoDismiss = scope.launch {
                    delay(3_000)
                    snackbarHostState.currentSnackbarData?.dismiss()
                }
                val result = snackbarHostState.showSnackbar(
                    message = msg.text,
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Indefinite,
                )
                autoDismiss.cancel()
                if (result == SnackbarResult.ActionPerformed) scope.launch { undo() }
            } else {
                snackbarHostState.showSnackbar(msg.text)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
            if (state.selectionMode) {
                SelectionBar(
                    count = state.selectedIds.size,
                    onClose = viewModel::clearSelection,
                    onSelectAll = viewModel::selectAll,
                    onMarkRead = { viewModel.markRead(state.selectedIds.toList()) },
                    onDelete = { buzz(); viewModel.delete(state.selectedIds.toList()) },
                )
            } else {
                InboxHeader(deliveredCount = state.totalDelivered, onMarkAllRead = viewModel::markAllRead)
                if (state.archivedCount > 0) {
                    ArchivedBanner(
                        archivedCount = state.archivedCount,
                        isDelivering = state.isDelivering,
                        onSeeNow = { buzz(); viewModel.seeNow() },
                    )
                }
                SearchField(query = state.query, onQueryChange = viewModel::onQueryChange)
            }

            if (state.isEmpty) {
                if (!state.selectionMode) {
                    InboxFilters(
                        apps = state.apps,
                        selectedApp = state.appFilter,
                        onSelectApp = viewModel::setAppFilter,
                        horizontalPadding = Spacing.screen,
                    )
                }
                EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = when {
                        state.query.isNotBlank() -> "No matches"
                        state.archivedCount > 0 -> "Nothing here yet"
                        else -> "Inbox zero"
                    },
                    subtitle = when {
                        state.query.isNotBlank() -> "Try a different search."
                        state.archivedCount > 0 -> "Tap See All Notifications Now to bring your archived ones here."
                        else -> "Delivered notifications live here, grouped by day. Archived ones stay hidden until you choose to see them."
                    },
                    modifier = Modifier.padding(top = Spacing.xl),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.screen,
                        end = Spacing.screen,
                        top = Spacing.xs,
                        bottom = contentPadding.calculateBottomPadding() + Spacing.xxxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    if (!state.selectionMode) {
                        item(key = "filters") {
                            InboxFilters(
                                apps = state.apps,
                                selectedApp = state.appFilter,
                                onSelectApp = viewModel::setAppFilter,
                                horizontalPadding = 0.dp,
                            )
                        }
                    }
                    state.groups.forEach { group ->
                        stickyHeader(key = "header-${group.key}") {
                            GroupHeader(
                                group = group,
                                onToggle = { viewModel.setGroupExpanded(group.key, !group.expanded) },
                            )
                        }
                        if (group.expanded) {
                            items(group.notifications, key = { it.id }) { notification ->
                                SwipeableNotificationRow(
                                    notification = notification,
                                    selectionMode = state.selectionMode,
                                    selected = notification.id in state.selectedIds,
                                    onOpen = { viewModel.open(notification) },
                                    onDelete = { buzz(); viewModel.delete(listOf(notification.id)) },
                                    onMakeRealtime = { buzz(); viewModel.makeAppRealtime(notification.packageName, notification.appName) },
                                    onToggleSelect = { viewModel.toggleSelection(notification.id) },
                                    onLongPress = { buzz(); viewModel.startSelection(notification.id) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Undo snackbar: larger action, lifted a bit higher above the nav bar.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding() + 56.dp),
        ) { data ->
            Snackbar(
                modifier = Modifier.padding(horizontal = Spacing.md),
                action = {
                    data.visuals.actionLabel?.let { label ->
                        TextButton(onClick = { data.performAction() }) {
                            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
            ) {
                Text(data.visuals.message, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun InboxHeader(deliveredCount: Int, onMarkAllRead: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.sm),
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

/** Slim, theme-tonal row (not a big card) so archived notifications can be delivered without dominating the screen. */
@Composable
private fun ArchivedBanner(archivedCount: Int, isDelivering: Boolean, onSeeNow: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen, vertical = Spacing.xs)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(enabled = !isDelivering, onClick = onSeeNow)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (isDelivering) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            Icon(Icons.Filled.Visibility, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
        }
        Text(
            "$archivedCount archived — tap to see all now",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun GroupHeader(group: DaySection, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (group.expanded) 180f else 0f, label = "chevron")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onToggle)
            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            group.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        CountPill(text = group.count.toString())
        Icon(
            Icons.Filled.ExpandMore,
            contentDescription = if (group.expanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp).rotate(rotation),
        )
    }
}

/**
 * App filter chips, rendered inside the list so they scroll away. (Date filtering was removed — the
 * collapsible Today / Yesterday / older day sections already organise the inbox by day, and having
 * date *chips* on top of date *sections* was confusing.)
 */
@Composable
private fun InboxFilters(
    apps: List<AppFilterOption>,
    selectedApp: String?,
    onSelectApp: (String?) -> Unit,
    horizontalPadding: Dp,
) {
    if (apps.isNotEmpty()) {
        AppFilterChips(apps = apps, selected = selectedApp, onSelect = onSelectApp, horizontalPadding = horizontalPadding)
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
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, "Clear selection", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text("$count selected", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.xs),
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
    horizontalPadding: Dp = Spacing.screen,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = horizontalPadding, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        apps.forEach { app ->
            FilterChip(
                selected = selected == app.packageName,
                onClick = { onSelect(app.packageName) },
                label = { Text("${app.appName} ${app.count}") },
                leadingIcon = { AppIcon(packageName = app.packageName, fallbackLabel = app.appName, size = 20.dp) },
                shape = MaterialTheme.shapes.large,
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
    onMakeRealtime: () -> Unit,
    onToggleSelect: () -> Unit,
    onLongPress: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onMakeRealtime(); false }
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
                    if (selected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            NotificationListItem(
                notification = notification,
                // No inner onClick — the row's combinedClickable handles tap AND long-press-to-select.
                onClick = null,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> SwipeReveal(
            color = NotDigestTheme.brand.positive,
            icon = Icons.Filled.Bolt,
            label = "Real-Time",
            alignment = Alignment.CenterStart,
            iconFirst = true,
        )
        SwipeToDismissBoxValue.EndToStart -> SwipeReveal(
            color = MaterialTheme.colorScheme.error,
            icon = Icons.Filled.Delete,
            label = "Delete",
            alignment = Alignment.CenterEnd,
            iconFirst = false,
        )
        SwipeToDismissBoxValue.Settled -> Box(Modifier.fillMaxSize())
    }
}

@Composable
private fun SwipeReveal(color: Color, icon: ImageVector, label: String, alignment: Alignment, iconFirst: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.large).background(color).padding(horizontal = Spacing.xl),
        contentAlignment = alignment,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (iconFirst) Icon(icon, null, tint = Color.White)
            Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
            if (!iconFirst) Icon(icon, null, tint = Color.White)
        }
    }
}
