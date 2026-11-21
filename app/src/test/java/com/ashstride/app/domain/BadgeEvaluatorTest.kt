package com.ashstride.app.domain

import com.ashstride.app.domain.TestData.TODAY
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeEvaluatorTest {

    private fun List<BadgeAward>.award(id: String, variant: BadgeVariant): BadgeAward? =
        firstOrNull { it.badgeId == id && it.variant == variant }

    private fun date(s: String): LocalDate = LocalDate.parse(s)

    @Test
    fun seedGrantsMilestonesRetroactivelyWithRealDates() {
        val badges = BadgeEvaluator.evaluate(TestData.seed())
        assertEquals(date("2025-09-05"), badges.award("streak_7", BadgeVariant.PURE)?.date)
        assertEquals(date("2025-09-12"), badges.award("streak_14", BadgeVariant.PURE)?.date)
        assertEquals(date("2025-11-15"), badges.award("streak_30", BadgeVariant.PURE)?.date)
        assertEquals(date("2025-12-05"), badges.award("streak_50", BadgeVariant.PURE)?.date)
        assertEquals(date("2026-01-24"), badges.award("streak_100", BadgeVariant.PURE)?.date)
        assertNull("best run is 132, so 150 is not earned yet", badges.award("streak_150", BadgeVariant.PURE))
        assertTrue(badges.none { it.variant == BadgeVariant.MENDED })
    }

    @Test
    fun seedGrantsLifetimeStepBadges() {
        val badges = BadgeEvaluator.evaluate(TestData.seed())
        assertEquals(date("2025-07-18"), badges.award("steps_100000", BadgeVariant.STANDARD)?.date)
        assertEquals(date("2025-08-04"), badges.award("steps_250000", BadgeVariant.STANDARD)?.date)
        assertEquals(date("2025-08-27"), badges.award("steps_500000", BadgeVariant.STANDARD)?.date)
        assertEquals(date("2025-09-30"), badges.award("steps_1000000", BadgeVariant.STANDARD)?.date)
        assertEquals(date("2026-01-17"), badges.award("steps_2500000", BadgeVariant.STANDARD)?.date)
        assertEquals(date("2026-07-31"), badges.award("steps_5000000", BadgeVariant.STANDARD)?.date)
        assertNull(badges.award("steps_10000000", BadgeVariant.STANDARD))
    }

    @Test
    fun seedBadgeXpAndOrder() {
        val badges = BadgeEvaluator.evaluate(TestData.seed())
        assertEquals(11, badges.size)
        assertEquals(8_700L, BadgeEvaluator.totalXp(badges))
        for (i in 1 until badges.size) {
            assertFalse("sorted oldest first", badges[i].date.isBefore(badges[i - 1].date))
        }
    }

    @Test
    fun mendedVariantsPayAboutSixtyPercent() {
        assertEquals(1_500, BadgeEvaluator.streakXp(100, BadgeVariant.PURE))
        assertEquals(900, BadgeEvaluator.streakXp(100, BadgeVariant.MENDED))
        assertEquals(1_200, BadgeEvaluator.streakXp(150, BadgeVariant.MENDED))
        assertEquals(120, BadgeEvaluator.streakXp(14, BadgeVariant.MENDED))
        assertEquals(60, BadgeEvaluator.streakXp(7, BadgeVariant.MENDED))
    }

    @Test
    fun jul25MendEarnsMendedSilverStitchedBadges() {
        val badges = BadgeEvaluator.evaluate(TestData.mend(TestData.seed(), "2026-07-25"))
        val b150 = badges.award("streak_150", BadgeVariant.MENDED)
        val b200 = badges.award("streak_200", BadgeVariant.MENDED)
        assertNotNull(b150)
        assertNotNull(b200)
        assertEquals(date("2026-08-11"), b150?.date)
        assertEquals(date("2026-09-30"), b200?.date)
        assertEquals(1_200, b150?.xp)
        assertEquals(1_500, b200?.xp)
        assertNull("natural run after the mend is only 118", badges.award("streak_150", BadgeVariant.PURE))
        // Earlier pure badges keep their real dates.
        assertEquals(date("2026-01-24"), badges.award("streak_100", BadgeVariant.PURE)?.date)
    }

    @Test
    fun fiveMendsEarnLongMendedMilestones() {
        val badges = BadgeEvaluator.evaluate(TestData.mend(TestData.seed(), *TestData.FIVE_MENDS))
        assertEquals(date("2026-07-16"), badges.award("streak_300", BadgeVariant.MENDED)?.date)
        assertEquals(date("2026-09-19"), badges.award("streak_365", BadgeVariant.MENDED)?.date)
        assertEquals(2_100, badges.award("streak_300", BadgeVariant.MENDED)?.xp)
        assertEquals(3_000, badges.award("streak_365", BadgeVariant.MENDED)?.xp)
    }

    @Test
    fun pureAndMendedVariantsCanBothBeEarnedForTheSameMilestone() {
        // Six natural days, a mended seventh, then seven more natural days.
        val start = date("2026-01-01")
        val days = ArrayList<LedgerDay>()
        for (i in 0 until 14) {
            val mended = i == 6
            days.add(LedgerDay(start.plusDays(i.toLong()), if (mended) 5_000 else 10_000, 0.0, 10_000, mended))
        }
        val badges = BadgeEvaluator.evaluate(days)
        assertEquals(date("2026-01-07"), badges.award("streak_7", BadgeVariant.MENDED)?.date)
        assertEquals(date("2026-01-14"), badges.award("streak_7", BadgeVariant.PURE)?.date)
        assertEquals(date("2026-01-14"), badges.award("streak_14", BadgeVariant.MENDED)?.date)
        assertNull(badges.award("streak_14", BadgeVariant.PURE))
    }

    @Test
    fun evaluationIsIdempotent() {
        val seed = TestData.seed()
        val first = BadgeEvaluator.evaluate(seed)
        assertEquals(first, BadgeEvaluator.evaluate(seed))

        val all = first.map { it.key }.toSet()
        assertTrue(BadgeEvaluator.newlyEarned(seed, all).isEmpty())
        assertEquals(first, BadgeEvaluator.newlyEarned(seed, emptySet()))

        val missingOne = all - BadgeKey("streak_50", BadgeVariant.PURE)
        val again = BadgeEvaluator.newlyEarned(seed, missingOne)
        assertEquals(1, again.size)
        assertEquals("streak_50", again[0].badgeId)
    }

    @Test
    fun badgeEarnedAfterNewDaysArrivesOnlyOnce() {
        val seed = TestData.seed()
        val before = BadgeEvaluator.evaluate(seed).map { it.key }.toSet()
        // 6,406,335 + 4,000,000 passes the 10M lifetime-step badge.
        val extra = ArrayList(seed)
        extra.add(LedgerDay(TODAY, 4_000_000, 3_000.0, 10_000))
        val fresh = BadgeEvaluator.newlyEarned(extra, before)
        assertEquals(1, fresh.size)
        assertEquals("steps_10000000", fresh[0].badgeId)
        assertEquals(TODAY, fresh[0].date)
    }
}
