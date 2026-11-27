package com.ashstride.app.domain

import java.time.LocalDate

/** The three ladders of great feats. A feat is a single big effort, whenever it happens. */
enum class FeatLadder(val label: String) {
    DAY_STEPS("Steps in one day"),
    DAY_DISTANCE("Distance in one day"),
    WEEK_STEPS("Steps in one week"),
}

/** One rung of a ladder. [threshold] is steps for the step ladders and kilometres for the distance ladder. */
data class FeatDef(
    val id: String,
    val ladder: FeatLadder,
    /** 0 = lowest rung of its ladder. */
    val rung: Int,
    val threshold: Double,
    val title: String,
    /** Short text on the badge: "30k", "15mi". */
    val glyph: String,
    val rim: BadgeRim,
    /** Honor the first time this rung is reached. Repeats pay less (see [HonorRules.repeatHonor]). */
    val honor: Int,
) {
    /** "30,000 steps in one day", "24 km in one day", "120,000 steps in one week". */
    val description: String
        get() = when (ladder) {
            FeatLadder.DAY_STEPS -> String.format(java.util.Locale.US, "%,d steps in one day", threshold.toInt())
            FeatLadder.DAY_DISTANCE -> String.format(java.util.Locale.US, "%s km in one day", kmText(threshold))
            FeatLadder.WEEK_STEPS -> String.format(java.util.Locale.US, "%,d steps in one week", threshold.toInt())
        }

    private fun kmText(v: Double): String = if (v == Math.floor(v)) v.toInt().toString() else String.format(java.util.Locale.US, "%.1f", v)
}

/** A rung that has been reached at least once. [times] counts every day (or week) that reached it. */
data class FeatRecord(
    val def: FeatDef,
    val firstDate: LocalDate,
    val times: Int,
    /** Honor this rung has paid so far, first time plus diminishing repeats. */
    val honorEarned: Int,
)

data class FeatSummary(
    val records: List<FeatRecord>,
    val honorEarned: Int,
    /** Best single day of steps, best single day of km and best week of steps, over the whole ledger. */
    val bestDaySteps: Double,
    val bestDayKm: Double,
    val bestWeekSteps: Double,
) {
    fun best(ladder: FeatLadder): Double = when (ladder) {
        FeatLadder.DAY_STEPS -> bestDaySteps
        FeatLadder.DAY_DISTANCE -> bestDayKm
        FeatLadder.WEEK_STEPS -> bestWeekSteps
    }

    fun has(id: String): Boolean = records.any { it.def.id == id }

    companion object {
        val EMPTY = FeatSummary(emptyList(), 0, 0.0, 0.0, 0.0)
    }
}

/**
 * Great feats: ledger-derived like badges, so they are recomputed (never counted) and survive imports, mends and
 * restores. Each rung gives a badge the first time and honor every time it is reached. Honor for repeats halves
 * each time but never drops below a guaranteed floor (one fifth of the first-time value).
 *
 * Only the highest rung reached on a day pays a repeat, so one 50,000-step day does not pay four rungs again; a lower
 * rung that has never been reached before is still unlocked (and paid once) by that day.
 */
object FeatEngine {
    private const val KM_15_MILES = 24.14
    private const val KM_MARATHON = 42.195

    val DEFS: List<FeatDef> = listOf(
        FeatDef("feat_day_steps_20000", FeatLadder.DAY_STEPS, 0, 20_000.0, "Long Day", "20k", BadgeRim.BRONZE, 10),
        FeatDef("feat_day_steps_30000", FeatLadder.DAY_STEPS, 1, 30_000.0, "Tireless", "30k", BadgeRim.SILVER, 25),
        FeatDef("feat_day_steps_40000", FeatLadder.DAY_STEPS, 2, 40_000.0, "Ironwalker", "40k", BadgeRim.GOLD, 60),
        FeatDef("feat_day_steps_50000", FeatLadder.DAY_STEPS, 3, 50_000.0, "Legend of the Road", "50k", BadgeRim.PRISMATIC, 150),

        FeatDef("feat_day_km_15", FeatLadder.DAY_DISTANCE, 0, 15.0, "Far Walker", "15km", BadgeRim.BRONZE, 10),
        FeatDef("feat_day_km_24", FeatLadder.DAY_DISTANCE, 1, KM_15_MILES, "Fifteen Miles", "15mi", BadgeRim.SILVER, 25),
        FeatDef("feat_day_km_35", FeatLadder.DAY_DISTANCE, 2, 35.0, "Pilgrim's Day", "35km", BadgeRim.GOLD, 60),
        FeatDef("feat_day_km_42", FeatLadder.DAY_DISTANCE, 3, KM_MARATHON, "The Marathon", "42km", BadgeRim.PRISMATIC, 150),

        FeatDef("feat_week_steps_100000", FeatLadder.WEEK_STEPS, 0, 100_000.0, "Century Week", "100k", BadgeRim.BRONZE, 10),
        FeatDef("feat_week_steps_120000", FeatLadder.WEEK_STEPS, 1, 120_000.0, "Wayfarer's Week", "120k", BadgeRim.SILVER, 25),
        FeatDef("feat_week_steps_140000", FeatLadder.WEEK_STEPS, 2, 140_000.0, "Iron Week", "140k", BadgeRim.GOLD, 60),
        FeatDef("feat_week_steps_170000", FeatLadder.WEEK_STEPS, 3, 170_000.0, "Mythic Week", "170k", BadgeRim.PRISMATIC, 150),
    )

