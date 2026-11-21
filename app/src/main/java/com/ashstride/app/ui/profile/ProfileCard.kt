package com.ashstride.app.ui.profile

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import com.ashstride.app.domain.FlameInfo
import com.ashstride.app.ui.components.FlameShapes
import com.ashstride.app.ui.components.flameColors

data class CardData(
    val name: String,
    val title: String,
    val rank: String,
    val level: Int,
    val streak: Int,
    val steps: String,
    val km: String,
    val journey: String,
    val tierColor: Int,
    val flame: FlameInfo,
)

/** Draws a shareable profile card and saves it to Pictures/Ashstride. Nothing leaves the device unless you share it. */
object ProfileCard {
    private const val W = 1080
    private const val H = 1350

    fun render(d: CardData): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        p.shader = LinearGradient(0f, 0f, 0f, H.toFloat(), 0xFF2B201A.toInt(), 0xFF171310.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
        p.shader = null

        val colors = flameColors(d.flame)
        p.shader = RadialGradient(
            W / 2f, 760f, 520f,
            (colors.mid.toArgb() and 0x00FFFFFF) or 0x55000000, 0x00000000, Shader.TileMode.CLAMP,
        )
        c.drawCircle(W / 2f, 760f, 520f, p)
        p.shader = null

        // Flame.
        val fh = 640f
        val fw = fh * FlameShapes.ASPECT
        val ox = (W - fw) / 2f
        val oy = 400f
        p.style = Paint.Style.FILL
        p.color = colors.outer.toArgb()
        c.drawPath(FlameShapes.path(FlameShapes.OUTER, ox, oy, fw, fh).asAndroidPath(), p)
        p.color = colors.mid.toArgb()
        c.drawPath(FlameShapes.path(FlameShapes.MID, ox, oy, fw, fh).asAndroidPath(), p)
        p.color = colors.core.toArgb()
        c.drawPath(FlameShapes.path(FlameShapes.CORE, ox, oy, fw, fh).asAndroidPath(), p)

        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        p.color = 0xFFF1E8DF.toInt()
        p.textSize = 104f
        c.drawText(d.name, W / 2f, 170f, p)

        p.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        p.color = d.tierColor
        p.textSize = 48f
        c.drawText(d.title + "  |  " + d.rank, W / 2f, 245f, p)

        p.color = 0xFFF1E8DF.toInt()
        p.textSize = 64f
        c.drawText("${d.streak} day streak", W / 2f, 1130f, p)

        p.color = 0xFFA39489.toInt()
        p.textSize = 42f
        c.drawText("Level ${d.level}  |  ${d.steps} steps  |  ${d.km}", W / 2f, 1200f, p)
        c.drawText(d.journey, W / 2f, 1255f, p)

        p.color = 0xFFE8581C.toInt()
        p.textSize = 36f
        c.drawText("ASHSTRIDE", W / 2f, 1310f, p)
        return bmp
    }

    /** Saves the card as a PNG in Pictures/Ashstride. Returns true on success. */
    fun save(context: Context, d: CardData): Boolean {
        return try {
            val bmp = render(d)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "ashstride_profile_" + System.currentTimeMillis() + ".png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Ashstride")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) {
                false
            } else {
                resolver.openOutputStream(uri)?.use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
