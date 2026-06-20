package com.notdigest.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdigest.app.core.util.TimeFormatter
import com.notdigest.app.domain.model.NotificationStats
import com.notdigest.app.ui.LocalIs24Hour
import com.notdigest.app.ui.components.AnimatedCount
import com.notdigest.app.ui.components.EmptyState
import com.notdigest.app.ui.components.NotDigestCard
import com.notdigest.app.ui.components.NotificationListItem
import com.notdigest.app.ui.components.SectionHeader
import com.notdigest.app.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onOpenInbox: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenDigest: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val is24Hour = LocalIs24Hour.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.screen,
                end = Spacing.screen,
                top = contentPadding.calculateTopPadding() + Spacing.sm,
                bottom = contentPadding.calculateBottomPadding() + Spacing.xxxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item { Greeting() }

            item {
                HeroCard(
                    waiting = state.stats.waitingCount,
                    nextDigestLabel = state.nextDigestAt?.let {
                        TimeFormatter.whenLabel(it, System.currentTimeMillis(), is24Hour)
                    },
                    isDelivering = state.isDelivering,
                    onReview = onOpenInbox,
                    onDeliver = viewModel::deliverNow,
                )
            }

            item { StatsRow(stats = state.stats, onOpenApps = onOpenApps) }

            if (state.recommendations.isNotEmpty()) {
                item { SectionHeader(title = "Suggestions") }
                items(state.recommendations, key = { it.packageName }) { rec ->
                    RecommendationCard(
                        recommendation = rec,
                        onApply = { viewModel.applyRecommendation(rec) },
                        onDismiss = { viewModel.dismissRecommendation(rec) },
                    )
                }
            }

            // By design, collected notifications are NOT previewed here — the dashboard shows only
            // counts so the user is never tempted to peek before delivering.
            item {
                NotDigestCard {
                    EmptyState(
                        icon = Icons.Filled.DoneAll,
                        title = if (state.stats.waitingCount == 0) "Nothing collecting" else "Collecting quietly",
                        subtitle = if (state.stats.waitingCount == 0) {
                            "When your Digest apps send notifications, they'll wait here out of sight until you deliver them."
                        } else {
                            "${state.stats.waitingCount} waiting, kept out of sight. Tap Deliver now, or wait for your next digest."
                        },
                    )
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
private fun Greeting() {
    val hour = remember {
        Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()).hour
    }
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
    Column(Modifier.padding(top = Spacing.sm)) {
        Text(greeting, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "Your digest",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun HeroCard(
    waiting: Int,
    nextDigestLabel: String?,
    isDelivering: Boolean,
    onReview: () -> Unit,
    onDeliver: () -> Unit,
) {
    com.notdigest.app.ui.components.GradientHeroCard {
        Text(
            text = if (waiting == 0) "You're all caught up" else "$waiting notifications waiting",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = nextDigestLabel?.let { "Next digest $it" } ?: "No schedules yet — add one in Settings",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Button(
                onClick = onReview,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Inbox, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.sm))
                Text("Review", fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onDeliver,
                enabled = !isDelivering,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                modifier = Modifier.weight(1f),
            ) {
                if (isDelivering) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Filled.Bolt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Spacing.sm))
                    Text("Deliver now", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StatsRow(stats: NotificationStats, onOpenApps: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
        StatTile(
            value = stats.batchedToday,
            label = "Batched today",
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = stats.avoidedLast7Days,
            label = "Avoided · 7 days",
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = stats.realtimeAppCount,
            label = "Real-Time apps",
            modifier = Modifier.weight(1f),
            onClick = onOpenApps,
        )
    }
}

@Composable
private fun StatTile(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    NotDigestCard(
        modifier = if (onClick != null) modifier.then(Modifier) else modifier,
        contentPadding = PaddingValues(Spacing.lg),
    ) {
        AnimatedCount(
            target = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Spacing.xxs))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecommendationCard(
    recommendation: com.notdigest.app.domain.model.AppRecommendation,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isMove = recommendation.type == com.notdigest.app.domain.model.RecommendationType.MOVE_TO_REALTIME
    NotDigestCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            com.notdigest.app.ui.components.AppIcon(
                packageName = recommendation.packageName,
                fallbackLabel = recommendation.appName,
                size = 40.dp,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    recommendation.appName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${recommendation.weeklyCount} notifications this week",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(onClick = onApply, modifier = Modifier.weight(1f)) {
                Text(if (isMove) "Make Real-Time" else "Keep in Digest")
            }
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}
