package com.baseline.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Surprise-scaled haptics — "felt confetti". A real ✓ (Surpass) fires a celebration whose richness
 * grows with surprise (the same `(100 - faced)/100` that drives the visual confetti); a ✗ (Under)
 * gets one soft, non-punitive tick; N/A is silent. This mirrors the encoding's *asymmetric
 * celebration* — the strong, scaled channel is upside-only, which is what keeps the upside louder.
 *
 * Degrades by device capability: haptic primitives (API 30+, hardware-dependent) → an
 * amplitude-controlled waveform → a plain one-shot. No-ops cleanly where there is no vibrator.
 */
class BaselineHaptics(context: Context) {

    private val vibrator: Vibrator? = run {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        v?.takeIf { it.hasVibrator() }
    }

    /** A real ✓: a celebration scaled by [surprise] (0 = expected, 1 = a long-shot win). */
    fun surpass(surprise: Float) {
        val v = vibrator ?: return
        val s = surprise.coerceIn(0f, 1f)
        // Try the richest channel the device supports, but always keep a fallback so the
        // celebration can never silently go missing. Some vibrator HALs accept a primitive
        // composition yet refuse to *play* certain shapes (notably longer ones that use
        // inter-primitive delays), so the composition below is kept deliberately compact.
        if (supportsPrimitives()) {
            try {
                v.vibrate(buildSurpassComposition(s)); return
            } catch (_: Throwable) { /* fall through to the waveform */ }
        }
        try {
            v.vibrate(buildSurpassWaveform(s)); return
        } catch (_: Throwable) { /* fall through to a plain one-shot */ }
        try {
            v.vibrate(oneShot((16 + 40 * s).toLong(), (120 + 135 * s).toInt()))
        } catch (_: Throwable) { /* no usable channel */ }
    }

    /** A ✗: one soft, gentle, unscaled tick — it registers, it doesn't scold. */
    fun under() {
        val v = vibrator ?: return
        val effect = if (supportsPrimitives()) {
            val low = if (hasPrimitive(lowTickOrTick())) lowTickOrTick() else VibrationEffect.Composition.PRIMITIVE_TICK
            VibrationEffect.startComposition().addPrimitive(low, 0.35f).compose()
        } else {
            oneShot(durationMs = 14, amplitude = 60)
        }
        v.vibrate(effect)
    }

    // ---- builders ----

    private fun buildSurpassComposition(s: Float): VibrationEffect {
        val c = VibrationEffect.startComposition()
        // At most two back-to-back primitives, no inter-primitive delay: a higher-surprise win
        // swells (QUICK_RISE) into the pop; an expected one goes straight to a firm click. Some
        // vibrator HALs silently drop longer or delay-spaced compositions, so the whole dynamic
        // range rides on scaling these two well-supported primitives rather than a longer shape.
        if (s >= 0.30f && hasPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE)) {
            c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, (0.45f + 0.55f * s).coerceIn(0.05f, 1f))
        }
        // The pop. Scales with surprise but keeps a firm floor so even an expected win lands.
        c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (0.55f + 0.45f * s).coerceIn(0.05f, 1f))
        return c.compose()
    }

    private fun buildSurpassWaveform(s: Float): VibrationEffect {
        if (vibrator?.hasAmplitudeControl() == true) {
            val amp = (70 + 185 * s).toInt().coerceIn(1, 255)
            return if (s < 0.5f) {
                VibrationEffect.createOneShot((12 + 18 * s).toLong().coerceAtLeast(1), amp)
            } else {
                val pre = (amp * 0.4f).toInt().coerceIn(1, 255)
                VibrationEffect.createWaveform(
                    longArrayOf(0, 10, 28, (20 + 30 * s).toLong()),
                    intArrayOf(0, pre, 0, amp),
                    -1,
                )
            }
        }
        // No amplitude control: convey "more surprise" via duration / a second pulse.
        return if (s < 0.5f) {
            VibrationEffect.createOneShot((12 + 24 * s).toLong().coerceAtLeast(1), VibrationEffect.DEFAULT_AMPLITUDE)
        } else {
            VibrationEffect.createWaveform(longArrayOf(0, 18, 40, 28), -1)
        }
    }

    private fun oneShot(durationMs: Long, amplitude: Int): VibrationEffect =
        if (vibrator?.hasAmplitudeControl() == true) {
            VibrationEffect.createOneShot(durationMs.coerceAtLeast(1), amplitude.coerceIn(1, 255))
        } else {
            VibrationEffect.createOneShot(durationMs.coerceAtLeast(1), VibrationEffect.DEFAULT_AMPLITUDE)
        }

    private fun lowTickOrTick(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) VibrationEffect.Composition.PRIMITIVE_LOW_TICK
        else VibrationEffect.Composition.PRIMITIVE_TICK

    private fun supportsPrimitives(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK)

    private fun hasPrimitive(primitive: Int): Boolean {
        val v = vibrator ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return try {
            v.areAllPrimitivesSupported(primitive)
        } catch (e: Throwable) {
            false
        }
    }
}

@Composable
fun rememberBaselineHaptics(): BaselineHaptics {
    val context = LocalContext.current
    return remember(context) { BaselineHaptics(context.applicationContext) }
}
