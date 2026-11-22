package com.ashstride.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

/** User choices for alerts. Times are minutes after midnight. */
data class NotifySettings(
    val atRiskEnabled: Boolean = true,
    val atRiskMinutes: Int = 20 * 60,
    val shrineEnabled: Boolean = true,
    val shrineMinutes: Int = 9 * 60,
    val milestonesEnabled: Boolean = true,
    val quietEnabled: Boolean = true,
    val quietStartMinutes: Int = 22 * 60,
    val quietEndMinutes: Int = 7 * 60,
)

/** Pure rules for when an alert may fire. The caller supplies the clock and the data; nothing here touches Android. */
object NotificationRules {
    const val DAY_MINUTES = 24 * 60

    /** Quiet hours may wrap past midnight (22:00 to 07:00). Equal start and end means no quiet time. */
    fun inQuietHours(s: NotifySettings, minuteOfDay: Int): Boolean {
        if (!s.quietEnabled || s.quietStartMinutes == s.quietEndMinutes) return false
        val a = s.quietStartMinutes
        val b = s.quietEndMinutes
        return if (a < b) minuteOfDay in a until b else (minuteOfDay >= a || minuteOfDay < b)
    }

    /**
     * Evening alert: the streak is alive, today is below goal, the alert time has passed, it is not quiet,
     * and nothing was sent today yet.
     */
    fun shouldAlertAtRisk(
        s: NotifySettings,
        minuteOfDay: Int,
        todaySteps: Int,
        todayGoal: Int,
        streakTotal: Int,
        sentOn: String?,
        today: LocalDate,
    ): Boolean {
        if (!s.atRiskEnabled) return false
        if (streakTotal <= 0 || todaySteps >= todayGoal) return false
        if (minuteOfDay < s.atRiskMinutes) return false
        if (inQuietHours(s, minuteOfDay)) return false
        return sentOn != today.toString()
    }

    /** Saturday, after the chosen time, cards still sealed, and not yet announced for this week. */
    fun shouldAlertShrine(
        s: NotifySettings,
        minuteOfDay: Int,
        today: LocalDate,
        weekStatus: String?,
        sentForWeek: String?,
    ): Boolean {
        if (!s.shrineEnabled) return false
        if (today.dayOfWeek != DayOfWeek.SATURDAY) return false
        if (minuteOfDay < s.shrineMinutes) return false
        if (inQuietHours(s, minuteOfDay)) return false
        if (weekStatus != WeekStatus.OPEN) return false
        return sentForWeek != ChallengeGenerator.weekStartFor(today).toString()
    }

    fun clockText(minutes: Int): String {
        val m = ((minutes % DAY_MINUTES) + DAY_MINUTES) % DAY_MINUTES
        return String.format(java.util.Locale.US, "%02d:%02d", m / 60, m % 60)
    }

    /** Moves a time by [delta] minutes, wrapping around the day. */
    fun shift(minutes: Int, delta: Int): Int = ((minutes + delta) % DAY_MINUTES + DAY_MINUTES) % DAY_MINUTES

    /** Short line for the milestone alert. */
    fun milestoneText(titles: List<String>): String = when (titles.size) {
        0 -> ""
        1 -> titles[0]
        else -> titles[0] + " and ${titles.size - 1} more"
    }
}
