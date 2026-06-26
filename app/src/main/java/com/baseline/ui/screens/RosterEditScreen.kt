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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material3.Text
import com.baseline.model.DragKind
import com.baseline.model.Logic
import com.baseline.state.BaselineViewModel
import com.baseline.ui.components.Bar
import com.baseline.ui.components.CrossGlyph
import com.baseline.ui.components.GripVertical
import com.baseline.ui.components.LockIcon
import com.baseline.ui.components.tap
import com.baseline.ui.theme.Archivo
import com.baseline.ui.theme.BaselineTheme
import com.baseline.ui.theme.newsreaderStyle

@Composable
fun RosterEditScreen(vm: BaselineViewModel) {
    val c = BaselineTheme.colors
    val state = vm.state
    val groups = state.groups
    val dirty = vm.editDirty

    // root-space vertical bounds of each card, for drag reorder
    val bounds = remember { mutableMapOf<String, ClosedFloatingPointRange<Float>>() }
    val gripTop = remember { mutableMapOf<String, Float>() }

    fun targetIndex(pointerY: Float): Int {
        for (i in groups.indices) {
            val b = bounds[groups[i].id] ?: continue
            val mid = (b.start + b.endInclusive) / 2f
            if (pointerY < mid) return i
        }
        return groups.lastIndex
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // header
        Row(
            Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, top = 26.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Text("baseline", style = newsreaderStyle(28.sp, c.ink, FontWeight.Medium), modifier = Modifier.weight(1f))
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

        Column(Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, bottom = 26.dp)) {
            groups.forEach { o -> key(o.id) {
                val locked = Logic.groupStatus(o) != Logic.Status.FRESH
                val editable = !locked
                val isDragging = state.drag?.kind == DragKind.GROUP && state.drag?.id == o.id
                val suPct = Logic.showUp(o)
                val shape = RoundedCornerShape(4.dp)

                Box(
                    Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 6f else 0f)
                        .onGloballyPositioned {
                            val b = it.boundsInRoot()
                            bounds[o.id] = b.top..b.bottom
                        }
                        .padding(bottom = 9.dp)
                        .then(if (isDragging) Modifier.shadow(12.dp, shape) else Modifier)
                        .clip(shape)
                        .background(c.paper)
                        .border(1.dp, if (isDragging) c.ink else c.bd, shape)
                        .tap { vm.openGroupEdit(o.id) }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            if (editable) {
                                Box(
                                    Modifier
                                        .onGloballyPositioned { gripTop[o.id] = it.positionInRoot().y }
                                        .tap { /* consume tap so it doesn't open the row */ }
                                        .pointerInput(o.id) {
                                            detectDragGestures(
                                                onDragStart = { vm.beginDrag(DragKind.GROUP, o.id, o.id) },
                                                onDragEnd = { vm.endDrag() },
                                                onDragCancel = { vm.endDrag() },
                                            ) { change, _ ->
                                                change.consume()
                                                val pointerY = (gripTop[o.id] ?: 0f) + change.position.y
                                                vm.moveGroup(o.id, targetIndex(pointerY))
                                            }
                                        }
                                        .padding(horizontal = 1.dp, vertical = 2.dp),
                                ) { GripVertical(c.mut) }
                            }
                            if (locked) LockIcon(c.mut, 13.dp)
                            Text(
                                o.name,
                                color = c.ink,
                                style = TextStyle(fontFamily = Archivo, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.weight(1f),
                            )
                            if (editable) {
                                Box(Modifier.tap { vm.deleteGroup(o.id) }.padding(5.dp)) { CrossGlyph(c.mut, 14.dp) }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Bar(
                            fill = suPct / 100f,
                            washColor = c.washNeutral,
                            knobColor = c.knobNeutral,
                            tickColor = c.tick,
                            baseColor = c.bd,
                            animate = false,
                        )
                    }
                }
            } }

            Row(
                Modifier.fillMaxWidth().tap { vm.addGroup() }.padding(top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Box(Modifier.size(width = 15.dp, height = 17.dp), contentAlignment = Alignment.Center) {
                    Text("+", color = c.mut, style = TextStyle(fontFamily = Archivo, fontSize = 17.sp))
                }
                Text("Add group", color = c.mut, style = TextStyle(fontFamily = Archivo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}
