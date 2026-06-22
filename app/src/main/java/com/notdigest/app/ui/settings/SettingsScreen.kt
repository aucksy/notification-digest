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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdigest.app.BuildConfig
import com.notdigest.app.core.Constants
import com.notdigest.app.core.util.TimeFormatter
import com.notdigest.app.domain.model.ThemeMode
import com.notdigest.app.service.BatteryOptimizationState
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

    val backgroundDone by viewModel.backgroundSetupDone.collectAsStateWithLifecycle()
    var accessGranted by remember { mutableStateOf(NotificationAccessState.isGranted(context)) }
    var batteryExempt by remember { mutableStateOf(BatteryOptimizationState.isIgnoring(context)) }
    var batteryRestricted by remember { mutableStateOf(BatteryOptimizationState.isBackgroundRestricted(context)) }
    // After the standard battery dialog is dismissed, take the user straight to their phone's own
    // (OEM) background-control screen — the second setting only they can change.
    var pendingOemGuide by remember { mutableStateOf(false) }
    // Set when we send the user to the OEM battery screen; on their return we mark the setup "done"
    // (self-attested — the OEM "Unrestricted" choice isn't readable by any API).
    var awaitingBackgroundReturn by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        accessGranted = NotificationAccessState.isGranted(context)
        batteryExempt = BatteryOptimizationState.isIgnoring(context)
        batteryRestricted = BatteryOptimizationState.isBackgroundRestricted(context)
        if (pendingOemGuide) {
            pendingOemGuide = false
            BatteryOptimizationState.openAppBatterySettings(context)
            awaitingBackgroundReturn = true
        } else if (awaitingBackgroundReturn) {
            awaitingBackgroundReturn = false
            viewModel.setBackgroundSetupDone(true)
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { viewModel.backupToFile(it) } }
    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.restoreFromFile(it) } }

    val driveState by viewModel.driveState.collectAsStateWithLifecycle()
    val driveSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result -> viewModel.onDriveSignInResult(result.data) }

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

            // --- Keep running reliably (battery / background) ---
            // "Done" once the user has been through the setup — unless the phone reports the app is
            // outright "Restricted", which we CAN read and which always means it's misconfigured.
            SettingsGroup(title = "Keep running reliably") {
                if (backgroundDone && !batteryRestricted) {
                    NotDigestCard {
                        Text(
                            "✓ Background running is set up",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "Notifications should arrive reliably. If some still slip through, re-check your phone's battery setting for this app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = {
                            awaitingBackgroundReturn = true
                            BatteryOptimizationState.openAppBatterySettings(context)
                        }) { Text("Re-check battery setting") }
                    }
                } else {
                    NotDigestCard {
                        Text(
                            "To save power, your phone can stop this app in the background — then it misses notifications. Let it always run in the background:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            "1.  Tap “Allow background running” below.\n2.  Open Battery and choose “Unrestricted” — some phones say “Allow background activity”.\n3.  Avoid “Optimised” or “Smart”; they can still stop the app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (batteryRestricted) {
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                "⚠ This app is “Restricted” in the background — that keeps stopping it. Switch it to “Unrestricted”.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    NavRow(
                        title = "Allow background running",
                        subtitle = "Set battery to “Unrestricted”",
                        onClick = {
                            if (batteryExempt) {
                                awaitingBackgroundReturn = true
                                BatteryOptimizationState.openAppBatterySettings(context)
                            } else {
                                pendingOemGuide = true
                                BatteryOptimizationState.requestIgnore(context)
                            }
                        },
                    )
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
                        "Why an app set to Digest may still beep once",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Android plays a notification's sound the moment it arrives — before any app can step in — then we hide it. So the first one may beep; repeats from the same app are silenced.\n\nWant a noisy app completely silent? Set it to Silent in its own settings. It'll still be collected here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                NavRow(
                    title = "Silence an app at the source",
                    subtitle = "Open its notification settings and choose Silent",
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

            // --- Google Drive ---
            if (driveState.configured) {
                DriveSection(
                    state = driveState,
                    onConnect = { driveSignInLauncher.launch(viewModel.driveSignInIntent()) },
                    onBackup = viewModel::driveBackupNow,
                    onRestore = viewModel::driveRestore,
                    onSignOut = viewModel::driveSignOut,
                    onToggleAuto = viewModel::setDriveAutoBackup,
                )
            }

            // --- Backup to a file ---
            SettingsGroup(title = "Backup to a file") {
                NavRow(
                    title = "Back up to a file",
                    subtitle = "Save a copy anywhere — including Google Drive — from the file picker",
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
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
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
private fun DriveSection(
    state: DriveUiState,
    onConnect: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onSignOut: () -> Unit,
    onToggleAuto: (Boolean) -> Unit,
) {
    val busy = state.busy != DriveBusy.NONE
    SettingsGroup(title = "Google Drive") {
        if (!state.connected) {
            Text(
                "Sign in once and Notification Digest keeps a single backup file on your Drive — your app classifications, schedules and settings, synced automatically. It can only ever see that one file, nothing else in your Drive.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.md))
            Button(onClick = onConnect, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                if (state.busy == DriveBusy.SIGN_IN) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.Cloud, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Spacing.sm))
                    Text("Connect Google Drive")
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Cloud, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(
                        state.email ?: "Signed in",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (state.lastBackupAt > 0L) {
                            "Last backup · ${TimeFormatter.relative(state.lastBackupAt, System.currentTimeMillis())}"
                        } else {
                            "Not backed up yet"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onSignOut, enabled = !busy) { Text("Sign out") }
            }
            Spacer(Modifier.height(Spacing.md))
            Button(onClick = onBackup, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                if (state.busy == DriveBusy.BACKUP) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.CloudUpload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Spacing.sm))
                    Text("Back up now")
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(onClick = onRestore, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                if (state.busy == DriveBusy.RESTORE) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.CloudDownload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Spacing.sm))
                    Text("Restore from Drive")
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Auto-back up", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Save automatically a few seconds after you change classifications, schedules or settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.autoBackup, onCheckedChange = onToggleAuto, enabled = !busy)
            }
        }
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
