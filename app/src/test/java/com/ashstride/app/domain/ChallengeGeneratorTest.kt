package com.ashstride.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeGeneratorTest {

    private fun date(s: String): LocalDate = LocalDate.parse(s)

    /** Offers for [weeks] consecutive weeks starting at the anchor Saturday. */
    private fun manyWeeks(goal: Int, hourly: Boolean, weeks: Int = 40): List<Challenge> {
        val out = ArrayList<Challenge>()
        for (w in 0 until weeks) {
            val start = ChallengeGenerator.ANCHOR_SATURDAY.plusWeeks(w.toLong())
            out.addAll(ChallengeGenerator.offersFor(start, emptyList(), goal, hourly))
        }
        return out
    }

    private fun List<Challenge>.pick(tier: ChallengeTier, type: ChallengeType): Challenge =
        first { it.tier == tier && it.type == type }

    private fun steadyWeeks(before: LocalDate, weeks: Int, stepsPerDay: Int): List<LedgerDay> {
        val out = ArrayList<LedgerDay>()
        val first = before.minusWeeks(weeks.toLong())
        for (i in 0 until weeks * 7) {
            out.add(LedgerDay(first.plusDays(i.toLong()), stepsPerDay, stepsPerDay * 0.00075, 10_000))
        }
        return out
    }

    // ---- Calendar -------------------------------------------------------------------------

    @Test
    fun weeksRunMondayToSundayFromTheCutover() {
        val mon = date("2026-11-30")
        assertEquals(mon, ChallengeGenerator.weekStartFor(mon))
        assertEquals(mon, ChallengeGenerator.weekStartFor(date("2026-12-03")))
        assertEquals(mon, ChallengeGenerator.weekStartFor(date("2026-12-06"))) // Sun
        assertEquals(date("2026-12-07"), ChallengeGenerator.weekStartFor(date("2026-12-07")))
        assertEquals(date("2026-12-06"), ChallengeGenerator.weekEndFor(mon))
        // The two days between the old Friday and the first Monday belong to the bridge week.
        assertEquals(date("2026-11-21"), ChallengeGenerator.weekStartFor(date("2026-11-28")))
        assertEquals(date("2026-11-21"), ChallengeGenerator.weekStartFor(date("2026-11-29")))
        assertEquals(date("2026-11-29"), ChallengeGenerator.weekEndFor(date("2026-11-21")))
    }

    @Test
    fun weekendIsTheLastTwoDaysOfANewWeekAndTheFirstTwoOfAnOldOne() {
        assertEquals(listOf(date("2026-12-05"), date("2026-12-06")), ChallengeGenerator.weekendDays(date("2026-11-30")))
        assertEquals(listOf(date("2026-11-21"), date("2026-11-22")), ChallengeGenerator.weekendDays(date("2026-11-21")))
        assertEquals(listOf(date("2026-11-14"), date("2026-11-15")), ChallengeGenerator.weekendDays(date("2026-11-14")))
    }

    @Test
    fun oldWeeksRanSaturdayToFriday() {
        assertEquals(date("2026-11-21"), ChallengeGenerator.weekStartFor(date("2026-11-21"))) // Sat
        assertEquals(date("2026-11-21"), ChallengeGenerator.weekStartFor(date("2026-11-22"))) // Sun
        assertEquals(date("2026-11-14"), ChallengeGenerator.weekStartFor(date("2026-11-16"))) // Mon
        assertEquals(date("2026-11-14"), ChallengeGenerator.weekStartFor(date("2026-11-20"))) // Fri
        assertEquals(date("2026-11-20"), ChallengeGenerator.weekEndFor(date("2026-11-14")))
        assertEquals(java.time.DayOfWeek.SATURDAY, ChallengeGenerator.ANCHOR_SATURDAY.dayOfWeek)
    }

    @Test
    fun everyFourthWeekIsLegendary() {
        val anchor = ChallengeGenerator.ANCHOR_SATURDAY
        assertFalse(ChallengeGenerator.isLegendaryWeek(anchor))
        assertFalse(ChallengeGenerator.isLegendaryWeek(anchor.plusWeeks(1)))
        assertFalse(ChallengeGenerator.isLegendaryWeek(anchor.plusWeeks(2)))
        assertTrue(ChallengeGenerator.isLegendaryWeek(anchor.plusWeeks(3)))
        assertFalse(ChallengeGenerator.isLegendaryWeek(anchor.plusWeeks(4)))
        assertTrue(ChallengeGenerator.isLegendaryWeek(anchor.plusWeeks(7)))
        assertEquals(72, ChallengeGenerator.weekIndex(date("2026-11-21")))
    }

    // ---- Offers ---------------------------------------------------------------------------

    @Test
    fun threeCardsEasyMediumAndHardOrLegendary() {
        val normal = ChallengeGenerator.offersFor(date("2026-11-21"), emptyList(), 10_000)
        assertEquals(
            listOf(ChallengeTier.EASY, ChallengeTier.MEDIUM, ChallengeTier.HARD),
            normal.map { it.tier },
        )
        val legendaryWeek = ChallengeGenerator.ANCHOR_SATURDAY.plusWeeks(3)
        val special = ChallengeGenerator.offersFor(legendaryWeek, emptyList(), 10_000)
        assertEquals(
            listOf(ChallengeTier.EASY, ChallengeTier.MEDIUM, ChallengeTier.LEGENDARY),
            special.map { it.tier },
        )
    }

    @Test
    fun sameInputsAlwaysGiveTheSameCards() {
        val seed = TestData.seed()
        val a = ChallengeGenerator.offersFor(date("2026-11-21"), seed, 10_000)
        val b = ChallengeGenerator.offersFor(date("2026-11-21"), seed, 10_000)
        assertEquals(a, b)
        assertEquals("2026-11-21:EASY", a[0].id)
    }

    @Test
    fun anyDayOfTheWeekGivesTheSameCardsAsItsSaturday() {
        val seed = TestData.seed()
        assertEquals(
            ChallengeGenerator.offersFor(date("2026-11-21"), seed, 10_000),
            ChallengeGenerator.offersFor(date("2026-11-25"), seed, 10_000),
        )
    }

    @Test
    fun targetsAtTheDefaultGoalMatchTheSpec() {
        val all = manyWeeks(10_000, hourly = false)
        assertEquals(66_000, all.pick(ChallengeTier.EASY, ChallengeType.WEEKLY_TOTAL).targetSteps)
        assertEquals(80_000, all.pick(ChallengeTier.MEDIUM, ChallengeType.WEEKLY_TOTAL).targetSteps)
        assertEquals(88_000, all.pick(ChallengeTier.HARD, ChallengeType.WEEKLY_TOTAL).targetSteps)
        assertEquals(102_000, all.pick(ChallengeTier.LEGENDARY, ChallengeType.WEEKLY_TOTAL).targetSteps)

        val easyDays = all.pick(ChallengeTier.EASY, ChallengeType.DAYS_ABOVE)
        assertEquals(5, easyDays.targetDays)
        assertEquals(10_000, easyDays.targetSteps)
        val mediumDays = all.pick(ChallengeTier.MEDIUM, ChallengeType.DAYS_ABOVE)
        assertEquals(5, mediumDays.targetDays)
        assertEquals(11_000, mediumDays.targetSteps)
        val hardDays = all.pick(ChallengeTier.HARD, ChallengeType.DAYS_ABOVE)
        assertEquals(3, hardDays.targetDays)
        assertEquals(13_000, hardDays.targetSteps)
        assertEquals(18_000, all.pick(ChallengeTier.HARD, ChallengeType.SINGLE_DAY_PEAK).targetSteps)
        assertEquals(23_000, all.pick(ChallengeTier.LEGENDARY, ChallengeType.SINGLE_DAY_PEAK).targetSteps)
        assertEquals(20_000, all.pick(ChallengeTier.EASY, ChallengeType.WEEKEND_ONLY).targetSteps)
    }

    @Test
    fun targetsScaleWithTheGoal() {
        val all = manyWeeks(20_000, hourly = false)
        assertEquals(132_000, all.pick(ChallengeTier.EASY, ChallengeType.WEEKLY_TOTAL).targetSteps)
        assertEquals(36_000, all.pick(ChallengeTier.HARD, ChallengeType.SINGLE_DAY_PEAK).targetSteps)
        assertEquals(20_000, all.pick(ChallengeTier.EASY, ChallengeType.DAYS_ABOVE).targetSteps)
    }

    @Test
    fun hardCardRotatesThroughTotalDaysAndPeak() {
        val types = manyWeeks(10_000, hourly = false).filter { it.tier == ChallengeTier.HARD }.map { it.type }.toSet()
        assertEquals(
            setOf(ChallengeType.WEEKLY_TOTAL, ChallengeType.DAYS_ABOVE, ChallengeType.SINGLE_DAY_PEAK),
            types,
        )
    }

    @Test
    fun continuousRunOnlyOfferedWhenHourlyDataExists() {
        assertTrue(manyWeeks(10_000, hourly = false).none { it.type == ChallengeType.CONTINUOUS_RUN })

        val all = manyWeeks(10_000, hourly = true, weeks = 80)
        val easy = all.pick(ChallengeTier.EASY, ChallengeType.CONTINUOUS_RUN)
        assertEquals(4_000, easy.targetSteps)
        assertEquals(60, easy.windowMinutes)
        val medium = all.pick(ChallengeTier.MEDIUM, ChallengeType.CONTINUOUS_RUN)
        assertEquals(5_000, medium.targetSteps)
        assertEquals(60, medium.windowMinutes)
        val hard = all.pick(ChallengeTier.HARD, ChallengeType.CONTINUOUS_RUN)
        assertEquals(10_000, hard.targetSteps)
        assertEquals(120, hard.windowMinutes)
        val legendary = all.pick(ChallengeTier.LEGENDARY, ChallengeType.CONTINUOUS_RUN)
        assertEquals(15_000, legendary.targetSteps)
        assertEquals(180, legendary.windowMinutes)
    }

    @Test
    fun rewardsXpShardsAndRims() {
        val all = manyWeeks(10_000, hourly = false)
        val easy = all.first { it.tier == ChallengeTier.EASY }
        val medium = all.first { it.tier == ChallengeTier.MEDIUM }
        val hard = all.first { it.tier == ChallengeTier.HARD }
        val legendary = all.first { it.tier == ChallengeTier.LEGENDARY }
        assertEquals(listOf(300, 700, 1_500, 3_000), listOf(easy.xp, medium.xp, hard.xp, legendary.xp))
        assertEquals(listOf(0, 0, 1, 2), listOf(easy.shards, medium.shards, hard.shards, legendary.shards))
        assertEquals(
            listOf(BadgeRim.BRONZE, BadgeRim.SILVER, BadgeRim.GOLD, BadgeRim.PRISMATIC),
            listOf(easy.rim, medium.rim, hard.rim, legendary.rim),
        )
        val doubled = manyWeeks(20_000, hourly = false).first { it.tier == ChallengeTier.HARD }
        assertEquals(3_000, doubled.xp)
        val halved = manyWeeks(5_000, hourly = false).first { it.tier == ChallengeTier.EASY }
        assertEquals(150, halved.xp)
    }

    @Test
    fun mediumDistanceCardUsesTheRealKmPerStepOfTheHistory() {
        val all = manyWeeks(10_000, hourly = false)
        val distance = all.pick(ChallengeTier.MEDIUM, ChallengeType.DISTANCE)
        assertEquals(62.0, distance.targetKm, 0.0001) // 80,000 x 0.00077 = 61.6 -> 62
    }

    // ---- Balance --------------------------------------------------------------------------

    /** Session 6 regression: Easy must be easier than Medium, and Medium than Hard, whatever card types come up. */
    @Test
    fun tiersGetHarderOnTheImportedHistoryWhateverTheCardType() {
        val seed = TestData.seed()
        var week = date("2026-01-03")
        var checked = 0
        while (!week.isAfter(date("2026-11-21"))) {
            val offers = ChallengeGenerator.offersFor(week, seed, 10_000)
            val odds = offers.map { ChallengeOdds.historicalRate(it, seed) }
            assertTrue("week $week has odds", odds.all { it != null })
            assertTrue("week $week: ${offers.map { it.type }} $odds", odds[0]!! > odds[1]!! && odds[1]!! > odds[2]!!)
            checked++
            week = week.plusWeeks(1)
        }
        assertEquals(47, checked)
    }

    @Test
    fun oddsAreNullForContinuousRunAndShortHistories() {
        val run = Challenge("x", date("2026-11-21"), ChallengeTier.EASY, ChallengeType.CONTINUOUS_RUN, 300, 0, BadgeRim.BRONZE, targetSteps = 4_000, windowMinutes = 60)
        assertEquals(null, ChallengeOdds.historicalRate(run, TestData.seed()))
        val total = Challenge("y", date("2026-11-21"), ChallengeTier.EASY, ChallengeType.WEEKLY_TOTAL, 300, 0, BadgeRim.BRONZE, targetSteps = 60_000)
        assertEquals(null, ChallengeOdds.historicalRate(total, emptyList()))
    }

    @Test
    fun oddsCountClearedWeeks() {
        val week = date("2026-11-21")
        val days = steadyWeeks(week, 10, 10_000) // 70,000 every week
        val easy = Challenge("a", week, ChallengeTier.EASY, ChallengeType.WEEKLY_TOTAL, 300, 0, BadgeRim.BRONZE, targetSteps = 70_000)
        val hard = Challenge("b", week, ChallengeTier.HARD, ChallengeType.WEEKLY_TOTAL, 1_500, 1, BadgeRim.GOLD, targetSteps = 70_001)
        assertEquals(1.0, ChallengeOdds.historicalRate(easy, days)!!, 0.0001)
        assertEquals(0.0, ChallengeOdds.historicalRate(hard, days)!!, 0.0001)
        assertEquals("You clear this almost every week", ChallengeOdds.label(1.0))
        assertEquals("Cleared in 69% of your recent weeks", ChallengeOdds.label(0.69))
        assertEquals(null, ChallengeOdds.label(null))
    }

    // ---- Adaptation -----------------------------------------------------------------------

    @Test
    fun noHistoryMeansNoAdaptation() {
        assertEquals(1.0, ChallengeGenerator.adaptationFactor(date("2026-11-21"), emptyList(), 10_000), 0.0001)
    }

    @Test
    fun strongRecentWeeksRaiseTargetsUpToFifteenPercent() {
        val week = date("2026-11-21")
        val days = steadyWeeks(week, 8, 14_000) // 98,000 a week
        assertEquals(1.15, ChallengeGenerator.adaptationFactor(week, days, 10_000), 0.0001)
        val offers = ChallengeGenerator.offersFor(week, days, 10_000, false)
        assertEquals(ChallengeType.WEEKLY_TOTAL, offers[0].type)
        assertEquals(76_000, offers[0].targetSteps) // 66,000 x 1.15 = 75,900 -> 76,000
    }

    @Test
    fun weakRecentWeeksLowerTargetsDownToTenPercent() {
        val week = date("2026-11-21")
        val days = steadyWeeks(week, 8, 4_000) // 28,000 a week
        assertEquals(0.9, ChallengeGenerator.adaptationFactor(week, days, 10_000), 0.0001)
    }

    @Test
    fun averageWeeksChangeLittle() {
        val week = date("2026-11-21")
        val days = steadyWeeks(week, 8, 12_000) // 84,000 vs the 85,000 reference
        val f = ChallengeGenerator.adaptationFactor(week, days, 10_000)
        assertTrue("factor $f", f > 0.98 && f < 1.0)
    }

    @Test
    fun incompleteWeeksAreIgnored() {
        val week = date("2026-11-21")
        val partial = ArrayList<LedgerDay>()
        for (i in 1..6) partial.add(LedgerDay(week.minusDays(i.toLong()), 30_000, 20.0, 10_000))
        assertEquals(1.0, ChallengeGenerator.adaptationFactor(week, partial, 10_000), 0.0001)
    }

    @Test
    fun importedHistoryFactorStaysInsideItsBounds() {
        val f = ChallengeGenerator.adaptationFactor(date("2026-11-21"), TestData.seed(), 10_000)
        assertTrue("factor $f", f >= 0.9 && f <= 1.15)
        val offers = ChallengeGenerator.offersFor(date("2026-11-21"), TestData.seed(), 10_000)
        assertEquals(3, offers.size)
    }

    // ---- Progress -------------------------------------------------------------------------

    private fun challenge(type: ChallengeType, steps: Int = 0, days: Int = 0, km: Double = 0.0, minutes: Int = 0): Challenge =
        Challenge(
            id = "t", weekStart = date("2026-11-21"), tier = ChallengeTier.EASY, type = type,
            xp = 300, shards = 0, rim = BadgeRim.BRONZE,
            targetSteps = steps, targetDays = days, targetKm = km, windowMinutes = minutes,
        )

    private fun week(vararg steps: Int): List<LedgerDay> {
        val out = ArrayList<LedgerDay>()
        for (i in steps.indices) {
            out.add(LedgerDay(date("2026-11-21").plusDays(i.toLong()), steps[i], steps[i] * 0.001, 10_000))
        }
        return out
    }

    @Test
    fun weeklyTotalProgress() {
        val p = ChallengeProgressEngine.evaluate(challenge(ChallengeType.WEEKLY_TOTAL, steps = 75_000), week(11_000, 11_000, 11_000, 11_000, 11_000, 11_000, 11_000))
        assertEquals(77_000.0, p.current, 0.0001)
        assertTrue(p.done)
        assertEquals(1.0, p.fraction, 0.0001)

        val short = ChallengeProgressEngine.evaluate(challenge(ChallengeType.WEEKLY_TOTAL, steps = 75_000), week(10_000, 10_000, 10_000))
        assertFalse(short.done)
        assertEquals(0.4, short.fraction, 0.0001)
    }

    @Test
    fun onlyDaysInsideTheWeekCount() {
        val days = week(10_000, 10_000) + LedgerDay(date("2026-11-20"), 50_000, 0.0, 10_000) + LedgerDay(date("2026-11-30"), 50_000, 0.0, 10_000)
        val p = ChallengeProgressEngine.evaluate(challenge(ChallengeType.WEEKLY_TOTAL, steps = 75_000), days)
        assertEquals(20_000.0, p.current, 0.0001)
    }

    @Test
    fun daysAboveProgress() {
        val c = challenge(ChallengeType.DAYS_ABOVE, steps = 14_000, days = 3)
        assertFalse(ChallengeProgressEngine.evaluate(c, week(14_000, 13_999, 15_000)).done)
        val ok = ChallengeProgressEngine.evaluate(c, week(14_000, 13_999, 15_000, 20_000))
        assertTrue(ok.done)
        assertEquals(3.0, ok.current, 0.0001)
    }

    @Test
    fun singleDayPeakProgress() {
        val c = challenge(ChallengeType.SINGLE_DAY_PEAK, steps = 22_000)
        assertFalse(ChallengeProgressEngine.evaluate(c, week(10_000, 21_999)).done)
        assertTrue(ChallengeProgressEngine.evaluate(c, week(10_000, 22_000)).done)
    }

    @Test
    fun weekendOnlyCountsSaturdayAndSundayOnly() {
        val c = challenge(ChallengeType.WEEKEND_ONLY, steps = 28_000)
        val p = ChallengeProgressEngine.evaluate(c, week(14_000, 14_000, 30_000, 30_000))
        assertEquals(28_000.0, p.current, 0.0001)
        assertTrue(p.done)
    }

    @Test
    fun distanceProgress() {
        val c = challenge(ChallengeType.DISTANCE, km = 19.9)
        val p = ChallengeProgressEngine.evaluate(c, week(10_000, 10_000))
        assertEquals(20.0, p.current, 0.0001)
        assertTrue(p.done)
    }

    @Test
    fun continuousRunUsesASlidingWindow() {
        val min = 60_000L
        val samples = listOf(
            StepSample(59 * min, 1_000),
            StepSample(0L, 1_000),
            StepSample(30 * min, 1_000),
            StepSample(60 * min, 1_000),
        )
        // Any hour (start inclusive, end exclusive) holds at most three of the four samples.
        assertEquals(3_000, ChallengeProgressEngine.bestWindow(samples, 60 * min))
        assertEquals(4_000, ChallengeProgressEngine.bestWindow(samples, 61 * min))
        assertEquals(2_000, ChallengeProgressEngine.bestWindow(samples, 10 * min)) // the 59 and 60 minute samples
        assertEquals(0, ChallengeProgressEngine.bestWindow(emptyList(), 60 * min))
        assertEquals(0, ChallengeProgressEngine.bestWindow(samples, 0L))

        val c = challenge(ChallengeType.CONTINUOUS_RUN, steps = 4_000, minutes = 60)
        assertFalse(ChallengeProgressEngine.evaluate(c, emptyList(), samples).done)
        val busy = samples + StepSample(45 * min, 1_000)
        assertTrue(ChallengeProgressEngine.evaluate(c, emptyList(), busy).done)
    }
}
