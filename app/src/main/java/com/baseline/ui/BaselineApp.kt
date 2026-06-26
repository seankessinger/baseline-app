package com.baseline.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import com.baseline.model.Screen
import com.baseline.state.BaselineViewModel
import com.baseline.ui.screens.GroupEditScreen
import com.baseline.ui.screens.GroupScreen
import com.baseline.ui.screens.ReviewScreen
import com.baseline.ui.screens.RosterEditScreen
import com.baseline.ui.screens.RosterScreen
import com.baseline.ui.screens.SettingsSheet
import com.baseline.ui.theme.BaselineTheme

@Composable
fun BaselineApp(vm: BaselineViewModel) {
    BaselineTheme(themeMode = vm.state.theme) {
        val c = BaselineTheme.colors
        val s = vm.state
        val group = vm.openGroupOrNull

        Box(Modifier.fillMaxSize().background(c.paper)) {
            // The app surface, blurred behind the Settings sheet (blur is a no-op below API 31).
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (s.settingsOpen) Modifier.blur(6.dp) else Modifier)
                    .systemBarsPadding(),
            ) {
                // Hold the blank paper surface until the persisted blob has loaded.
                if (s.loading) return@Box
                when {
                    s.editing && s.screen == Screen.GROUP && group != null ->
                        GroupEditScreen(vm = vm, group = group)

                    s.editing ->
                        RosterEditScreen(vm = vm)

                    s.screen == Screen.REVIEW ->
                        ReviewScreen(groups = s.groups, onBack = vm::back, onConfirm = vm::confirm)

                    s.screen == Screen.GROUP && group != null ->
                        GroupScreen(
                            group = group,
                            confettiOn = s.confettiOn,
                            hapticsOn = s.hapticsOn,
                            onBack = vm::back,
                            onMark = vm::mark,
                            onUndo = vm::undo,
                        )

                    else ->
                        RosterScreen(
                            groups = s.groups,
                            onOpenGroup = vm::openGroup,
                            onEnterEdit = vm::enterEdit,
                            onOpenSettings = vm::openSettings,
                            onReview = vm::openReview,
                        )
                }
            }

            if (s.settingsOpen) {
                SettingsSheet(
                    theme = s.theme,
                    confettiOn = s.confettiOn,
                    hapticsOn = s.hapticsOn,
                    onSetTheme = vm::setTheme,
                    onSetConfetti = vm::setConfetti,
                    onSetHaptics = vm::setHaptics,
                    onClose = vm::closeSettings,
                )
            }
        }

        // Device back: mirror the in-app back affordances.
        val backEnabled = s.settingsOpen || s.editing || s.screen != Screen.ROSTER
        BackHandler(enabled = backEnabled) {
            when {
                s.settingsOpen -> vm.closeSettings()
                s.editing && s.screen == Screen.GROUP -> vm.editBackToRoster()
                s.editing -> vm.exitEdit()
                else -> vm.back()
            }
        }
    }
}
