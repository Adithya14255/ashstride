package com.ashstride.app.ui.components

import androidx.compose.ui.graphics.Path

/**
 * The Ashstride flame mark: five crystal shards (a tall tongue, a floating sliver, a leaning blade, a hooked
 * crystal and a chevron base). Everything is in unit coordinates, u across and v down, both 0..1, and the
 * mark is [ASPECT] wide for every 1.0 of height. The numbers were traced from the tier concept art.
 *
 * Each shard is an outline plus a list of facets. A facet is a list of vertex indexes; an index of 100 or more
 * points into the shard's own extra interior points (index - 100). Every facet has a base light value, 1 =
 * facing the light (upper left), 0 = facing away.
 */
object FlameShapes {
    const val ASPECT = 0.4967f

    class Shard(
        val name: String,
        val pts: FloatArray,
        val extra: FloatArray,
        val facets: Array<IntArray>,
        val light: FloatArray,
        /** Where the inner glow of the shard is centred. */
        val hx: Float,
        val hy: Float,
        /** Glow radius in units of the mark height. */
        val hr: Float,
    )

    val A: Shard = Shard(
        name = "A",
        pts = floatArrayOf(0.5067f, 0.0728f, 0.6400f, 0.2940f, 0.3653f, 0.5762f, 0.1680f, 0.4503f, 0.1813f, 0.2874f, 0.3067f, 0.3510f, 0.3280f, 0.2411f),
        extra = floatArrayOf(0.4133f, 0.4106f),
        facets = arrayOf(intArrayOf(0, 6, 5, 100), intArrayOf(0, 100, 1), intArrayOf(1, 2, 100), intArrayOf(2, 3, 5, 100), intArrayOf(3, 4, 5)),
        light = floatArrayOf(0.88f, 0.52f, 0.22f, 0.70f, 0.97f),
        hx = 0.4267f, hy = 0.3974f, hr = 0.17f,
    )

    val B: Shard = Shard(
        name = "B",
        pts = floatArrayOf(0.6853f, 0.0132f, 0.6907f, 0.2119f, 0.5893f, 0.1258f),
        extra = floatArrayOf(0.6587f, 0.1219f),
        facets = arrayOf(intArrayOf(0, 2, 100), intArrayOf(0, 100, 1), intArrayOf(2, 1, 100)),
        light = floatArrayOf(0.95f, 0.40f, 0.15f),
        hx = 0.6533f, hy = 0.1258f, hr = 0.07f,
    )

    val C: Shard = Shard(
        name = "C",
        pts = floatArrayOf(0.0213f, 0.4742f, 0.4213f, 0.6649f, 0.4133f, 0.8808f, 0.0453f, 0.6755f),
        extra = floatArrayOf(0.2320f, 0.6384f),
        facets = arrayOf(intArrayOf(0, 1, 100), intArrayOf(1, 2, 100), intArrayOf(2, 3, 100), intArrayOf(3, 0, 100)),
        light = floatArrayOf(0.98f, 0.38f, 0.18f, 0.72f),
        hx = 0.2480f, hy = 0.6755f, hr = 0.15f,
    )

    val D: Shard = Shard(
        name = "D",
        pts = floatArrayOf(0.7333f, 0.3311f, 0.9760f, 0.4901f, 0.9787f, 0.6821f, 0.5787f, 0.8834f, 0.5840f, 0.7060f, 0.6987f, 0.6596f, 0.8240f, 0.5894f, 0.8400f, 0.5298f, 0.7227f, 0.5166f, 0.6320f, 0.6318f, 0.5147f, 0.5536f),
        extra = floatArrayOf(),
        facets = arrayOf(intArrayOf(0, 10, 9, 8), intArrayOf(0, 8, 7, 1), intArrayOf(1, 7, 6, 2), intArrayOf(2, 6, 5, 3), intArrayOf(5, 4, 3)),
        light = floatArrayOf(0.90f, 0.62f, 0.35f, 0.25f, 0.55f),
        hx = 0.7653f, hy = 0.6755f, hr = 0.18f,
    )

    val E: Shard = Shard(
        name = "E",
        pts = floatArrayOf(0.1253f, 0.8344f, 0.4987f, 0.9987f, 0.8773f, 0.8344f, 0.4987f, 0.9298f),
        extra = floatArrayOf(),
        facets = arrayOf(intArrayOf(0, 1, 3), intArrayOf(3, 1, 2)),
        light = floatArrayOf(0.85f, 0.35f),
        hx = 0.4987f, hy = 0.9219f, hr = 0.12f,
    )

    /** Draw order: back to front. The chevron is last. */
    val SHARDS: List<Shard> = listOf(A, B, C, D, E)

    /** The gold thread of a mended streak runs up the gap between the shards. */
    val SEAM: FloatArray = floatArrayOf(0.4987f, 0.9351f, 0.4987f, 0.7709f, 0.5307f, 0.6887f, 0.6160f, 0.6278f, 0.7067f, 0.5377f, 0.7387f, 0.4265f)

    /** Fills [into] with one facet of [s] placed in the box at ([ox], [oy]) of size [w] x [h]. */
    fun facet(into: Path, s: Shard, f: Int, ox: Float, oy: Float, w: Float, h: Float) {
        into.reset()
        val idx = s.facets[f]
        for (n in idx.indices) {
            val i = idx[n]
            val x: Float
            val y: Float
            if (i >= 100) {
                x = s.extra[(i - 100) * 2]
                y = s.extra[(i - 100) * 2 + 1]
            } else {
                x = s.pts[i * 2]
                y = s.pts[i * 2 + 1]
            }
            if (n == 0) into.moveTo(ox + x * w, oy + y * h) else into.lineTo(ox + x * w, oy + y * h)
        }
        into.close()
    }

    /** Fills [into] with the outline of [s]. */
    fun outline(into: Path, s: Shard, ox: Float, oy: Float, w: Float, h: Float) {
        into.reset()
        var i = 0
        while (i < s.pts.size) {
            val px = ox + s.pts[i] * w
            val py = oy + s.pts[i + 1] * h
            if (i == 0) into.moveTo(px, py) else into.lineTo(px, py)
            i += 2
        }
        into.close()
    }

    /** The whole mark as one filled shape, for the small icons: nav bar, badges, avatar, journey marker. */
    fun silhouette(ox: Float, oy: Float, w: Float, h: Float): Path {
        val all = Path()
        val one = Path()
        for (s in SHARDS) {
            outline(one, s, ox, oy, w, h)
            all.addPath(one)
        }
        return all
    }

    /** The mark fitted and centred in a box of [boxW] x [boxH] at ([left], [top]). */
    fun silhouetteIn(left: Float, top: Float, boxW: Float, boxH: Float): Path {
        var h = boxH
        var w = h * ASPECT
        if (w > boxW) {
            w = boxW
            h = w / ASPECT
        }
        return silhouette(left + (boxW - w) / 2f, top + (boxH - h) / 2f, w, h)
    }
}
