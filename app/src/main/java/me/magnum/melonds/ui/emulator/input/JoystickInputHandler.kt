package me.magnum.melonds.ui.emulator.input

import android.view.MotionEvent
import android.view.View
import me.magnum.melonds.common.vibration.TouchVibrator
import me.magnum.melonds.domain.model.Input
import me.magnum.melonds.ui.emulator.input.view.JoystickView
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Fixed-center joystick input handler. The joystick base is always anchored at the view center.
 * Maps touch position into 4 cardinal directions (UP/DOWN/LEFT/RIGHT) using polar coordinates.
 * Touches within the dead zone radius produce no input.
 */
class JoystickInputHandler(
    inputListener: IInputListener,
    enableHapticFeedback: Boolean,
    touchVibrator: TouchVibrator,
) : FeedbackInputHandler(inputListener, enableHapticFeedback, touchVibrator) {

    companion object {
        // Dead zone: fraction of the view's half-width that requires no input
        private const val DEAD_ZONE_RATIO = 0.25f
    }

    private val activeInputs = mutableSetOf<Input>()

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val joystickView = v as? JoystickView

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val cx = v.width / 2f
                val cy = v.height / 2f
                val dx = event.x - cx
                val dy = event.y - cy
                val distance = sqrt(dx * dx + dy * dy)
                val deadZone = cx * DEAD_ZONE_RATIO

                joystickView?.updateStickPosition(dx, dy, distance)

                val newInputs = if (distance < deadZone) {
                    emptySet()
                } else {
                    // atan2 returns angle in [-π, π]; 0 is right, π/2 is down (screen coords)
                    val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    directionFromAngle(angle)
                }

                dispatchChanges(v, newInputs)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                joystickView?.resetStickPosition()
                dispatchChanges(v, emptySet())
            }
        }
        return true
    }

    private fun directionFromAngle(degrees: Float): Set<Input> {
        // Normalize to [0, 360)
        val angle = (degrees + 360f) % 360f
        return when {
            angle < 45f || angle >= 315f -> setOf(Input.RIGHT)
            angle < 135f -> setOf(Input.DOWN)
            angle < 225f -> setOf(Input.LEFT)
            else -> setOf(Input.UP)
        }
    }

    private fun dispatchChanges(v: View, newInputs: Set<Input>) {
        val released = activeInputs - newInputs
        val pressed = newInputs - activeInputs

        if (released.isNotEmpty()) {
            released.forEach { inputListener.onKeyReleased(it) }
            performHapticFeedback(v, HapticFeedbackType.KEY_RELEASE)
        }
        if (pressed.isNotEmpty()) {
            pressed.forEach { inputListener.onKeyPress(it) }
            performHapticFeedback(v, HapticFeedbackType.KEY_PRESS)
        }

        activeInputs.clear()
        activeInputs.addAll(newInputs)
    }
}
