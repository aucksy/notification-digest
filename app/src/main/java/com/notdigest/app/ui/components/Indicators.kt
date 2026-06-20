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
import androidx.compose.ui.unit.dp
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
    Text(text = animated.toString(), style = style, color = color, modifier = modifier)
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
