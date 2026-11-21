package com.ashstride.app.domain

import com.ashstride.app.data.db.DayEntry
import java.time.LocalDate

/**
 * Pure-Kotlin view of one ledger row. All rules in this package work on [LedgerDay],
 * never on Room entities, so they can be unit-tested on the JVM with no Android pieces.
 */
data class LedgerDay(
    val date: LocalDate,
    val steps: Int,
    val distanceKm: Double,
    /** The goal that applied on this day (history is never rewritten when the goal changes). */
    val goal: Int,
    val mended: Boolean = false,
) {
    /** The day was earned naturally. */
    val met: Boolean get() = steps >= goal

    /** The day counts for the streak only because a Mender fixed it. */
    val mendedOnly: Boolean get() = mended && !met

    /** The day keeps a streak alive (natural or mended). */
    val counts: Boolean get() = met || mended
}

fun DayEntry.toLedgerDay(): LedgerDay =
    LedgerDay(
        date = LocalDate.parse(date),
        steps = steps,
        distanceKm = distanceKm,
        goal = goal,
        mended = mended,
    )

/** Converts Room rows (any order) to a ledger sorted oldest first. */
fun List<DayEntry>.toLedger(): List<LedgerDay> = map { it.toLedgerDay() }.sortedBy { it.date }
