package com.ashstride.app.domain

import java.time.LocalDate

enum class BadgeKind { STREAK, STEPS, CHALLENGE, FEAT }

/** One badge as the UI shows it: streak and lifetime badges from the ledger, challenge badges from won weeks. */
data class BadgeEntry(
    /** Stable key used for equipping and the profile showcase: "badgeId|VARIANT". */
    val key: String,
    val badgeId: String,
    val kind: BadgeKind,
    val title: String,
    /** Short text drawn on the badge: "7", "1M". Empty for challenge badges (they draw a star). */
    val glyph: String,
    val rim: BadgeRim,
    /** Mended streak badges are silver with a stitched ring. */
    val stitched: Boolean,
    /** "Pure", "Mended", "Lifetime" or the challenge tier. */
    val variantLabel: String,
    val date: LocalDate,
    val xp: Int,
    /** Honor paid by a great feat (0 for every other badge). */
    val honor: Int = 0,
)

data class WonChallenge(val challenge: Challenge, val date: LocalDate)

object BadgeCatalog {
    fun keyOf(badgeId: String, variant: BadgeVariant): String = badgeId + "|" + variant.name

    fun challengeBadgeId(c: Challenge): String = "challenge_" + c.weekStart + "_" + c.tier.name

    fun glyphForSteps(milestone: Long): String = when {
        milestone >= 1_000_000L && milestone % 1_000_000L == 0L -> "${milestone / 1_000_000L}M"
        milestone >= 1_000_000L -> {
            val tenths = milestone / 100_000L
            "${tenths / 10}.${tenths % 10}M"
        }
        else -> "${milestone / 1_000L}k"
    }

    fun titleForId(badgeId: String): String = when {
        badgeId.startsWith("streak_") -> "${badgeId.removePrefix("streak_")}-day streak"
        badgeId.startsWith("steps_") -> {
            val m = badgeId.removePrefix("steps_").toLongOrNull() ?: 0L
            String.format(java.util.Locale.US, "%,d lifetime steps", m)
        }
        else -> badgeId
    }

    private fun fromAward(a: BadgeAward): BadgeEntry {
        val isStreak = a.badgeId.startsWith("streak_")
        val glyph = if (isStreak) {
            a.badgeId.removePrefix("streak_")
        } else {
            glyphForSteps(a.badgeId.removePrefix("steps_").toLongOrNull() ?: 0L)
        }
        return BadgeEntry(
            key = keyOf(a.badgeId, a.variant),
            badgeId = a.badgeId,
            kind = if (isStreak) BadgeKind.STREAK else BadgeKind.STEPS,
            title = titleForId(a.badgeId),
            glyph = glyph,
            rim = when (a.variant) {
                BadgeVariant.PURE -> BadgeRim.GOLD
                BadgeVariant.MENDED -> BadgeRim.SILVER
                BadgeVariant.STANDARD -> BadgeRim.BRONZE
            },
            stitched = a.variant == BadgeVariant.MENDED,
            variantLabel = when (a.variant) {
                BadgeVariant.PURE -> "Pure"
                BadgeVariant.MENDED -> "Mended"
                BadgeVariant.STANDARD -> "Lifetime"
            },
            date = a.date,
            xp = a.xp,
        )
    }

    private fun fromWin(w: WonChallenge): BadgeEntry {
        val c = w.challenge
        val id = challengeBadgeId(c)
        return BadgeEntry(
            key = keyOf(id, BadgeVariant.STANDARD),
            badgeId = id,
            kind = BadgeKind.CHALLENGE,
            title = ChallengeText.title(c),
            glyph = "",
            rim = c.rim,
            stitched = false,
            variantLabel = ChallengeText.tierName(c.tier) + " trial",
            date = w.date,
            xp = c.xp,
        )
    }

    private fun fromFeat(f: FeatRecord): BadgeEntry = BadgeEntry(
        key = keyOf(f.def.id, BadgeVariant.STANDARD),
        badgeId = f.def.id,
        kind = BadgeKind.FEAT,
        title = f.def.title,
        glyph = f.def.glyph,
        rim = f.def.rim,
        stitched = false,
        variantLabel = if (f.times > 1) "Great feat x${f.times}" else "Great feat",
        date = f.firstDate,
        xp = 0,
        honor = f.honorEarned,
    )

    /** Every badge earned, newest first. */
    fun entries(awards: List<BadgeAward>, wins: List<WonChallenge>, feats: List<FeatRecord> = emptyList()): List<BadgeEntry> {
        val all = ArrayList<BadgeEntry>()
        for (a in awards) all.add(fromAward(a))
        for (w in wins) all.add(fromWin(w))
        for (f in feats) all.add(fromFeat(f))
        return all.sortedWith(compareByDescending<BadgeEntry> { it.date }.thenBy { it.badgeId })
    }
}
