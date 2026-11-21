package com.ashstride.app.domain

import java.time.LocalDate

enum class DayStatus { MET, MENDED, MISSED }

enum class MendCheck { OK, FUTURE, ALREADY_MET, ALREADY_MENDED, BELOW_FLOOR }

/** Streak counters as they stood at the end of [day]. */
data class DayStreakState(val day: LedgerDay, val pure: Int, val total: Int)

data class StreakSummary(
    /** Consecutive natural days (a mended day resets this counter). */
    val pure: Int,
    /** Consecutive days including mended ones. */
    val total: Int,
    val bestPure: Int,
    val bestTotal: Int,
    /** The current run contains at least one mended day (drives the gold-thread seam on the flame). */
    val runHasMend: Boolean,
    /** A streak is alive but today has not been completed yet. */
    val atRisk: Boolean,
)

data class MendPreview(
    val check: MendCheck,
    val totalBefore: Int,
    val totalAfter: Int,
) {
    val allowed: Boolean get() = check == MendCheck.OK
}

object StreakEngine {

    /** A Mender may only fix a day that reached this many steps (25% of that day's goal). */
    fun mendFloor(goal: Int): Int = goal / 4

    fun statusOf(day: LedgerDay): DayStatus =
        if (day.met) DayStatus.MET else if (day.mended) DayStatus.MENDED else DayStatus.MISSED

    /**
     * Walks the ledger oldest to newest and records both counters after every day.
     * A day with no row breaks the run, exactly like a missed day.
     */
    fun timeline(days: List<LedgerDay>): List<DayStreakState> {
        val sorted = days.sortedBy { it.date }
        val out = ArrayList<DayStreakState>(sorted.size)
        var pure = 0
        var total = 0
        var prev: LocalDate? = null
        for (d in sorted) {
            val p = prev
            if (p != null && d.date != p.plusDays(1)) {
                pure = 0
                total = 0
            }
            if (d.met) {
                pure += 1
                total += 1
            } else if (d.mended) {
                pure = 0
                total += 1
            } else {
                pure = 0
                total = 0
            }
            out.add(DayStreakState(d, pure, total))
            prev = d.date
        }
        return out
    }

    /**
     * Current and best streaks as of [today].
     * Today is still in progress: if it has not counted yet, the streak carries on from
     * yesterday (and is flagged [StreakSummary.atRisk]) instead of dropping to zero.
     */
    fun summary(days: List<LedgerDay>, today: LocalDate): StreakSummary {
        val tl = timeline(days.filter { !it.date.isAfter(today) })
        var bestPure = 0
        var bestTotal = 0
        for (s in tl) {
            if (s.pure > bestPure) bestPure = s.pure
            if (s.total > bestTotal) bestTotal = s.total
        }
        var pure = 0
        var total = 0
        var atRisk = false
        val last = tl.lastOrNull()
        if (last != null) {
            if (last.day.date == today) {
                if (last.day.counts) {
                    pure = last.pure
                    total = last.total
                } else {
                    val y = if (tl.size >= 2) tl[tl.size - 2] else null
                    if (y != null && y.day.date == today.minusDays(1)) {
                        pure = y.pure
                        total = y.total
                        atRisk = total > 0
                    }
                }
            } else if (last.day.date == today.minusDays(1)) {
                pure = last.pure
                total = last.total
                atRisk = total > 0
            }
        }
        return StreakSummary(
            pure = pure,
            total = total,
            bestPure = bestPure,
            bestTotal = bestTotal,
            runHasMend = total != pure,
            atRisk = atRisk,
        )
    }

    fun checkMend(days: List<LedgerDay>, date: LocalDate, today: LocalDate): MendCheck {
        if (date.isAfter(today)) return MendCheck.FUTURE
        val day = days.firstOrNull { it.date == date } ?: return MendCheck.BELOW_FLOOR
        if (day.mended) return MendCheck.ALREADY_MENDED
        if (day.met) return MendCheck.ALREADY_MET
        if (day.steps < mendFloor(day.goal)) return MendCheck.BELOW_FLOOR
        return MendCheck.OK
    }

    /** The ledger as it would look after mending [date]. Used for previews and by the real mend flow. */
    fun withMend(days: List<LedgerDay>, date: LocalDate): List<LedgerDay> =
        days.map { if (it.date == date) it.copy(mended = true) else it }

    /** "Total streak 118 -> 251": what the app shows before the person confirms. */
    fun previewMend(days: List<LedgerDay>, date: LocalDate, today: LocalDate): MendPreview {
        val before = summary(days, today).total
        val check = checkMend(days, date, today)
        val after = if (check == MendCheck.OK) summary(withMend(days, date), today).total else before
        return MendPreview(check, before, after)
    }
}

/** Resource chain: 3 Mender Shards make 1 Mender, and at most 2 Menders can be held. */
data class MenderStash(val shards: Int, val menders: Int)

object MenderRules {
    const val SHARDS_PER_MENDER = 3
    const val MAX_MENDERS = 2

    /** Shards from Hard (1) and Legendary (2) weekly challenges. */
    fun addShards(stash: MenderStash, count: Int): MenderStash {
        var shards = stash.shards + count
        var menders = stash.menders
        while (shards >= SHARDS_PER_MENDER && menders < MAX_MENDERS) {
            shards -= SHARDS_PER_MENDER
            menders += 1
        }
        return MenderStash(shards, menders)
    }

    fun canUse(stash: MenderStash): Boolean = stash.menders > 0

    /** Spends one Mender; shards waiting behind the cap convert straight away. */
    fun use(stash: MenderStash): MenderStash {
        if (stash.menders <= 0) return stash
        return addShards(MenderStash(stash.shards, stash.menders - 1), 0)
    }
}
