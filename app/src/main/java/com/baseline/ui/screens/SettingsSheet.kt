package com.baseline.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.baseline.model.ThemeMode
import com.baseline.ui.components.SegmentedControl
import com.baseline.ui.components.tap
import com.baseline.ui.theme.Archivo
import com.baseline.ui.theme.BaselineTheme
import com.baseline.ui.theme.newsreaderStyle

@Composable
fun SettingsSheet(
    theme: ThemeMode,
    confettiOn: Boolean,
    hapticsOn: Boolean,
    onSetTheme: (ThemeMode) -> Unit,
    onSetConfetti: (Boolean) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val c = BaselineTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(c.ink.copy(alpha = 0.04f))
            .tap { onClose() }
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(20.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(c.paper)
                .border(1.dp, c.bd, RoundedCornerShape(10.dp))
                .tap { /* consume taps on the card */ }
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", style = newsreaderStyle(22.sp, c.ink, FontWeight.Medium), modifier = Modifier.weight(1f))
                Text(
                    "Done",
                    color = c.ink,
                    style = TextStyle(fontFamily = Archivo, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.tap { onClose() },
                )
            }
            Spacer(Modifier.height(18.dp))

            SectionLabel("THEME")
            Spacer(Modifier.height(8.dp))
            SegmentedControl(
                options = listOf("Light", "Dark", "Auto"),
                selectedIndex = when (theme) {
                    ThemeMode.LIGHT -> 0
                    ThemeMode.DARK -> 1
                    ThemeMode.AUTO -> 2
                },
                onSelect = { onSetTheme(listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.AUTO)[it]) },
            )
            Spacer(Modifier.height(18.dp))

            SectionLabel("CONFETTI")
            Spacer(Modifier.height(8.dp))
            SegmentedControl(
                options = listOf("On", "Off"),
                selectedIndex = if (confettiOn) 0 else 1,
                onSelect = { onSetConfetti(it == 0) },
            )
            Spacer(Modifier.height(18.dp))

            SectionLabel("HAPTICS")
            Spacer(Modifier.height(8.dp))
            SegmentedControl(
                options = listOf("On", "Off"),
                selectedIndex = if (hapticsOn) 0 else 1,
                onSelect = { onSetHaptics(it == 0) },
            )

            Spacer(Modifier.height(14.dp))
            Text(
                "Auto follows your device's appearance.",
                color = c.mut,
                style = TextStyle(fontFamily = Archivo, fontSize = 11.5.sp, lineHeight = 16.675.sp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val c = BaselineTheme.colors
    Text(
        text,
        color = c.mut,
        style = TextStyle(fontFamily = Archivo, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.32.sp),
    )
}
