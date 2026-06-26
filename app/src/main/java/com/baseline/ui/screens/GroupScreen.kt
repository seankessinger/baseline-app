package com.baseline.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.baseline.model.Group
import com.baseline.model.Logic
import com.baseline.model.Mark
import com.baseline.model.Series
import com.baseline.ui.components.Bar
import com.baseline.ui.components.BaselineDivider
import com.baseline.ui.components.BaselineHaptics
import com.baseline.ui.components.CheckGlyph
import com.baseline.ui.components.ChevronLeft
import com.baseline.ui.components.Confetti
import com.baseline.ui.components.CrossGlyph
import com.baseline.ui.components.DashGlyph
import com.baseline.ui.components.Hairline
import com.baseline.ui.components.rememberBaselineHaptics
import com.baseline.ui.components.tap
import com.baseline.ui.theme.Archivo
import com.baseline.ui.theme.BaselineTheme
import com.baseline.ui.theme.NumText
import com.baseline.ui.theme.newsreaderStyle

@Composable
fun GroupScreen(
    group: Group,
    confettiOn: Boolean,
    hapticsOn: Boolean,
    onBack: () -> Unit,
    onMark: (groupId: String, si: Int, idx: Int, kind: Mark) -> Unit,
    onUndo: (groupId: String, si: Int, idx: Int) -> Unit,
) {
    val c = BaselineTheme.colors
    val haptics = rememberBaselineHaptics()
    val forecasts = Logic.groupForecasts(group)
    val done = forecasts.count { it.mark != null }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, top = 24.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.tap { onBack() }) { ChevronLeft(c.mut) }
            Text(group.name, style = newsreaderStyle(26.sp, c.ink, FontWeight.Medium), modifier = Modifier.weight(1f))
            NumText("$done / ${forecasts.size}", color = c.mut, fontSize = 13.sp)
        }

        Column(Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, bottom = 24.dp)) {
            group.series.forEachIndexed { si, series ->
                if (si > 0) Hairline(c.bd)
                val live = Logic.liveIndex(series)
                val b = Logic.baselineIndex(series)
                val n = series.forecasts.size
                series.forecasts.forEachIndexed { idx, _ ->
                    ForecastRow(
                        group = group, series = series, si = si, idx = idx, live = live, b = b, n = n,
                        confettiOn = confettiOn, hapticsOn = hapticsOn, haptics = haptics,
                        onMark = onMark, onUndo = onUndo,
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastRow(
    group: Group,
    series: Series,
    si: Int,
    idx: Int,
    live: Int,
    b: Int,
    n: Int,
    confettiOn: Boolean,
    hapticsOn: Boolean,
    haptics: BaselineHaptics,
    onMark: (String, Int, Int, Mark) -> Unit,
    onUndo: (String, Int, Int) -> Unit,
) {
    val c = BaselineTheme.colors
    val st = series.forecasts[idx]
    val isLive = idx == live
    val addressed = st.mark != null
    val isCheck = st.mark == Mark.CHECK
    val isCross = st.mark == Mark.CROSS
    val isNa = st.mark == Mark.NA
    val locked = !addressed && !isLive
    val cascaded = st.cascaded
    val surprise = Logic.surprise(st).toFloat()

    val washColor = when {
        isCheck -> c.green.copy(alpha = (0.12f + 0.4f * surprise))
        isCross -> c.amber.copy(alpha = (0.12f + 0.4f * surprise))
        else -> c.washNeutral
    }
    val knobColor = when {
        isCheck -> c.green
        isCross -> c.amber
        else -> c.knobNeutral
    }
    val pctColor = when {
        isCheck -> c.green
        isCross -> c.amber
        else -> c.mut
    }
    val showGhost = (isCheck || isCross) && st.prevP != null
    val ghostColor = if (isCheck) c.green.copy(alpha = 0.6f) else c.amber.copy(alpha = 0.6f)
    val dividerAfter = idx == b && b >= 0 && b < n - 1 && n > 1
    val rowOpacity = when {
        locked -> 0.42f
        cascaded -> 0.45f
        isNa -> 0.72f
        else -> 1f
    }
    val labelWeight = if (isLive) FontWeight.SemiBold else FontWeight.Medium

    Column(Modifier.fillMaxWidth().graphicsLayer { alpha = rowOpacity }.padding(vertical = 13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            if (addressed) {
                Box(
                    Modifier
                        .then(if (!cascaded) Modifier.tap { onUndo(group.id, si, idx) } else Modifier)
                        .padding(4.dp),
                ) {
                    when {
                        isCheck -> CheckGlyph(c.green, 15.dp)
                        isCross -> CrossGlyph(c.amber, 14.dp)
                        else -> DashGlyph(c.na, 14.dp)
                    }
                }
            }
            Text(
                st.label,
                color = c.ink,
                style = TextStyle(fontFamily = Archivo, fontSize = 14.5.sp, fontWeight = labelWeight),
                modifier = Modifier.weight(1f),
            )
            NumText("${st.p}%", color = pctColor, fontSize = 13.sp)
            if (isLive) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // ✓ — surprise-scaled celebration haptic (faced = the live estimate, pre-move)
                    Box(Modifier.tap {
                        if (hapticsOn) haptics.surpass((100f - st.p) / 100f)
                        onMark(group.id, si, idx, Mark.CHECK)
                    }.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Box(
                            Modifier.size(width = 40.dp, height = 32.dp)
                                .background(c.green.copy(alpha = 0.15f))
                                .border(1.dp, c.green.copy(alpha = 0.42f)),
                            contentAlignment = Alignment.Center,
                        ) { CheckGlyph(c.green, 15.dp) }
                    }
                    // ✗ — one soft, non-punitive tick
                    Box(Modifier.tap {
                        if (hapticsOn) haptics.under()
                        onMark(group.id, si, idx, Mark.CROSS)
                    }.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Box(
                            Modifier.size(width = 40.dp, height = 32.dp)
                                .border(1.dp, c.amber.copy(alpha = 0.42f)),
                            contentAlignment = Alignment.Center,
                        ) { CrossGlyph(c.amber, 13.dp) }
                    }
                    // N/A (per-series)
                    if (series.na) {
                        Box(Modifier.tap { onMark(group.id, si, idx, Mark.NA) }.padding(horizontal = 4.dp, vertical = 8.dp)) {
                            Box(
                                Modifier.height(32.dp)
                                    .border(1.dp, c.bd)
                                    .padding(horizontal = 11.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "N/A",
                                    color = c.mut,
                                    style = TextStyle(fontFamily = Archivo, fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Box {
            Bar(
                fill = st.p / 100f,
                washColor = washColor,
                knobColor = knobColor,
                tickColor = c.tick,
                baseColor = c.bd,
                ghost = if (showGhost) st.prevP!! / 100f else null,
                ghostColor = ghostColor,
                animate = true,
            )
            if (isCheck) {
                Confetti(
                    forecastId = st.id,
                    burst = st.burst,
                    surprise = surprise,
                    originFraction = st.p / 100f,
                    green = c.green,
                    enabled = confettiOn,
                )
            }
        }

        if (dividerAfter) {
            BaselineDivider(Modifier.padding(top = 13.dp))
        }
    }
    if (!dividerAfter && idx != n - 1) Hairline(c.bd)
}
