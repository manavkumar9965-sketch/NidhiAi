package com.nidhi.ai.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nidhi.ai.domain.AssistantStatus
import com.nidhi.ai.ui.theme.NidhiColors

/** Glowing animated orb. Animation speed reflects the assistant status. */
@Composable
fun NidhiOrb(
    status: AssistantStatus,
    animated: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    val pulseMs = when (status) {
        AssistantStatus.IDLE -> 3200
        AssistantStatus.LISTENING -> 1400
        AssistantStatus.THINKING -> 1000
        AssistantStatus.SPEAKING -> 700
    }
    val spinMs = when (status) {
        AssistantStatus.IDLE -> 12000
        AssistantStatus.LISTENING -> 5000
        AssistantStatus.THINKING -> 2500
        AssistantStatus.SPEAKING -> 4000
    }

    val transition = rememberInfiniteTransition(label = "orb")
    val rotationAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(spinMs, easing = LinearEasing)),
        label = "rotation"
    )
    val pulseAnim by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            tween(pulseMs, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val rotation = if (animated) rotationAnim else 0f
    val pulse = if (animated) pulseAnim else 1f

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Nidhi: ${status.label}" }
    ) {
        val r = this.size.minDimension / 2f
        val c = Offset(this.size.width / 2f, this.size.height / 2f)

        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NidhiColors.Cyan.copy(alpha = 0.35f),
                    NidhiColors.Purple.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = c,
                radius = r * pulse
            ),
            radius = r * pulse,
            center = c
        )

        // Core
        val coreR = r * 0.5f * pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    NidhiColors.Cyan,
                    NidhiColors.Blue,
                    NidhiColors.Purple.copy(alpha = 0.85f)
                ),
                center = c,
                radius = coreR
            ),
            radius = coreR,
            center = c
        )

        // Orbiting rings
        val stroke = Stroke(width = r * 0.04f, cap = StrokeCap.Round)
        val ringR = r * 0.78f
        val ringTopLeft = Offset(c.x - ringR, c.y - ringR)
        val ringSize = Size(ringR * 2, ringR * 2)
        rotate(rotation, pivot = c) {
            drawArc(
                color = NidhiColors.Cyan,
                startAngle = 0f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = ringTopLeft,
                size = ringSize,
                style = stroke
            )
        }
        rotate(-rotation * 1.5f, pivot = c) {
            drawArc(
                color = NidhiColors.Purple,
                startAngle = 180f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = ringTopLeft,
                size = ringSize,
                style = stroke
            )
        }
    }
}
