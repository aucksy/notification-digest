package com.notdigest.app.ui.onboarding

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdigest.app.service.BatteryOptimizationState
import com.notdigest.app.service.NotificationAccessState
import com.notdigest.app.ui.theme.Spacing
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 7

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val selectedPreset by viewModel.selectedPreset.collectAsStateWithLifecycle()

    var accessGranted by remember { mutableStateOf(NotificationAccessState.isGranted(context)) }
    var batteryExempt by remember { mutableStateOf(BatteryOptimizationState.isIgnoring(context)) }
    var pendingOemGuide by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        accessGranted = NotificationAccessState.isGranted(context)
        batteryExempt = BatteryOptimizationState.isIgnoring(context)
        if (pendingOemGuide) {
            pendingOemGuide = false
            BatteryOptimizationState.openAppBatterySettings(context)
        }
    }

    val postNotifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result handled implicitly; collection works regardless */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.safeDrawing.asPaddingValues()),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            when (page) {
                0 -> OnboardingPage(
                    icon = Icons.Filled.SelfImprovement,
                    title = "Welcome to Notification Digest",
                    body = "A calmer phone, without missing what matters. Let's set it up in under a minute.",
                )
                1 -> OnboardingPage(
                    icon = Icons.Filled.NotificationsActive,
                    title = "Notifications never stop",
                    body = "Every buzz pulls your attention away. The interruptions add up to hours of lost focus and quiet anxiety.",
                )
                2 -> OnboardingPage(
                    icon = Icons.Filled.Inbox,
                    title = "We batch them, beautifully",
                    body = "Notifications are collected silently and delivered as clean, organised digests at the times you choose.",
                )
                3 -> GrantAccessPage(
                    granted = accessGranted,
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            postNotifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                )
                4 -> BackgroundReliabilityPage(
                    onAllow = {
                        if (batteryExempt) {
                            BatteryOptimizationState.openAppBatterySettings(context)
                        } else {
                            pendingOemGuide = true
                            BatteryOptimizationState.requestIgnore(context)
                        }
                    },
                )
                5 -> SchedulePresetPage(
                    selected = selectedPreset,
                    onSelect = viewModel::selectPreset,
                )
                6 -> ModesExplainedPage()
            }
        }

        PageIndicator(
            pageCount = PAGE_COUNT,
            currentPage = pagerState.currentPage,
            modifier = Modifier.padding(vertical = Spacing.lg),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { onFinished() },
                enabled = pagerState.currentPage < PAGE_COUNT - 1,
            ) {
                Text(if (pagerState.currentPage < PAGE_COUNT - 1) "Skip" else "")
            }

            if (pagerState.currentPage < PAGE_COUNT - 1) {
                Button(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }) {
                    Text("Next")
                    Spacer(Modifier.size(Spacing.sm))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            } else {
                Button(onClick = { viewModel.finish(onFinished) }) {
                    Text("Start being calm")
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    icon: ImageVector,
    title: String,
    body: String,
    extra: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(108.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(52.dp),
            )
        }
        Spacer(Modifier.height(Spacing.xxl))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (extra != null) {
            Spacer(Modifier.height(Spacing.xxl))
            extra()
        }
    }
}

@Composable
private fun GrantAccessPage(granted: Boolean, onGrant: () -> Unit) {
    OnboardingPage(
        icon = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Bolt,
        title = if (granted) "You're all set" else "Grant notification access",
        body = if (granted) {
            "Notification access is on. We'll start collecting notifications from your Digest apps right away."
        } else {
            "To collect and batch notifications, the app needs Notification Access. Your data never leaves this device."
        },
        extra = {
            if (granted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(Spacing.sm))
                    Text("Access granted", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(onClick = onGrant) { Text("Open settings & grant access") }
            }
        },
    )
}

@Composable
private fun BackgroundReliabilityPage(onAllow: () -> Unit) {
    OnboardingPage(
        icon = Icons.Filled.Bolt,
        title = "Keep it running reliably",
        body = "Phones — especially OnePlus, Oppo and Realme — stop apps in the background to save power, which would let notifications slip through. Allow Notification Digest to keep running so nothing is missed.",
        extra = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = onAllow) { Text("Allow background running") }
                Spacer(Modifier.height(Spacing.md))
                Text(
                    "Then open Battery Settings and choose “Allow background activity” — not Smart mode. This is the key setting for reliable delivery.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
    )
}

@Composable
private fun SchedulePresetPage(selected: SchedulePreset, onSelect: (SchedulePreset) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(Spacing.xl))
        Text("When should digests arrive?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(Spacing.xs))
        Text("You can fine-tune this any time.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.xl))
        SchedulePreset.entries.forEach { preset ->
            SelectableRow(
                title = preset.label,
                subtitle = preset.description,
                selected = selected == preset,
                onClick = { onSelect(preset) },
                modifier = Modifier.padding(vertical = Spacing.xs),
            )
        }
    }
}

@Composable
private fun ModesExplainedPage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Two simple modes", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(Spacing.xl))
        InfoRow(
            icon = Icons.Filled.Inbox,
            title = "Digest",
            body = "Collected quietly and delivered in batches. This is the default for your apps.",
        )
        Spacer(Modifier.height(Spacing.lg))
        InfoRow(
            icon = Icons.Filled.Bolt,
            title = "Real-Time",
            body = "Comes through instantly, as normal. Messages, OTPs and calls stay here by default — you decide the rest.",
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SelectableRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(if (selected) 2.dp else 1.dp, border, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(visible = selected) {
            Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(pageCount) { index ->
            val width by animateDpAsState(if (index == currentPage) 24.dp else 8.dp, label = "dot")
            Box(
                modifier = Modifier
                    .padding(horizontal = Spacing.xxs)
                    .height(8.dp)
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}
