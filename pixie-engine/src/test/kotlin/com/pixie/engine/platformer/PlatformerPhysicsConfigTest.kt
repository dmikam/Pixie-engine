package com.pixie.engine.platformer

import com.pixie.engine.core.platformer.PlatformerPhysicsConfig
import com.pixie.engine.core.platformer.PlayerAnimationState
import com.pixie.engine.core.platformer.PlayerFacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformerPhysicsConfigTest {

    @Test
    fun testDefaultPhysicsConfigMatchesNESParameters() {
        val config = PlatformerPhysicsConfig()

        assertEquals(420f, config.walkAcceleration, 0.001f)
        assertEquals(750f, config.runAcceleration, 0.001f)
        assertEquals(110f, config.maxWalkSpeed, 0.001f)
        assertEquals(185f, config.maxRunSpeed, 0.001f)
        assertEquals(550f, config.groundFriction, 0.001f)
        assertEquals(950f, config.skidDeceleration, 0.001f)
        assertEquals(920f, config.gravity, 0.001f)
        assertEquals(-270f, config.jumpImpulse, 0.001f)
        assertEquals(2.4f, config.jumpCutGravityMultiplier, 0.001f)
        assertEquals(0.12f, config.coyoteTimeDuration, 0.001f)
        assertEquals(0.10f, config.jumpBufferDuration, 0.001f)
    }

    @Test
    fun testFluentBuilderMethods() {
        val config = PlatformerPhysicsConfig()
            .withSpeeds(walk = 120f, run = 200f)
            .withAccelerations(walkAcc = 450f, runAcc = 800f)
            .withJump(impulse = -300f, cutMultiplier = 2.5f)
            .withGravity(grav = 1000f, maxFall = 400f)
            .withTolerances(coyote = 0.15f, buffer = 0.12f)
            .withFriction(friction = 600f, skid = 1100f)

        assertEquals(120f, config.maxWalkSpeed, 0.001f)
        assertEquals(200f, config.maxRunSpeed, 0.001f)
        assertEquals(450f, config.walkAcceleration, 0.001f)
        assertEquals(800f, config.runAcceleration, 0.001f)
        assertEquals(-300f, config.jumpImpulse, 0.001f)
        assertEquals(2.5f, config.jumpCutGravityMultiplier, 0.001f)
        assertEquals(1000f, config.gravity, 0.001f)
        assertEquals(400f, config.maxFallSpeed, 0.001f)
        assertEquals(0.15f, config.coyoteTimeDuration, 0.001f)
        assertEquals(0.12f, config.jumpBufferDuration, 0.001f)
        assertEquals(600f, config.groundFriction, 0.001f)
        assertEquals(1100f, config.skidDeceleration, 0.001f)
    }

    @Test
    fun testPlayerAnimationStateProperties() {
        assertTrue(PlayerAnimationState.IDLE.isGrounded)
        assertFalse(PlayerAnimationState.IDLE.isAirborne)
        assertTrue(PlayerAnimationState.IDLE.canMove)
        assertTrue(PlayerAnimationState.IDLE.canJump)

        assertTrue(PlayerAnimationState.JUMP_ASCENT.isAirborne)
        assertFalse(PlayerAnimationState.JUMP_ASCENT.isGrounded)
        assertFalse(PlayerAnimationState.JUMP_ASCENT.canJump)

        assertTrue(PlayerAnimationState.FALL.isAirborne)
        assertFalse(PlayerAnimationState.FALL.isGrounded)

        assertFalse(PlayerAnimationState.DUCK.canMove)
        assertTrue(PlayerAnimationState.DUCK.isGrounded)

        assertFalse(PlayerAnimationState.HURT.canMove)
        assertFalse(PlayerAnimationState.HURT.canJump)

        assertFalse(PlayerAnimationState.VICTORY_FLAG.canMove)

        assertTrue(PlayerAnimationState.CARRY_IDLE.isGrounded)
        assertFalse(PlayerAnimationState.CARRY_IDLE.isAirborne)
        assertTrue(PlayerAnimationState.CARRY_WALK.isMovingHorizontally)
        assertTrue(PlayerAnimationState.CARRY_JUMP.isAirborne)
        assertFalse(PlayerAnimationState.CARRY_JUMP.isGrounded)

        assertTrue(PlayerAnimationState.WALK.isMovingHorizontally)
        assertTrue(PlayerAnimationState.RUN.isMovingHorizontally)
        assertTrue(PlayerAnimationState.SKID.isMovingHorizontally)
        assertFalse(PlayerAnimationState.IDLE.isMovingHorizontally)
    }

    @Test
    fun testPlayerFacingProperties() {
        assertEquals(1f, PlayerFacing.RIGHT.sign, 0.001f)
        assertEquals(-1f, PlayerFacing.LEFT.sign, 0.001f)
        assertEquals(PlayerFacing.LEFT, PlayerFacing.RIGHT.opposite)
        assertEquals(PlayerFacing.RIGHT, PlayerFacing.LEFT.opposite)
    }
}
