package com.ashstride.app.domain

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyEngineTest {
    private val story = Story(
        title = "T",
        premise = "",
        routeKm = 100.0,
        chapters = listOf(Chapter("a", "A", 0.0, 50.0, ""), Chapter("b", "B", 50.0, 100.0, "")),
        waypoints = listOf(
            Waypoint(0.0, "start", "", "a"),
            Waypoint(30.0, "mid", "", "a"),
            Waypoint(50.0, "gate", "", "b"),
            Waypoint(100.0, "end", "", "b"),
        ),
        scenes = listOf(ShrineScene("a", "arrive a", "won a", "lost a")),
    )

    @Test
    fun startOfTheRoad() {
        val p = JourneyEngine.position(story, 0.0)
        assertEquals(listOf("start"), p.reached.map { it.name })
        assertEquals("mid", p.next?.name)
        assertEquals(30.0, p.kmToNext, 0.001)
        assertEquals(0f, p.legFraction, 0.0001f)
        assertEquals("A", p.chapter.title)
        assertFalse(p.finished)
    }

    @Test
    fun betweenWaypoints() {
        val p = JourneyEngine.position(story, 45.0)
        assertEquals(listOf("start", "mid"), p.reached.map { it.name })
        assertEquals("gate", p.next?.name)
        assertEquals(5.0, p.kmToNext, 0.001)
        assertEquals(0.75f, p.legFraction, 0.0001f)
        assertEquals("A", p.chapter.title)
        assertEquals(0.45f, p.fraction, 0.0001f)
    }

    @Test
    fun exactlyOnAWaypointCountsAsReachedAndChangesChapter() {
        val p = JourneyEngine.position(story, 50.0)
        assertEquals("gate", p.reached.last().name)
        assertEquals("B", p.chapter.title)
        assertEquals("end", p.next?.name)
    }

    @Test
    fun endOfTheRoadAndClamping() {
        val p = JourneyEngine.position(story, 150.0)
        assertTrue(p.finished)
        assertNull(p.next)
        assertEquals(100.0, p.km, 0.001)
        assertEquals(1f, p.legFraction, 0.0001f)
        assertEquals(4, p.reached.size)
        assertEquals(0.0, JourneyEngine.position(story, -5.0).km, 0.001)
    }

    @Test
    fun sceneFallsBackToTheFirst() {
        assertEquals("arrive a", story.sceneFor("a").arrive)
        assertEquals("arrive a", story.sceneFor("zzz").arrive)
    }

    // ---- The real story.json ---------------------------------------------------------------

    private fun realStory(): Story {
        val candidates = listOf(File("src/main/assets/story.json"), File("app/src/main/assets/story.json"))
        val file = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("story.json not found from " + File(".").absolutePath)
        return StoryParser.parse(file.readText())
    }

    @Test
    fun realStoryIsConsistent() {
        val s = realStory()
        assertEquals(12_000.0, s.routeKm, 0.001)
        assertEquals(0.0, s.waypoints.first().km, 0.001)
        assertEquals(s.routeKm, s.waypoints.last().km, 0.001)
        assertEquals(s.waypoints.map { it.km }, s.waypoints.map { it.km }.sorted())
        assertEquals(s.waypoints.size, s.waypoints.map { it.km }.toSet().size)
        val chapterIds = s.chapters.map { it.id }.toSet()
        for (w in s.waypoints) assertTrue("waypoint ${w.name} has an unknown chapter", w.chapterId in chapterIds)
        for (c in s.chapters) assertNotNull("chapter ${c.id} needs a scene", s.scenes.firstOrNull { it.chapterId == c.id })
        assertEquals(0.0, s.chapters.first().startKm, 0.001)
        assertEquals(s.routeKm, s.chapters.last().endKm, 0.001)
        for (i in 1 until s.chapters.size) assertEquals(s.chapters[i - 1].endKm, s.chapters[i].startKm, 0.001)
    }

    @Test
    fun importedHistoryLandsPartwayAlongTheRoad() {
        val p = JourneyEngine.position(realStory(), 4_932.0)
        assertFalse(p.finished)
        assertTrue(p.fraction > 0.3f && p.fraction < 0.6f)
        assertEquals("The Glass Hills", p.chapter.title)
        assertEquals(13, p.reached.size)
        assertEquals("The Glass Overlook", p.next?.name)
        assertEquals(218.0, p.kmToNext, 0.001)
    }
}
