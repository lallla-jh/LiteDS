package me.magnum.melonds.ui.emulator.input.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Virtual joystick view. Draws a base ring and a movable stick nub clamped to the base radius.
 *
 * By default the base is centered in the view (fixed mode). In floating mode the base is only
 * drawn once a finger lands, anchored at the touch origin set via [setOrigin] / [clearOrigin].
 */
class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    companion object {
        // Base ring occupies this fraction of the view's smaller dimension
        private const val BASE_RADIUS_RATIO = 0.42f
        // Stick nub radius relative to base radius
        private const val NUB_RADIUS_RATIO = 0.38f
        private const val BASE_ALPHA = 120
        private const val NUB_ALPHA = 200
    }

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = BASE_ALPHA
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val baseFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 30
        style = Paint.Style.FILL
    }

    private val nubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = NUB_ALPHA
        style = Paint.Style.FILL
    }

    // Stick offset from center in pixels; updated by JoystickInputHandler
    private var stickDx = 0f
    private var stickDy = 0f

    // Floating mode: base anchored at the touch origin. null origin = not currently touched.
    private var floatingMode = false
    private var baseOriginX: Float? = null
    private var baseOriginY: Float? = null

    fun setFloatingMode(floating: Boolean) {
        floatingMode = floating
        invalidate()
    }

    fun setOrigin(x: Float, y: Float) {
        baseOriginX = x
        baseOriginY = y
        invalidate()
    }

    fun clearOrigin() {
        baseOriginX = null
        baseOriginY = null
        invalidate()
    }

    fun updateStickPosition(dx: Float, dy: Float, rawDistance: Float) {
        val baseRadius = min(width, height) / 2f * BASE_RADIUS_RATIO
        val clampedRadius = min(rawDistance, baseRadius)
        val scale = if (rawDistance > 0f) clampedRadius / rawDistance else 0f
        stickDx = dx * scale
        stickDy = dy * scale
        invalidate()
    }

    fun resetStickPosition() {
        stickDx = 0f
        stickDy = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        // In floating mode, draw nothing until the finger lands and an origin is set.
        if (floatingMode && baseOriginX == null) {
            return
        }

        val cx = baseOriginX ?: (width / 2f)
        val cy = baseOriginY ?: (height / 2f)
        val baseRadius = min(width, height) / 2f * BASE_RADIUS_RATIO
        val nubRadius = baseRadius * NUB_RADIUS_RATIO

        // Base ring fill + stroke
        canvas.drawCircle(cx, cy, baseRadius, baseFillPaint)
        canvas.drawCircle(cx, cy, baseRadius, basePaint)

        // Stick nub
        canvas.drawCircle(cx + stickDx, cy + stickDy, nubRadius, nubPaint)
    }
}
