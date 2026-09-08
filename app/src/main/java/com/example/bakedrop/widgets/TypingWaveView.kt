package com.example.bakedrop.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class TypingWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val dotRadius = 5.5f * density
    private val dotSpacing = 16f * density
    private val maxJumpHeight = 8f * density

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0B3575")
        style = Paint.Style.FILL
    }

    private val phaseOffsets = floatArrayOf(0f, 0.18f, 0.36f)
    private var animatedFraction = 0f
    private var animator: ValueAnimator? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun startAnimating() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener {
                this@TypingWaveView.animatedFraction = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stopAnimating() {
        animator?.cancel()
        animator = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = height / 2f
        val startX = (width / 2f) - dotSpacing

        for (i in 0 until 3) {
            val cx = startX + (i * dotSpacing)
            val t = (animatedFraction + phaseOffsets[i]) % 1f
            val jump = if (t < 0.5f) {
                sinEase(t * 2f)
            } else {
                sinEase((1f - t) * 2f)
            }
            val cy = centerY - (jump * maxJumpHeight)
            dotPaint.alpha = if (jump > 0.1f) 255 else 120
            canvas.drawCircle(cx, cy, dotRadius, dotPaint)
        }
    }

    private fun sinEase(x: Float): Float =
        (Math.sin((x * Math.PI / 2f))).toFloat().coerceIn(0f, 1f)

    override fun onDetachedFromWindow() {
        stopAnimating()
        super.onDetachedFromWindow()
    }
}