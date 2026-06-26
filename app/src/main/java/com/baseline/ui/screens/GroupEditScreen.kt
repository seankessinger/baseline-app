package com.baseline.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material3.Text
import com.baseline.model.DragKind
import com.baseline.model.Group
import com.baseline.model.Logic
import com.baseline.state.BaselineViewModel
import com.baseline.ui.components.BaselineDivider
import com.baseline.ui.components.ChevronLeft
import com.baseline.ui.components.CrossGlyph
import com.baseline.ui.components.GripHorizontal
import com.baseline.ui.components.GripVertical
import com.baseline.ui.components.LockIcon
import com.baseline.ui.components.UndoArrowIcon
import com.baseline.ui.components.tap
import com.baseline.ui.theme.Archivo
import com.baseline.ui.theme.BaselineTheme
import com.baseline.ui.theme.Newsreader
import com.baseline.ui.theme.NumText
import com.baseline.ui.theme.newsreaderStyle

@Composable
fun GroupEditScreen(vm: BaselineViewModel, group: Group) {
    val c = BaselineTheme.colors
    val state = vm.state
    val locked = Logic.groupStatus(group) != Logic.Status.FRESH
    val editable = !locked
    val dirty = vm.editDirty
    val liveForecasts = Logic.groupForecasts(group).count { !it.deleted }

    // root-space bounds for drag reorder
    val seriesBounds = remember { mutableMapOf<String, ClosedFloatingPointRange<Float>>() }
    val forecastBounds = remember { mutableMapOf<String, ClosedFloatingPointRange<Float>>() }
    val gripTop = remember { mutableMapOf<String, Float>() }

    fun seriesTargetIndex(pointerY: Float): Int {
        for (i in group.series.indices) {
            val b = seriesBounds[group.series[i].id] ?: continue
            if (pointerY < (b.start + b.endInclusive) / 2f) return i
        }
        return group.series.lastIndex
    }

    fun dragForecast(forecastId: String, pointerY: Float) {
        var targetSeriesId: String? = null
        for (s in group.series) {
            val b = seriesBounds[s.id] ?: continue
            if (pointerY in (b.start - 6f)..(b.endInclusive + 6f)) { targetSeriesId = s.id; break }
        }
        if (targetSeriesId == null) {
            val first = group.series.firstOrNull()?.id
            val fb = first?.let { seriesBounds[it] }
            targetSeriesId = if (fb != null && pointerY < fb.start) first else group.series.lastOrNull()?.id
        }
        val tid = targetSeriesId ?: return
        val target = group.series.first { it.id == tid }
        val ids = target.forecasts.map { it.id }.filter { it != forecastId }
        var idx = ids.size
        for (i in ids.indices) {
            val b = forecastBounds[ids[i]] ?: continue
            if (pointerY < (b.start + b.endInclusive) / 2f) { idx = i; break }
        }
        vm.moveForecast(group.id, forecastId, tid, idx)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // header
        Row(
            Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.tap { vm.editBackToRoster() }) { ChevronLeft(c.mut) }
            if (editable) {
                BasicTextField(
                    value = group.name,
                    onValueChange = { vm.setGroupName(group.id, it) },
                    textStyle = newsreaderStyle(24.sp, c.ink, FontWeight.Medium),
                    singleLine = true,
                    cursorBrush = SolidColor(c.ink),
                    modifier = Modifier.weight(1f),
                )
            } else {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LockIcon(c.mut, 14.dp)
                    Text(group.name, style = newsreaderStyle(24.sp, c.ink, FontWeight.Medium))
                }
            }
            Text(
                "Undo",
                color = if (dirty) c.ink else c.mut,
                style = TextStyle(fontFamily = Archivo, fontSize = 13.sp, fontWeight = if (dirty) FontWeight.Bold else FontWeight.SemiBold),
                modifier = Modifier.tap(enabled = dirty) { vm.undoEdit() },
            )
            Text(
                "Done",
                color = c.ink,
                style = TextStyle(fontFamily = Archivo, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.tap { vm.exitEdit() },
            )
        }
        if (locked) {
            Text(
                "Groups with resolved forecasts can't be edited.",
                color = c.mut,
                style = TextStyle(fontFamily = Archivo, fontSize = 11.5.sp),
                modifier = Modifier.padding(start = 56.dp, end = 26.dp, bottom = 6.dp),
            )
        }

        Column(Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, top = 8.dp, bottom = 24.dp)) {
            group.series.forEach { series -> key(series.id) {
                val seriesDragging = state.drag?.kind == DragKind.SERIES && state.drag?.id == series.id
                val seriesShape = RoundedCornerShape(4.dp)
                val b = Logic.baselineIndex(series)
                val n = series.forecasts.size

                Box(
                    Modifier
                        .fillMaxWidth()
                        .zIndex(if (seriesDragging) 6f else 0f)
                        .onGloballyPositioned { seriesBounds[series.id] = it.boundsInRoot().let { r -> r.top..r.bottom } }
                        .padding(bottom = 10.dp)
                        .then(if (seriesDragging) Modifier.shadow(12.dp, seriesShape) else Modifier)
                        .clip(seriesShape)
                        .background(c.field)
                        .border(1.dp, if (seriesDragging) c.ink else c.bd, seriesShape)
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 1.dp),
                ) {
                    Column {
                        if (editable) {
                            // series header: centered grip + N/A toggle
                            Row(Modifier.fillMaxWidth().padding(top = 1.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f))
                                Box(
                                    Modifier
                                        .onGloballyPositioned { gripTop[series.id] = it.positionInRoot().y }
                                        .pointerInput(series.id) {
                                            detectDragGestures(
                                                onDragStart = { vm.beginDrag(DragKind.SERIES, group.id, series.id) },
                                                onDragEnd = { vm.endDrag() },
                                                onDragCancel = { vm.endDrag() },
                                            ) { change, _ ->
                                                change.consume()
                                                val py = (gripTop[series.id] ?: 0f) + change.position.y
                                                vm.moveSeries(group.id, series.id, seriesTargetIndex(py))
                                            }
                                        },
                                ) { GripHorizontal(c.mut) }
                                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                    NaToggle(on = series.na) { vm.toggleSeriesNA(group.id, series.id) }
                                }
                            }
                        }

                        if (n == 0) {
                            Text(
                                "empty series",
                                color = c.mut,
                                style = TextStyle(fontFamily = Newsreader, fontStyle = FontStyle.Italic, fontSize = 12.5.sp),
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp),
                                textAlign = TextAlign.Center,
                            )
                        }

                        series.forecasts.forEachIndexed { idx, st -> key(st.id) {
                            val fDragging = state.drag?.kind == DragKind.FORECAST && state.drag?.id == st.id
                            val struck = st.deleted
                            val canDelete = struck || liveForecasts > 1
                            val cardShape = RoundedCornerShape(3.dp)
                            val pctOpen = state.pctOpen == st.id

                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .zIndex(if (fDragging) 5f else 0f)
                                    .onGloballyPositioned { forecastBounds[st.id] = it.boundsInRoot().let { r -> r.top..r.bottom } }
                                    .padding(bottom = 6.dp)
                                    .then(if (fDragging) Modifier.shadow(10.dp, cardShape) else Modifier)
                                    .clip(cardShape)
                                    .background(c.paper)
                                    .graphicsLayer { alpha = if (struck) 0.5f else 1f }
                                    .padding(9.dp),
                            ) {
                                if (editable) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Box(
                                                    Modifier
                                                        .onGloballyPositioned { gripTop[st.id] = it.positionInRoot().y }
                                                        .pointerInput(st.id) {
                                                            detectDragGestures(
                                                                onDragStart = { vm.beginDrag(DragKind.FORECAST, group.id, st.id) },
                                                                onDragEnd = { vm.endDrag() },
                                                                onDragCancel = { vm.endDrag() },
                                                            ) { change, _ ->
                                                                change.consume()
                                                                val py = (gripTop[st.id] ?: 0f) + change.position.y
                                                                dragForecast(st.id, py)
                                                            }
                                                        }
                                                        .padding(horizontal = 1.dp, vertical = 2.dp),
                                                ) { GripVertical(c.mut) }

                                                BasicTextField(
                                                    value = st.label,
                                                    onValueChange = { vm.setForecastLabel(group.id, st.id, it) },
                                                    enabled = !struck,
                                                    singleLine = true,
                                                    cursorBrush = SolidColor(c.ink),
                                                    textStyle = TextStyle(
                                                        fontFamily = Archivo, fontSize = 14.5.sp, color = c.ink,
                                                        textDecoration = if (struck) TextDecoration.LineThrough else null,
                                                    ),
                                                    modifier = Modifier.weight(1f),
                                                )

                                                if (pctOpen && !struck) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Text("−", color = c.mut, style = TextStyle(fontFamily = Archivo, fontSize = 18.sp), modifier = Modifier.tap { vm.bumpForecast(group.id, st.id, -Logic.STEP) }.padding(horizontal = 5.dp, vertical = 2.dp))
                                                        NumText("${st.p}%", color = c.ink, fontSize = 13.sp, modifier = Modifier.widthIn(min = 38.dp).tap { vm.togglePct(st.id) }, textAlign = TextAlign.Center)
                                                        Text("+", color = c.mut, style = TextStyle(fontFamily = Archivo, fontSize = 16.sp), modifier = Modifier.tap { vm.bumpForecast(group.id, st.id, Logic.STEP) }.padding(horizontal = 5.dp, vertical = 2.dp))
                                                    }
                                                } else {
                                                    NumText(
                                                        "${st.p}%", color = c.mut, fontSize = 13.sp,
                                                        modifier = Modifier.widthIn(min = 34.dp).then(if (!struck) Modifier.tap { vm.togglePct(st.id) } else Modifier),
                                                        textAlign = TextAlign.End,
                                                    )
                                                }
                                            }
                                            if (struck) {
                                                Box(Modifier.fillMaxWidth().height(2.dp).background(c.ink.copy(alpha = 0.7f)).align(Alignment.Center))
                                            }
                                        }
                                        Box(Modifier.width(16.dp), contentAlignment = Alignment.Center) {
                                            if (canDelete) {
                                                Box(Modifier.tap { vm.toggleForecastDelete(group.id, st.id) }.padding(4.dp)) {
                                                    if (struck) UndoArrowIcon(c.mut, 14.dp) else CrossGlyph(c.mut, 12.dp)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(st.label, color = c.ink, style = TextStyle(fontFamily = Archivo, fontSize = 14.5.sp), modifier = Modifier.weight(1f))
                                        NumText("${st.p}%", color = c.mut, fontSize = 13.sp)
                                    }
                                }
                            }

                            val dividerAfter = idx == b && b >= 0 && b < n - 1 && n > 1
                            if (dividerAfter) {
                                BaselineDivider(Modifier.padding(start = 2.dp, end = 2.dp, bottom = 8.dp))
                            }
                        } }
                    }
                }
            } }

            if (editable) {
                Row(
                    Modifier.fillMaxWidth().tap { vm.addForecastNewSeries(group.id) }.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Box(Modifier.size(width = 15.dp, height = 16.dp), contentAlignment = Alignment.Center) {
                        Text("+", color = c.mut, style = TextStyle(fontFamily = Archivo, fontSize = 16.sp))
                    }
                    Text("Add forecast", color = c.mut, style = TextStyle(fontFamily = Archivo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
private fun NaToggle(on: Boolean, onToggle: () -> Unit) {
    val c = BaselineTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(if (on) c.ink.copy(alpha = 0.07f) else Color.Transparent)
            .border(1.dp, if (on) c.ink else c.bd, RoundedCornerShape(2.dp))
            .tap { onToggle() }
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            "N/A",
            color = if (on) c.ink else c.mut,
            style = TextStyle(fontFamily = Archivo, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp),
        )
    }
}
