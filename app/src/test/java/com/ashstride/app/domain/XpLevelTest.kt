package com.ashstride.app.domain

import com.ashstride.app.domain.TestData.TODAY
import com.ashstride.app.domain.TestData.d
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XpLevelTest {

    // ---- XP rules -------------------------------------------------------------------------

    @Test
    fun stepsGiveOneXpPerHundredWithDailyCap() {
        assertEquals(0, XpEngine.stepsXp(99, 10_000))
        assertEquals(50, XpEngine.stepsXp(5_000, 10_000))
        assertEquals(300, XpEngine.stepsXp(30_000, 10_000))
        assertEquals(300, XpEngine.stepsXp(45_000, 10_000))
    }

    @Test
    fun goalMultiplierIsClampedHalfToDouble() {
        assertEquals(1.0, XpEngine.goalMultiplier(10_000), 0.0001)
        assertEquals(2.0, XpEngine.goalMultiplier(20_000), 0.0001)
        assertEquals(2.0, XpEngine.goalMultiplier(35_000), 0.0001)
        assertEquals(0.5, XpEngine.goalMultiplier(5_000), 0.0001)
        assertEquals(0.5, XpEngine.goalMultiplier(2_000), 0.0001)
        assertEquals(600, XpEngine.stepsXp(30_000, 20_000))
        assertEquals(600, XpEngine.stepsXp(30_000, 35_000))
        assertEquals(25, XpEngine.stepsXp(5_000, 5_000))
        assertEquals(700, XpEngine.scaled(700, 10_000))
        assertEquals(1_400, XpEngine.scaled(700, 20_000))
        assertEquals(1_050, XpEngine.scaled(700, 15_000))
    }

    @Test
    fun streakBonusGrowsWithFlameStageAndCapsAtSixty() {
        assertEquals(20, XpEngine.streakBonus(0, 0))
        assertEquals(20, XpEngine.streakBonus(4, 4))
        assertEquals(22, XpEngine.streakBonus(5, 5))
        assertEquals(40, XpEngine.streakBonus(50, 50))
        assertEquals(60, XpEngine.streakBonus(100, 100))
        assertEquals(60, XpEngine.streakBonus(118, 118))
        assertEquals(60, XpEngine.streakBonus(500, 500))
    }

    @Test
    fun daysOnTopOfAMendPaySixtyPercent() {
        assertEquals(36, XpEngine.streakBonus(118, 50))
        assertEquals(14, XpEngine.streakBonus(10, 3)) // 24 x 60% = 14.4 -> 14
    }

    @Test
    fun mendedDayPaysNothing() {
        val days = listOf(d("2026-01-01", 10_000), d("2026-01-02", 6_000, mended = true), d("2026-01-03", 10_000))
        val tl = StreakEngine.timeline(days)
        assertEquals(0, XpEngine.dayXp(tl[1]).total)
        assertTrue(XpEngine.dayXp(tl[0]).total > 0)
        // Day 3 sits on top of the mend: 100 steps-XP + 60% of the bonus at stage 0 (20 -> 12).
        assertEquals(100, XpEngine.dayXp(tl[2]).stepsXp)
        assertEquals(12, XpEngine.dayXp(tl[2]).streakXp)
    }

    @Test
    fun unmetDayStillEarnsStepXpButNoStreakBonus() {
        val tl = StreakEngine.timeline(listOf(d("2026-01-01", 6_000)))
        assertEquals(60, XpEngine.dayXp(tl[0]).stepsXp)
        assertEquals(0, XpEngine.dayXp(tl[0]).streakXp)
    }

    @Test
    fun seedLedgerXp() {
        val x = XpEngine.ledgerXp(TestData.seed())
        assertEquals(63_417L, x.steps)
        assertEquals(18_016L, x.streak)
    }

    @Test
    fun mendingJul25RemovesItsStepsAndTrimsTheStreakBonus() {
        val x = XpEngine.ledgerXp(TestData.mend(TestData.seed(), "2026-07-25"))
        assertEquals("the mended day (9,919 steps) pays no step XP", 63_318L, x.steps)
        assertEquals("the 118 days after the mend pay 60%", 17_244L, x.streak)
    }

    @Test
    fun breakdownAddsBadgesAndOtherXp() {
        val seed = TestData.seed()
        val badges = BadgeEvaluator.evaluate(seed)
        val b = XpEngine.breakdown(seed, badges, 1_000L)
        assertEquals(8_700L, b.badges)
        assertEquals(1_000L, b.other)
        assertEquals(63_417L + 18_016L + 8_700L + 1_000L, b.total)
    }

    // ---- Level curve ----------------------------------------------------------------------

    @Test
    fun curveIsConsistent() {
        assertEquals(0L, LevelCurve.xpToReach(1))
        for (level in 1..130) {
            assertEquals(
                "level $level",
                LevelCurve.xpToNext(level),
                LevelCurve.xpToReach(level + 1) - LevelCurve.xpToReach(level),
            )
        }
        assertEquals(149L, LevelCurve.xpToNext(1))
        assertEquals(89_829L, LevelCurve.xpToReach(82))
    }

    @Test
    fun levelBoundaries() {
        assertEquals(1, LevelCurve.infoFor(0).level)
        assertEquals(1, LevelCurve.infoFor(-50).level)
        assertEquals(1, LevelCurve.infoFor(148).level)
        assertEquals(2, LevelCurve.infoFor(149).level)
        val at100 = LevelCurve.xpToReach(100)
        assertEquals(99, LevelCurve.infoFor(at100 - 1).level)
        assertEquals(100, LevelCurve.infoFor(at100).level)
    }

    @Test
    fun infoReportsProgressInsideLevel() {
        val i = LevelCurve.infoFor(LevelCurve.xpToReach(40) + 100)
        assertEquals(40, i.level)
        assertEquals(100L, i.xpIntoLevel)
        assertEquals(LevelCurve.xpToNext(40), i.xpForLevel)
        assertTrue(i.fractionIntoLevel > 0f && i.fractionIntoLevel < 1f)
    }

    @Test
    fun immortalIsUncappedWithNewRankEveryTwentyFiveLevels() {
        val at100 = LevelCurve.xpToReach(100)
        val per = LevelCurve.IMMORTAL_XP_PER_LEVEL.toLong()
        val l100 = LevelCurve.infoFor(at100)
        assertEquals(Tier.IMMORTAL, l100.tier)
        assertEquals(1, l100.immortalRank)
        assertEquals("Immortal I", l100.rankTitle)
        assertEquals(1, LevelCurve.infoFor(at100 + per * 24).immortalRank) // level 124
        assertEquals(2, LevelCurve.infoFor(at100 + per * 25).immortalRank) // level 125
        assertEquals("Immortal III", LevelCurve.infoFor(at100 + per * 50).rankTitle) // level 150
        val huge = LevelCurve.infoFor(at100 + per * 1_000)
        assertEquals(1_100, huge.level)
        assertEquals(41, huge.immortalRank)
        assertEquals("Immortal XLI", huge.rankTitle)
    }

    @Test
    fun tierBoundaries() {
        assertEquals(Tier.WANDERER, Tier.forLevel(1))
        assertEquals(Tier.WANDERER, Tier.forLevel(9))
        assertEquals(Tier.PATHFINDER, Tier.forLevel(10))
        assertEquals(Tier.PATHFINDER, Tier.forLevel(19))
        assertEquals(Tier.RANGER, Tier.forLevel(20))
        assertEquals(Tier.RANGER, Tier.forLevel(34))
        assertEquals(Tier.TRAILBLAZER, Tier.forLevel(35))
        assertEquals(Tier.TRAILBLAZER, Tier.forLevel(49))
        assertEquals(Tier.VANGUARD, Tier.forLevel(50))
        assertEquals(Tier.VANGUARD, Tier.forLevel(64))
        assertEquals(Tier.WARDEN, Tier.forLevel(65))
        assertEquals(Tier.WARDEN, Tier.forLevel(79))
        assertEquals(Tier.PARAGON, Tier.forLevel(80))
        assertEquals(Tier.PARAGON, Tier.forLevel(99))
        assertEquals(Tier.IMMORTAL, Tier.forLevel(100))
    }

    @Test
    fun gemsRiseInsideATier() {
        assertEquals(0, LevelCurve.infoFor(LevelCurve.xpToReach(80)).gems)
        assertEquals(1, LevelCurve.infoFor(LevelCurve.xpToReach(85)).gems)
        assertEquals(3, LevelCurve.infoFor(LevelCurve.xpToReach(99)).gems)
        assertEquals(0, LevelCurve.infoFor(LevelCurve.xpToReach(120)).gems)
    }

    @Test
    fun romanNumerals() {
        assertEquals("I", LevelCurve.roman(1))
        assertEquals("IV", LevelCurve.roman(4))
        assertEquals("IX", LevelCurve.roman(9))
        assertEquals("XIV", LevelCurve.roman(14))
        assertEquals("XL", LevelCurve.roman(40))
    }

    // ---- The imported history -------------------------------------------------------------

    @Test
    fun importedHistoryLandsInParagonAroundLevel82() {
        val snap = Progress.compute(TestData.seed(), TODAY)
        assertEquals(90_133L, snap.xp.total)
        assertTrue("level was ${snap.level.level}", snap.level.level in 80..85)
        assertEquals(82, snap.level.level)
        assertEquals(Tier.PARAGON, snap.level.tier)
        assertEquals(89_829L + 304L, snap.xp.total)
        assertEquals(304L, snap.level.xpIntoLevel)
        assertEquals("Paragon", snap.level.rankTitle)
    }

    @Test
    fun snapshotMatchesTheIndividualEngines() {
        val snap = Progress.compute(TestData.seed(), TODAY)
        assertEquals(118, snap.streak.total)
        assertEquals(FlameLook.SOLAR, snap.flame.look)
        assertEquals(LocalDate.parse("2026-01-24"), snap.badges.first { it.badgeId == "streak_100" }.date)
    }
}
