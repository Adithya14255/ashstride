package com.ashstride.app.ui.components

import androidx.compose.ui.graphics.Path

/**
 * The flame silhouette, shared by the streak flame, the app icon, nav icon, badges and the profile card.
 *
 * Session 6 redesign: one slim flame whose tip sweeps up and to the left, plus up to three smaller tongues
 * that are scaled copies of the same shape (the right ones mirrored so they sweep outward).
 *
 * Everything is in unit coordinates (u across, v down, both 0..1). The main body is described by two boundary
 * chains, [LEFT] and [RIGHT], that run from the base point up to the tip. Because both chains have the same
 * number of control points, every layer inside the flame (mid, inner, core) and every flowing contour line is
 * just a blend of the two chains, so they all follow the curve of the silhouette for free.
 *
 * A chain is 10 points (20 floats): the start point, then three cubic segments of three points (c1, c2, end).
 */
object FlameShapes {
    /** Width / height of the flame box. */
    const val ASPECT = 0.78f

    const val POINTS = 10

    /** Base to tip along the left edge: a soft belly, a waist, then a long sweeping tip. */
    val LEFT = floatArrayOf(
        0.48f, 1.00f,
        0.20f, 0.97f, 0.10f, 0.76f, 0.18f, 0.54f,
        0.26f, 0.35f, 0.52f, 0.30f, 0.43f, 0.12f,
        0.39f, 0.05f, 0.31f, 0.02f, 0.22f, 0.00f,
    )

    /** Base to tip along the right edge. Ends at the same tip as [LEFT]. */
    val RIGHT = floatArrayOf(
        0.48f, 1.00f,
        0.76f, 0.98f, 0.88f, 0.76f, 0.77f, 0.56f,
        0.69f, 0.40f, 0.66f, 0.34f, 0.56f, 0.22f,
        0.46f, 0.12f, 0.32f, 0.06f, 0.22f, 0.00f,
    )

    // Closed outlines in the older "start point + cubic segments of six numbers" format, still used by the small
    // icons (nav bar, badges, avatar, journey marker) through [path]. OUTER is the full body, MID and CORE are
    // nested copies of it.
    val OUTER = floatArrayOf(
        0.480f, 1.000f,
        0.200f, 0.970f, 0.100f, 0.760f, 0.180f, 0.540f,
        0.260f, 0.350f, 0.520f, 0.300f, 0.430f, 0.120f,
        0.390f, 0.050f, 0.310f, 0.020f, 0.220f, 0.000f,
        0.320f, 0.060f, 0.460f, 0.120f, 0.560f, 0.220f,
        0.660f, 0.340f, 0.690f, 0.400f, 0.770f, 0.560f,
        0.880f, 0.760f, 0.760f, 0.980f, 0.480f, 1.000f,
    )

    val MID = floatArrayOf(
        0.480f, 1.000f,
        0.301f, 0.972f, 0.240f, 0.760f, 0.286f, 0.544f,
        0.337f, 0.359f, 0.545f, 0.307f, 0.453f, 0.138f,
        0.403f, 0.063f, 0.312f, 0.027f, 0.220f, 0.000f,
        0.318f, 0.053f, 0.447f, 0.107f, 0.537f, 0.202f,
        0.635f, 0.333f, 0.613f, 0.391f, 0.664f, 0.556f,
        0.740f, 0.760f, 0.659f, 0.978f, 0.480f, 1.000f,
    )

    val CORE = floatArrayOf(
        0.480f, 1.000f,
        0.402f, 0.974f, 0.381f, 0.760f, 0.392f, 0.547f,
        0.415f, 0.368f, 0.570f, 0.314f, 0.477f, 0.156f,
        0.415f, 0.075f, 0.314f, 0.034f, 0.220f, 0.000f,
        0.316f, 0.046f, 0.435f, 0.095f, 0.513f, 0.184f,
        0.610f, 0.326f, 0.535f, 0.382f, 0.558f, 0.553f,
        0.599f, 0.760f, 0.558f, 0.976f, 0.480f, 1.000f,
    )

    /** Left and right edge chains of one flame body. */
    class Body(val left: FloatArray, val right: FloatArray)

    /**
     * A scaled copy of the main body: base point at u = [bx] on the bottom edge, [sx] and [sy] shrink it sideways
     * and upward, and [mirror] flips it so its tip sweeps to the right. The chains are swapped when mirrored so
     * the outline keeps the same winding and overlapping bodies fill cleanly.
     */
    fun body(bx: Float, sx: Float, sy: Float, mirror: Boolean): Body {
        fun move(src: FloatArray): FloatArray {
            val out = FloatArray(src.size)
            var i = 0
            while (i < src.size) {
                val dx = (src[i] - 0.48f) * (if (mirror) -sx else sx)
                out[i] = bx + dx
                out[i + 1] = 1f - (1f - src[i + 1]) * sy
                i += 2
            }
            return out
        }
        return if (mirror) Body(move(RIGHT), move(LEFT)) else Body(move(LEFT), move(RIGHT))
    }

