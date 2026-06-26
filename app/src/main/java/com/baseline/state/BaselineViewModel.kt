package com.baseline.state

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baseline.model.AppState
import com.baseline.model.DragKind
import com.baseline.model.DragState
import com.baseline.model.Group
import com.baseline.model.Logic
import com.baseline.model.Mark
import com.baseline.model.PersistedState
import com.baseline.model.Screen
import com.baseline.model.ThemeMode
import kotlinx.coroutines.launch

/**
 * Single source of truth = [AppState] held in one observable [state]. Every transition routes
 * through [set], which mirrors the prototype's `componentDidUpdate`: persist the [PersistedState]
 * slice on every change EXCEPT while editing or dragging.
 */
class BaselineViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = BaselineRepository(app)

    var state by mutableStateOf(AppState(groups = Logic.seed()))
        private set

    init {
        // Load the persisted blob OFF the main thread; the first frame is gated on `loading`
        // (BaselineApp). DataStore dispatches its own IO, so a plain coroutine suffices.
        // Assigned directly (not via set()) so adopting saved state never re-persists it.
        viewModelScope.launch {
            val persisted = repo.load()
            state = if (persisted != null && persisted.groups.isNotEmpty()) {
                state.copy(
                    groups = persisted.groups,
                    theme = persisted.theme,
                    confettiOn = persisted.confettiOn,
                    hapticsOn = persisted.hapticsOn,
                    loading = false,
                )
            } else {
                state.copy(loading = false)
            }
        }
    }

    private fun set(next: AppState) {
        state = next
        if (!next.editing && next.drag == null) persist()
    }

    private fun persist() {
        val snap = PersistedState(state.groups, state.theme, state.confettiOn, state.hapticsOn)
        viewModelScope.launch { repo.save(snap) }
    }

    val editDirty: Boolean
        get() = state.editSnapshot != null && state.groups != state.editSnapshot

    // ---- navigation ----
    fun openGroup(id: String) = set(state.copy(screen = Screen.GROUP, openId = id))
    fun openReview() {
        if (Logic.rosterComplete(state.groups)) set(state.copy(screen = Screen.REVIEW, openId = null))
    }
    fun back() = set(state.copy(screen = Screen.ROSTER, openId = null))

    val openGroupOrNull: Group?
        get() = state.groups.find { it.id == state.openId }

    // ---- marking ----
    fun mark(groupId: String, si: Int, idx: Int, kind: Mark) =
        set(state.copy(groups = Logic.mark(state.groups, groupId, si, idx, kind)))

    fun undo(groupId: String, si: Int, idx: Int) =
        set(state.copy(groups = Logic.undo(state.groups, groupId, si, idx)))

    fun confirm() {
        if (!Logic.rosterComplete(state.groups)) return
        set(state.copy(groups = Logic.confirm(state.groups), screen = Screen.ROSTER, openId = null))
    }

    // ---- settings ----
    fun openSettings() = set(state.copy(settingsOpen = true))
    fun closeSettings() = set(state.copy(settingsOpen = false))
    fun setTheme(mode: ThemeMode) = set(state.copy(theme = mode))
    fun setConfetti(on: Boolean) = set(state.copy(confettiOn = on))
    fun setHaptics(on: Boolean) = set(state.copy(hapticsOn = on))

    fun reset() {
        set(
            AppState(
                groups = Logic.seed(),
                theme = state.theme,
                confettiOn = state.confettiOn,
                hapticsOn = state.hapticsOn,
                loading = false,
            )
        )
    }

    // ---- edit mode ----
    fun enterEdit() = set(state.copy(editing = true, screen = Screen.ROSTER, openId = null, editSnapshot = state.groups))
    fun openGroupEdit(id: String) = set(state.copy(screen = Screen.GROUP, openId = id))
    /** Back chevron inside group-edit: return to the roster-edit list, stay in edit mode. */
    fun editBackToRoster() = set(state.copy(screen = Screen.ROSTER, openId = null))

    fun undoEdit() {
        val snap = state.editSnapshot ?: return
        if (editDirty) set(state.copy(groups = snap))
    }

    /** Done: prune struck forecasts + empty series, normalize marks, commit, exit edit. */
    fun exitEdit() {
        val groups = Logic.exitEditNormalize(state.groups)
        set(
            state.copy(
                editing = false, screen = Screen.ROSTER, openId = null,
                groups = groups, drag = null, pctOpen = null, editSnapshot = null,
            )
        )
    }

    // ---- edit mutations (staged; not persisted until Done) ----
    fun setGroupName(groupId: String, name: String) = set(state.copy(groups = Logic.setGroupName(state.groups, groupId, name)))
    fun setForecastLabel(groupId: String, forecastId: String, label: String) =
        set(state.copy(groups = Logic.setForecastLabel(state.groups, groupId, forecastId, label)))
    fun toggleSeriesNA(groupId: String, seriesId: String) = set(state.copy(groups = Logic.toggleSeriesNA(state.groups, groupId, seriesId)))
    fun togglePct(forecastId: String) = set(state.copy(pctOpen = if (state.pctOpen == forecastId) null else forecastId))
    fun toggleForecastDelete(groupId: String, forecastId: String) =
        set(state.copy(groups = Logic.toggleForecastDelete(state.groups, groupId, forecastId)))
    fun bumpForecast(groupId: String, forecastId: String, d: Int) =
        set(state.copy(groups = Logic.bumpForecast(state.groups, groupId, forecastId, d)))
    fun deleteGroup(groupId: String) = set(state.copy(groups = Logic.deleteGroup(state.groups, groupId)))
    fun addGroup() = set(state.copy(groups = Logic.addGroup(state.groups)))
    fun addForecastNewSeries(groupId: String) = set(state.copy(groups = Logic.addForecastNewSeries(state.groups, groupId)))

    // ---- drag reorder (edit) ----
    fun beginDrag(kind: DragKind, groupId: String?, id: String) = set(state.copy(drag = DragState(kind, groupId, id)))
    fun endDrag() {
        if (state.drag != null) set(state.copy(drag = null))
    }
    fun moveGroup(groupId: String, idx: Int) = set(state.copy(groups = Logic.moveGroupTo(state.groups, groupId, idx)))
    fun moveSeries(groupId: String, seriesId: String, idx: Int) = set(state.copy(groups = Logic.moveSeriesTo(state.groups, groupId, seriesId, idx)))
    fun moveForecast(groupId: String, forecastId: String, targetSeriesId: String, idx: Int) =
        set(state.copy(groups = Logic.moveForecastTo(state.groups, groupId, forecastId, targetSeriesId, idx)))
}
