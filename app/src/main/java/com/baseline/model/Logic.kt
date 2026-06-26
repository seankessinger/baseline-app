package com.baseline.model

import kotlin.random.Random

/**
 * Pure logic — the engine (spec §3). Derived reads and immutable transitions, ported 1:1 from
 * the canonical prototype. Nothing here mutates input; every transition returns a fresh
 * `List<Group>`. The estimate `p` math all goes through one [clamp].
 */
object Logic {
    const val STEP = 5
    const val FLOOR = 5
    const val CEIL = 95
    const val BASELINE = 50

    fun clamp(p: Int): Int = p.coerceIn(FLOOR, CEIL)

    // ---- derived reads ----

    fun groupForecasts(group: Group): List<Forecast> = group.series.flatMap { it.forecasts }

    /** Show-up = `max p` of the group: the rung you'll most reliably clear. */
    fun showUp(group: Group): Int = groupForecasts(group).maxOfOrNull { it.p } ?: BASELINE

    enum class Status { FRESH, PASS, DONE }

    fun groupStatus(group: Group): Status {
        val st = groupForecasts(group)
        val addressed = st.count { it.mark != null }
        return when {
            addressed == 0 -> Status.FRESH
            addressed == st.size -> Status.DONE
            else -> Status.PASS
        }
    }

    /** Index of the one live (unmarked) rung in a series, or -1 if none. */
    fun liveIndex(series: Series): Int = series.forecasts.indexOfFirst { it.mark == null }

    /**
     * The baseline divider sits after the highest prefix forecast with `p >= 50`
     * (stop at the first below). -1 if the first rung is already below 50.
     */
    fun baselineIndex(series: Series): Int {
        var b = -1
        for (i in series.forecasts.indices) {
            if (series.forecasts[i].p >= BASELINE) b = i else break
        }
        return b
    }

    /** `faced` = the estimate before the move. */
    fun faced(fc: Forecast): Int = fc.prevP ?: fc.p

    /**
     * Surprise ∈ [0,1]: how far the mark was from certain, against `p` *before* the move.
     * The single quantity behind both wash saturation and confetti.
     */
    fun surprise(fc: Forecast): Double = when (fc.mark) {
        Mark.CHECK -> (100 - faced(fc)) / 100.0   // a long-shot win saturates
        Mark.CROSS -> faced(fc) / 100.0           // dropping a sure thing saturates
        else -> 0.0
    }

    val Group.allDone: Boolean get() = groupStatus(this) == Status.DONE

    fun rosterComplete(groups: List<Group>): Boolean =
        groups.isNotEmpty() && groups.all { groupStatus(it) == Status.DONE }

    // ---- id / construction ----

    fun genId(): String = "x" + System.currentTimeMillis().toString(36) + Random.nextInt(0, 10000).toString(36)

    fun newForecast(label: String, p: Int): Forecast =
        Forecast(id = genId(), label = label, p = p, startP = p, mark = null, prevP = null, burst = 0, cascaded = false)

    // ---- immutable update helpers ----

    private inline fun List<Group>.mapGroup(groupId: String, transform: (Group) -> Group): List<Group> =
        map { if (it.id == groupId) transform(it) else it }

    private inline fun Group.mapSeriesAt(index: Int, transform: (Series) -> Series): Group =
        copy(series = series.mapIndexed { i, s -> if (i == index) transform(s) else s })

    private inline fun Group.mapAllForecasts(transform: (Forecast) -> Forecast): Group =
        copy(series = series.map { s -> s.copy(forecasts = s.forecasts.map(transform)) })

    // ---- marking ----

