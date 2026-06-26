package com.baseline.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/** Drift easing `cubic-bezier(.12,.78,.3,1)`. */
private val Drift = CubicBezierEasing(0.12f, 0.78f, 0.30f, 1f)

private data class Particle(
    val sz: Float,      // dp
    val dx: Float,      // dp horizontal drift
    val dy: Float,      // dp vertical drift (negative = up)
    val rot: Float,     // final rotation (deg)
    val delayMs: Float,
    val durMs: Float,
    val color: Color,
)

/** A deterministic 32-bit PRNG (mulberry32-style), seeded so a burst renders identically each time. */
private class Rng(seed: Int) {
    private var a = seed
    fun next(): Float {
        a += 0x6D2B79F5
        var t = a
        t = (t xor (t ushr 15)) * (t or 1)
        t = t xor (t + (t xor (t ushr 7)) * (t or 61))
        val r = (t xor (t ushr 14))
        return (r.toLong() and 0xFFFFFFFFL).toFloat() / 4294967296f
    }
    fun range(lo: Float, hi: Float): Float = lo + next() * (hi - lo)
}

private fun hash(s: String): Int {
    var h = -0x7ee3623b // 2166136261 as Int (FNV offset basis)
    for (ch in s) {
        h = h xor ch.code
        h *= 16777619
    }
    return h
}

private fun confettiColors(green: Color): List<Color> = listOf(
    green,
    lerp(green, Color.White, 0.20f),
    lerp(green, Color.White, 0.36f),
    lerp(green, Color.White, 0.50f),
)

private fun generate(seed: Int, surprise: Float, green: Color): List<Particle> {
    val n = (3 + 17 * surprise).roundToInt().coerceAtLeast(1)
    val dxMax = 36f + 80f * surprise
    val minGap = dxMax * 0.4f
    val tMin = 30f + 30f * surprise
    val tMax = 52f + 54f * surprise
    val cols = confettiColors(green)
    val rng = Rng(seed)
    val out = ArrayList<Particle>(n)
    var prevDx: Float? = null
    for (i in 0 until n) {
        val sz = rng.range(3f, 7f)
        var dx: Float
        var tries = 0
        do {
            dx = rng.range(-dxMax, dxMax)
            tries++
        } while (prevDx != null && abs(dx - prevDx!!) < minGap && tries < 12)
        prevDx = dx
        val dy = -rng.range(tMin, tMax)
        val dur = rng.range(3400f, 5600f)
        val delay = (i.toFloat() / n) * 2000f + rng.range(0f, 90f)
        out += Particle(sz, dx, dy, 220f, delay, dur, cols[i % cols.size])
    }
    return out
}

/**
 * Confetti — ✓ only; gentle slow drift + rotate. The burst is a deterministic function of
 * (forecastId, burst), so a re-render never re-fires a past burst; it regenerates only when
 * [burst] increments on a new ✓.
 */
@Composable
fun Confetti(
    forecastId: String,
    burst: Int,
    surprise: Float,
    originFraction: Float,
    green: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!enabled || burst <= 0) return
    val particles = remember(forecastId, burst, green) { generate(hash("$forecastId#$burst"), surprise, green) }
    val total = remember(particles) { particles.maxOf { it.delayMs + it.durMs } }
    val clock = remember(forecastId, burst) { Animatable(0f) }
    LaunchedEffect(forecastId, burst) {
        clock.snapTo(0f)
        clock.animateTo(1f, animationSpec = tween(total.toInt(), easing = LinearEasing))
    }

    Canvas(modifier.fillMaxWidth().height(15.dp)) {
        val w = size.width
        val originX = originFraction.coerceIn(0f, 1f) * w
        val originY = size.height - 6.dp.toPx()
        val elapsed = clock.value * total
        for (p in particles) {
            val lt = ((elapsed - p.delayMs) / p.durMs)
            if (lt <= 0f || lt >= 1f) continue
            val e = Drift.transform(lt)
            val opacity = if (lt < 0.14f) lt / 0.14f else 1f - (lt - 0.14f) / 0.86f
            val x = originX + p.dx.dp.toPx() * e
            val y = originY + p.dy.dp.toPx() * e
            val half = p.sz.dp.toPx() / 2f
            rotate(degrees = p.rot * e, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = opacity.coerceIn(0f, 1f)),
                    topLeft = Offset(x - half, y - half),
                    size = Size(p.sz.dp.toPx(), p.sz.dp.toPx()),
                )
            }
        }
    }
}
