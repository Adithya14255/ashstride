package com.ashstride.app.ui

import com.ashstride.app.data.db.ChallengeWeek
import com.ashstride.app.domain.Challenge
import com.ashstride.app.domain.ChallengeCodec
import com.ashstride.app.domain.ChallengeGenerator
import com.ashstride.app.domain.ChallengeOdds
import com.ashstride.app.domain.ChallengeProgress
import com.ashstride.app.domain.LedgerDay
import com.ashstride.app.domain.ShrineEngine
import com.ashstride.app.domain.StepSample
import java.time.LocalDate
import java.time.ZoneId

/** A settled (or settling) week shown in the shrine history. */
data class PastWeek(
    val weekStart: LocalDate,
    val status: String,
    val picked: Challenge?,
    val progress: ChallengeProgress?,
)

/** Everything the Shrine screen and the Home teaser need. [status] is "NONE" until this week's cards exist. */
data class ShrineState(
    val weekStart: LocalDate,
    val status: String,
    val offers: List<Challenge>,
    val picked: Challenge?,
    val progress: ChallengeProgress?,
    val daysLeft: Int,
    val canPick: Boolean,
    val past: List<PastWeek>,
    /** Challenge id -> share of your recent weeks it would have cleared (null when unknown). */
    val odds: Map<String, Double?> = emptyMap(),
) {
    companion object {
        const val NONE = "NONE"

        fun build(
            rows: List<ChallengeWeek>,
            ledger: List<LedgerDay>,
            samples: List<StepSample>,
            today: LocalDate,
            zone: ZoneId,
        ): ShrineState {
            val ws = ChallengeGenerator.weekStartFor(today)
            val cur = rows.firstOrNull { it.weekStart == ws.toString() }
            val offers = if (cur == null) emptyList() else ChallengeCodec.decode(cur.offersJson)
            val picked = offers.firstOrNull { it.id == cur?.picked }
            val progress = if (picked == null) null else ShrineEngine.progressOf(picked, ledger, samples, zone)
            val past = rows
                .filter { it.weekStart != ws.toString() }
                .sortedByDescending { it.weekStart }
                .take(8)
                .map { r ->
                    val os = ChallengeCodec.decode(r.offersJson)
                    val p = os.firstOrNull { it.id == r.picked }
                    PastWeek(
                        weekStart = LocalDate.parse(r.weekStart),
                        status = r.status,
                        picked = p,
                        progress = if (p == null) null else ShrineEngine.progressOf(p, ledger, samples, zone),
                    )
                }
            val status = cur?.status ?: NONE
            return ShrineState(
                weekStart = ws,
                status = status,
                offers = offers,
                picked = picked,
                progress = progress,
                daysLeft = ShrineEngine.daysLeft(ws, today),
                canPick = ShrineEngine.canPick(status, ws, today),
                past = past,
                odds = offers.associate { it.id to ChallengeOdds.historicalRate(it, ledger) },
            )
        }
    }
}
