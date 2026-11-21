package com.ashstride.app.ui

import java.text.NumberFormat
import java.util.Locale

object Fmt {
    private val nf: NumberFormat = NumberFormat.getIntegerInstance(Locale.US)
    fun n(v: Long): String = nf.format(v)
    fun n(v: Int): String = nf.format(v.toLong())
    fun km(v: Double): String = String.format(Locale.US, "%.1f km", v)
}
