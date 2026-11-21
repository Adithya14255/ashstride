package com.ashstride.app.domain

import java.time.LocalDate

enum class BadgeVariant { PURE, MENDED, STANDARD }

data class BadgeKey(val badgeId: String, val variant: BadgeVariant)

data class BadgeAward(
    val badgeId: String,
    val variant: BadgeVariant,
    /** The real date the badge was earned (retroactive for imported history). */
    val date: LocalDate,
    val xp: Int,
) {
    val key: BadgeKey get() = BadgeKey(badgeId, variant)
}

/**
 * Pure and idempotent: the full set of badges is recomputed from the ledger every time,
 * so mends and imports can never leave it inconsistent. Callers diff against badge_unlock.
 *
 * Streak milestones come in two variants:
 *  - PURE: the natural-day counter reached the milestone (full XP).
 *  - MENDED: the total counter reached it while the pure counter had not (60% XP).
 * Lifetime step milestones are STANDARD.
 */
object BadgeEvaluator {
    val STREAK_MILESTONES: List<Int> = listOf(7, 14, 30, 50, 100, 150, 200, 300, 365, 500, 730, 1000)
    val STEP_MILESTONES: List<Long> =
        listOf(100_000L, 250_000L, 500_000L, 1_000_000L, 2_500_000L, 5_000_000L, 10_000_000L)

    const val MENDED_PERCENT = 60

    fun streakBadgeId(milestone: Int): String = "streak_$milestone"

    fun stepsBadgeId(milestone: Long): String = "steps_$milestone"

    private fun pureStreakXp(milestone: Int): Int = when (milestone) {
        7 -> 100
        14 -> 200
        30 -> 500
        50 -> 800
        100 -> 1_500
        150 -> 2_000
        200 -> 2_500
        300 -> 3_500
        365 -> 5_000
        500 -> 6_500
        730 -> 9_000
        1000 -> 12_000
        else -> 0
    }

    fun streakXp(milestone: Int, variant: BadgeVariant): Int {
        val pure = pureStreakXp(milestone)
        return if (variant == BadgeVariant.MENDED) (pure * MENDED_PERCENT + 50) / 100 else pure
    }

    fun stepsXp(milestone: Long): Int = when (milestone) {
        100_000L -> 150
        250_000L -> 250
        500_000L -> 400
        1_000_000L -> 800
        2_500_000L -> 1_500
        5_000_000L -> 2_500
        10_000_000L -> 4_000
        else -> 0
    }

    /** Every badge the ledger has earned, each with the first date it was earned, oldest first. */
    fun evaluate(days: List<LedgerDay>): List<BadgeAward> {
        val earned = LinkedHashMap<BadgeKey, BadgeAward>()
        var cumulativeSteps = 0L
        for (s in StreakEngine.timeline(days)) {
            val date = s.day.date
            for (n in STREAK_MILESTONES) {
                if (s.pure == n) {
                    add(earned, BadgeAward(streakBadgeId(n), BadgeVariant.PURE, date, streakXp(n, BadgeVariant.PURE)))
                } else if (s.total == n) {
                    add(earned, BadgeAward(streakBadgeId(n), BadgeVariant.MENDED, date, streakXp(n, BadgeVariant.MENDED)))
                }
            }
            cumulativeSteps += s.day.steps
            for (m in STEP_MILESTONES) {
                if (cumulativeSteps >= m) {
                    add(earned, BadgeAward(stepsBadgeId(m), BadgeVariant.STANDARD, date, stepsXp(m)))
                }
            }
        }
        return earned.values.sortedWith(compareBy<BadgeAward>({ it.date }, { it.badgeId }))
    }

    /** Badges earned by the ledger that are not yet in [unlocked]. Safe to call repeatedly. */
    fun newlyEarned(days: List<LedgerDay>, unlocked: Set<BadgeKey>): List<BadgeAward> =
        evaluate(days).filter { it.key !in unlocked }

    fun totalXp(awards: List<BadgeAward>): Long {
        var sum = 0L
        for (a in awards) sum += a.xp
        return sum
    }

    private fun add(map: MutableMap<BadgeKey, BadgeAward>, award: BadgeAward) {
        if (!map.containsKey(award.key)) map[award.key] = award
    }
}
