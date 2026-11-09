package com.pixie.engine.platformer

import com.pixie.engine.core.platformer.PlatformerPhysicsConfig
import com.pixie.engine.core.platformer.PlatformerPhysicsController
import com.pixie.engine.core.platformer.PlayerAnimationState
import com.pixie.engine.core.platformer.PlayerFacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformerPhysicsControllerTest {

    @Test
    fun testWalkAccelerationReachesMaxWalkSpeed() {
        val controller = PlatformerPhysicsController()
            .withGrounded(true)
            .setInput(moveX = 1f, run = false)

        for (i in 0 until 30) {
            controller.update(1f / 60f)
        }

        assertEquals(110f, controller.velocityX, 0.01f)
        assertEquals(PlayerFacing.RIGHT, controller.facing)
        assertEquals(PlayerAnimationState.WALK, controller.state)
        assertFalse(controller.isSkidding)
    }

    @Test
    fun testRunAccelerationReachesMaxRunSpeed() {
        val controller = PlatformerPhysicsController()
            .withGrounded(true)
            .setInput(moveX = 1f, run = true)

        for (i in 0 until 30) {
            controller.update(1f / 60f)
        }

        assertEquals(185f, controller.velocityX, 0.01f)
        assertEquals(PlayerFacing.RIGHT, controller.facing)
        assertEquals(PlayerAnimationState.RUN, controller.state)
        assertFalse(controller.isSkidding)
    }

    @Test
    fun testGroundFrictionDeceleratesToStop() {
        val controller = PlatformerPhysicsController()
            .withGrounded(true)
            .withVelocity(vx = 100f, vy = 0f)
            .setInput(moveX = 0f)

        for (i in 0 until 20) {
            controller.update(1f / 60f)
        }

        assertEquals(0f, controller.velocityX, 0.001f)
        assertEquals(PlayerAnimationState.IDLE, controller.state)
        assertFalse(controller.isSkidding)
    }

    @Test
    fun testSkiddingWhenReversingDirectionAtSpeed() {
        var skidStarted = false
        var skidEnded = false

        val controller = PlatformerPhysicsController()
            .withGrounded(true)
            .withVelocity(vx = 150f, vy = 0f)
            .withFacing(PlayerFacing.RIGHT)

        controller.onSkidStart = { skidStarted = true }
        controller.onSkidEnd = { skidEnded = true }

        controller.setInput(moveX = -1f, run = false)
        controller.update(1f / 60f)

        assertTrue("Skid should start when reversing at high speed", skidStarted)
        assertTrue(controller.isSkidding)
        assertEquals(PlayerAnimationState.SKID, controller.state)

        for (i in 0 until 12) {
            controller.update(1f / 60f)
        }

        assertFalse("Skid should end once velocity reaches zero", controller.isSkidding)
        assertTrue("Skid end callback should have fired", skidEnded)
        assertEquals(PlayerFacing.LEFT, controller.facing)
    }

    @Test
    fun testNoSkidWhenReversingBelowSpeedThreshold() {
        var skidStarted = false
        val controller = PlatformerPhysicsController()
            .withGrounded(true)
            .withVelocity(vx = 25f, vy = 0f)

        controller.onSkidStart = { skidStarted = true }
        controller.setInput(moveX = -1f)
        controller.update(1f / 60f)

        assertFalse("Should not enter skid when reversing at low speed", skidStarted)
        assertFalse(controller.isSkidding)
    }

    @Test
    fun testNoSkidInMidair() {
        var skidStarted = false
        val controller = PlatformerPhysicsController()
            .withGrounded(false)
            .withVelocity(vx = 150f, vy = -100f)

        controller.onSkidStart = { skidStarted = true }
        controller.setInput(moveX = -1f)
        controller.update(1f / 60f)

        assertFalse("Should not trigger skid while airborne", skidStarted)
        assertFalse(controller.isSkidding)
    }

    @Test
    fun testVariableJumpCutProducesLowerApex() {
        // Full jump: Jump held throughout the ascent
        val fullJumpController = PlatformerPhysicsController()
            .withPosition(0f, 200f)
            .withGrounded(true)
        fullJumpController.pressJump()

        var fullJumpMinY = 200f
        for (i in 0 until 30) {
            fullJumpController.update(1f / 60f)
            if (fullJumpController.position.y < fullJumpMinY) {
                fullJumpMinY = fullJumpController.position.y
            }
        }
        val fullJumpApexHeight = 200f - fullJumpMinY

        // Cut jump: Jump pressed and immediately released (short hop)
        val cutJumpController = PlatformerPhysicsController()
            .withPosition(0f, 200f)
            .withGrounded(true)
        cutJumpController.pressJump()
        cutJumpController.releaseJump()

        var cutJumpMinY = 200f
        for (i in 0 until 30) {
            cutJumpController.update(1f / 60f)
            if (cutJumpController.position.y < cutJumpMinY) {
                cutJumpMinY = cutJumpController.position.y
            }
        }
        val cutJumpApexHeight = 200f - cutJumpMinY

        assertTrue("Full jump height ($fullJumpApexHeight) should be significantly higher than cut jump ($cutJumpApexHeight)",
            fullJumpApexHeight > cutJumpApexHeight * 1.8f)
    }

    @Test
    fun testCoyoteTimeAllowsJumpAfterSlippingOffLedge() {
        var jumpFired = false
        val controller = PlatformerPhysicsController()
            .withGrounded(true)

        controller.onJump = { jumpFired = true }

        // Player walks off ledge
        controller.leaveGround()
        assertFalse(controller.isGrounded)

        // Advance 3 frames (3 * 16.6ms = 50ms, well within 120ms coyote window)
        for (i in 0 until 3) {
            controller.update(1f / 60f)
        }

        assertTrue(controller.timeSinceGrounded > 0f)
        assertTrue(controller.timeSinceGrounded <= controller.config.coyoteTimeDuration)

        // Press jump within coyote window
        controller.pressJump()

        assertTrue("Jump should trigger during coyote time", jumpFired)
        assertEquals(controller.config.jumpImpulse, controller.velocityY, 0.01f)
        assertFalse(controller.isGrounded)
    }

    @Test
    fun testCoyoteTimeExpiresAfterThreshold() {
        var jumpFired = false
        val controller = PlatformerPhysicsController()
            .withGrounded(true)

        controller.onJump = { jumpFired = true }

        // Player walks off ledge
        controller.leaveGround()

        // Advance past coyote window (10 frames * 16.6ms = 166ms > 120ms coyote window)
        for (i in 0 until 10) {
            controller.update(1f / 60f)
        }

        assertTrue(controller.timeSinceGrounded > controller.config.coyoteTimeDuration)

        // Attempt jump after coyote time expired
        controller.pressJump()

        assertFalse("Jump should NOT trigger after coyote time expires", jumpFired)
        assertTrue(controller.velocityY > 0f) // Falling downward due to gravity
    }

    @Test
    fun testCoyoteJumpCannotBeExecutedTwiceInMidair() {
        var jumpCount = 0
        val controller = PlatformerPhysicsController()
            .withGrounded(true)

        controller.onJump = { jumpCount++ }
        controller.leaveGround()

        // 1st jump in coyote window succeeds
        controller.update(1f / 60f)
        controller.pressJump()
        assertEquals(1, jumpCount)

        // 2nd jump attempt while airborne must fail
        controller.update(1f / 60f)
        controller.pressJump()
        assertEquals("Double jumping in midair should be prohibited", 1, jumpCount)
    }

    @Test
    fun testJumpBufferingTriggersImmediatelyOnLanding() {
        var jumpCount = 0
        val controller = PlatformerPhysicsController()
            .withGrounded(false)
            .withVelocity(0f, 150f) // Falling

        controller.onJump = { jumpCount++ }

        // Advance in air past coyote time
        for (i in 0 until 15) {
            controller.update(1f / 60f)
        }

        // Buffer jump 2 frames before landing (2 * 16.6ms = 33ms < 100ms buffer)
        controller.pressJump()
        assertEquals("Jump should not fire mid-air after coyote time", 0, jumpCount)
        assertTrue(controller.jumpBufferTimer > 0f)

        controller.update(1f / 60f)
        controller.update(1f / 60f)

        // Touch ground
        controller.landOnGround(200f)

        assertEquals("Buffered jump must execute immediately upon landing", 1, jumpCount)
        assertEquals(controller.config.jumpImpulse, controller.velocityY, 0.01f)
        assertFalse(controller.isGrounded)
    }

    @Test
    fun testExpiredJumpBufferDoesNotTriggerJumpOnLanding() {
        var jumpCount = 0
        val controller = PlatformerPhysicsController()
            .withGrounded(false)
            .withVelocity(0f, 150f)

        controller.onJump = { jumpCount++ }

        // Advance in air
        for (i in 0 until 10) {
            controller.update(1f / 60f)
        }

        // Buffer jump early
        controller.pressJump()
        assertEquals(0, jumpCount)

        // Advance 10 frames (166ms > 100ms buffer window)
        for (i in 0 until 10) {
            controller.update(1f / 60f)
        }

        assertEquals(0f, controller.jumpBufferTimer, 0.001f)

        // Land on ground
        controller.landOnGround(200f)

        assertEquals("Expired buffer must not execute jump upon landing", 0, jumpCount)
        assertTrue(controller.isGrounded)
        assertEquals(0f, controller.velocityY, 0.001f)
    }

    @Test
    fun testCeilingBonkZeroesVelocityAndFiresCallback() {
        var bonkFired = false
        val controller = PlatformerPhysicsController()
            .withGrounded(false)
            .withPosition(50f, 100f)
            .withVelocity(0f, -200f) // Moving up rapidly

        controller.onCeilingBonk = { bonkFired = true }

        controller.bonkCeiling(80f)

        assertTrue(bonkFired)
        assertEquals(80f, controller.position.y, 0.001f)
        assertEquals(0f, controller.velocityY, 0.001f)
        assertFalse(controller.isJumpCut)
    }

    @Test
    fun testAirAccelerationMultiplierApplies() {
        val groundController = PlatformerPhysicsController()
            .withGrounded(true)
            .setInput(moveX = 1f)

        val airController = PlatformerPhysicsController()
            .withGrounded(false)
            .setInput(moveX = 1f)

        val dt = 1f / 60f
        groundController.update(dt)
        airController.update(dt)

        assertTrue(groundController.velocityX > airController.velocityX)
        assertEquals(
            groundController.velocityX * PlatformerPhysicsConfig.NES_CLASSIC.airAccelerationMultiplier,
            airController.velocityX,
            0.01f
        )
    }

    @Test
    fun testFluentChainingAndConfiguration() {
        val customConfig = PlatformerPhysicsConfig().withSpeeds(walk = 90f, run = 150f)
        val controller = PlatformerPhysicsController()
            .withConfig(customConfig)
            .withPosition(100f, 200f)
            .withVelocity(50f, 0f)
            .withFacing(PlayerFacing.LEFT)
            .withDuck(true)

        assertEquals(90f, controller.config.maxWalkSpeed, 0.001f)
        assertEquals(100f, controller.position.x, 0.001f)
        assertEquals(200f, controller.position.y, 0.001f)
        assertEquals(50f, controller.velocityX, 0.001f)
        assertEquals(PlayerFacing.LEFT, controller.facing)
        assertTrue(controller.isDucking)
    }
}
