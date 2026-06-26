@file:OptIn(ExperimentalTextApi::class)

package com.baseline.ui.theme

import androidx.compose.material3.Text
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.baseline.R

/**
 * Typography: Newsreader (italic — wordmark, group titles, the `baseline` divider, status) over
 * Archivo (UI/body). Both are bundled VARIABLE fonts; each weight is a `wght`-axis instance, so a
 * single TTF serves every weight (requires API 26+, matching minSdk).
 */

val Newsreader = FontFamily(
    Font(R.font.newsreader_italic, weight = FontWeight.Normal, style = FontStyle.Italic, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.newsreader_italic, weight = FontWeight.Medium, style = FontStyle.Italic, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.newsreader_italic, weight = FontWeight.SemiBold, style = FontStyle.Italic, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)

val Archivo = FontFamily(
    Font(R.font.archivo, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.archivo, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.archivo, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.archivo, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/** Tabular numerals — for every percentage (handoff: "Tabular numerals for every percentage"). */
const val TNUM = "tnum"

/**
 * A percentage / count readout: Archivo with tabular figures. Centralizes the `tnum` feature so
 * digits never shift width as estimates move.
 */
@Composable
fun NumText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.SemiBold,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        color = color,
        modifier = modifier,
        style = TextStyle(
            fontFamily = Archivo,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFeatureSettings = TNUM,
            textAlign = textAlign ?: TextAlign.Unspecified,
        ),
    )
}

/** Newsreader italic — wordmark / titles / divider. */
fun newsreaderStyle(fontSize: TextUnit, color: Color, weight: FontWeight = FontWeight.Medium): TextStyle =
    TextStyle(fontFamily = Newsreader, fontStyle = FontStyle.Italic, fontWeight = weight, fontSize = fontSize, color = color)
