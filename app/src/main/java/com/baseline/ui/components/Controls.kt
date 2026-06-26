package com.baseline.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.baseline.ui.theme.Archivo
import com.baseline.ui.theme.BaselineTheme
import com.baseline.ui.theme.newsreaderStyle
import androidx.compose.material3.Text

/** Tap with no ripple — the flat, "no chrome" vocabulary. */
fun Modifier.tap(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val ix = remember { MutableInteractionSource() }
    clickable(interactionSource = ix, indication = null, enabled = enabled, onClick = onClick)
}

/** Tap that dims to [pressedAlpha] while pressed (the prototype's `style-active="opacity:…"`). */
fun Modifier.tapFade(pressedAlpha: Float = 0.6f, enabled: Boolean = true, onClick: () -> Unit): Modifier =
    composed {
        val ix = remember { MutableInteractionSource() }
        val pressed by ix.collectIsPressedAsState()
        this
            .graphicsLayer { alpha = if (pressed && enabled) pressedAlpha else 1f }
            .clickable(interactionSource = ix, indication = null, enabled = enabled, onClick = onClick)
    }

@Composable
fun Hairline(color: Color, modifier: Modifier = Modifier, thickness: Dp = 1.dp) {
    Box(modifier.fillMaxWidth().height(thickness).background(color))
}

/**
 * Primary action (Review / Confirm): a full-width ink hairline outline over a faint ink wash;
 * disabled drops to a `bd` hairline + muted text. No solid fills — enabled vs disabled differ by
 * ink strength.
 */
@Composable
fun PrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = BaselineTheme.colors
    val ix = remember { MutableInteractionSource() }
    val pressed by ix.collectIsPressedAsState()
    Box(
        modifier
            .fillMaxWidth()
            .background(if (!enabled) Color.Transparent else if (pressed) c.btnActive else c.btnBg)
            .border(if (enabled) 1.5.dp else 1.dp, if (enabled) c.ink else c.bd)
            .clickable(interactionSource = ix, indication = null, enabled = enabled, onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) c.ink else c.btnDisabledText,
            style = TextStyle(fontFamily = Archivo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

/** A segmented control (Theme · Confetti in Settings). */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = BaselineTheme.colors
    Row(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, label ->
            val sel = i == selectedIndex
            val shape = RoundedCornerShape(
                topStart = if (i == 0) 4.dp else 0.dp,
                bottomStart = if (i == 0) 4.dp else 0.dp,
                topEnd = if (i == options.lastIndex) 4.dp else 0.dp,
                bottomEnd = if (i == options.lastIndex) 4.dp else 0.dp,
            )
            Box(
                Modifier
                    .weight(1f)
                    .zIndex(if (sel) 1f else 0f)
                    .offset(x = if (i == 0) 0.dp else (-i).dp)
                    .clip(shape)
                    .background(if (sel) c.btnActive else Color.Transparent)
                    .border(1.dp, if (sel) c.ink else c.bd, shape)
                    .tap { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (sel) c.ink else c.mut,
                    style = TextStyle(fontFamily = Archivo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

/** The `baseline` divider: an italic word flanked by hairlines. */
@Composable
fun BaselineDivider(modifier: Modifier = Modifier) {
    val c = BaselineTheme.colors
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(c.bd))
        Text("baseline", style = newsreaderStyle(12.sp, c.mut, FontWeight.Normal))
        Box(Modifier.weight(1f).height(1.dp).background(c.bd))
    }
}
