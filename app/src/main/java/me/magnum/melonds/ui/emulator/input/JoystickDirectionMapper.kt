package me.magnum.melonds.ui.emulator.input

import me.magnum.melonds.domain.model.Input
import me.magnum.melonds.domain.model.JoystickDirectionMode
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Pure mapping from a joystick touch offset to the set of D-pad [Input]s that should be active.
 *
 * Kept free of Android dependencies so the direction logic can be unit-tested in isolation.
 * Screen coordinates: +x is right, +y is down, so atan2(dy, dx) gives 0° = RIGHT, 90° = DOWN.
 */
object JoystickDirectionMapper {

    fun map(dx: Float, dy: Float, deadZoneRadius: Float, mode: JoystickDirectionMode): Set<Input> {
        val distance = sqrt(dx * dx + dy * dy)
        if (distance <= deadZoneRadius) {
            return emptySet()
        }

        val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
        return when (mode) {
            JoystickDirectionMode.FOUR_WAY -> fourWay(angle)
            JoystickDirectionMode.EIGHT_WAY -> eightWay(angle)
        }
    }

    // 90° sectors, boundaries at 45°/135°/225°/315°
    private fun fourWay(angle: Float): Set<Input> = when {
        angle < 45f || angle >= 315f -> setOf(Input.RIGHT)
        angle < 135f -> setOf(Input.DOWN)
        angle < 225f -> setOf(Input.LEFT)
        else -> setOf(Input.UP)
    }

    // 45° sectors, boundaries at ±22.5° around each direction; diagonals press two cardinals
    private fun eightWay(angle: Float): Set<Input> = when {
        angle < 22.5f || angle >= 337.5f -> setOf(Input.RIGHT)
        angle < 67.5f -> setOf(Input.DOWN, Input.RIGHT)
        angle < 112.5f -> setOf(Input.DOWN)
        angle < 157.5f -> setOf(Input.DOWN, Input.LEFT)
        angle < 202.5f -> setOf(Input.LEFT)
        angle < 247.5f -> setOf(Input.UP, Input.LEFT)
        angle < 292.5f -> setOf(Input.UP)
        else -> setOf(Input.UP, Input.RIGHT)
    }
}
