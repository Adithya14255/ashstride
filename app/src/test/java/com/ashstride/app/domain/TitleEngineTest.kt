package com.ashstride.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleEngineTest {
    private val paragon = CareerStats(
        level = 82, tier = Tier.PARAGON, bestTotalStreak = 132, bestPureStreak = 132,
        totalSteps = 6_406_335L, totalKm = 4_932.0, challengesWon = 0, legendaryWon = 0,
        mendersUsed = 5, journeyFraction = 0.41f,
    )

    private val newPlayer = CareerStats(
        level = 1, tier = Tier.WANDERER, bestTotalStreak = 0, bestPureStreak = 0,
        totalSteps = 0L, totalKm = 0.0, challengesWon = 0, legendaryWon = 0,
        mendersUsed = 0, journeyFraction = 0f,
    )

    @Test
    fun catalogSize() {
        assertEquals(21, TitleEngine.all(paragon).size)
    }

    @Test
    fun importedHistoryEarnsFourteenTitles() {
        val earned = TitleEngine.earned(paragon).map { it.id }
        assertEquals(14, earned.size)
        for (id in listOf("tier_WANDERER", "tier_PARAGON", "ember_initiate", "flamekeeper", "unbroken", "mended_soul", "far_walker", "million_strider", "stride_of_ages")) {
            assertTrue("missing $id", id in earned)
        }
        for (id in listOf("tier_IMMORTAL", "sunwalker", "pathbreaker", "shrine_pilgrim", "legends_mark")) {
            assertFalse("should not have $id", id in earned)
        }
    }

    @Test
    fun newPlayerOnlyHasTheFirstRank() {
        assertEquals(listOf("tier_WANDERER"), TitleEngine.earned(newPlayer).map { it.id })
    }

    @Test
    fun resolveUsesTheChoiceOnlyIfEarned() {
        assertEquals("tier_PARAGON", TitleEngine.resolve(paragon, null).id)
        assertEquals("flamekeeper", TitleEngine.resolve(paragon, "flamekeeper").id)
        assertEquals("tier_PARAGON", TitleEngine.resolve(paragon, "sunwalker").id)
        assertEquals("tier_PARAGON", TitleEngine.resolve(paragon, "nonsense").id)
    }

    @Test
    fun shrineTitlesFollowWins() {
        val won = paragon.copy(challengesWon = 10, legendaryWon = 1)
        val earned = TitleEngine.earned(won).map { it.id }
        assertTrue("shrine_pilgrim" in earned)
        assertTrue("shrine_warden" in earned)
        assertTrue("legends_mark" in earned)
    }
}
