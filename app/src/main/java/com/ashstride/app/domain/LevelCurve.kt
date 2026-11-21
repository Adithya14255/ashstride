package com.ashstride.app.domain

enum class Tier(val title: String, val firstLevel: Int) {
    WANDERER("Wanderer", 1),
    PATHFINDER("Pathfinder", 10),
    RANGER("Ranger", 20),
    TRAILBLAZER("Trailblazer", 35),
    VANGUARD("Vanguard", 50),
    WARDEN("Warden", 65),
    PARAGON("Paragon", 80),
    IMMORTAL("Immortal", 100);

    companion object {
        fun forLevel(level: Int): Tier = values().last { level >= it.firstLevel || it == Tier.WANDERER }
    }
}

data class LevelInfo(
    val level: Int,
    val totalXp: Long,
    /** XP earned inside the current level. */
    val xpIntoLevel: Long,
    /** XP the current level needs in total. */
    val xpForLevel: Long,
    val tier: Tier,
    /** Gems on the tier emblem, 0..4 (always 0 for Immortal, which uses [immortalRank]). */
    val gems: Int,
    /** 0 below level 100; 1 = Immortal I, 2 = Immortal II, a new rank every 25 levels. */
    val immortalRank: Int,
) {
    val fractionIntoLevel: Float
        get() = if (xpForLevel <= 0L) 0f else xpIntoLevel.toFloat() / xpForLevel.toFloat()

    /** "Paragon" below 100, "Immortal III" from 100 up. */
    val rankTitle: String
        get() = if (immortalRank > 0) "Immortal " + LevelCurve.roman(immortalRank) else tier.title
}

/**
 * Levels 1-100 follow a gently rising curve (each level costs 125 + 24 x level XP),
 * tuned so the imported history lands in the Paragon band (Lv 80-85).
 * From level 100 every level costs the same flat amount and there is no cap.
 */
object LevelCurve {
    const val BASE_XP = 125
    const val SLOPE_XP = 24
    const val IMMORTAL_START = 100
    const val IMMORTAL_XP_PER_LEVEL = 2_600
    const val LEVELS_PER_IMMORTAL_RANK = 25

    /** XP needed to go from [level] to level + 1. */
    fun xpToNext(level: Int): Long =
        if (level >= IMMORTAL_START) IMMORTAL_XP_PER_LEVEL.toLong() else (BASE_XP + SLOPE_XP * level).toLong()

    /** Total XP at which [level] begins (level 1 begins at 0). */
    fun xpToReach(level: Int): Long {
        if (level <= 1) return 0L
        if (level <= IMMORTAL_START) {
            val n = level.toLong()
            return BASE_XP * (n - 1) + (SLOPE_XP / 2) * n * (n - 1)
        }
        return xpToReach(IMMORTAL_START) + (level - IMMORTAL_START).toLong() * IMMORTAL_XP_PER_LEVEL
    }

    fun infoFor(totalXp: Long): LevelInfo {
        val xp = if (totalXp < 0L) 0L else totalXp
        var level = 1
        while (level < IMMORTAL_START && xp >= xpToReach(level + 1)) level++
        if (level == IMMORTAL_START) {
            level = IMMORTAL_START + ((xp - xpToReach(IMMORTAL_START)) / IMMORTAL_XP_PER_LEVEL).toInt()
        }
        val tier = Tier.forLevel(level)
        val rank = if (level >= IMMORTAL_START) (level - IMMORTAL_START) / LEVELS_PER_IMMORTAL_RANK + 1 else 0
        val gems = if (tier == Tier.IMMORTAL) 0 else minOf(4, (level - tier.firstLevel) / 5)
        return LevelInfo(
            level = level,
            totalXp = xp,
            xpIntoLevel = xp - xpToReach(level),
            xpForLevel = xpToNext(level),
            tier = tier,
            gems = gems,
            immortalRank = rank,
        )
    }

    fun roman(n: Int): String {
        var rest = n
        val sb = StringBuilder()
        val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
        val symbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
        for (i in values.indices) {
            while (rest >= values[i]) {
                sb.append(symbols[i])
                rest -= values[i]
            }
        }
        return sb.toString()
    }
}
