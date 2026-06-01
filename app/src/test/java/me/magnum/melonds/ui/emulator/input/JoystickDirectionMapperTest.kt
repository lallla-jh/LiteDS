package me.magnum.melonds.ui.emulator.input

import me.magnum.melonds.domain.model.Input
import me.magnum.melonds.domain.model.JoystickDirectionMode
import org.junit.Assert.assertEquals
import org.junit.Test

class JoystickDirectionMapperTest {

    private val deadZone = 10f

    private fun map(dx: Float, dy: Float, mode: JoystickDirectionMode) =
        JoystickDirectionMapper.map(dx, dy, deadZone, mode)

    // ── Dead zone ──

    @Test
    fun `inside dead zone yields no input`() {
        assertEquals(emptySet<Input>(), map(0f, 0f, JoystickDirectionMode.FOUR_WAY))
        assertEquals(emptySet<Input>(), map(5f, 5f, JoystickDirectionMode.EIGHT_WAY))
    }

    @Test
    fun `exactly at dead zone edge yields no input`() {
        // distance == deadZone is still considered no input (strictly greater required)
        assertEquals(emptySet<Input>(), map(10f, 0f, JoystickDirectionMode.FOUR_WAY))
    }

    @Test
    fun `dead zone radius is configurable`() {
        // distance = sqrt(20^2 + 20^2) ≈ 28.28
        // A large dead zone suppresses what a small one would register
        assertEquals(
            emptySet<Input>(),
            JoystickDirectionMapper.map(20f, 20f, 30f, JoystickDirectionMode.EIGHT_WAY),
        )
        assertEquals(
            setOf(Input.DOWN, Input.RIGHT),
            JoystickDirectionMapper.map(20f, 20f, 10f, JoystickDirectionMode.EIGHT_WAY),
        )
    }

    // ── 4-way cardinals ──

    @Test
    fun `four way maps cardinals`() {
        val m = JoystickDirectionMode.FOUR_WAY
        assertEquals(setOf(Input.RIGHT), map(50f, 0f, m))
        assertEquals(setOf(Input.DOWN), map(0f, 50f, m))
        assertEquals(setOf(Input.LEFT), map(-50f, 0f, m))
        assertEquals(setOf(Input.UP), map(0f, -50f, m))
    }

    @Test
    fun `four way maps diagonal to a single cardinal`() {
        // 45° down-right falls on the RIGHT/DOWN boundary -> DOWN (angle < 135 region)
        val m = JoystickDirectionMode.FOUR_WAY
        val result = map(50f, 50f, m)
        assertEquals(1, result.size)
        assertEquals(setOf(Input.DOWN), result)
    }

    // ── 8-way directions ──

    @Test
    fun `eight way maps cardinals`() {
        val m = JoystickDirectionMode.EIGHT_WAY
        assertEquals(setOf(Input.RIGHT), map(50f, 0f, m))
        assertEquals(setOf(Input.DOWN), map(0f, 50f, m))
        assertEquals(setOf(Input.LEFT), map(-50f, 0f, m))
        assertEquals(setOf(Input.UP), map(0f, -50f, m))
    }

    @Test
    fun `eight way maps diagonals to two cardinals`() {
        val m = JoystickDirectionMode.EIGHT_WAY
        assertEquals(setOf(Input.DOWN, Input.RIGHT), map(50f, 50f, m))   // down-right
        assertEquals(setOf(Input.DOWN, Input.LEFT), map(-50f, 50f, m))   // down-left
        assertEquals(setOf(Input.UP, Input.LEFT), map(-50f, -50f, m))    // up-left
        assertEquals(setOf(Input.UP, Input.RIGHT), map(50f, -50f, m))    // up-right
    }

    @Test
    fun `eight way sector boundaries stay cardinal near axes`() {
        val m = JoystickDirectionMode.EIGHT_WAY
        // ~10° below the +x axis: within ±22.5° of RIGHT -> RIGHT only
        assertEquals(setOf(Input.RIGHT), map(50f, 8f, m))
        // ~35° -> into the down-right diagonal sector
        assertEquals(setOf(Input.DOWN, Input.RIGHT), map(50f, 35f, m))
    }
}
