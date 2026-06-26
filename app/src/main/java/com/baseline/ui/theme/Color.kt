package com.baseline.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The token layer (handoff "Design tokens"), mirroring the prototype's CSS custom properties.
 * Verdict green/amber are reserved for verdicts only — never chrome or actions. Translucent
 * washes/ticks are derived from [ink] / [green] / [amber] via [Color.copy] alpha.
 */
data class BaselineColors(
    val field: Color,
    val caption: Color,
    val shell: Color,
    val paper: Color,
    val ink: Color,
    val mut: Color,
    val bd: Color,
    val knobNeutral: Color,
    val green: Color,
    val amber: Color,
    val na: Color,
    val arrow: Color,
    val btnDisabledText: Color,
) {
    val tick: Color get() = ink.copy(alpha = 0.34f)
    val washNeutral: Color get() = ink.copy(alpha = 0.10f)
    val btnBg: Color get() = ink.copy(alpha = 0.05f)
    val btnActive: Color get() = ink.copy(alpha = 0.11f)
}

val LightColors = BaselineColors(
    field = Color(0xFFE7E3DA),
    caption = Color(0xFF8A8073),
    shell = Color(0xFFEFE9DF),
    paper = Color(0xFFF3EFE6),
    ink = Color(0xFF221D16),
    mut = Color(0xFF8D8472),
    bd = Color(0xFFD9D1C0),
    knobNeutral = Color(0xFF6D655A),
    green = Color(0xFF1F8A52),
    amber = Color(0xFFB9791E),
    na = Color(0xFFA59C8C),
    arrow = Color(0xFFC3BBAB),
    btnDisabledText = Color(0xFFB8AF9F),
)

val DarkColors = BaselineColors(
    field = Color(0xFF14110C),
    caption = Color(0xFF6F6657),
    shell = Color(0xFF201B14),
    paper = Color(0xFF262019),
    ink = Color(0xFFECE5D6),
    mut = Color(0xFF9A907E),
    bd = Color(0xFF3A3328),
    knobNeutral = Color(0xFF8C8474),
    green = Color(0xFF36AB69),
    amber = Color(0xFFD09A44),
    na = Color(0xFF8A8273),
    arrow = Color(0xFF574F42),
    btnDisabledText = Color(0xFF5E584B),
)

val LocalBaselineColors = staticCompositionLocalOf { LightColors }
