package me.magnum.melonds.domain.model

/**
 * Determines how the on-screen joystick maps a touch angle to D-pad inputs.
 *
 * - [FOUR_WAY]: only cardinal directions (UP/DOWN/LEFT/RIGHT), one at a time.
 * - [EIGHT_WAY]: adds diagonals as simultaneous presses of two adjacent cardinals.
 */
enum class JoystickDirectionMode {
    FOUR_WAY,
    EIGHT_WAY,
}
