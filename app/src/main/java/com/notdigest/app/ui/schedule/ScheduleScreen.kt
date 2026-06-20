package com.notdigest.app.ui.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notdigest.app.core.util.TimeFormatter
import com.notdigest.app.domain.model.Schedule
import com.notdigest.app.ui.LocalIs24Hour
import com.notdigest.app.ui.components.EmptyState
import com.notdigest.app.ui.components.NotDigestCard
import com.notdigest.app.ui.components.TimePickerDialog
import com.notdigest.app.ui.theme.Spacing

private sealed interface ScheduleDialog {
    data object Add : ScheduleDialog
    data class Edit(val schedule: Schedule) : ScheduleDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val is24Hour = LocalIs24Hour.current
    var dialog by remember { mutableStateOf<ScheduleDialog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedules") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialog = ScheduleDialog.Add },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add time") },
            )
        },
    ) { padding ->
        if (schedules.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Schedule,
                title = "No delivery times",
                subtitle = "Add one or more times and your digests will arrive right on schedule.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.screen,
                    end = Spacing.screen,
                    top = padding.calculateTopPadding() + Spacing.sm,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                itemsIndexed(schedules, key = { _, it -> it.id }) { index, schedule ->
                    ScheduleRow(
                        schedule = schedule,
                        is24Hour = is24Hour,
                        isFirst = index == 0,
                        isLast = index == schedules.lastIndex,
                        onEdit = { dialog = ScheduleDialog.Edit(schedule) },
                        onToggle = { viewModel.setEnabled(schedule, it) },
                        onDelete = { viewModel.delete(schedule) },
                        onMoveUp = { viewModel.move(index, index - 1) },
                        onMoveDown = { viewModel.move(index, index + 1) },
                    )
                }
            }
        }
    }

    when (val current = dialog) {
        is ScheduleDialog.Add -> TimePickerDialog(
            initialHour = 12,
            initialMinute = 0,
            is24Hour = is24Hour,
            title = "Add delivery time",
            onConfirm = { h, m -> viewModel.addSchedule(h, m); dialog = null },
            onDismiss = { dialog = null },
        )
        is ScheduleDialog.Edit -> TimePickerDialog(
            initialHour = current.schedule.hour,
            initialMinute = current.schedule.minute,
            is24Hour = is24Hour,
            title = "Edit delivery time",
            onConfirm = { h, m -> viewModel.updateTime(current.schedule, h, m); dialog = null },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun ScheduleRow(
    schedule: Schedule,
    is24Hour: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    NotDigestCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.weight(1f).clickable(onClick = onEdit),
            ) {
                Text(
                    TimeFormatter.clockOf(schedule.hour, schedule.minute, is24Hour),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (schedule.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    schedule.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column {
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Filled.KeyboardArrowUp, "Move up", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Move down", modifier = Modifier.size(20.dp))
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
            Switch(checked = schedule.enabled, onCheckedChange = onToggle)
        }
    }
}
