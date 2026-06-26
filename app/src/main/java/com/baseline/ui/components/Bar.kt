package com.baseline.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/** Bar/knob slide easing: `cubic-bezier(.22,.61,.36,1)` over .55s. */
val Emphasized = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)

/**
 * The Wash bar: a hairline base, a translucent verdict-wash fill, the unnamed 50 center tick, an
 * optional dotted ghost at the pre-move value, and a 2px knob at the current estimate. Fill width
 * and knob position animate over .55s (an "update"); wash + knob colors cross-fade.
 *
 * @param fill current estimate as a fraction 0..1 (`p/100`).
 * @param ghost pre-move estimate as a fraction 0..1, or null for no ghost trace.
 */
@Composable
fun Bar(
    fill: Float,
    washColor: Color,
    knobColor: Color,
    tickColor: Color,
    baseColor: Color,
    modifier: Modifier = Modifier,
    ghost: Float? = null,
    ghostColor: Color = Color.Transparent,
    showTick: Boolean = true,
    animate: Boolean = true,
) {
    val animFill by animateFloatAsState(
        targetValue = fill,
        animationSpec = if (animate) tween(550, easing = Emphasized) else tween(0),
        label = "barFill",
    )
    val animWash by animateColorAsState(
        targetValue = washColor,
        animationSpec = if (animate) tween(450) else tween(0),
        label = "barWash",
    )
    val animKnob by animateColorAsState(
        targetValue = knobColor,
        animationSpec = if (animate) tween(450) else tween(0),
        label = "barKnob",
    )

    Canvas(modifier.fillMaxWidth().height(15.dp)) {
        val w = size.width
        val baseY = size.height - 2.dp.toPx()
        val f = animFill.coerceIn(0f, 1f)

        // base hairline (bottom)
        val hair = 1.5.dp.toPx()
        drawRect(
            color = baseColor,
            topLeft = Offset(0f, baseY - hair),
            size = androidx.compose.ui.geometry.Size(w, hair),
        )
        // wash fill
        val washH = 9.dp.toPx()
        drawRect(
            color = animWash,
            topLeft = Offset(0f, baseY - washH),
            size = androidx.compose.ui.geometry.Size(w * f, washH),
        )
        // center tick at 50%
        if (showTick) {
            val tx = w / 2f
            drawRect(
                color = tickColor,
                topLeft = Offset(tx - 0.5.dp.toPx(), baseY - 10.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(1.dp.toPx(), 12.dp.toPx()),
            )
        }
        // ghost: dotted vertical at pre-move value
        if (ghost != null) {
            val gx = (ghost.coerceIn(0f, 1f)) * w
            drawLine(
                color = ghostColor,
                start = Offset(gx, baseY - 11.dp.toPx()),
                end = Offset(gx, baseY),
                strokeWidth = 1.5.dp.toPx(),
                // CSS `dotted`: round caps over a near-zero on-segment render as 1.5dp dots.
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.01f, 3.dp.toPx()), 0f),
            )
        }
        // knob at current estimate
        val kx = w * f
        val kw = 2.dp.toPx()
        drawRect(
            color = animKnob,
            topLeft = Offset(kx - kw / 2f, baseY - 12.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(kw, 13.dp.toPx()),
        )
    }
}
