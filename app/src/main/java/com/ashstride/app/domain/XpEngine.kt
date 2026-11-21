package com.ashstride.app.domain

data class DayXp(val stepsXp: Int, val streakXp: Int) {
    val total: Int get() = stepsXp + streakXp
}

data class LedgerXp(val steps: Long, val streak: Long) {
    val total: Long get() = steps + streak
}

data class XpBreakdown(
    val steps: Long,
    val streak: Long,
    val badges: Long,
    /** Challenge XP and anything else recorded in the xp_event table. */
    val other: Long,
) {
    val total: Long get() = steps + streak + badges + other
}

object XpEngine {
    const val STEPS_PER_XP = 100
    const val MAX_STEPS_COUNTED = 30_000
    const val STREAK_BONUS_BASE = 20
    const val STREAK_BONUS_PER_STAGE = 2
    const val STREAK_BONUS_MAX = 60
    /** Percent of the streak bonus paid on days that only exist because of a mend. */
    const val MENDED_PERCENT = 60

    private const val GOAL_UNIT = 10_000
    private const val GOAL_MIN = 5_000   // 0.5x
    private const val GOAL_MAX = 20_000  // 2x

    /** goal / 10,000, clamped to 0.5x..2x. */
    fun goalMultiplier(goal: Int): Double = goal.coerceIn(GOAL_MIN, GOAL_MAX).toDouble() / GOAL_UNIT

    /** Scales an XP or reward amount by the goal (integer maths, rounds down). */
    fun scaled(value: Int, goal: Int): Int =
        (value.toLong() * goal.coerceIn(GOAL_MIN, GOAL_MAX) / GOAL_UNIT).toInt()

    /** 1 XP per 100 steps, at most 30,000 steps counted per day, times the goal multiplier. */
    fun stepsXp(steps: Int, goal: Int): Int {
        val counted = if (steps < 0) 0 else if (steps > MAX_STEPS_COUNTED) MAX_STEPS_COUNTED else steps
        return scaled(counted / STEPS_PER_XP, goal)
    }

    /**
     * Daily streak bonus = min(20 + 2 x flame stage, 60).
     * Pure days pay 100%; natural days that sit on top of a mend (pure < total) pay 60%.
     */
    fun streakBonus(totalStreak: Int, pureStreak: Int): Int {
        val stage = if (totalStreak < 0) 0 else totalStreak / FlameEngine.DAYS_PER_STAGE
        val full = minOf(STREAK_BONUS_BASE + STREAK_BONUS_PER_STAGE * stage, STREAK_BONUS_MAX)
        return if (pureStreak == totalStreak) full else full * MENDED_PERCENT / 100
    }

    /** A mended day counts for streak and badges only: no steps, no XP. */
    fun dayXp(state: DayStreakState): DayXp {
        val d = state.day
        if (d.mendedOnly) return DayXp(0, 0)
        val steps = stepsXp(d.steps, d.goal)
        val streak = if (d.met) streakBonus(state.total, state.pure) else 0
        return DayXp(steps, streak)
    }

    fun ledgerXp(days: List<LedgerDay>): LedgerXp {
        var steps = 0L
        var streak = 0L
        for (s in StreakEngine.timeline(days)) {
            val x = dayXp(s)
            steps += x.stepsXp
            streak += x.streakXp
        }
        return LedgerXp(steps, streak)
    }

    fun breakdown(days: List<LedgerDay>, badges: List<BadgeAward>, otherXp: Long = 0L): XpBreakdown {
        val ledger = ledgerXp(days)
        var badgeXp = 0L
        for (b in badges) badgeXp += b.xp
        return XpBreakdown(ledger.steps, ledger.streak, badgeXp, otherXp)
    }
}
