package com.nidhi.ai.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nidhi.ai.ui.theme.NidhiColors

/** "NIDHI AI" wordmark with a slowly shifting neon gradient. */
@Composable
fun NidhiLogo(animated: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "logo")
    val shiftAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift"
    )
    val shift = if (animated) shiftAnim else 0f
    val brush = Brush.linearGradient(
        colors = listOf(NidhiColors.Cyan, NidhiColors.Blue, NidhiColors.Purple),
        start = Offset(shift, 0f),
        end = Offset(shift + 300f, 0f),
        tileMode = TileMode.Mirror
    )
    Text(
        text = "NIDHI AI",
        modifier = modifier,
        style = TextStyle(
            brush = brush,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 5.sp
        )
    )
}
