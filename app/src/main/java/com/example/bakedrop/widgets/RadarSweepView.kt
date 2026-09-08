package com.example.bakedrop.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class RadarSweepView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var waveRadius1 = 0f
    private var waveRadius2 = 0f
    private var waveRadius3 = 0f

    private var waveAlpha1 = 255
    private var waveAlpha2 = 255
    private var waveAlpha3 = 255

    private var animator: ValueAnimator? = null

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#38BDF8") // Neon Cyan
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1538BDF8")
    }

    init {
        setupAnimator()
    }

    private fun setupAnimator() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2600
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val progress = anim.animatedValue as Float

                val p1 = progress
                val p2 = (progress + 0.33f) % 1.0f
                val p3 = (progress + 0.66f) % 1.0f

                val maxR = (Math.min(width, height) / 2f) * 0.95f

                waveRadius1 = p1 * maxR
                waveAlpha1 = ((1f - p1) * 200).toInt()

                waveRadius2 = p2 * maxR
                waveAlpha2 = ((1f - p2) * 200).toInt()

                waveRadius3 = p3 * maxR
                waveAlpha3 = ((1f - p3) * 200).toInt()

                invalidate()
            }
        }
    }

    fun start() {
        if (animator?.isRunning != true) {
            animator?.start()
        }
    }

    fun stop() {
        animator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f

        canvas.save()
        // 🌟 ৪৫° ৩ডি ম্যাপের সমান্তরালে পার্সপেক্টিভ স্কেলিং
        canvas.scale(1.0f, 0.68f, cx, cy)

        drawPulseWave(canvas, cx, cy, waveRadius1, waveAlpha1)
        drawPulseWave(canvas, cx, cy, waveRadius2, waveAlpha2)
        drawPulseWave(canvas, cx, cy, waveRadius3, waveAlpha3)

        canvas.restore()
    }

    private fun drawPulseWave(canvas: Canvas, cx: Float, cy: Float, radius: Float, alpha: Int) {
        if (radius <= 5f || alpha <= 0) return

        wavePaint.alpha = alpha
        fillPaint.alpha = (alpha * 0.25f).toInt()

        canvas.drawCircle(cx, cy, radius, fillPaint)
        canvas.drawCircle(cx, cy, radius, wavePaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }
}