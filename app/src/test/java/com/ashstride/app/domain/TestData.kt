package com.ashstride.app.domain

import java.io.File
import java.time.LocalDate

/** Shared helpers for the rules-engine tests. */
object TestData {
    /** The imported history was seeded on 2026-11-21, so "today" in the reference scenarios is that day. */
    val TODAY: LocalDate = LocalDate.of(2026, 11, 21)

    fun d(date: String, steps: Int, goal: Int = 10_000, mended: Boolean = false, km: Double = 0.0): LedgerDay =
        LedgerDay(LocalDate.parse(date), steps, km, goal, mended)

    /** Reads the real seed file (505 days). Gradle runs unit tests from the app/ folder. */
    fun seed(): List<LedgerDay> {
        val candidates = listOf(
            File("src/main/assets/seed_steps.json"),
            File("app/src/main/assets/seed_steps.json"),
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("seed_steps.json not found from " + File(".").absolutePath)
        val text = file.readText()
        val rx = Regex("\"d\":\"(\\d{4}-\\d{2}-\\d{2})\",\"s\":(\\d+),\"km\":([0-9.]+)")
        return rx.findAll(text).map { m ->
            LedgerDay(
                date = LocalDate.parse(m.groupValues[1]),
                steps = m.groupValues[2].toInt(),
                distanceKm = m.groupValues[3].toDouble(),
                goal = 10_000,
            )
        }.toList()
    }

    fun mend(days: List<LedgerDay>, vararg dates: String): List<LedgerDay> {
        var out = days
        for (s in dates) out = StreakEngine.withMend(out, LocalDate.parse(s))
        return out
    }

    /** The five single-day gaps (six calendar days with Feb 8-9) back to Sep 20 2025. */
    val FIVE_MENDS = arrayOf("2025-10-16", "2026-02-08", "2026-02-09", "2026-03-14", "2026-07-25")
}
