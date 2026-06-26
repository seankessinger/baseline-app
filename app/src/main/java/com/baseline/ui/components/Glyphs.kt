package com.baseline.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * All glyphs are inline vectors recreated from the prototype's SVGs (no raster assets). Stroke
 * paths are drawn on a 24×24 viewport scaled to the requested size, preserving stroke ratios.
 */

private fun DrawScope.strokePath(pathData: String, svgStroke: Float, color: Color, viewport: Float = 24f) {
    val s = size.minDimension / viewport
    val path = PathParser().parsePathString(pathData).toPath()
    path.transform(Matrix().apply { scale(s, s, 1f) })
    drawPath(path, color, style = Stroke(width = svgStroke * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
private fun StrokeGlyph(pathData: String, svgStroke: Float, size: Dp, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { strokePath(pathData, svgStroke, color) }
}

@Composable
fun CheckGlyph(color: Color, size: Dp = 15.dp, modifier: Modifier = Modifier) =
    StrokeGlyph("M20 6L9 17l-5-5", 3f, size, color, modifier)

@Composable
fun CrossGlyph(color: Color, size: Dp = 14.dp, modifier: Modifier = Modifier) =
    StrokeGlyph("M18 6L6 18M6 6l12 12", 3f, size, color, modifier)

@Composable
fun DashGlyph(color: Color, size: Dp = 14.dp, modifier: Modifier = Modifier) =
    StrokeGlyph("M5 12h14", 3f, size, color, modifier)

@Composable
fun ChevronLeft(color: Color, size: Dp = 18.dp, modifier: Modifier = Modifier) =
    StrokeGlyph("M15 18l-6-6 6-6", 2.4f, size, color, modifier)

@Composable
fun ArrowRight(color: Color, size: Dp = 13.dp, modifier: Modifier = Modifier) =
    StrokeGlyph("M5 12h14M13 6l6 6-6 6", 2.2f, size, color, modifier)

private const val GEAR_PATH =
    "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 " +
        "1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 " +
        "1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 " +
        "1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"

@Composable
fun GearIcon(color: Color, size: Dp = 17.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        strokePath(GEAR_PATH, 2f, color)
        drawCircle(color, radius = 3f * s, center = Offset(12f * s, 12f * s), style = Stroke(width = 2f * s))
    }
}

@Composable
fun LockIcon(color: Color, size: Dp = 13.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        drawRoundRect(
            color,
            topLeft = Offset(5f * s, 11f * s),
            size = Size(14f * s, 9f * s),
            cornerRadius = CornerRadius(1.5f * s, 1.5f * s),
            style = Stroke(width = 2f * s),
        )
        strokePath("M8 11V7a4 4 0 0 1 8 0v4", 2f, color)
    }
}

/** The "undo" curved arrow used on a struck (delete-staged) forecast in group-edit. */
@Composable
fun UndoArrowIcon(color: Color, size: Dp = 14.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        strokePath("M9 14 4 9l5-5", 2f, color)
        strokePath("M4 9h11a5 5 0 0 1 0 10h-1", 2f, color)
    }
}

/** Vertical 2×3 grip dots (forecast / group reorder handle). SVG viewport 8×14. */
@Composable
fun GripVertical(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 8.dp, height = 14.dp)) {
        val sx = size.width / 8f
        val sy = size.height / 14f
        val r = 1.2f * sx
        for (cy in intArrayOf(2, 7, 12)) for (cx in intArrayOf(2, 6)) {
            drawCircle(color, radius = r, center = Offset(cx * sx, cy * sy))
        }
    }
}

/** Horizontal 3×2 grip dots (series reorder handle). SVG viewport 24×10. */
@Composable
fun GripHorizontal(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 22.dp, height = 9.dp)) {
        val sx = size.width / 24f
        val sy = size.height / 10f
        val r = 1.3f * sx
        for (cy in intArrayOf(3, 7)) for (cx in intArrayOf(6, 12, 18)) {
            drawCircle(color, radius = r, center = Offset(cx * sx, cy * sy))
        }
    }
}
