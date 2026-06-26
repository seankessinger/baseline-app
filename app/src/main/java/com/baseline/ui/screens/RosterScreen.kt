package com.baseline.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.baseline.model.Group
import com.baseline.model.Logic
import com.baseline.model.Mark
import com.baseline.ui.components.Bar
import com.baseline.ui.components.CheckGlyph
import com.baseline.ui.components.CrossGlyph
import com.baseline.ui.components.DashGlyph
import com.baseline.ui.components.GearIcon
import com.baseline.ui.components.Hairline
import com.baseline.ui.components.PrimaryButton
import com.baseline.ui.components.tap
import com.baseline.ui.components.tapFade
import com.baseline.ui.theme.Archivo
import com.baseline.ui.theme.BaselineTheme
import com.baseline.ui.theme.NumText
import com.baseline.ui.theme.newsreaderStyle
import androidx.compose.ui.text.TextStyle

@Composable
fun RosterScreen(
    groups: List<Group>,
    onOpenGroup: (String) -> Unit,
    onEnterEdit: () -> Unit,
    onOpenSettings: () -> Unit,
    onReview: () -> Unit,
) {
    val c = BaselineTheme.colors
    val complete = Logic.rosterComplete(groups)
    val totalForecasts = groups.sumOf { Logic.groupForecasts(it).size }
    val totalResolved = groups.sumOf { o -> Logic.groupForecasts(o).count { it.mark != null } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // header
        Row(
            Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, top = 26.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Text("baseline", style = newsreaderStyle(28.sp, c.ink, FontWeight.Medium), modifier = Modifier.weight(1f))
            Text(
                "Edit",
                color = c.mut,
                style = TextStyle(fontFamily = Archivo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.tapFade { onEnterEdit() }.padding(6.dp),
            )
            Box(Modifier.tapFade { onOpenSettings() }.padding(6.dp)) { GearIcon(c.mut) }
        }

        // rows
        Column(Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, bottom = 26.dp)) {
            groups.forEachIndexed { i, o ->
                RosterRow(o, last = i == groups.lastIndex, onOpen = { onOpenGroup(o.id) })
            }

            Spacer(Modifier.height(24.dp))
            Hairline(c.bd, thickness = 1.5.dp)
            Spacer(Modifier.height(16.dp))
            NumText(
                "($totalResolved/$totalForecasts) resolved",
                color = c.mut,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton(text = "Review", enabled = complete, onClick = onReview)
        }
    }
}

@Composable
private fun RosterRow(o: Group, last: Boolean, onOpen: () -> Unit) {
    val c = BaselineTheme.colors
    val forecasts = Logic.groupForecasts(o)
    val total = forecasts.size
    val resolved = forecasts.count { it.mark != null }
    val isDone = resolved == total
    val anyCheck = forecasts.any { it.mark == Mark.CHECK }
    val anyCross = forecasts.any { it.mark == Mark.CROSS }
    val suPct = Logic.showUp(o)
    val startPct = forecasts.maxOfOrNull { it.startP } ?: suPct

    Column(
        Modifier
            .fillMaxWidth()
            .tapFade(pressedAlpha = 0.42f) { onOpen() }
            .padding(vertical = 15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            if (isDone && anyCheck) CheckGlyph(c.green, 15.dp)
            else if (isDone && !anyCheck && anyCross) CrossGlyph(c.amber, 14.dp)
            else if (isDone && !anyCheck && !anyCross) DashGlyph(c.na, 14.dp)

            Text(
                o.name,
                color = c.ink,
                style = TextStyle(fontFamily = Archivo, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold),
            )
            if (!isDone) {
                Text("($resolved/$total)", style = newsreaderStyle(13.5.sp, c.mut, FontWeight.Normal))
            }
            Spacer(Modifier.weight(1f))
            NumText("$suPct%", color = c.mut, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        Bar(
            fill = suPct / 100f,
            washColor = c.washNeutral,
            knobColor = c.knobNeutral,
            tickColor = c.tick,
            baseColor = c.bd,
            ghost = if (startPct != suPct) startPct / 100f else null,
            ghostColor = c.tick,
            animate = false,
        )
    }
    if (!last) Hairline(c.bd)
}
