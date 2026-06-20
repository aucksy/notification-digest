package com.notdigest.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdigest.app.core.util.TimeFormatter
import com.notdigest.app.domain.model.Digest
import com.notdigest.app.domain.model.DigestType
import com.notdigest.app.ui.LocalIs24Hour
import com.notdigest.app.ui.components.CountPill
import com.notdigest.app.ui.components.EmptyState
import com.notdigest.app.ui.components.NotDigestCard
import com.notdigest.app.ui.theme.NotDigestTheme
import com.notdigest.app.ui.theme.Spacing

@Composable
fun HistoryScreen(
    contentPadding: PaddingValues,
    onOpenDigest: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbarHostState.showSnackbar(it) } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
            Text(
                "History",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.sm),
            )
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.xs),
                placeholder = { Text("Search delivered notifications") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) { Icon(Icons.Filled.Close, "Clear") }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )

            if (state.digests.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.History,
                    title = if (state.query.isBlank()) "No digests yet" else "No matches",
                    subtitle = if (state.query.isBlank()) {
                        "Delivered digests are kept here so you can revisit anything, any time."
                    } else {
                        "Try a different search."
                    },
                    modifier = Modifier.padding(top = Spacing.xxxl),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.screen,
                        end = Spacing.screen,
                        top = Spacing.sm,
                        bottom = contentPadding.calculateBottomPadding() + Spacing.xxxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(state.digests, key = { it.id }) { digest ->
                        DigestRow(
                            digest = digest,
                            onClick = { onOpenDigest(digest.id) },
                            onDelete = { viewModel.delete(digest.id) },
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
private fun DigestRow(
    digest: Digest,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val is24Hour = LocalIs24Hour.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        },
    )
    val isManual = digest.type == DigestType.MANUAL

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = Spacing.xl),
                contentAlignment = Alignment.CenterEnd,
            ) { Icon(Icons.Filled.Delete, null, tint = Color.White) }
        },
    ) {
        NotDigestCard {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isManual) Icons.Filled.Bolt else Icons.Filled.Schedule,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "${digest.notificationCount} notifications · ${digest.appCount} apps",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        TimeFormatter.absolute(digest.createdAt, is24Hour),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                CountPill(
                    text = if (isManual) "Manual" else "Scheduled",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
