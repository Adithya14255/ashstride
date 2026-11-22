package com.ashstride.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.ashstride.app.AshstrideApp
import com.ashstride.app.MainActivity
import com.ashstride.app.R
import com.ashstride.app.domain.FlameInfo
import com.ashstride.app.domain.Progress
import com.ashstride.app.domain.toLedger
import com.ashstride.app.sync.SyncPrefs
import com.ashstride.app.ui.Fmt
import com.ashstride.app.ui.components.drawFlame
import com.ashstride.app.ui.components.flameName
import com.ashstride.app.ui.theme.AshHigh
import com.ashstride.app.ui.theme.Ember
import com.ashstride.app.ui.theme.Gold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Home-screen widget: today's goal ring with the streak flame inside, steps and streak underneath.
 * Built on plain RemoteViews (no extra library): the ring and flame are drawn into a bitmap with the
 * same drawing code as the app, so the widget flame always matches the in-app flame.
 */
class AshWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                update(context, appWidgetManager, appWidgetIds)
            } catch (e: Exception) {
                // Leave the previous picture in place.
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Redraws every placed widget. Call after steps change. */
        suspend fun refresh(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, AshWidgetProvider::class.java))
            if (ids.isEmpty()) return
            update(context, mgr, ids)
        }

        private suspend fun update(context: Context, mgr: AppWidgetManager, ids: IntArray) {
            val app = context.applicationContext as AshstrideApp
            val ledger = app.db.dayDao().getAll().toLedger()
            if (ledger.isEmpty()) return
            val today = LocalDate.now()
            val snap = Progress.compute(ledger, today, 0L)
            val row = ledger.firstOrNull { it.date == today }
            val steps = row?.steps ?: 0
            val goal = row?.goal ?: SyncPrefs(app).goal()
            val bmp = renderArt(steps, goal, snap.flame)
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            for (id in ids) {
                val rv = RemoteViews(context.packageName, R.layout.widget_ash)
                rv.setImageViewBitmap(R.id.widget_art, bmp)
                rv.setTextViewText(R.id.widget_steps, Fmt.n(steps))
                rv.setTextViewText(R.id.widget_sub, "of ${Fmt.n(goal)}  |  ${Fmt.n(snap.streak.total)} day streak")
                rv.setOnClickPendingIntent(R.id.widget_root, open)
                mgr.updateAppWidget(id, rv)
            }
        }

        private fun renderArt(steps: Int, goal: Int, flame: FlameInfo): Bitmap {
            val px = 420
            val image = ImageBitmap(px, px)
            val canvas = Canvas(image)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(px.toFloat(), px.toFloat())) {
                val stroke = 26f
                val pad = 18f + stroke / 2f
                val arc = Size(size.width - pad * 2f, size.height - pad * 2f)
                drawArc(
                    color = AshHigh, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(pad, pad), size = arc, style = Stroke(stroke),
                )
                val f = if (goal <= 0) 0f else (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
                if (f > 0f) {
                    drawArc(
                        color = if (steps >= goal) Gold else Ember, startAngle = -90f, sweepAngle = 360f * f,
                        useCenter = false, topLeft = Offset(pad, pad), size = arc,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                inset(78f) {
                    drawFlame(flame, 0.5f, 0.4f, 0.8f, 0.3f, 0.2f, false)
                }
            }
            return image.asAndroidBitmap()
        }
    }
}
