package com.ashstride.app.domain

/** Everything the profile and its titles are computed from. */
data class CareerStats(
    val level: Int,
    val tier: Tier,
    val bestTotalStreak: Int,
    val bestPureStreak: Int,
    val totalSteps: Long,
    val totalKm: Double,
    val challengesWon: Int,
    val legendaryWon: Int,
    val mendersUsed: Int,
    /** 0..1 along the story route. */
    val journeyFraction: Float,
)

data class Title(val id: String, val name: String, val how: String, val earned: Boolean)

object TitleEngine {
    fun tierTitleId(tier: Tier): String = "tier_" + tier.name

    /** The rank title used when the player has not picked one. */
    fun defaultTitleId(stats: CareerStats): String = tierTitleId(stats.tier)

    /** Every title, earned or not, ordered tiers first then special titles. */
    fun all(stats: CareerStats): List<Title> {
        val out = ArrayList<Title>()
        for (t in Tier.values()) {
            out.add(Title(tierTitleId(t), t.title, "Reach level ${t.firstLevel}", stats.level >= t.firstLevel))
        }
        fun add(id: String, name: String, how: String, ok: Boolean) { out.add(Title(id, name, how, ok)) }
        add("ember_initiate", "Ember Initiate", "Keep a 7-day streak", stats.bestTotalStreak >= 7)
        add("flamekeeper", "Flamekeeper", "Keep a 30-day streak", stats.bestTotalStreak >= 30)
        add("unbroken", "Unbroken", "Keep a pure 100-day streak", stats.bestPureStreak >= 100)
        add("sunwalker", "Sunwalker", "Keep a 365-day streak", stats.bestTotalStreak >= 365)
        add("mended_soul", "Mended Soul", "Use a Mender", stats.mendersUsed >= 1)
        add("shrine_pilgrim", "Shrine Pilgrim", "Win a weekly challenge", stats.challengesWon >= 1)
        add("shrine_warden", "Shrine Warden", "Win 10 weekly challenges", stats.challengesWon >= 10)
        add("legends_mark", "Legend's Mark", "Win a Legendary challenge", stats.legendaryWon >= 1)
        add("far_walker", "Far Walker", "Travel a quarter of the route", stats.journeyFraction >= 0.25f)
        add("pathbreaker", "Pathbreaker", "Travel half the route", stats.journeyFraction >= 0.5f)
        add("arrived", "Arrived", "Reach the end of the route", stats.journeyFraction >= 1f)
        add("million_strider", "Million Strider", "Walk 1,000,000 steps", stats.totalSteps >= 1_000_000L)
        add("stride_of_ages", "Stride of Ages", "Walk 5,000,000 steps", stats.totalSteps >= 5_000_000L)
        return out
    }

    fun earned(stats: CareerStats): List<Title> = all(stats).filter { it.earned }

    /** The chosen title if it is earned, otherwise the rank title. */
    fun resolve(stats: CareerStats, chosenId: String?): Title {
        val earned = earned(stats)
        return earned.firstOrNull { it.id == chosenId }
            ?: earned.first { it.id == defaultTitleId(stats) }
    }
}