    /** Mark the one live rung [idx] of series [si] in group [groupId]. No-op unless idx is live. */
    fun mark(groups: List<Group>, groupId: String, si: Int, idx: Int, kind: Mark): List<Group> {
        val group = groups.find { it.id == groupId } ?: return groups
        val series = group.series.getOrNull(si) ?: return groups
        if (idx != liveIndex(series)) return groups

        val fs = series.forecasts.toMutableList()
        val st = fs[idx]
        when (kind) {
            Mark.CHECK -> {
                fs[idx] = st.copy(prevP = st.p, p = clamp(st.p + STEP), mark = Mark.CHECK, cascaded = false, burst = st.burst + 1)
            }
            Mark.CROSS -> {
                fs[idx] = st.copy(prevP = st.p, p = clamp(st.p - STEP), mark = Mark.CROSS, cascaded = false)
                for (j in idx + 1 until fs.size) {
                    val f = fs[j]
                    if (f.mark == null) fs[j] = f.copy(prevP = f.p, p = clamp(f.p - STEP), mark = Mark.CROSS, cascaded = true)
                }
            }
            Mark.NA -> {
                // N/A stops the series and cascades N/A down it, leaving every p untouched.
                fs[idx] = st.copy(mark = Mark.NA, prevP = null, cascaded = false)
                for (j in idx + 1 until fs.size) {
                    val f = fs[j]
                    if (f.mark == null) fs[j] = f.copy(mark = Mark.NA, prevP = null, cascaded = true)
                }
            }
        }
        return groups.mapGroup(groupId) { g -> g.mapSeriesAt(si) { it.copy(forecasts = fs) } }
    }

    /** Undo the source mark at [idx] and everything after it in the series. */
    fun undo(groups: List<Group>, groupId: String, si: Int, idx: Int): List<Group> {
        val group = groups.find { it.id == groupId } ?: return groups
        val series = group.series.getOrNull(si) ?: return groups
        val fs = series.forecasts.toMutableList()
        for (j in fs.size - 1 downTo idx) {
            val s = fs[j]
            if (s.mark != null) {
                fs[j] = s.copy(
                    p = if (s.prevP != null) s.prevP else s.p,
                    mark = null, prevP = null, cascaded = false, burst = 0,
                )
            }
        }
        return groups.mapGroup(groupId) { g -> g.mapSeriesAt(si) { it.copy(forecasts = fs) } }
    }

    /** Close the cycle: clear all marks; re-snapshot startP; `p` carries forward. */
    fun confirm(groups: List<Group>): List<Group> =
        groups.map { o ->
            o.copy(series = o.series.map { s ->
                s.copy(forecasts = s.forecasts.map { x ->
                    x.copy(startP = x.p, mark = null, prevP = null, cascaded = false, burst = 0)
                })
            })
        }

    // ---- editing (setup) ----

    fun setGroupName(groups: List<Group>, groupId: String, name: String): List<Group> =
        groups.mapGroup(groupId) { it.copy(name = name) }

    fun setForecastLabel(groups: List<Group>, groupId: String, forecastId: String, label: String): List<Group> =
        groups.mapGroup(groupId) { g -> g.mapAllForecasts { if (it.id == forecastId) it.copy(label = label) else it } }

    fun toggleSeriesNA(groups: List<Group>, groupId: String, seriesId: String): List<Group> =
        groups.mapGroup(groupId) { g ->
            g.copy(series = g.series.map { if (it.id == seriesId) it.copy(na = !it.na) else it })
        }

    fun toggleForecastDelete(groups: List<Group>, groupId: String, forecastId: String): List<Group> =
        groups.mapGroup(groupId) { g -> g.mapAllForecasts { if (it.id == forecastId) it.copy(deleted = !it.deleted) else it } }

    /** Adjust an estimate in edit mode by ±STEP; resets the rung's cycle state. */
    fun bumpForecast(groups: List<Group>, groupId: String, forecastId: String, d: Int): List<Group> =
        groups.mapGroup(groupId) { g ->
            g.mapAllForecasts { st ->
                if (st.id == forecastId) {
                    val np = clamp(st.p + d)
                    st.copy(p = np, startP = np, mark = null, prevP = null, cascaded = false, burst = 0)
                } else st
            }
        }

    fun deleteGroup(groups: List<Group>, groupId: String): List<Group> =
        groups.filterNot { it.id == groupId }

    fun addGroup(groups: List<Group>): List<Group> =
        groups + Group(
            id = genId(), name = "New group",
            series = listOf(Series(id = genId(), name = null, na = false, forecasts = listOf(newForecast("New forecast", BASELINE)))),
        )

