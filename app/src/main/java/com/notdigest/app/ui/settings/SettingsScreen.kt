package com.notdigest.app.ui.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdigest.app.BuildConfig
import com.notdigest.app.core.Constants
import com.notdigest.app.domain.model.ThemeMode
import com.notdigest.app.service.NotificationAccessState
import com.notdigest.app.ui.components.NotDigestCard
import com.notdigest.app.ui.theme.Spacing

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onOpenSchedule: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearConfirm by remember { mutableStateOf(false) }

    var accessGranted by remember { mutableStateOf(NotificationAccessState.isGranted(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        accessGranted = NotificationAccessState.isGranted(context)
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { viewModel.backupToFile(it) } }
    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.restoreFromFile(it) } }

    LaunchedEffect(Unit) { viewModel.events.collect { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(Unit) {
        viewModel.exportData.collect { text ->
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Notification Digest export")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(send, "Export notifications"))
        }
    }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = Spacing.screen,
                    end = Spacing.screen,
                    top = contentPadding.calculateTopPadding() + Spacing.sm,
                    bottom = contentPadding.calculateBottomPadding() + Spacing.xxxl,
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)

            // --- Notification access ---
            if (!accessGranted) {
                NotDigestCard {
                    Text("Notification access is off", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                    Text(
                        "The app can't collect notifications until access is granted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { openListenerSettings(context) }) { Text("Grant access") }
                }
            }

            // --- Appearance ---
            SettingsGroup(title = "Appearance") {
                LabeledControl("Theme") {
                    val options = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        options.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = prefs.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            ) { Text(label) }
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchRow(
                        title = "Dynamic color",
                        subtitle = "Match your wallpaper's palette",
                        checked = prefs.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                    )
                }
            }

            // --- Delivery ---
            SettingsGroup(title = "Delivery") {
                NavRow(
                    icon = { Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.primary) },
                    title = "Schedules",
                    subtitle = "When digests are delivered",
                    onClick = onOpenSchedule,
                )
                SwitchRow(
                    title = "Collection status",
                    subtitle = "Show a quiet ongoing count while collecting (off by default)",
                    checked = prefs.statusNotificationEnabled,
                    onCheckedChange = viewModel::setStatusNotification,
                )
            }

            // --- About sounds ---
            SettingsGroup(title = "Sounds") {
                NotDigestCard {
                    Text(
                        "Why a Digest app can still make one sound",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Android only lets this app tuck a notification away *after* it arrives, so a brand-new Digest notification can make one brief sound before it disappears (repeated sounds from the same app are already prevented).\n\nFor complete silence from a specific noisy app, open its notification settings and switch it to Silent — it'll still be collected and batched here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                NavRow(
                    title = "Open notification settings",
                    subtitle = "Set a noisy app to Silent at the source",
                    onClick = { openNotificationSettings(context) },
                )
            }

            // --- Behaviour ---
            SettingsGroup(title = "Behaviour") {
                SwitchRow(
                    title = "Smart suggestions",
                    subtitle = "Occasional, dismissible tips about noisy apps",
                    checked = prefs.recommendationsEnabled,
                    onCheckedChange = viewModel::setRecommendations,
                )
                SwitchRow(
                    title = "Haptics",
                    subtitle = "Subtle feedback on key actions",
                    checked = prefs.hapticsEnabled,
                    onCheckedChange = viewModel::setHaptics,
                )
            }

            // --- Backup & restore ---
            SettingsGroup(title = "Backup & restore") {
                NotDigestCard {
                    Text("Auto-backup to Google", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Your app classifications, schedules and settings (never notification content) ride along with Android's system backup, so they come back automatically when you reinstall on the same Google account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                NavRow(
                    title = "Back up to a file",
                    subtitle = "Save a copy you can keep or move to another phone",
                    onClick = { createBackupLauncher.launch(viewModel.backupFileName) },
                )
                NavRow(
                    title = "Restore from a file",
                    subtitle = "Import classifications & settings from a backup file",
                    onClick = { openBackupLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                )
            }

            // --- Data & privacy ---
            SettingsGroup(title = "Data & privacy") {
                LabeledControl("Keep notifications for") {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Constants.RETENTION_OPTIONS.forEachIndexed { index, days ->
                            SegmentedButton(
                                selected = prefs.retentionDays == days,
                                onClick = { viewModel.setRetentionDays(days) },
                                shape = SegmentedButtonDefaults.itemShape(index, Constants.RETENTION_OPTIONS.size),
                            ) { Text("${days}d") }
                        }
                    }
                }
                NavRow(title = "Export notifications", subtitle = "Share a plain-text copy", onClick = viewModel::requestExport)
                NavRow(
                    title = "Clear all data",
                    subtitle = "Delete every collected notification & digest",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { showClearConfirm = true },
                )
                NotDigestCard {
                    Text("Private by design", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Everything stays on this device. No account, no cloud, no analytics, no tracking.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                "Notification Digest v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding() + Spacing.sm),
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all data?") },
            text = { Text("This permanently deletes every collected notification and digest on this device. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllData(); showClearConfirm = false }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") } },
        )
    }
}

private fun openListenerSettings(context: android.content.Context) {
    context.startActivity(
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/** Open the per-app notification settings list so the user can set a noisy app to Silent. */
private fun openNotificationSettings(context: android.content.Context) {
    // ACTION_ALL_APPS_NOTIFICATION_SETTINGS (API 31+). Use the raw action string so we don't depend
    // on an SDK constant; fall back to top-level Settings on older versions or if it's unavailable.
    val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        "android.settings.ALL_APPS_NOTIFICATION_SETTINGS"
    } else {
        Settings.ACTION_SETTINGS
    }
    val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Spacing.xs),
        )
        NotDigestCard {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) { content() }
        }
    }
}

@Composable
private fun LabeledControl(label: String, control: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        control()
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        icon?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = titleColor)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