    fun rungs(ladder: FeatLadder): List<FeatDef> = DEFS.filter { it.ladder == ladder }.sortedBy { it.rung }

    /** The lowest rung of [ladder] that has not been reached yet, or null when the ladder is complete. */
    fun nextRung(summary: FeatSummary, ladder: FeatLadder): FeatDef? =
        rungs(ladder).firstOrNull { !summary.has(it.id) }

    fun evaluate(days: List<LedgerDay>): FeatSummary {
        val sorted = days.sortedBy { it.date }
        val daySteps = sorted.map { it.date to it.steps.toDouble() }
        val dayKm = sorted.map { it.date to it.distanceKm }
        val weeks = LinkedHashMap<LocalDate, Double>()
        for (d in sorted) {
            val ws = ChallengeGenerator.weekStartFor(d.date)
            weeks[ws] = (weeks[ws] ?: 0.0) + d.steps
        }
        val weekSteps = weeks.entries.sortedBy { it.key }.map { it.key to it.value }

        val records = ArrayList<FeatRecord>()
        var honor = 0
        honor += walk(FeatLadder.DAY_STEPS, daySteps, records)
        honor += walk(FeatLadder.DAY_DISTANCE, dayKm, records)
        honor += walk(FeatLadder.WEEK_STEPS, weekSteps, records)
        return FeatSummary(
            records = records.sortedWith(compareBy<FeatRecord>({ it.firstDate }, { it.def.id })),
            honorEarned = honor,
            bestDaySteps = daySteps.maxOfOrNull { it.second } ?: 0.0,
            bestDayKm = dayKm.maxOfOrNull { it.second } ?: 0.0,
            bestWeekSteps = weekSteps.maxOfOrNull { it.second } ?: 0.0,
        )
    }

    private fun walk(ladder: FeatLadder, observations: List<Pair<LocalDate, Double>>, out: MutableList<FeatRecord>): Int {
        val defs = rungs(ladder)
        val first = arrayOfNulls<LocalDate>(defs.size)
        val times = IntArray(defs.size)
        val paid = IntArray(defs.size)
        val honor = IntArray(defs.size)
        for ((date, value) in observations) {
            var top = -1
            for (i in defs.indices) if (value >= defs[i].threshold) top = i
            if (top < 0) continue
            for (r in 0..top) {
                times[r] += 1
                if (first[r] == null) {
                    first[r] = date
                    paid[r] = 1
                    honor[r] += defs[r].honor
                } else if (r == top) {
                    paid[r] += 1
                    honor[r] += HonorRules.repeatHonor(defs[r].honor, paid[r])
                }
            }
        }
        var total = 0
        for (i in defs.indices) {
            val f = first[i] ?: continue
            out.add(FeatRecord(defs[i], f, times[i], honor[i]))
            total += honor[i]
        }
        return total
    }
}

/** Honor is the proof of effort that feats pay. It buys Mender Shards at the shrine. */
object HonorRules {
    /** Honor for one Mender Shard. */
    const val SHARD_COST = 100

    /** At most this many shards can be bought per challenge week (Monday to Sunday), so Menders stay hard to earn. */
    const val WEEKLY_LIMIT = 2

    /** Share of the first-time honor that every repeat still pays at minimum, as a divisor (1/5). */
    private const val FLOOR_DIVISOR = 5

    /** Honor for the [occurrence]-th time a rung pays (1 = first time). Halves each time, never below the floor. */
    fun repeatHonor(base: Int, occurrence: Int): Int {
        if (occurrence <= 1) return base
        val floor = maxOf(1, base / FLOOR_DIVISOR)
        var v = base
        var n = 1
        while (n < occurrence && v > floor) {
            v /= 2
            n += 1
        }
        return maxOf(floor, v)
    }

    fun balance(earned: Int, spent: Int): Int = (earned - spent).coerceAtLeast(0)

    fun canRedeem(balance: Int, redeemedThisWeek: Int): Boolean =
        balance >= SHARD_COST && redeemedThisWeek < WEEKLY_LIMIT
}
