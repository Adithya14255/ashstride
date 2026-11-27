package com.ashstride.app.domain

/** A one-off Mender Shard payout for an immense achievement. */
data class ShardReward(
    /** Stable id, stored once paid so it can never pay twice: "streak_365|PURE", "steps_5000000|STANDARD", "road_5000". */
    val id: String,
    val shards: Int,
    val title: String,
    val detail: String,
)

/** Distance destinations on the road that pay shards. [km] is cumulative distance walked. */
data class DistanceReward(val km: Int, val shards: Int, val title: String)

/**
 * Shards for the big milestones, on top of Hard and Legendary trials. Three shards forge a Mender, so a 3-shard reward
 * is a Mender on the spot. Streak rewards follow the pure-pays-more rule: a mended milestone pays about half.
 * Rewards already earned when this system arrived are paid once, as a backlog ([PAY_BACKLOG] = true).
 */
object RewardRules {
    const val PAY_BACKLOG = true

    private val PURE_STREAK: Map<Int, Int> = mapOf(100 to 1, 200 to 1, 365 to 2, 500 to 2, 730 to 3, 1000 to 3)
    private val STEPS: Map<Long, Int> = mapOf(2_500_000L to 1, 5_000_000L to 2, 10_000_000L to 3)

    val DISTANCE: List<DistanceReward> = listOf(
        DistanceReward(2_200, 1, "Saltgate"),
        DistanceReward(3_800, 1, "Foot of the Glass Hills"),
        DistanceReward(5_000, 3, "Five thousand kilometres"),
        DistanceReward(7_600, 2, "Lastlight Pass"),
        DistanceReward(9_800, 2, "Foot of the Sunward Stair"),
        DistanceReward(11_400, 3, "The Last Shrine"),
        DistanceReward(12_000, 3, "The First Flame"),
    )

    fun streakShards(milestone: Int, variant: BadgeVariant): Int {
        val pure = PURE_STREAK[milestone] ?: 0
        return if (variant == BadgeVariant.MENDED) pure / 2 else pure
    }

    fun stepsShards(milestone: Long): Int = STEPS[milestone] ?: 0

    /** Every shard reward the ledger has earned so far (paid or not; the caller diffs against what is stored). */
    fun earned(badges: List<BadgeAward>, totalKm: Double): List<ShardReward> {
        val out = ArrayList<ShardReward>()
        for (b in badges) {
            if (b.badgeId.startsWith("streak_")) {
                val m = b.badgeId.removePrefix("streak_").toIntOrNull() ?: continue
                val n = streakShards(m, b.variant)
                if (n > 0) {
                    val kind = if (b.variant == BadgeVariant.MENDED) "mended" else "pure"
                    out.add(ShardReward(BadgeCatalog.keyOf(b.badgeId, b.variant), n, "$m-day streak", "A $kind $m-day streak."))
                }
            } else if (b.badgeId.startsWith("steps_")) {
                val m = b.badgeId.removePrefix("steps_").toLongOrNull() ?: continue
                val n = stepsShards(m)
                if (n > 0) {
                    out.add(ShardReward(BadgeCatalog.keyOf(b.badgeId, b.variant), n, BadgeCatalog.titleForId(b.badgeId), "A lifetime of steps."))
                }
            }
        }
        for (d in DISTANCE) {
            if (totalKm >= d.km) {
                out.add(ShardReward("road_" + d.km, d.shards, d.title, "${d.km} km walked along the road."))
            }
        }
        return out
    }

    fun menderText(shards: Int): String = when {
        shards >= MenderRules.SHARDS_PER_MENDER -> "a whole Mender"
        shards == 1 -> "1 shard"
        else -> "$shards shards"
    }
}
