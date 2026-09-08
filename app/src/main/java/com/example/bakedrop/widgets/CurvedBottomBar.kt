package com.example.bakedrop.widgets

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.LinearLayout

class CurvedBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0F172A") // Deep Navy Slate
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = Color.parseColor("#2238BDF8")
    }

    init {
        setWillNotDraw(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cx = w / 2f
        val curveRadius = 42f
        val curveDepth = 28f

        path.reset()
        path.moveTo(0f, 0f)

        // Left straight line to curve start
        path.lineTo(cx - curveRadius * 1.6f, 0f)

        // Smooth U-Curve Notch in Center (Screenshot Design)
        path.cubicTo(
            cx - curveRadius, 0f,
            cx - curveRadius * 0.8f, curveDepth,
            cx, curveDepth
        )
        path.cubicTo(
            cx + curveRadius * 0.8f, curveDepth,
            cx + curveRadius, 0f,
            cx + curveRadius * 1.6f, 0f
        )

        // Right straight line
        path.lineTo(w.toFloat(), 0f)
        path.lineTo(w.toFloat(), h.toFloat())
        path.lineTo(0f, h.toFloat())
        path.close()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(path, paint)
        canvas.drawPath(path, strokePaint)
        super.onDraw(canvas)
    }
}