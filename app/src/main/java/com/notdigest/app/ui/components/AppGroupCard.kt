package com.notdigest.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notdigest.app.domain.model.AppGroup
import com.notdigest.app.domain.model.AppNotification
import com.notdigest.app.ui.theme.Spacing

/**
 * An expandable, animated card grouping one app's notifications — the building block of the
 * inbox, digest and history screens. Collapsed it shows a one-line preview; expanded it reveals
 * every notification in the group.
 */
@Composable
fun AppGroupCard(
    group: AppGroup,
    onNotificationClick: (AppNotification) -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
) {
    var expanded by remember(group.packageName) { mutableStateOf(initiallyExpanded) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    NotDigestCard(modifier = modifier, contentPadding = PaddingValues(Spacing.md)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AppIcon(packageName = group.packageName, fallbackLabel = group.appName, size = 44.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    text = group.appName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = group.notifications.firstOrNull()?.preview.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CountPill(text = group.count.toString())
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp).rotate(chevronRotation),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                group.notifications.forEachIndexed { index, item ->
                    NotificationListItem(
                        notification = item,
                        onClick = { onNotificationClick(item) },
                        showAppName = false,
                    )
                    if (index < group.notifications.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 56.dp),
                        )
                    }
                }
            }
        }
    }
}
