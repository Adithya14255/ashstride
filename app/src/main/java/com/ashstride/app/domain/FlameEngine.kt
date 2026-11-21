package com.ashstride.app.domain

enum class FlameLook {
    ASH_GREY, RED, DEEP_ORANGE, ORANGE, AMBER, GOLD, YELLOW, WHITE_HOT,
    PALE_BLUE, ELECTRIC_BLUE, CYAN, TEAL, EMERALD, LIME, VIOLET, PURPLE,
    MAGENTA, ROSE, CRIMSON_GOLD, GOLDEN_WHITE,
    SOLAR, PRISMATIC,
}

data class FlameInfo(
    /** Total streak / 5. A new look every stage. */
    val stage: Int,
    val look: FlameLook,
    /** 0..4: days since the stage began. */
    val dayInStage: Int,
    /** 0.0..1.0 slight daily growth inside a stage, so something changes every day. */
    val growth: Float,
    /** Extra rings/layers on the Solar flame: +1 every 5 days past 100. */
    val solarLayers: Int,
    /** Colour rotation for the prismatic cycle (200+), 0 otherwise. */
    val hueShiftDegrees: Int,
    /** A run containing a mend shows a thin gold-thread seam. */
    val hasMendSeam: Boolean,
)

object FlameEngine {
    const val DAYS_PER_STAGE = 5
    const val SOLAR_FROM = 100
    const val PRISMATIC_FROM = 200

    private val bands: List<FlameLook> = listOf(
        FlameLook.ASH_GREY, FlameLook.RED, FlameLook.DEEP_ORANGE, FlameLook.ORANGE,
        FlameLook.AMBER, FlameLook.GOLD, FlameLook.YELLOW, FlameLook.WHITE_HOT,
        FlameLook.PALE_BLUE, FlameLook.ELECTRIC_BLUE, FlameLook.CYAN, FlameLook.TEAL,
        FlameLook.EMERALD, FlameLook.LIME, FlameLook.VIOLET, FlameLook.PURPLE,
        FlameLook.MAGENTA, FlameLook.ROSE, FlameLook.CRIMSON_GOLD, FlameLook.GOLDEN_WHITE,
    )

    fun stageFor(totalStreak: Int): Int = (if (totalStreak < 0) 0 else totalStreak) / DAYS_PER_STAGE

    fun info(totalStreak: Int, runHasMend: Boolean): FlameInfo {
        val total = if (totalStreak < 0) 0 else totalStreak
        val stage = total / DAYS_PER_STAGE
        val dayInStage = total % DAYS_PER_STAGE
        val look = when {
            total >= PRISMATIC_FROM -> FlameLook.PRISMATIC
            total >= SOLAR_FROM -> FlameLook.SOLAR
            else -> bands[stage]
        }
        val layers = if (total >= SOLAR_FROM) (total - SOLAR_FROM) / DAYS_PER_STAGE else 0
        val hue = if (total >= PRISMATIC_FROM) ((total - PRISMATIC_FROM) * 3) % 360 else 0
        return FlameInfo(
            stage = stage,
            look = look,
            dayInStage = dayInStage,
            growth = dayInStage / (DAYS_PER_STAGE - 1).toFloat(),
            solarLayers = layers,
            hueShiftDegrees = hue,
            hasMendSeam = runHasMend,
        )
    }
}
