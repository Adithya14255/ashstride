package com.ashstride.app.domain

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Values stored in challenge_week.status. SKIPPED means the week ended with no card picked. */
object WeekStatus {
    const val OPEN = "OPEN"
    const val ACTIVE = "ACTIVE"
    const val WON = "WON"
    const val LOST = "LOST"
    const val SKIPPED = "SKIPPED"

    fun isSettled(status: String): Boolean = status == WON || status == LOST || status == SKIPPED
}

/**
 * Turns a week's offers into the text stored in challenge_week.offersJson and back,
 * so reopening the app (or changing the goal) never reshuffles or retargets the cards.
 */
object ChallengeCodec {
    fun encode(offers: List<Challenge>): String {
        val arr = JSONArray()
        for (c in offers) {
            val o = JSONObject()
            o.put("id", c.id)
            o.put("week", c.weekStart.toString())
            o.put("tier", c.tier.name)
            o.put("type", c.type.name)
            o.put("xp", c.xp)
            o.put("shards", c.shards)
            o.put("steps", c.targetSteps)
            o.put("days", c.targetDays)
            o.put("km", c.targetKm)
            o.put("window", c.windowMinutes)
            arr.put(o)
        }
        return arr.toString()
    }

    fun decode(json: String): List<Challenge> {
        val arr = JSONArray(json)
        val out = ArrayList<Challenge>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val tier = ChallengeTier.valueOf(o.getString("tier"))
            out.add(
                Challenge(
                    id = o.getString("id"),
                    weekStart = LocalDate.parse(o.getString("week")),
                    tier = tier,
                    type = ChallengeType.valueOf(o.getString("type")),
                    xp = o.getInt("xp"),
                    shards = o.getInt("shards"),
                    rim = tier.rim,
                    targetSteps = o.optInt("steps", 0),
                    targetDays = o.optInt("days", 0),
                    targetKm = o.optDouble("km", 0.0),
                    windowMinutes = o.optInt("window", 0),
                ),
            )
        }
        return out
    }
}

/** Display text for the cards. Kept here so the wording is tested and the UI stays dumb. */
object ChallengeText {
    private fun n(v: Int): String = String.format(java.util.Locale.US, "%,d", v)

    fun tierName(tier: ChallengeTier): String = when (tier) {
        ChallengeTier.EASY -> "Easy"
        ChallengeTier.MEDIUM -> "Medium"
        ChallengeTier.HARD -> "Hard"
        ChallengeTier.LEGENDARY -> "Legendary"
    }

    fun title(c: Challenge): String = when (c.type) {
        ChallengeType.WEEKLY_TOTAL -> "The Long Road"
        ChallengeType.DAYS_ABOVE -> "Steady Flame"
        ChallengeType.SINGLE_DAY_PEAK -> "One Great Day"
        ChallengeType.DISTANCE -> "Far Reach"
        ChallengeType.WEEKEND_ONLY -> "Weekend Pilgrim"
        ChallengeType.CONTINUOUS_RUN -> "Fire Sprint"
    }

    fun windowText(minutes: Int): String = when {
        minutes == 60 -> "one hour"
        minutes % 60 == 0 -> "${minutes / 60} hours"
        else -> "$minutes minutes"
    }

    fun describe(c: Challenge): String = when (c.type) {
        ChallengeType.WEEKLY_TOTAL ->
            if (c.weekStart.isBefore(ChallengeGenerator.WEEK_CUTOVER)) "Walk ${n(c.targetSteps)} steps between Saturday and Friday."
            else "Walk ${n(c.targetSteps)} steps between Monday and Sunday."
        ChallengeType.DAYS_ABOVE -> "Reach ${n(c.targetSteps)} steps on ${c.targetDays} different days."
        ChallengeType.SINGLE_DAY_PEAK -> "Walk ${n(c.targetSteps)} steps in a single day."
        ChallengeType.DISTANCE -> "Cover ${n(Math.round(c.targetKm).toInt())} km this week."
        ChallengeType.WEEKEND_ONLY -> "Walk ${n(c.targetSteps)} steps over Saturday and Sunday."
        ChallengeType.CONTINUOUS_RUN ->
            "Walk ${n(c.targetSteps)} steps within ${windowText(c.windowMinutes)}, in one stretch."
    }

    /** Short progress line such as "42,000 / 85,000 steps". */
    fun progress(c: Challenge, p: ChallengeProgress): String = when (c.type) {
        ChallengeType.DAYS_ABOVE -> "${p.current.toInt()} / ${c.targetDays} days"
        ChallengeType.DISTANCE ->
            String.format(java.util.Locale.US, "%.1f / %.0f km", p.current, p.target)
        else -> "${n(p.current.toInt())} / ${n(p.target.toInt())} steps"
    }

    fun rewardText(c: Challenge): String {
        val shard = when (c.shards) {
            0 -> ""
            1 -> " + 1 shard"
            else -> " + ${c.shards} shards"
        }
        return "+${n(c.xp)} XP + badge$shard"
    }
}

/** Rules for the weekend shrine. Pure: the caller supplies the date, the ledger and the step samples. */
object ShrineEngine {
    /** Days from Saturday during which a card may still be picked. 7 = any day of the week (progress counts from the first day of the week). */
    const val PICK_WINDOW_DAYS = 7

    fun weekEnd(weekStart: LocalDate): LocalDate = ChallengeGenerator.weekEndFor(weekStart)

    fun isOver(weekStart: LocalDate, today: LocalDate): Boolean = today.isAfter(weekEnd(weekStart))

    /** Days left in the week counting today; 0 once the week is over. */
    fun daysLeft(weekStart: LocalDate, today: LocalDate): Int {
        if (isOver(weekStart, today)) return 0
        if (today.isBefore(weekStart)) return (ChronoUnit.DAYS.between(weekStart, weekEnd(weekStart)) + 1).toInt()
        return (ChronoUnit.DAYS.between(today, weekEnd(weekStart)) + 1).toInt()
    }

    fun canPick(status: String, weekStart: LocalDate, today: LocalDate): Boolean {
        if (status != WeekStatus.OPEN) return false
        if (today.isBefore(weekStart)) return false
        return ChronoUnit.DAYS.between(weekStart, today) < PICK_WINDOW_DAYS
    }

    /** Only samples from inside the week (Monday to Sunday) count (local midnight to midnight). */
    fun samplesInWeek(samples: List<StepSample>, weekStart: LocalDate, zone: ZoneId): List<StepSample> {
        val from = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = weekEnd(weekStart).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return samples.filter { it.epochMs >= from && it.epochMs < to }
    }

    fun progressOf(
        challenge: Challenge,
        days: List<LedgerDay>,
        samples: List<StepSample>,
        zone: ZoneId,
    ): ChallengeProgress =
        ChallengeProgressEngine.evaluate(challenge, days, samplesInWeek(samples, challenge.weekStart, zone))

    /**
     * The status a week should have now. A picked card that is complete wins at once (no need
     * to wait for Friday); an unfinished one is lost only when the week is over; a week that
     * ends with no pick is SKIPPED. Settled weeks never change.
     */
    fun nextStatus(status: String, picked: Challenge?, progress: ChallengeProgress?, weekOver: Boolean): String {
        if (WeekStatus.isSettled(status)) return status
        if (picked == null) return if (weekOver) WeekStatus.SKIPPED else status
        if (progress != null && progress.done) return WeekStatus.WON
        return if (weekOver) WeekStatus.LOST else WeekStatus.ACTIVE
    }
}
