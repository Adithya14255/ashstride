package com.ashstride.app.domain

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShrineEngineTest {
    private val ws: LocalDate = LocalDate.of(2026, 11, 21) // a Saturday
    private val zone: ZoneId = ZoneId.of("UTC")

    private fun total(steps: Int) =
        Challenge("2026-11-21:EASY", ws, ChallengeTier.EASY, ChallengeType.WEEKLY_TOTAL, 300, 0, BadgeRim.BRONZE, targetSteps = steps)

    private fun run(steps: Int, minutes: Int) =
        Challenge("2026-11-21:EASY", ws, ChallengeTier.EASY, ChallengeType.CONTINUOUS_RUN, 300, 0, BadgeRim.BRONZE, targetSteps = steps, windowMinutes = minutes)

    @Test
    fun bridgeWeekEndsOnSundayBeforeTheFirstMondayWeek() {
        // The Saturday-to-Friday week running at the switch is stretched to Sunday 29 Nov.
        assertEquals(LocalDate.of(2026, 11, 29), ShrineEngine.weekEnd(ws))
        assertFalse(ShrineEngine.isOver(ws, LocalDate.of(2026, 11, 29)))
        assertTrue(ShrineEngine.isOver(ws, LocalDate.of(2026, 11, 30)))
    }

    @Test
    fun oldWeeksEndOnFridayAndNewWeeksOnSunday() {
        assertEquals(LocalDate.of(2026, 11, 20), ShrineEngine.weekEnd(LocalDate.of(2026, 11, 14)))
        assertEquals(LocalDate.of(2026, 12, 6), ShrineEngine.weekEnd(LocalDate.of(2026, 11, 30)))
        assertFalse(ShrineEngine.isOver(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 6)))
        assertTrue(ShrineEngine.isOver(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 7)))
    }

    @Test
    fun daysLeftCountsTodayAndStopsAtZero() {
        assertEquals(9, ShrineEngine.daysLeft(ws, ws)) // the bridge week is nine days long
        assertEquals(3, ShrineEngine.daysLeft(ws, LocalDate.of(2026, 11, 27)))
        assertEquals(1, ShrineEngine.daysLeft(ws, LocalDate.of(2026, 11, 29)))
        assertEquals(0, ShrineEngine.daysLeft(ws, LocalDate.of(2026, 11, 30)))
        assertEquals(9, ShrineEngine.daysLeft(ws, LocalDate.of(2026, 11, 20)))
        assertEquals(7, ShrineEngine.daysLeft(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 30)))
        assertEquals(1, ShrineEngine.daysLeft(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 6)))
    }

    @Test
    fun pickingOnlyWhileOpenAndInsideTheWindow() {
        assertTrue(ShrineEngine.canPick(WeekStatus.OPEN, ws, ws))
        assertTrue(ShrineEngine.canPick(WeekStatus.OPEN, ws, ws.plusDays(6)))
        assertFalse(ShrineEngine.canPick(WeekStatus.OPEN, ws, ws.plusDays(7)))
        assertFalse(ShrineEngine.canPick(WeekStatus.OPEN, ws, ws.minusDays(1)))
        assertFalse(ShrineEngine.canPick(WeekStatus.ACTIVE, ws, ws))
        assertFalse(ShrineEngine.canPick(WeekStatus.WON, ws, ws))
    }

    @Test
    fun settledWeeksNeverChange() {
        val done = ChallengeProgress(100.0, 10.0)
        assertEquals(WeekStatus.WON, ShrineEngine.nextStatus(WeekStatus.WON, null, null, true))
        assertEquals(WeekStatus.LOST, ShrineEngine.nextStatus(WeekStatus.LOST, total(10), done, false))
        assertEquals(WeekStatus.SKIPPED, ShrineEngine.nextStatus(WeekStatus.SKIPPED, null, null, false))
    }

    @Test
    fun unpickedWeekIsSkippedOnlyWhenOver() {
        assertEquals(WeekStatus.OPEN, ShrineEngine.nextStatus(WeekStatus.OPEN, null, null, false))
        assertEquals(WeekStatus.SKIPPED, ShrineEngine.nextStatus(WeekStatus.OPEN, null, null, true))
    }

    @Test
    fun completeCardWinsAtOnceIncompleteLosesOnlyAtTheEnd() {
        val c = total(75_000)
        val done = ChallengeProgress(80_000.0, 75_000.0)
        val short = ChallengeProgress(40_000.0, 75_000.0)
        assertEquals(WeekStatus.WON, ShrineEngine.nextStatus(WeekStatus.ACTIVE, c, done, false))
        assertEquals(WeekStatus.ACTIVE, ShrineEngine.nextStatus(WeekStatus.ACTIVE, c, short, false))
        assertEquals(WeekStatus.LOST, ShrineEngine.nextStatus(WeekStatus.ACTIVE, c, short, true))
        assertEquals(WeekStatus.WON, ShrineEngine.nextStatus(WeekStatus.ACTIVE, c, done, true))
    }

    @Test
    fun onlySamplesInsideTheWeekCount() {
        val week = LocalDate.of(2026, 11, 30) // a seven-day Monday week
        val from = week.atStartOfDay(zone).toInstant().toEpochMilli()
        val weekMs = 7L * 24 * 3_600_000L
        val samples = listOf(
            StepSample(from - 1, 1),
            StepSample(from, 2),
            StepSample(from + weekMs - 1, 3),
            StepSample(from + weekMs, 4),
        )
        val kept = ShrineEngine.samplesInWeek(samples, week, zone)
        assertEquals(listOf(2, 3), kept.map { it.steps })
    }

    @Test
    fun continuousRunIgnoresStepsFromOutsideTheWeek() {
        val from = ws.atStartOfDay(zone).toInstant().toEpochMilli()
        val samples = listOf(
            StepSample(from - 1_000L, 9_000), // Friday night, previous week
            StepSample(from, 1_000),
        )
        val p = ShrineEngine.progressOf(run(4_000, 60), emptyList(), samples, zone)
        assertEquals(1_000.0, p.current, 0.001)
        assertFalse(p.done)
    }

    @Test
    fun continuousRunFindsAHotHourAcrossBuckets() {
        val from = ws.atStartOfDay(zone).toInstant().toEpochMilli() + 10 * 3_600_000L
        val step = 15 * 60_000L
        val samples = (0 until 4).map { StepSample(from + it * step, 1_100) }
        val p = ShrineEngine.progressOf(run(4_000, 60), emptyList(), samples, zone)
        assertEquals(4_400.0, p.current, 0.001)
        assertTrue(p.done)
    }
}