    /** Blend of two chains: [s0] at the base, [s1] at the tip (0 = all [a], 1 = all [b]). */
    fun mix(a: FloatArray, b: FloatArray, s0: Float, s1: Float): FloatArray {
        val out = FloatArray(a.size)
        for (i in 0 until POINTS) {
            val s = s0 + (s1 - s0) * i / (POINTS - 1).toFloat()
            out[2 * i] = a[2 * i] + (b[2 * i] - a[2 * i]) * s
            out[2 * i + 1] = a[2 * i + 1] + (b[2 * i + 1] - a[2 * i + 1]) * s
        }
        return out
    }

    /**
     * Builds a path from [pts] (the older single-outline format) inside a box at ([ox], [oy]) of size [w] x [h].
     * [lean] shears the shape sideways, more toward the tip (0 at the base), so the flame can sway
     * without its base moving.
     */
    fun path(pts: FloatArray, ox: Float, oy: Float, w: Float, h: Float, lean: Float = 0f, close: Boolean = true): Path {
        fun px(i: Int): Float {
            val k = 1f - pts[i + 1]
            return ox + (pts[i] + lean * k * k) * w
        }
        fun py(i: Int): Float = oy + pts[i + 1] * h
        val path = Path()
        path.moveTo(px(0), py(0))
        var i = 2
        while (i + 5 < pts.size) {
            path.cubicTo(px(i), py(i), px(i + 2), py(i + 2), px(i + 4), py(i + 4))
            i += 6
        }
        if (close) path.close()
        return path
    }

    /** A closed leaf: up the [left] chain to the tip and back down the [right] chain. Same box rules as [path]. */
    fun leaf(left: FloatArray, right: FloatArray, ox: Float, oy: Float, w: Float, h: Float, lean: Float = 0f): Path {
        fun px(x: Float, y: Float): Float {
            val k = 1f - y
            return ox + (x + lean * k * k) * w
        }
        fun py(y: Float): Float = oy + y * h
        val p = Path()
        p.moveTo(px(left[0], left[1]), py(left[1]))
        var i = 1
        while (i <= 7) {
            p.cubicTo(
                px(left[2 * i], left[2 * i + 1]), py(left[2 * i + 1]),
                px(left[2 * i + 2], left[2 * i + 3]), py(left[2 * i + 3]),
                px(left[2 * i + 4], left[2 * i + 5]), py(left[2 * i + 5]),
            )
            i += 3
        }
        var j = 9
        while (j >= 3) {
            p.cubicTo(
                px(right[2 * (j - 1)], right[2 * (j - 1) + 1]), py(right[2 * (j - 1) + 1]),
                px(right[2 * (j - 2)], right[2 * (j - 2) + 1]), py(right[2 * (j - 2) + 1]),
                px(right[2 * (j - 3)], right[2 * (j - 3) + 1]), py(right[2 * (j - 3) + 1]),
            )
            j -= 3
        }
        p.close()
        return p
    }

    /** An open curve along one chain (a flowing contour line, the mend seam). Same box rules as [path]. */
    fun line(chain: FloatArray, ox: Float, oy: Float, w: Float, h: Float, lean: Float = 0f): Path {
        fun px(x: Float, y: Float): Float {
            val k = 1f - y
            return ox + (x + lean * k * k) * w
        }
        fun py(y: Float): Float = oy + y * h
        val p = Path()
        p.moveTo(px(chain[0], chain[1]), py(chain[1]))
        var i = 1
        while (i <= 7) {
            p.cubicTo(
                px(chain[2 * i], chain[2 * i + 1]), py(chain[2 * i + 1]),
                px(chain[2 * i + 2], chain[2 * i + 3]), py(chain[2 * i + 3]),
                px(chain[2 * i + 4], chain[2 * i + 5]), py(chain[2 * i + 5]),
            )
            i += 3
        }
        return p
    }

    /** The whole flame as one filled shape: main body plus its two side tongues. Used by badges, the avatar and the app icon. */
    fun silhouette(ox: Float, oy: Float, w: Float, h: Float, lean: Float = 0f): Path {
        val p = Path()
        val r = body(0.79f, 0.40f, 0.72f, true)
        val l = body(0.21f, 0.36f, 0.60f, false)
        p.addPath(leaf(r.left, r.right, ox, oy, w, h, lean))
        p.addPath(leaf(l.left, l.right, ox, oy, w, h, lean))
        p.addPath(leaf(LEFT, RIGHT, ox, oy, w, h, lean))
        return p
    }
}
