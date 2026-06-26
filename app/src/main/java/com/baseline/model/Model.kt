package com.baseline.model

import kotlinx.serialization.Serializable

/**
 * The data model (spec §3). `roster` (the list of [Group]s) is the single source of truth;
 * everything else — status, live rung, baseline divider, show-up, surprise — is derived on
 * render and never stored. See [com.baseline.model.Logic].
 *
 * Roster   = Group+
 * Group    = Series+        // one behavior; one or more parallel series
 * Series   = Forecast → …   // a gated ladder; rung i is live once rung i-1 is checked
 */

/** A forecast's verdict, signed by the mark. */
@Serializable
enum class Mark { CHECK, CROSS, NA }

/**
 * One rung of a ladder. Carries a single estimate [p] — a whole percent, the walk's position
 * (50 is the baseline), clamped to [Logic.FLOOR]..[Logic.CEIL].
 */
@Serializable
data class Forecast(
    val id: String,
    val label: String,
    /** estimate `p`: a whole percent in [5, 95]. 50 is the baseline. */
    val p: Int,
    /** start-of-cycle snapshot of `p` (for the Review `from → to`). */
    val startP: Int,
    val mark: Mark? = null,
    /** pre-move value of `p`, for undo + the ghost trace. */
    val prevP: Int? = null,
    /** counter; bumps on a real ✓ to re-key its confetti burst. */
    val burst: Int = 0,
    /** true when this mark was applied by a cascade, not acted on directly (non-undoable). */
    val cascaded: Boolean = false,
    /** edit-mode strike-delete flag; pruned on Done. Not part of normal cycle state. */
    val deleted: Boolean = false,
)

/**
 * One series: a vertical ladder of forecasts. [na] is per-series — when on, every live rung in
 * the series offers the N/A action (which structurally guarantees a cascade never lands on a
 * non-N/A rung).
 */
@Serializable
data class Series(
    val id: String,
    val name: String? = null,
    val na: Boolean = false,
    val forecasts: List<Forecast>,
)

/** One tracked behavior: one or more parallel [Series]. */
@Serializable
data class Group(
    val id: String,
    val name: String,
    val series: List<Series>,
)

/** Theme selection. AUTO follows the device color scheme. */
@Serializable
enum class ThemeMode { LIGHT, DARK, AUTO }

/** Which top-level surface is showing (edit mode is tracked separately on [AppState]). */
enum class Screen { ROSTER, GROUP, REVIEW }

/** What is being dragged in an edit screen, and from which group. */
data class DragState(
    val kind: DragKind,
    val groupId: String?,
    val id: String,
)

enum class DragKind { GROUP, SERIES, FORECAST }

/**
 * The whole application state. The persisted subset is [groups] + [theme] + [confettiOn] +
 * [hapticsOn] (mirrors the prototype's single `localStorage['baseline']` blob); the rest is transient.
 */
data class AppState(
    val groups: List<Group>,
    val screen: Screen = Screen.ROSTER,
    val openId: String? = null,
    val editing: Boolean = false,
    // Fresh install defaults to Light (matches the prototype's `_loadInitial`); persisted state
    // overrides whatever the user explicitly chose (including Auto).
    val theme: ThemeMode = ThemeMode.LIGHT,
    val confettiOn: Boolean = true,
    /** Surprise-scaled haptics on a ✓ / soft tick on a ✗ (respects the OS, off-switchable). */
    val hapticsOn: Boolean = true,
    val settingsOpen: Boolean = false,
    /** true until the persisted blob has loaded; the first frame is gated on it. */
    val loading: Boolean = true,
    // transient — never persisted
    val drag: DragState? = null,
    val pctOpen: String? = null,
    val editSnapshot: List<Group>? = null,
)

/** The persisted slice of [AppState]. */
@Serializable
data class PersistedState(
    val groups: List<Group>,
    val theme: ThemeMode = ThemeMode.LIGHT,
    val confettiOn: Boolean = true,
    val hapticsOn: Boolean = true,
)
