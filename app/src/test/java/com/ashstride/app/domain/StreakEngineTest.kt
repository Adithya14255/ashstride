package com.ashstride.app.domain

import com.ashstride.app.domain.TestData.TODAY
import com.ashstride.app.domain.TestData.d
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakEngineTest {

    @Test
    fun seedIsComplete() {
        val seed = TestData.seed()
        assertEquals(505, seed.size)
        assertEquals(6_406_335L, seed.sumOf { it.steps.toLong() })
    }

    @Test
    fun seedCurrentAndBestStreaks() {
        val s = StreakEngine.summary(TestData.seed(), TODAY)
        assertEquals(118, s.pure)
        assertEquals(118, s.total)
        assertEquals(132, s.bestPure)
        assertEquals(132, s.bestTotal)
        assertFalse(s.runHasMend)
        assertTrue("today (Nov 21) has no row yet, so the streak is waiting on today", s.atRisk)
    }

    @Test
    fun mendJul25MergesTo251() {
        val p = StreakEngine.previewMend(TestData.seed(), LocalDate.parse("2026-07-25"), TODAY)
        assertEquals(MendCheck.OK, p.check)
        assertEquals(118, p.totalBefore)
        assertEquals(251, p.totalAfter)
        assertTrue(p.allowed)
    }

    @Test
    fun mendedRunKeepsPureAt118AndShowsSeam() {
        val mended = TestData.mend(TestData.seed(), "2026-07-25")
        val s = StreakEngine.summary(mended, TODAY)
        assertEquals(251, s.total)
        assertEquals(118, s.pure)
        assertTrue(s.runHasMend)
    }

    @Test
    fun fiveMendsBackToSep20Give427() {
        val mended = TestData.mend(TestData.seed(), *TestData.FIVE_MENDS)
        assertEquals(427, StreakEngine.summary(mended, TODAY).total)
    }

    @Test
    fun everyMendInTheReferenceScenarioIsAllowed() {
        val seed = TestData.seed()
        for (date in TestData.FIVE_MENDS) {
            assertEquals(date, MendCheck.OK, StreakEngine.checkMend(seed, LocalDate.parse(date), TODAY))
        }
    }

    @Test
    fun twoDayGapNeedsTwoMenders() {
        val seed = TestData.seed()
        val asOf = LocalDate.parse("2026-03-13")
        assertEquals(32, StreakEngine.summary(seed, asOf).total)
        val one = TestData.mend(seed, "2026-02-08")
        assertEquals("Feb 9 is still a hole", 32, StreakEngine.summary(one, asOf).total)
        val two = TestData.mend(seed, "2026-02-08", "2026-02-09")
        assertEquals(114 + 2 + 32, StreakEngine.summary(two, asOf).total)
    }

    @Test
    fun mendRules() {
        val days = listOf(
            d("2026-01-01", 10_000),
            d("2026-01-02", 2_499),
            d("2026-01-03", 2_500),
            d("2026-01-04", 9_999, mended = true),
        )
        val today = LocalDate.parse("2026-01-10")
        assertEquals(MendCheck.ALREADY_MET, StreakEngine.checkMend(days, LocalDate.parse("2026-01-01"), today))
        assertEquals(MendCheck.BELOW_FLOOR, StreakEngine.checkMend(days, LocalDate.parse("2026-01-02"), today))
        assertEquals(MendCheck.OK, StreakEngine.checkMend(days, LocalDate.parse("2026-01-03"), today))
        assertEquals(MendCheck.ALREADY_MENDED, StreakEngine.checkMend(days, LocalDate.parse("2026-01-04"), today))
        assertEquals(MendCheck.FUTURE, StreakEngine.checkMend(days, LocalDate.parse("2026-01-11"), today))
        assertEquals("no row means no steps", MendCheck.BELOW_FLOOR, StreakEngine.checkMend(days, LocalDate.parse("2026-01-08"), today))
    }

    @Test
    fun mendFloorIsQuarterOfThatDaysGoal() {
        assertEquals(2_500, StreakEngine.mendFloor(10_000))
        assertEquals(5_000, StreakEngine.mendFloor(20_000))
        // A day recorded under a 6,000 goal is judged by its own goal, not today's.
        val days = listOf(d("2026-01-01", 1_600, goal = 6_000), d("2026-01-02", 1_400, goal = 6_000))
        val today = LocalDate.parse("2026-01-05")
        assertEquals(MendCheck.OK, StreakEngine.checkMend(days, LocalDate.parse("2026-01-01"), today))
        assertEquals(MendCheck.BELOW_FLOOR, StreakEngine.checkMend(days, LocalDate.parse("2026-01-02"), today))
    }

    @Test
    fun todayInProgressDoesNotBreakTheStreak() {
        val base = listOf(d("2026-01-01", 10_000), d("2026-01-02", 10_000), d("2026-01-03", 10_000))
        val today = LocalDate.parse("2026-01-04")

        val noRow = StreakEngine.summary(base, today)
        assertEquals(3, noRow.total)
        assertTrue(noRow.atRisk)

        val partial = StreakEngine.summary(base + d("2026-01-04", 4_000), today)
        assertEquals(3, partial.total)
        assertTrue(partial.atRisk)

        val done = StreakEngine.summary(base + d("2026-01-04", 10_500), today)
        assertEquals(4, done.total)
        assertFalse(done.atRisk)
    }

    @Test
    fun streakIsLostOnceYesterdayIsMissed() {
        val base = listOf(d("2026-01-01", 10_000), d("2026-01-02", 10_000), d("2026-01-03", 10_000))
        val s = StreakEngine.summary(base, LocalDate.parse("2026-01-05"))
        assertEquals(0, s.total)
        assertEquals(3, s.bestTotal)
        assertFalse(s.atRisk)
    }

    @Test
    fun missingRowBreaksTheRun() {
        val days = listOf(d("2026-01-01", 10_000), d("2026-01-03", 10_000))
        assertEquals(1, StreakEngine.summary(days, LocalDate.parse("2026-01-03")).total)
    }

    @Test
    fun mendingTodayCountsToday() {
        val days = listOf(d("2026-01-01", 10_000), d("2026-01-02", 10_000), d("2026-01-03", 6_000))
        val today = LocalDate.parse("2026-01-03")
        val p = StreakEngine.previewMend(days, today, today)
        assertEquals(MendCheck.OK, p.check)
        assertEquals(2, p.totalBefore)
        assertEquals(3, p.totalAfter)
    }

    @Test
    fun statusOfDay() {
        assertEquals(DayStatus.MET, StreakEngine.statusOf(d("2026-01-01", 10_000)))
        assertEquals(DayStatus.MENDED, StreakEngine.statusOf(d("2026-01-01", 5_000, mended = true)))
        assertEquals(DayStatus.MISSED, StreakEngine.statusOf(d("2026-01-01", 5_000)))
    }

    @Test
    fun menderShardChain() {
        var stash = MenderStash(0, 0)
        stash = MenderRules.addShards(stash, 2)
        assertEquals(MenderStash(2, 0), stash)
        stash = MenderRules.addShards(stash, 1)
        assertEquals(MenderStash(0, 1), stash)
        stash = MenderRules.addShards(stash, 6)
        assertEquals("cap is 2 Menders; extra shards wait", MenderStash(3, 2), stash)
        stash = MenderRules.use(stash)
        assertEquals("waiting shards convert once there is room", MenderStash(0, 2), stash)
        assertTrue(MenderRules.canUse(stash))
        assertFalse(MenderRules.canUse(MenderStash(2, 0)))
        assertEquals(MenderStash(2, 0), MenderRules.use(MenderStash(2, 0)))
    }
}
