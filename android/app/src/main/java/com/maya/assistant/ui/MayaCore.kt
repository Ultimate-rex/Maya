package com.maya.assistant.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

enum class MayaState { IDLE, LISTENING, THINKING, SPEAKING, ERROR }

/**
 * An animated, layered "liquid core" drawn with Compose's Canvas and
 * infinite-transition animations - genuinely animated, not a static image.
 * `micLevel` (0f..1f) should be fed from live mic amplitude while
 * LISTENING/SPEAKING for the core to visibly react to sound.
 */
@Composable
fun MayaCore(
    state: MayaState,
    micLevel: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "maya-core")

    val breathing by infinite.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathing",
    )

    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    MayaState.THINKING -> 1400
                    MayaState.LISTENING -> 5000
                    else -> 9000
                },
                easing = LinearEasing,
            ),
        ),
        label = "rotation",
    )

    val ringPulse by infinite.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ring-pulse",
    )

    val (primary, secondary) = when (state) {
        MayaState.IDLE -> Color(0xFF6A3EC9) to Color(0xFF2E1B54)
        MayaState.LISTENING -> Color(0xFF9B5CF0) to Color(0xFF4A2A8C)
        MayaState.THINKING -> Color(0xFF3E9EC9) to Color(0xFF1B4A6E)
        MayaState.SPEAKING -> Color(0xFFC58CFF) to Color(0xFF5A2FA0)
        MayaState.ERROR -> Color(0xFFC94E4E) to Color(0xFF5A1E1E)
    }

    val scale = breathing + (micLevel.coerceIn(0f, 1f) * 0.3f)

    Canvas(modifier = modifier.size(280.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension / 2.6f

        // Outer sweeping energy ring - stronger and faster while listening/speaking.
        val ringAlpha = when (state) {
            MayaState.LISTENING, MayaState.SPEAKING, MayaState.THINKING -> 0.5f * ringPulse
            else -> 0.22f * ringPulse
        }
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(primary.copy(alpha = 0f), primary.copy(alpha = ringAlpha), primary.copy(alpha = 0f)),
                center = center,
            ),
            radius = baseRadius * 1.55f,
            center = center,
        )

        // Layered glow rings that wobble slightly for a "liquid" feel.
        for (ring in 0 until 4) {
            val ringPhase = rotation + ring * 35f
            val wobble = sin(Math.toRadians(ringPhase.toDouble())).toFloat() * 9f
            val radius = (baseRadius * scale) - (ring * 16f) + wobble

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.6f - ring * 0.13f),
                        secondary.copy(alpha = 0f),
                    ),
                    center = center,
                    radius = radius * 1.5f,
                ),
                radius = radius,
                center = center,
            )
        }

        // Bright core center.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.95f), primary.copy(alpha = 0.4f)),
                center = center,
                radius = 22f + micLevel * 16f,
            ),
            radius = 10f + micLevel * 14f,
            center = center,
        )
    }
}
