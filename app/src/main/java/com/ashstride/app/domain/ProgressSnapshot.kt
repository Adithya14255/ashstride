package com.ashstride.app.domain

import java.time.LocalDate

/** Everything the UI needs, computed in one go from the ledger. The UI never contains rules. */
data class ProgressSnapshot(
    val streak: StreakSummary,
    val flame: FlameInfo,
    val badges: List<BadgeAward>,
    val xp: XpBreakdown,
    val level: LevelInfo,
)

object Progress {
    /** @param otherXp challenge XP and other events from the xp_event table. */
    fun compute(days: List<LedgerDay>, today: LocalDate, otherXp: Long = 0L): ProgressSnapshot {
        val streak = StreakEngine.summary(days, today)
        val badges = BadgeEvaluator.evaluate(days)
        val xp = XpEngine.breakdown(days, badges, otherXp)
        return ProgressSnapshot(
            streak = streak,
            flame = FlameEngine.info(streak.total, streak.runHasMend),
            badges = badges,
            xp = xp,
            level = LevelCurve.infoFor(xp.total),
        )
    }
}
