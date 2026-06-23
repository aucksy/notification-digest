package com.notdigest.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.ui.theme.NotDigestTheme
import com.notdigest.app.ui.theme.Spacing

/** A number that animates smoothly when its value changes (used by the savings counter). */
@Composable
fun AnimatedCount(
    target: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animated by animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "animated-count",
    )
    val text = animated.toString()
    // Shrink big numbers so a long-time user's "all time" total (100,000+) never clips the narrow tile.
    val scaledStyle = if (style.fontSize.isSpecified && text.length > 4) {
        val factor = when (text.length) {
            5 -> 0.82f
            6 -> 0.68f
            else -> 0.56f
        }
        style.copy(fontSize = style.fontSize * factor)
    } else {
        style
    }
    Text(text = text, style = scaledStyle, color = color, modifier = modifier, maxLines = 1, softWrap = false, overflow = TextOverflow.Visible)
}

/** Small dot used to indicate unread state. */
@Composable
fun UnreadDot(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 8.dp) {
    Box(modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
}

/** Compact badge showing whether an app is in Digest or Real-Time mode. */
@Composable
fun ModeBadge(mode: DigestMode, modifier: Modifier = Modifier) {
    val (label, container, content) = when (mode) {
        DigestMode.DIGEST -> Triple(
            "Digest",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        DigestMode.REALTIME -> Triple(
            "Real-Time",
            NotDigestTheme.brand.positiveContainer,
            NotDigestTheme.brand.positive,
        )
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = Spacing.md, vertical = Spacing.xxs),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = content)
    }
}
