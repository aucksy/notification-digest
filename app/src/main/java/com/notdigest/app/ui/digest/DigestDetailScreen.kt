package com.notdigest.app.ui.digest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdigest.app.core.util.TimeFormatter
import com.notdigest.app.ui.LocalIs24Hour
import com.notdigest.app.ui.components.AppGroupCard
import com.notdigest.app.ui.components.EmptyState
import com.notdigest.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigestDetailScreen(
    digestId: Long,
    onBack: () -> Unit,
    viewModel: DigestDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val is24Hour = LocalIs24Hour.current

    LaunchedEffect(Unit) { viewModel.events.collect { snackbarHostState.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Digest", style = MaterialTheme.typography.titleLarge)
                        state.digest?.let {
                            Text(
                                TimeFormatter.absolute(it.createdAt, is24Hour),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.loaded && state.groups.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Inbox,
                title = "Empty digest",
                subtitle = "This digest no longer has any notifications.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.screen,
                    end = Spacing.screen,
                    top = padding.calculateTopPadding() + Spacing.sm,
                    bottom = padding.calculateBottomPadding() + Spacing.xxxl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                state.digest?.let { digest ->
                    item {
                        Text(
                            "${digest.notificationCount} notifications · ${digest.appCount} apps",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.groups, key = { it.packageName }) { group ->
                    AppGroupCard(
                        group = group,
                        onNotificationClick = viewModel::open,
                        initiallyExpanded = state.groups.size <= 4,
                    )
                }
            }
        }
    }
}
