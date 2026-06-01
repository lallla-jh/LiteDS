package me.magnum.melonds.ui.emulator.input

import android.view.MotionEvent
import android.view.View
import me.magnum.melonds.common.vibration.TouchVibrator
import me.magnum.melonds.domain.model.Input
import me.magnum.melonds.domain.model.JoystickDirectionMode
import me.magnum.melonds.ui.emulator.input.view.JoystickView
import kotlin.math.sqrt

/**
 * Joystick input handler. Direction mapping is delegated to [JoystickDirectionMapper]
 * (4-way or 8-way per [directionMode]). Touches within the dead zone produce no input.
 *
 * - Fixed mode ([floating] = false): the base is anchored at the view center.
 * - Floating mode ([floating] = true): the base is anchored wherever the finger first lands,
 *   and movement is measured relative to that origin.
 *
 * [deadZonePercent] is the dead-zone radius as a percentage of the view's half-width.
 */
class JoystickInputHandler(
    inputListener: IInputListener,
    enableHapticFeedback: Boolean,
    touchVibrator: TouchVibrator,
    private val directionMode: JoystickDirectionMode = JoystickDirectionMode.FOUR_WAY,
    private val deadZonePercent: Int = 25,
    private val floating: Boolean = false,
) : FeedbackInputHandler(inputListener, enableHapticFeedback, touchVibrator) {

    private val activeInputs = mutableSetOf<Input>()

    // Origin the touch offset is measured from. Updated on ACTION_DOWN.
    private var originX = 0f
    private var originY = 0f

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val joystickView = v as? JoystickView

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (floating) {
                    originX = event.x
                    originY = event.y
                    joystickView?.setOrigin(event.x, event.y)
                } else {
                    originX = v.width / 2f
                    originY = v.height / 2f
                }
                processMove(v, joystickView, event)
            }
            MotionEvent.ACTION_MOVE -> {
                processMove(v, joystickView, event)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                joystickView?.resetStickPosition()
                if (floating) {
                    joystickView?.clearOrigin()
                }
                dispatchChanges(v, emptySet())
            }
        }
        return true
    }

    private fun processMove(v: View, joystickView: JoystickView?, event: MotionEvent) {
        val dx = event.x - originX
        val dy = event.y - originY
        val distance = sqrt(dx * dx + dy * dy)
        val deadZone = (v.width / 2f) * (deadZonePercent / 100f)

        joystickView?.updateStickPosition(dx, dy, distance)

        val newInputs = JoystickDirectionMapper.map(dx, dy, deadZone, directionMode)
        dispatchChanges(v, newInputs)
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
