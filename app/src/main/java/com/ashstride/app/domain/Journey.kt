package com.ashstride.app.domain

import org.json.JSONObject

data class Waypoint(val km: Double, val name: String, val text: String, val chapterId: String)

data class Chapter(val id: String, val title: String, val startKm: Double, val endKm: Double, val intro: String)

/** Lines shown on the Shrine screen for a chapter. */
data class ShrineScene(val chapterId: String, val arrive: String, val won: String, val lost: String)

data class Story(
    val title: String,
    val premise: String,
    val routeKm: Double,
    val chapters: List<Chapter>,
    /** Sorted by km. The last one is the destination at [routeKm]. */
    val waypoints: List<Waypoint>,
    val scenes: List<ShrineScene>,
) {
    val destination: Waypoint get() = waypoints.last()

    fun sceneFor(chapterId: String): ShrineScene =
        scenes.firstOrNull { it.chapterId == chapterId } ?: scenes.first()
}

data class JourneyPosition(
    /** Distance walked, capped at the route length. */
    val km: Double,
    val routeKm: Double,
    val chapter: Chapter,
    /** Waypoints already passed, nearest the start first. */
    val reached: List<Waypoint>,
    val next: Waypoint?,
    val kmToNext: Double,
    /** 0..1 progress between the last waypoint passed and the next one. */
    val legFraction: Float,
    val finished: Boolean,
) {
    val fraction: Float get() = if (routeKm <= 0.0) 0f else (km / routeKm).toFloat().coerceIn(0f, 1f)
}

object JourneyEngine {
    fun position(story: Story, totalKm: Double): JourneyPosition {
        val km = totalKm.coerceIn(0.0, story.routeKm)
        val reached = story.waypoints.filter { it.km <= km }
        val next = story.waypoints.firstOrNull { it.km > km }
        val chapter = story.chapters.lastOrNull { km >= it.startKm } ?: story.chapters.first()
        val last = reached.lastOrNull()
        val legStart = last?.km ?: 0.0
        val legFraction = if (next == null) 1f else {
            val span = next.km - legStart
            if (span <= 0.0) 0f else ((km - legStart) / span).toFloat().coerceIn(0f, 1f)
        }
        return JourneyPosition(
            km = km,
            routeKm = story.routeKm,
            chapter = chapter,
            reached = reached,
            next = next,
            kmToNext = if (next == null) 0.0 else next.km - km,
            legFraction = legFraction,
            finished = km >= story.routeKm,
        )
    }
}

/** Reads assets/story.json. The lore can be rewritten at any time with no code changes. */
object StoryParser {
    fun parse(json: String): Story {
        val root = JSONObject(json)
        val chapters = ArrayList<Chapter>()
        val ca = root.getJSONArray("chapters")
        for (i in 0 until ca.length()) {
            val o = ca.getJSONObject(i)
            chapters.add(
                Chapter(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    startKm = o.getDouble("startKm"),
                    endKm = o.getDouble("endKm"),
                    intro = o.optString("intro", ""),
                ),
            )
        }
        val waypoints = ArrayList<Waypoint>()
        val wa = root.getJSONArray("waypoints")
        for (i in 0 until wa.length()) {
            val o = wa.getJSONObject(i)
            waypoints.add(
                Waypoint(
                    km = o.getDouble("km"),
                    name = o.getString("name"),
                    text = o.optString("text", ""),
                    chapterId = o.getString("chapter"),
                ),
            )
        }
        val scenes = ArrayList<ShrineScene>()
        val sa = root.getJSONArray("scenes")
        for (i in 0 until sa.length()) {
            val o = sa.getJSONObject(i)
            scenes.add(
                ShrineScene(
                    chapterId = o.getString("chapter"),
                    arrive = o.getString("arrive"),
                    won = o.getString("won"),
                    lost = o.getString("lost"),
                ),
            )
        }
        return Story(
            title = root.getString("title"),
            premise = root.optString("premise", ""),
            routeKm = root.getDouble("routeKm"),
            chapters = chapters.sortedBy { it.startKm },
            waypoints = waypoints.sortedBy { it.km },
            scenes = scenes,
        )
    }
}
