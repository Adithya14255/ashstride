package com.ashstride.app.domain

/** Steps recorded in one slice of time starting at [epochMs]. Only exists for data recorded after install. */
data class StepSample(val epochMs: Long, val steps: Int)

data class ChallengeProgress(val current: Double, val target: Double) {
    val done: Boolean get() = current >= target

    val fraction: Double
        get() = if (target <= 0.0) 1.0 else (current / target).coerceIn(0.0, 1.0)
}

object ChallengeProgressEngine {

    /**
     * Progress counts from the start of the challenge's week (Monday) through Sunday night.
     * [samples] are only needed for CONTINUOUS_RUN.
     */
    fun evaluate(
        challenge: Challenge,
        days: List<LedgerDay>,
        samples: List<StepSample> = emptyList(),
    ): ChallengeProgress {
        val start = challenge.weekStart
        val end = ChallengeGenerator.weekEndFor(start)
        val weekend = ChallengeGenerator.weekendDays(start).toSet()
        val week = days.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
        return when (challenge.type) {
            ChallengeType.WEEKLY_TOTAL -> {
                var sum = 0L
                for (d in week) sum += d.steps
                ChallengeProgress(sum.toDouble(), challenge.targetSteps.toDouble())
            }
            ChallengeType.DAYS_ABOVE -> {
                var n = 0
                for (d in week) if (d.steps >= challenge.targetSteps) n++
                ChallengeProgress(n.toDouble(), challenge.targetDays.toDouble())
            }
            ChallengeType.SINGLE_DAY_PEAK -> {
                var best = 0
                for (d in week) if (d.steps > best) best = d.steps
                ChallengeProgress(best.toDouble(), challenge.targetSteps.toDouble())
            }
            ChallengeType.DISTANCE -> {
                var km = 0.0
                for (d in week) km += d.distanceKm
                ChallengeProgress(km, challenge.targetKm)
            }
            ChallengeType.WEEKEND_ONLY -> {
                var sum = 0L
                for (d in week) {
                    if (d.date in weekend) sum += d.steps
                }
                ChallengeProgress(sum.toDouble(), challenge.targetSteps.toDouble())
            }
            ChallengeType.CONTINUOUS_RUN -> {
                val best = bestWindow(samples, challenge.windowMinutes * 60_000L)
                ChallengeProgress(best.toDouble(), challenge.targetSteps.toDouble())
            }
        }
    }

    /** The most steps inside any window shorter than [windowMs] (start inclusive, end exclusive). */
    fun bestWindow(samples: List<StepSample>, windowMs: Long): Int {
        if (windowMs <= 0L) return 0
        val s = samples.sortedBy { it.epochMs }
        var best = 0L
        var sum = 0L
        var left = 0
        for (right in s.indices) {
            sum += s[right].steps
            while (s[right].epochMs - s[left].epochMs >= windowMs) {
                sum -= s[left].steps
                left++
            }
            if (sum > best) best = sum
        }
        return best.toInt()
    }
}
