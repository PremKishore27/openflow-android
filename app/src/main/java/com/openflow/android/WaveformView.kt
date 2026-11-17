package com.openflow.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** Live level bars shown while recording. */
class WaveformView(context: Context) : View(context) {
    private val levels = FloatArray(21)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val rect = RectF()

    fun push(level: Float) {
        System.arraycopy(levels, 1, levels, 0, levels.size - 1)
        levels[levels.size - 1] = level
        invalidate()
    }

    fun clear() {
        levels.fill(0f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val barW = width.toFloat() / (levels.size * 2f)
        val h = height.toFloat()
        for (i in levels.indices) {
            val bh = maxOf(barW, levels[i] * h)
            val x = i * barW * 2f + barW / 2f
            rect.set(x, (h - bh) / 2f, x + barW, (h + bh) / 2f)
            canvas.drawRoundRect(rect, barW / 2f, barW / 2f, paint)
        }
    }
}
