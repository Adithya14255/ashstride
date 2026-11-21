package com.ashstride.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeCatalogTest {
    private fun d(s: String): LocalDate = LocalDate.parse(s)

    @Test
    fun stepGlyphs() {
        assertEquals("100k", BadgeCatalog.glyphForSteps(100_000L))
        assertEquals("250k", BadgeCatalog.glyphForSteps(250_000L))
        assertEquals("1M", BadgeCatalog.glyphForSteps(1_000_000L))
        assertEquals("2.5M", BadgeCatalog.glyphForSteps(2_500_000L))
        assertEquals("10M", BadgeCatalog.glyphForSteps(10_000_000L))
    }

    @Test
    fun titles() {
        assertEquals("30-day streak", BadgeCatalog.titleForId("streak_30"))
        assertEquals("1,000,000 lifetime steps", BadgeCatalog.titleForId("steps_1000000"))
    }

    private val hard = Challenge("2026-11-21:HARD", d("2026-11-21"), ChallengeTier.HARD, ChallengeType.WEEKLY_TOTAL, 1_500, 1, BadgeRim.GOLD, targetSteps = 97_000)

    @Test
    fun entriesMixAwardsAndWinsNewestFirst() {
        val awards = listOf(
            BadgeAward("streak_7", BadgeVariant.PURE, d("2025-09-05"), 100),
            BadgeAward("steps_100000", BadgeVariant.STANDARD, d("2025-09-01"), 150),
            BadgeAward("streak_30", BadgeVariant.MENDED, d("2025-11-15"), 300),
        )
        val list = BadgeCatalog.entries(awards, listOf(WonChallenge(hard, d("2026-11-28"))))
        assertEquals(
            listOf("challenge_2026-11-21_HARD", "streak_30", "streak_7", "steps_100000"),
            list.map { it.badgeId },
        )

        val win = list[0]
        assertEquals(BadgeKind.CHALLENGE, win.kind)
        assertEquals(BadgeRim.GOLD, win.rim)
        assertEquals("Hard trial", win.variantLabel)
        assertEquals("challenge_2026-11-21_HARD|STANDARD", win.key)
        assertEquals(1_500, win.xp)

        val mended = list[1]
        assertEquals(BadgeRim.SILVER, mended.rim)
        assertTrue(mended.stitched)
        assertEquals("Mended", mended.variantLabel)

        val pure = list[2]
        assertEquals(BadgeRim.GOLD, pure.rim)
        assertFalse(pure.stitched)
        assertEquals("7", pure.glyph)
        assertEquals("streak_7|PURE", pure.key)

        val steps = list[3]
        assertEquals(BadgeKind.STEPS, steps.kind)
        assertEquals(BadgeRim.BRONZE, steps.rim)
        assertEquals("100k", steps.glyph)
    }
}
