package com.example.platformer

import com.example.platformer.entities.PlatformerPlayer
import com.example.platformer.entities.crate.CrateState
import com.example.platformer.entities.crate.ThrowableCrate
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.platformer.PlayerAnimationState
import com.pixie.engine.core.platformer.PlayerFacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformerPlayerCarryTest {

    @Test
    fun testPickUpAdjacentCrateOnActionPress() {
        val player = PlatformerPlayer(position = Vec2(100f, 80f))
            .withFacing(PlayerFacing.RIGHT)
        val crate = ThrowableCrate(position = Vec2(115f, 80f)) // Adjacent in facing direction
        val crates = listOf(crate)

        assertFalse(player.isCarrying)
        assertNull(player.carriedObject)

        // Press action button (B) next to crate
        val pickedUp = player.onActionPress(crates)

        assertTrue("Action press should succeed in picking up adjacent crate", pickedUp)
        assertTrue(player.isCarrying)
        assertEquals(crate, player.carriedObject)
        assertEquals(player, crate.carrier)
        assertEquals(CrateState.CARRIED, crate.state)

        // Crate position should be overhead at (player.x + 0f, player.y - 14f) = (100f, 66f)
        assertEquals(100f, crate.position.x, 0.001f)
        assertEquals(66f, crate.position.y, 0.001f)

        // When stationary on ground, player state is CARRY_IDLE
        assertEquals(PlayerAnimationState.CARRY_IDLE, player.animationState)
    }

    @Test
    fun testCrateTracksPlayerMovementOverhead() {
        val player = PlatformerPlayer(position = Vec2(100f, 80f))
        val crate = ThrowableCrate(position = Vec2(100f, 66f))
        val context = GameContext()

        player.pickUpCrate(crate)
        assertEquals(100f, crate.position.x, 0.001f)
        assertEquals(66f, crate.position.y, 0.001f)

        // Move player to the right
        player.physics.setInput(moveX = 1f)
        player.physics.update(0.1f)
        player.update(0.1f, context)

        // Player is walking with crate -> state is CARRY_WALK
        assertEquals(PlayerAnimationState.CARRY_WALK, player.animationState)
        assertTrue(player.position.x > 100f)

        // Crate overhead position stays locked to player.x, player.y - 14f
        assertEquals(player.position.x, crate.position.x, 0.001f)
        assertEquals(player.position.y - 14f, crate.position.y, 0.001f)
    }

    @Test
    fun testJumpingPhysicsIntactWhileCarrying() {
        val player = PlatformerPlayer(position = Vec2(50f, 100f))
        val crate = ThrowableCrate(position = Vec2(50f, 86f))
        val context = GameContext()

        player.pickUpCrate(crate)
        assertEquals(PlayerAnimationState.CARRY_IDLE, player.animationState)

        // Press jump button (A)
        player.physics.pressJump()
        player.physics.update(1f / 60f)
        player.update(1f / 60f, context)

        // Airborne carrying state should be CARRY_JUMP
        assertEquals(PlayerAnimationState.CARRY_JUMP, player.animationState)
        assertFalse(player.physics.isGrounded)
        assertTrue("Vertical velocity should be upward (negative)", player.velocity.y < 0f)

        // Verify crate follows jump arc overhead
        assertEquals(player.position.x, crate.position.x, 0.001f)
        assertEquals(player.position.y - 14f, crate.position.y, 0.001f)

        // Advance simulation: gravity pulls player and crate down
        for (i in 0 until 30) {
            player.physics.update(1f / 60f)
            player.update(1f / 60f, context)
        }

        // Crate is still overhead
        assertEquals(player.position.x, crate.position.x, 0.001f)
        assertEquals(player.position.y - 14f, crate.position.y, 0.001f)
    }

    @Test
    fun testDuckingWhileCarryingEntersDuckState() {
        val player = PlatformerPlayer(position = Vec2(50f, 100f))
        val crate = ThrowableCrate(position = Vec2(50f, 86f))

        player.pickUpCrate(crate)
        assertEquals(PlayerAnimationState.CARRY_IDLE, player.animationState)

        player.isDucking = true
        player.physics.updateStateFromResolution()

        assertEquals(PlayerAnimationState.DUCK, player.animationState)
        assertTrue(player.isDucking)
    }

    @Test
    fun testActionPressWhileCarryingThrowsCrate() {
        val player = PlatformerPlayer(position = Vec2(100f, 80f))
            .withFacing(PlayerFacing.RIGHT)
        val crate = ThrowableCrate(position = Vec2(100f, 66f))

        player.pickUpCrate(crate)
        assertTrue(player.isCarrying)

        // Press action (B) while carrying -> throws crate forward
        val actionSuccess = player.onActionPress()

        assertTrue(actionSuccess)
        assertFalse(player.isCarrying)
        assertNull(player.carriedObject)
        assertEquals(CrateState.THROWN_AIRBORNE, crate.state)
        assertEquals(210f, crate.velocity.x, 0.001f)
        assertEquals(-70f, crate.velocity.y, 0.001f)
        assertEquals(PlayerAnimationState.IDLE, player.animationState)
    }

    @Test
    fun testCannotPickUpCrateWhenOutOfReach() {
        val player = PlatformerPlayer(position = Vec2(100f, 80f))
            .withFacing(PlayerFacing.RIGHT)
        val farCrate = ThrowableCrate(position = Vec2(200f, 80f)) // 100 px away

        val result = player.tryPickUpAdjacentCrate(listOf(farCrate))

        assertNull("Far crate cannot be reached", result)
        assertFalse(player.isCarrying)
        assertNull(player.carriedObject)
        assertEquals(CrateState.PLACED_ON_GROUND, farCrate.state)
    }

    @Test
    fun testDefeatReleasesCarriedCrate() {
        val player = PlatformerPlayer(position = Vec2(100f, 80f))
        val crate = ThrowableCrate(position = Vec2(100f, 66f))
        val context = GameContext()

        player.pickUpCrate(crate)
        assertTrue(player.isCarrying)

        player.defeat(context)

        assertTrue(player.isDefeated)
        assertFalse(player.isCarrying)
        assertNull(player.carriedObject)
        assertNull(crate.carrier)
    }
}