    /** Add forecast always starts a NEW series at the group's foot. */
    fun addForecastNewSeries(groups: List<Group>, groupId: String): List<Group> =
        groups.mapGroup(groupId) { g ->
            g.copy(series = g.series + Series(id = genId(), name = "", na = false, forecasts = listOf(newForecast("New forecast", BASELINE))))
        }

    // ---- reorder ----

    fun moveForecastTo(groups: List<Group>, groupId: String, forecastId: String, targetSeriesId: String, idx: Int): List<Group> =
        groups.mapGroup(groupId) { g ->
            var moved: Forecast? = null
            val stripped = g.series.map { s ->
                val i = s.forecasts.indexOfFirst { it.id == forecastId }
                if (i >= 0 && moved == null) {
                    moved = s.forecasts[i]
                    s.copy(forecasts = s.forecasts.filterIndexed { fi, _ -> fi != i })
                } else s
            }
            val m = moved ?: return@mapGroup g
            g.copy(series = stripped.map { s ->
                if (s.id == targetSeriesId) {
                    val c = idx.coerceIn(0, s.forecasts.size)
                    s.copy(forecasts = s.forecasts.toMutableList().apply { add(c, m) })
                } else s
            })
        }

    fun moveGroupTo(groups: List<Group>, groupId: String, idx: Int): List<Group> {
        val i = groups.indexOfFirst { it.id == groupId }
        if (i < 0) return groups
        val list = groups.toMutableList()
        val m = list.removeAt(i)
        val c = idx.coerceIn(0, list.size)
        list.add(c, m)
        return list
    }

    fun moveSeriesTo(groups: List<Group>, groupId: String, seriesId: String, idx: Int): List<Group> =
        groups.mapGroup(groupId) { g ->
            val i = g.series.indexOfFirst { it.id == seriesId }
            if (i < 0) return@mapGroup g
            val list = g.series.toMutableList()
            val m = list.removeAt(i)
            val c = idx.coerceIn(0, list.size)
            list.add(c, m)
            g.copy(series = list)
        }

    /**
     * Commit edits on Done: prune struck forecasts + empty series, and normalize each series'
     * marks to a top prefix (a gated ladder can't keep a mark after an unmarked rung).
     */
    fun exitEditNormalize(groups: List<Group>): List<Group> =
        groups.map { o ->
            val series = o.series
                .map { s -> s.copy(forecasts = s.forecasts.filterNot { it.deleted }) }
                .filter { it.forecasts.isNotEmpty() }
                .map { s ->
                    var seenUnmarked = false
                    s.copy(forecasts = s.forecasts.map { st ->
                        when {
                            st.mark == null -> { seenUnmarked = true; st }
                            seenUnmarked -> st.copy(
                                p = if (st.prevP != null) st.prevP else st.p,
                                mark = null, prevP = null, cascaded = false, burst = 0,
                            )
                            else -> st
                        }
                    })
                }
            o.copy(series = series)
        }

    // ---- demonstration roster (spec §5): one group per distinct structure ----

    fun seed(): List<Group> {
        fun f(id: String, label: String, p: Int) = Forecast(id, label, p, p)
        fun s(id: String, name: String?, na: Boolean, forecasts: List<Forecast>) = Series(id, name, na, forecasts)
        return listOf(
            // single-forecast series
            Group("stretch", "Stretch", listOf(s("s0", null, false, listOf(f("s0a", "Stretch", 55))))),
            // climb (gated reveal + baseline divider that relocates)
            Group("practice", "Practice", listOf(s("p0", null, false, listOf(
                f("p0a", "5 min", 85), f("p0b", "15 min", 45), f("p0c", "30 min", 20),
            )))),
            // toggle set — three independent single-forecast series
            Group("meds", "Meds", listOf(
                s("m0", null, false, listOf(f("m0a", "Morning", 90))),
                s("m1", null, false, listOf(f("m1a", "Midday", 75))),
                s("m2", null, false, listOf(f("m2a", "Evening", 40))),
            )),
            // mixed + N/A — a single-forecast series plus a climb
            Group("reading", "Reading", listOf(
                s("r0", null, true, listOf(f("r0a", "A page", 60))),
                s("r1", "Deep read", true, listOf(f("r1a", "10 min", 50), f("r1b", "20 min", 30))),
            )),
        )
    }
}
