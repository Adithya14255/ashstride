package com.ashstride.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRulesTest {
    private val s = NotifySettings()
    private val sat = LocalDate.of(2026, 11, 21)
    private val sun = LocalDate.of(2026, 11, 22)

    @Test
    fun quietHoursWrapPastMidnight() {
        assertTrue(NotificationRules.inQuietHours(s, 23 * 60))
        assertTrue(NotificationRules.inQuietHours(s, 3 * 60))
        assertTrue(NotificationRules.inQuietHours(s, 6 * 60 + 59))
        assertFalse(NotificationRules.inQuietHours(s, 7 * 60))
        assertFalse(NotificationRules.inQuietHours(s, 12 * 60))
        assertFalse(NotificationRules.inQuietHours(s, 21 * 60 + 59))
        assertTrue(NotificationRules.inQuietHours(s, 22 * 60))
    }

    @Test
    fun quietHoursCanBeOffOrSameDay() {
        assertFalse(NotificationRules.inQuietHours(s.copy(quietEnabled = false), 23 * 60))
        assertFalse(NotificationRules.inQuietHours(s.copy(quietStartMinutes = 600, quietEndMinutes = 600), 600))
        val day = s.copy(quietStartMinutes = 13 * 60, quietEndMinutes = 15 * 60)
        assertTrue(NotificationRules.inQuietHours(day, 14 * 60))
        assertFalse(NotificationRules.inQuietHours(day, 16 * 60))
    }

    @Test
    fun atRiskFiresOnceAfterTheAlertTimeWhenBelowGoal() {
        assertFalse(NotificationRules.shouldAlertAtRisk(s, 19 * 60, 4000, 10_000, 118, null, sun))
        assertTrue(NotificationRules.shouldAlertAtRisk(s, 20 * 60, 4000, 10_000, 118, null, sun))
        assertFalse(NotificationRules.shouldAlertAtRisk(s, 20 * 60, 4000, 10_000, 118, sun.toString(), sun))
        assertTrue(NotificationRules.shouldAlertAtRisk(s, 20 * 60, 4000, 10_000, 118, sat.toString(), sun))
    }

    @Test
    fun atRiskStaysQuietWhenGoalMetNoStreakOrDisabledOrQuiet() {
        assertFalse(NotificationRules.shouldAlertAtRisk(s, 20 * 60, 10_000, 10_000, 118, null, sun))
        assertFalse(NotificationRules.shouldAlertAtRisk(s, 20 * 60, 100, 10_000, 0, null, sun))
        assertFalse(NotificationRules.shouldAlertAtRisk(s.copy(atRiskEnabled = false), 20 * 60, 100, 10_000, 5, null, sun))
        assertFalse(NotificationRules.shouldAlertAtRisk(s, 22 * 60 + 30, 100, 10_000, 5, null, sun))
    }

    @Test
    fun shrineAlertIsSaturdayOnlyOncePerWeek() {
        assertTrue(NotificationRules.shouldAlertShrine(s, 9 * 60, sat, WeekStatus.OPEN, null))
        assertFalse(NotificationRules.shouldAlertShrine(s, 8 * 60, sat, WeekStatus.OPEN, null))
        assertFalse(NotificationRules.shouldAlertShrine(s, 9 * 60, sun, WeekStatus.OPEN, null))
        assertFalse(NotificationRules.shouldAlertShrine(s, 9 * 60, sat, WeekStatus.ACTIVE, null))
        assertFalse(NotificationRules.shouldAlertShrine(s, 9 * 60, sat, WeekStatus.OPEN, "2026-11-21"))
        assertFalse(NotificationRules.shouldAlertShrine(s.copy(shrineEnabled = false), 9 * 60, sat, WeekStatus.OPEN, null))
    }

    @Test
    fun clockTextAndShiftWrap() {
        assertEquals("20:00", NotificationRules.clockText(20 * 60))
        assertEquals("07:05", NotificationRules.clockText(7 * 60 + 5))
        assertEquals(23 * 60 + 30, NotificationRules.shift(0, -30))
        assertEquals(30, NotificationRules.shift(23 * 60 + 30, 60))
        assertEquals("Streak 100", NotificationRules.milestoneText(listOf("Streak 100")))
        assertEquals("A and 2 more", NotificationRules.milestoneText(listOf("A", "B", "C")))
    }
}
