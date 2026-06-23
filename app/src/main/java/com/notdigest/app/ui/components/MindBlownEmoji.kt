package com.notdigest.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A little 🤯 that keeps "blowing": the head pops and wobbles while bursts of sparks fly outward —
 * an approximation of the animated exploding-head emoji. The all-time milestone easter egg (100k+),
 * and previewable by tapping the version 10× in Settings.
 */
@Composable
fun MindBlownEmoji(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val transition = rememberInfiniteTransition(label = "mind-blown")
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(620, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale",
    )
    val rotation by transition.animateFloat(
        initialValue = -9f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "rotation",
    )
    // 0→1 burst phase, restarting — drives sparks flying out and fading.
    val burst by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "burst",
    )

    val field = size * 1.9f
    Box(modifier.size(field), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(field)) {
            val rays = 8
            val maxR = this.size.minDimension / 2f
            val r = burst * maxR
            val alpha = (1f - burst).coerceIn(0f, 1f)
            val dot = 1.6.dp.toPx() * (1f - burst * 0.4f)
            for (i in 0 until rays) {
                val ang = (2.0 * PI * i / rays).toFloat()
                val p = Offset(center.x + cos(ang) * r, center.y + sin(ang) * r)
                val color = if (i % 2 == 0) SparkWarm else SparkHot
                drawCircle(color = color.copy(alpha = alpha), radius = dot, center = p)
            }
        }
        Text(
            text = "🤯",
            style = TextStyle(fontSize = size.value.sp),
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            },
        )
    }
}

private val SparkWarm = Color(0xFFFFC400)
private val SparkHot = Color(0xFFFF5722)
