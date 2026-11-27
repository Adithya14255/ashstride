package com.ashstride.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatEngineTest {
    private val start = LocalDate.of(2026, 11, 21) // a Saturday

    private fun day(offset: Int, steps: Int, km: Double = steps * 0.00077) =
        LedgerDay(start.plusDays(offset.toLong()), steps, km, 10_000)

    @Test
    fun nothingWhenNoBigDays() {
        val s = FeatEngine.evaluate(listOf(day(0, 12_000), day(1, 15_000)))
        assertTrue(s.records.isEmpty())
        assertEquals(0, s.honorEarned)
        assertEquals(15_000.0, s.bestDaySteps, 0.0)
    }

    @Test
    fun firstTimePaysFullHonorAndLowerRungsUnlockToo() {
        // One 52,000-step day: every step rung unlocks and pays its first-time honor (10 + 25 + 60 + 150).
        val s = FeatEngine.evaluate(listOf(day(0, 52_000, 20.0)))
        for (id in listOf("feat_day_steps_20000", "feat_day_steps_30000", "feat_day_steps_40000", "feat_day_steps_50000")) assertTrue(id, s.has(id))
        // Distance 20 km reaches the 15 km rung only: +10. Week total 52,000 reaches nothing on the week ladder.
        assertEquals(10 + 25 + 60 + 150 + 10, s.honorEarned)
    }

    @Test
    fun repeatsPayLessDownToAFloor() {
        assertEquals(10, HonorRules.repeatHonor(10, 1))
        assertEquals(5, HonorRules.repeatHonor(10, 2))
        assertEquals(2, HonorRules.repeatHonor(10, 3))
        assertEquals(2, HonorRules.repeatHonor(10, 50))
        assertEquals(150, HonorRules.repeatHonor(150, 1))
        assertEquals(75, HonorRules.repeatHonor(150, 2))
        assertEquals(30, HonorRules.repeatHonor(150, 40))
        assertTrue(HonorRules.repeatHonor(25, 99) >= 5)
    }

    @Test
    fun onlyTheHighestRungOfADayPaysARepeat() {
        // Day 0: a 20k day (rung 1 first time = 10). Day 1: a 30k day: rung 2 first time (25); rung 1 is a lower rung
        // that was already earned, so it does not pay again.
        val s = FeatEngine.evaluate(listOf(day(0, 21_000, 5.0), day(1, 31_000, 5.0)))
        val steps = s.records.filter { it.def.ladder == FeatLadder.DAY_STEPS }
        assertEquals(10, steps.first { it.def.rung == 0 }.honorEarned)
        assertEquals(2, steps.first { it.def.rung == 0 }.times)
        assertEquals(25, steps.first { it.def.rung == 1 }.honorEarned)
    }

    @Test
    fun repeatingTheSameRungHalves() {
        val s = FeatEngine.evaluate(listOf(day(0, 21_000, 5.0), day(2, 22_000, 5.0), day(3, 23_000, 5.0)))
        val r = s.records.first { it.def.id == "feat_day_steps_20000" }
        assertEquals(3, r.times)
        assertEquals(10 + 5 + 2, r.honorEarned)
    }

    @Test
    fun distanceLadderUsesKilometres() {
        val s = FeatEngine.evaluate(listOf(day(0, 31_000, 24.5)))
        assertTrue(s.has("feat_day_km_15"))
        assertTrue(s.has("feat_day_km_24"))
        assertFalse(s.has("feat_day_km_35"))
    }

    @Test
    fun weekLadderSumsSaturdayToFriday() {
        // 7 days of 15,000 = 105,000 in one Saturday-Friday week: Century Week.
        val days = (0..6).map { day(it, 15_000, 8.0) }
        val s = FeatEngine.evaluate(days)
        assertTrue(s.has("feat_week_steps_100000"))
        assertFalse(s.has("feat_week_steps_120000"))
        // The same days split across two weeks do not make a feat.
        val split = (3..9).map { day(it, 15_000, 8.0) }
        assertFalse(FeatEngine.evaluate(split).has("feat_week_steps_100000"))
    }

    @Test
    fun nextRungIsTheLowestUnearned() {
        val s = FeatEngine.evaluate(listOf(day(0, 31_000, 5.0)))
        assertEquals("feat_day_steps_40000", FeatEngine.nextRung(s, FeatLadder.DAY_STEPS)?.id)
        assertEquals("feat_day_km_15", FeatEngine.nextRung(s, FeatLadder.DAY_DISTANCE)?.id)
    }

    @Test
    fun honorRedeemRules() {
        assertEquals(0, HonorRules.balance(50, 80))
        assertEquals(40, HonorRules.balance(140, 100))
        assertFalse(HonorRules.canRedeem(99, 0))
        assertTrue(HonorRules.canRedeem(100, 0))
        assertTrue(HonorRules.canRedeem(500, HonorRules.WEEKLY_LIMIT - 1))
        assertFalse(HonorRules.canRedeem(500, HonorRules.WEEKLY_LIMIT))
    }
}
