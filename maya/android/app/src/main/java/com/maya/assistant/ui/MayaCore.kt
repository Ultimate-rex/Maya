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
 * A real, animated liquid-morph style core drawn with Compose's Canvas +
 * infinite transition system - not a static image. `micLevel` (0f..1f)
 * should be fed from the live microphone amplitude while LISTENING, and
 * from the TTS output amplitude while SPEAKING, so the core visibly reacts.
 */
@Composable
fun MayaCore(
    state: MayaState,
    micLevel: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "maya-core")

    val breathing by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.05f,
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
            animation = tween(if (state == MayaState.THINKING) 1600 else 9000, easing = LinearEasing),
        ),
        label = "rotation",
    )

    val baseColor = when (state) {
        MayaState.IDLE -> Color(0xFF6A3EC9)
        MayaState.LISTENING -> Color(0xFF8E5CF0)
        MayaState.THINKING -> Color(0xFF3E9EC9)
        MayaState.SPEAKING -> Color(0xFFB58CFF)
        MayaState.ERROR -> Color(0xFFC94E4E)
    }

    val scale = breathing + (micLevel.coerceIn(0f, 1f) * 0.25f)

    Canvas(modifier = modifier.size(220.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension / 2.6f

        for (ring in 0 until 3) {
            val ringPhase = rotation + ring * 40f
            val wobble = sin(Math.toRadians(ringPhase.toDouble())).toFloat() * 8f
            val radius = (baseRadius * scale) - (ring * 18f) + wobble

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.55f - ring * 0.15f),
                        Color.Black.copy(alpha = 0f),
                    ),
                    center = center,
                    radius = radius * 1.4f,
                ),
                radius = radius,
                center = center,
            )
        }

        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = 6f + micLevel * 10f,
            center = center,
        )
    }
}
