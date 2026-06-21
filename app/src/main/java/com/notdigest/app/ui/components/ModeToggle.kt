package com.notdigest.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notdigest.app.ui.LocalHapticsEnabled

/**
 * A compact, balanced two-segment control for switching an app between Digest and Real-Time.
 * Fixed width with equal-weight halves so it looks consistent down a long list and leaves room
 * for the app name.
 */
@Composable
fun ModeToggle(
    mode: com.notdigest.app.domain.model.DigestMode,
    onChange: (com.notdigest.app.domain.model.DigestMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val hapticsOn = LocalHapticsEnabled.current
    val change: (com.notdigest.app.domain.model.DigestMode) -> Unit = { target ->
        if (target != mode) {
            if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onChange(target)
        }
    }
    Row(
        modifier = modifier
            .width(148.dp)
            .height(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Segment("Digest", mode == com.notdigest.app.domain.model.DigestMode.DIGEST) {
            change(com.notdigest.app.domain.model.DigestMode.DIGEST)
        }
        Segment("Real-Time", mode == com.notdigest.app.domain.model.DigestMode.REALTIME) {
            change(com.notdigest.app.domain.model.DigestMode.REALTIME)
        }
    }
}

@Composable
private fun RowScope.Segment(label: String, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "segment-bg",
    )
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}
