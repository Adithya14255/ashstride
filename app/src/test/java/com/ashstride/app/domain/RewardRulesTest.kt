package com.ashstride.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardRulesTest {
    private val d = LocalDate.of(2026, 1, 1)

    @Test
    fun pureStreaksPayMoreThanMended() {
        assertEquals(1, RewardRules.streakShards(100, BadgeVariant.PURE))
        assertEquals(0, RewardRules.streakShards(100, BadgeVariant.MENDED))
        assertEquals(2, RewardRules.streakShards(365, BadgeVariant.PURE))
        assertEquals(1, RewardRules.streakShards(365, BadgeVariant.MENDED))
        assertEquals(3, RewardRules.streakShards(1000, BadgeVariant.PURE))
        assertEquals(0, RewardRules.streakShards(30, BadgeVariant.PURE))
    }

    @Test
    fun fiveThousandKmIsAWholeMender() {
        val five = RewardRules.DISTANCE.first { it.km == 5_000 }
        assertEquals(MenderRules.SHARDS_PER_MENDER, five.shards)
        val none = RewardRules.earned(emptyList(), 4_999.0)
        assertTrue(none.none { it.id == "road_5000" })
        val got = RewardRules.earned(emptyList(), 5_000.5)
        assertTrue(got.any { it.id == "road_5000" && it.shards == 3 })
    }

    @Test
    fun badgesBecomeRewardsWithStableIds() {
        val badges = listOf(
            BadgeAward("streak_365", BadgeVariant.PURE, d, 0),
            BadgeAward("streak_7", BadgeVariant.PURE, d, 0),
            BadgeAward("steps_10000000", BadgeVariant.STANDARD, d, 0),
            BadgeAward("steps_100000", BadgeVariant.STANDARD, d, 0),
        )
        val ids = RewardRules.earned(badges, 0.0).associate { it.id to it.shards }
        assertEquals(mapOf("streak_365|PURE" to 2, "steps_10000000|STANDARD" to 3), ids)
    }

    @Test
    fun distanceRewardsAreNotTooGenerous() {
        // Early destinations pay at most one shard; the total over the whole road stays modest.
        assertTrue(RewardRules.DISTANCE.filter { it.km < 5_000 }.all { it.shards <= 1 })
        assertTrue(RewardRules.DISTANCE.sumOf { it.shards } <= 20)
    }
}
