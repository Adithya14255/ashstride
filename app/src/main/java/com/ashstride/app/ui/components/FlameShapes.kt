package com.ashstride.app.ui.components

import androidx.compose.ui.graphics.Path

/**
 * The flame silhouette, shared by the streak flame, the app icon, nav icon, badges and the profile card.
 * Three layers in unit coordinates (u across, v down, both 0..1): a big outer tongue with a hooked right tail,
 * a lighter inner flame leaning right with a small left tongue, and a small ember heart.
 * Each array is a start point followed by cubic segments of six numbers (c1, c2, end).
 */
object FlameShapes {
    /** Width / height of the flame box. */
    const val ASPECT = 0.772f

    val OUTER = floatArrayOf(
        0.400f, 0.000f,  // start
        0.470f, 0.130f, 0.800f, 0.270f, 0.790f, 0.520f,
        0.800f, 0.480f, 0.840f, 0.450f, 0.865f, 0.430f,
        0.930f, 0.500f, 1.000f, 0.600f, 0.990f, 0.740f,
        0.980f, 0.880f, 0.800f, 1.000f, 0.500f, 1.000f,
        0.200f, 1.000f, 0.010f, 0.900f, 0.010f, 0.720f,
        0.010f, 0.560f, 0.150f, 0.460f, 0.230f, 0.320f,
        0.310f, 0.180f, 0.390f, 0.100f, 0.400f, 0.000f,
    )

    val MID = floatArrayOf(
        0.585f, 0.260f,  // start
        0.600f, 0.410f, 0.800f, 0.500f, 0.800f, 0.700f,
        0.800f, 0.880f, 0.670f, 0.975f, 0.500f, 0.975f,
        0.330f, 0.975f, 0.190f, 0.890f, 0.190f, 0.720f,
        0.190f, 0.620f, 0.200f, 0.550f, 0.225f, 0.490f,
        0.235f, 0.550f, 0.265f, 0.580f, 0.300f, 0.585f,
        0.310f, 0.440f, 0.520f, 0.400f, 0.585f, 0.260f,
    )

    val CORE = floatArrayOf(
        0.520f, 0.580f,  // start
        0.530f, 0.690f, 0.660f, 0.750f, 0.660f, 0.860f,
        0.660f, 0.930f, 0.590f, 0.960f, 0.500f, 0.960f,
        0.410f, 0.960f, 0.340f, 0.930f, 0.340f, 0.860f,
        0.340f, 0.750f, 0.500f, 0.690f, 0.520f, 0.580f,
    )


    /** The gold-thread seam for a run that contains a mend, drawn up the middle of the inner flame. */
    val SEAM = floatArrayOf(
        0.500f, 0.960f,
        0.470f, 0.800f, 0.640f, 0.600f, 0.585f, 0.300f,
    )

    /**
     * Builds a path from [pts] inside a box at ([ox], [oy]) of size [w] x [h].
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
}
