package com.openflow.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/** The OpenFlow logo (flat near-black circle, five white voice bars), used as the floating icon. */
class LogoView(context: Context) : View(context) {
    private val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0B0B0F.toInt() }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x30FFFFFF
        style = Paint.Style.STROKE
    }
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    // Same proportions as the launcher icon: x offsets and bar heights in a 80-unit box.
    private val xs = floatArrayOf(-20f, -10f, 0f, 10f, 20f)
    private val hs = floatArrayOf(14f, 26f, 40f, 26f, 14f)

    override fun onDraw(canvas: Canvas) {
        val s = minOf(width, height).toFloat()
        canvas.drawCircle(width / 2f, height / 2f, s / 2f, disc)
        ring.strokeWidth = maxOf(1f, s / 48f)
        canvas.drawCircle(width / 2f, height / 2f, s / 2f - ring.strokeWidth / 2f, ring)
        val k = s / 80f
        bar.strokeWidth = 6f * k
        val cx = width / 2f
        val cy = height / 2f
        for (i in xs.indices) {
            val half = (hs[i] * k) / 2f
            canvas.drawLine(cx + xs[i] * k, cy - half, cx + xs[i] * k, cy + half, bar)
        }
    }
}
