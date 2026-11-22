package com.ashstride.app.ui.theme

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Design tokens of the gilded card look (Session 7 design pass): night-blue surfaces, hairline gold, cream
 * lettering, chamfered corners. The profile card and stat sheet use these today; the rest of the app moves
 * onto them next, so every new colour for this look goes here and nowhere else.
 */
object Gilt {
    /** Deepest background, the card body. */
    val Night = Color(0xFF0A0C13)

    /** Panels and the art window sky. */
    val NightMid = Color(0xFF11141E)

    /** Rows, plates and chips sitting on a panel. */
    val NightLift = Color(0xFF1A1E2C)

    /** Hairline gold: borders, dividers, empty tracks' outlines. */
    val Line = Color(0xFF8A7338)

    /** Gold used for fills and strokes. */
    val GoldMid = Color(0xFFD1AE5C)
    val GoldBright = Color(0xFFF7DC8A)
    val GoldDeep = Color(0xFF8A6422)

    /** Label lettering. */
    val Cream = Color(0xFFF1E4BE)

    /** Primary text on night surfaces. */
    val Ink = Color(0xFFF6F0E2)

    /** Secondary text on night surfaces (at least 4.5:1 on [NightLift]). */
    val Muted = Color(0xFFB3A88F)

    /** Gradient stops that make metal catch the light. Use with a mirrored linear gradient. */
    val Metal: List<Color> = listOf(
        Color(0xFF8A6422), Color(0xFFF7DC8A), Color(0xFFC79A38), Color(0xFFFFF3C4),
        Color(0xFFA67C2A), Color(0xFFF2CF76), Color(0xFF8A6422),
    )

    /** Rainbow stops for the Immortal border and foil. */
    val Prism: List<Color> = listOf(
        Color(0xFFFF5E5E), Color(0xFFFFD25E), Color(0xFF7DFF9E), Color(0xFF5EE6FF),
        Color(0xFF9D7DFF), Color(0xFFFF7DE0), Color(0xFFFF5E5E),
    )
}

/** A rectangle with its four corners cut at 45 degrees: the shape of every panel in the gilded look. */
class ChamferShape(private val cut: Dp = 14.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val c = minOf(with(density) { cut.toPx() }, size.minDimension / 2f)
        val p = Path()
        p.moveTo(c, 0f)
        p.lineTo(size.width - c, 0f)
        p.lineTo(size.width, c)
        p.lineTo(size.width, size.height - c)
        p.lineTo(size.width - c, size.height)
        p.lineTo(c, size.height)
        p.lineTo(0f, size.height - c)
        p.lineTo(0f, c)
        p.close()
        return Outline.Generic(p)
    }
}

/** Gold metal as a brush, for borders. */
fun giltBrush(width: Float = 600f): Brush =
    Brush.linearGradient(Gilt.Metal, start = Offset(0f, 0f), end = Offset(width, width * 0.6f), tileMode = TileMode.Mirror)

/** A gold border in the given [shape]. */
fun Modifier.giltBorder(shape: Shape, width: Dp = 1.5.dp, alpha: Float = 0.85f): Modifier =
    this.border(width, Brush.linearGradient(Gilt.Metal.map { it.copy(alpha = alpha) }, start = Offset(0f, 0f), end = Offset(700f, 420f), tileMode = TileMode.Mirror), shape)
