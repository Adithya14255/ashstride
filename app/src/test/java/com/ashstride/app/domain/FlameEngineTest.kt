package com.ashstride.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlameEngineTest {

    @Test
    fun stageIsTotalStreakDividedByFive() {
        assertEquals(0, FlameEngine.stageFor(0))
        assertEquals(0, FlameEngine.stageFor(4))
        assertEquals(1, FlameEngine.stageFor(5))
        assertEquals(23, FlameEngine.stageFor(118))
        assertEquals(0, FlameEngine.stageFor(-3))
    }

    @Test
    fun bandsMatchTheSpec() {
        assertEquals(FlameLook.ASH_GREY, FlameEngine.info(0, false).look)
        assertEquals(FlameLook.ASH_GREY, FlameEngine.info(4, false).look)
        assertEquals(FlameLook.RED, FlameEngine.info(5, false).look)
        assertEquals(FlameLook.DEEP_ORANGE, FlameEngine.info(10, false).look)
        assertEquals(FlameLook.GOLD, FlameEngine.info(25, false).look)
        assertEquals(FlameLook.WHITE_HOT, FlameEngine.info(35, false).look)
        assertEquals(FlameLook.PALE_BLUE, FlameEngine.info(40, false).look)
        assertEquals(FlameLook.EMERALD, FlameEngine.info(60, false).look)
        assertEquals(FlameLook.VIOLET, FlameEngine.info(70, false).look)
        assertEquals(FlameLook.MAGENTA, FlameEngine.info(80, false).look)
        assertEquals(FlameLook.CRIMSON_GOLD, FlameEngine.info(90, false).look)
        assertEquals(FlameLook.GOLDEN_WHITE, FlameEngine.info(95, false).look)
        assertEquals(FlameLook.GOLDEN_WHITE, FlameEngine.info(99, false).look)
    }

    @Test
    fun solarFromHundredWithAnExtraLayerEveryFiveDays() {
        assertEquals(FlameLook.SOLAR, FlameEngine.info(100, false).look)
        assertEquals(0, FlameEngine.info(100, false).solarLayers)
        assertEquals(0, FlameEngine.info(104, false).solarLayers)
        assertEquals(1, FlameEngine.info(105, false).solarLayers)
        assertEquals(3, FlameEngine.info(118, false).solarLayers)
        assertEquals(FlameLook.SOLAR, FlameEngine.info(199, false).look)
        assertEquals(0, FlameEngine.info(99, false).solarLayers)
    }

    @Test
    fun prismaticCycleFromTwoHundred() {
        val at200 = FlameEngine.info(200, false)
        assertEquals(FlameLook.PRISMATIC, at200.look)
        assertEquals(0, at200.hueShiftDegrees)
        assertEquals(3, FlameEngine.info(201, false).hueShiftDegrees)
        assertEquals(0, FlameEngine.info(199, false).hueShiftDegrees)
        assertTrue(FlameEngine.info(251, false).solarLayers > FlameEngine.info(200, false).solarLayers)
    }

    @Test
    fun flameGrowsSlightlyEveryDay() {
        assertEquals(0f, FlameEngine.info(10, false).growth, 0.0001f)
        assertEquals(0.5f, FlameEngine.info(12, false).growth, 0.0001f)
        assertEquals(1f, FlameEngine.info(14, false).growth, 0.0001f)
        assertEquals(0f, FlameEngine.info(15, false).growth, 0.0001f)
        assertEquals(2, FlameEngine.info(12, false).dayInStage)
    }

    @Test
    fun mendSeamFollowsTheRun() {
        assertTrue(FlameEngine.info(251, true).hasMendSeam)
        assertFalse(FlameEngine.info(118, false).hasMendSeam)
    }
}
