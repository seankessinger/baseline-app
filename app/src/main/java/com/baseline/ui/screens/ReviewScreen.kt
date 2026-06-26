package com.baseline.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.baseline.model.Group
import com.baseline.model.Logic
import com.baseline.model.Mark
import com.baseline.ui.components.ArrowRight
import com.baseline.ui.components.CheckGlyph
import com.baseline.ui.components.ChevronLeft
import com.baseline.ui.components.CrossGlyph
import com.baseline.ui.components.DashGlyph
import com.baseline.ui.components.PrimaryButton
import com.baseline.ui.components.tap
import com.baseline.ui.theme.Archivo
import com.baseline.ui.theme.BaselineTheme
import com.baseline.ui.theme.NumText
import com.baseline.ui.theme.newsreaderStyle

@Composable
fun ReviewScreen(
    groups: List<Group>,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    val c = BaselineTheme.colors
    var resolved = 0
    var ups = 0
    groups.forEach { o ->
        Logic.groupForecasts(o).forEach { s ->
            if (s.mark != null) resolved++
            if (s.mark == Mark.CHECK) ups++
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, top = 24.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.tap { onBack() }) { ChevronLeft(c.mut) }
            Text("Review", style = newsreaderStyle(26.sp, c.ink, FontWeight.Medium), modifier = Modifier.weight(1f))
        }
        Text(
            "$resolved forecasts resolved · $ups revised up",
            color = c.mut,
            style = TextStyle(fontFamily = Archivo, fontSize = 13.sp, lineHeight = 19.5.sp),
            modifier = Modifier.padding(start = 56.dp, end = 26.dp, top = 2.dp, bottom = 16.dp),
        )

        Column(Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, bottom = 24.dp)) {
            groups.forEach { o -> ReviewGroup(o) }

            Spacer(Modifier.height(22.dp))
            PrimaryButton(text = "Confirm", enabled = true, onClick = onConfirm)
        }
    }
}

@Composable
private fun ReviewGroup(o: Group) {
    val c = BaselineTheme.colors
    val forecasts = Logic.groupForecasts(o)
    val allNA = forecasts.all { it.mark == Mark.NA }
    val anyCheck = forecasts.any { it.mark == Mark.CHECK }
    val anyCross = forecasts.any { it.mark == Mark.CROSS }
    val suLabel = if (anyCheck) "showed up" else if (anyCross) "no-show" else ""
    val suColor = if (anyCheck) c.green.copy(alpha = 0.6f) else if (anyCross) c.amber.copy(alpha = 0.65f) else c.na

    // group header
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(o.name, color = c.ink, style = TextStyle(fontFamily = Archivo, fontSize = 13.5.sp, fontWeight = FontWeight.Bold))
        Canvas(Modifier.weight(1f).height(1.dp)) {
            drawLine(
                color = c.bd,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1.dp.toPx(),
                // CSS `dotted`: round caps over a near-zero on-segment render as 1dp dots.
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 2.dp.toPx()), 0f),
            )
        }
        if (!allNA) {
            Text(suLabel, color = suColor, style = TextStyle(fontFamily = Archivo, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold))
        }
    }

    forecasts.forEach { s ->
        val isNa = s.mark == Mark.NA
        val toColor = when (s.mark) {
            Mark.NA -> c.mut
            Mark.CHECK -> c.green
            Mark.CROSS -> c.amber
            null -> c.mut
        }
        Row(
            Modifier.fillMaxWidth().graphicsLayer { alpha = if (isNa) 0.5f else 1f }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.width(15.dp), contentAlignment = Alignment.Center) {
                when (s.mark) {
                    Mark.CHECK -> CheckGlyph(c.green, 15.dp)
                    Mark.CROSS -> CrossGlyph(c.amber, 14.dp)
                    Mark.NA -> DashGlyph(c.na, 14.dp)
                    null -> {}
                }
            }
            Text(s.label, color = c.ink, style = TextStyle(fontFamily = Archivo, fontSize = 14.sp), modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                NumText("${s.startP}%", color = c.mut, fontSize = 12.5.sp, fontWeight = FontWeight.Normal, modifier = Modifier.width(38.dp), textAlign = TextAlign.End)
                ArrowRight(c.arrow, 13.dp)
                NumText("${s.p}%", color = toColor, fontSize = 13.sp, modifier = Modifier.width(38.dp), textAlign = TextAlign.End)
            }
        }
    }
}
