package com.ashstride.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeCodecTest {
    private val ws: LocalDate = LocalDate.of(2026, 11, 21)

    @Test
    fun roundTripKeepsEveryField() {
        val offers = ChallengeGenerator.offersFor(ws, emptyList(), 10_000, hourlyAvailable = true)
        assertEquals(offers, ChallengeCodec.decode(ChallengeCodec.encode(offers)))
    }

    @Test
    fun roundTripWorksForALegendaryWeekAndOtherGoals() {
        // The first Monday-week that is a Legendary week (offers are always made for a week start).
        val legendary = generateSequence(LocalDate.of(2026, 11, 30)) { it.plusWeeks(1) }.first { ChallengeGenerator.isLegendaryWeek(it) }
        assertTrue(ChallengeGenerator.isLegendaryWeek(legendary))
        for (goal in listOf(5_000, 12_500, 20_000)) {
            val offers = ChallengeGenerator.offersFor(legendary, emptyList(), goal, hourlyAvailable = true)
            val back = ChallengeCodec.decode(ChallengeCodec.encode(offers))
            assertEquals(offers, back)
            assertEquals(2, back.last().shards)
        }
    }

    @Test
    fun everyTypeSurvivesTheRoundTrip() {
        val seen = HashSet<ChallengeType>()
        for (w in 0 until 40) {
            val offers = ChallengeGenerator.offersFor(ws.plusWeeks(w.toLong()), emptyList(), 10_000, hourlyAvailable = true)
            assertEquals(offers, ChallengeCodec.decode(ChallengeCodec.encode(offers)))
            for (o in offers) seen.add(o.type)
        }
        assertEquals(ChallengeType.values().toSet(), seen)
    }

    private fun card(type: ChallengeType, tier: ChallengeTier = ChallengeTier.HARD, steps: Int = 0, days: Int = 0, km: Double = 0.0, window: Int = 0) =
        Challenge("x", ws, tier, type, tier.baseXp, tier.shards, tier.rim, steps, days, km, window)

    @Test
    fun wordingForEachType() {
        assertEquals("Walk 85,000 steps between Saturday and Friday.", ChallengeText.describe(card(ChallengeType.WEEKLY_TOTAL, steps = 85_000)))
        assertEquals("Reach 14,000 steps on 3 different days.", ChallengeText.describe(card(ChallengeType.DAYS_ABOVE, steps = 14_000, days = 3)))
        assertEquals("Walk 22,000 steps in a single day.", ChallengeText.describe(card(ChallengeType.SINGLE_DAY_PEAK, steps = 22_000)))
        assertEquals("Cover 64 km this week.", ChallengeText.describe(card(ChallengeType.DISTANCE, km = 64.0)))
        assertEquals("Walk 28,000 steps over Saturday and Sunday.", ChallengeText.describe(card(ChallengeType.WEEKEND_ONLY, steps = 28_000)))
        assertEquals(
            "Walk 10,000 steps within 2 hours, in one stretch.",
            ChallengeText.describe(card(ChallengeType.CONTINUOUS_RUN, steps = 10_000, window = 120)),
        )
    }

    @Test
    fun windowAndProgressText() {
        assertEquals("one hour", ChallengeText.windowText(60))
        assertEquals("3 hours", ChallengeText.windowText(180))
        assertEquals("90 minutes", ChallengeText.windowText(90))
        val steps = card(ChallengeType.WEEKLY_TOTAL, steps = 85_000)
        assertEquals("42,000 / 85,000 steps", ChallengeText.progress(steps, ChallengeProgress(42_000.0, 85_000.0)))
        val days = card(ChallengeType.DAYS_ABOVE, steps = 14_000, days = 4)
        assertEquals("2 / 4 days", ChallengeText.progress(days, ChallengeProgress(2.0, 4.0)))
        val km = card(ChallengeType.DISTANCE, km = 64.0)
        assertEquals("12.3 / 64 km", ChallengeText.progress(km, ChallengeProgress(12.34, 64.0)))
    }

    @Test
    fun rewardText() {
        assertEquals("+300 XP + badge", ChallengeText.rewardText(card(ChallengeType.WEEKLY_TOTAL, ChallengeTier.EASY)))
        assertEquals("+1,500 XP + badge + 1 shard", ChallengeText.rewardText(card(ChallengeType.WEEKLY_TOTAL, ChallengeTier.HARD)))
        assertEquals("+3,000 XP + badge + 2 shards", ChallengeText.rewardText(card(ChallengeType.WEEKLY_TOTAL, ChallengeTier.LEGENDARY)))
    }
}
