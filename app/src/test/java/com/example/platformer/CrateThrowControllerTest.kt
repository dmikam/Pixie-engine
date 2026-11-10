package com.example.platformer

import com.example.platformer.entities.PlatformerPlayer
import com.example.platformer.entities.crate.CrateState
import com.example.platformer.entities.crate.CrateThrowController
import com.example.platformer.entities.crate.ThrowableCrate
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.platformer.PlayerAnimationState
import com.pixie.engine.core.platformer.PlayerFacing
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.ports.InputPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrateThrowControllerTest {

    @Test
    fun testCalculateThrowVelocityForwardAndUpward() {
        val controller = CrateThrowController()

        // Facing Right (+1f)
        val rightForward = controller.calculateThrowVelocity(facingSign = 1f, isAimingUp = false)
        assertEquals(210f, rightForward.x, 0.001f)
        assertEquals(-70f, rightForward.y, 0.001f)

        val rightUpward = controller.calculateThrowVelocity(facingSign = 1f, isAimingUp = true)
        assertEquals(40f, rightUpward.x, 0.001f)
        assertEquals(-280f, rightUpward.y, 0.001f)

        // Facing Left (-1f)
        val leftForward = controller.calculateThrowVelocity(facingSign = -1f, isAimingUp = false)
        assertEquals(-210f, leftForward.x, 0.001f)
        assertEquals(-70f, leftForward.y, 0.001f)

        val leftUpward = controller.calculateThrowVelocity(facingSign = -1f, isAimingUp = true)
        assertEquals(-40f, leftUpward.x, 0.001f)
        assertEquals(-280f, leftUpward.y, 0.001f)
    }

    @Test
    fun testThrowForwardDetachesCrateAndInitializesProjectilePhysics() {
        val player = PlatformerPlayer(position = Vec2(100f, 80f))
            .withFacing(PlayerFacing.RIGHT)
        val crate = ThrowableCrate(position = Vec2(100f, 66f))
        player.pickUpCrate(crate)

        assertTrue(player.isCarrying)
        assertEquals(crate, player.carriedObject)

        val controller = CrateThrowController()
        var callbackInvoked = false
        var recordedVelocity: Vec2? = null
        var recordedAimUp = true

        controller.onCrateThrown = { thrown, vel, aimedUp ->
            callbackInvoked = true
            recordedVelocity = vel
            recordedAimUp = aimedUp
            assertEquals(crate, thrown)
        }

        val result = controller.throwCarriedCrate(player, isAimingUp = false)

        assertNotNull(result)
        assertEquals(crate, result)
        assertFalse("Player must no longer be carrying", player.isCarrying)
        assertNull("Player carried object reference cleared", player.carriedObject)
        assertEquals(CrateState.THROWN_AIRBORNE, crate.state)
        assertNull("Crate carrier reference cleared", crate.carrier)

        // Trajectory velocity check
        assertEquals(210f, crate.velocity.x, 0.001f)
        assertEquals(-70f, crate.velocity.y, 0.001f)
        assertEquals(650f, crate.gravity, 0.001f)

        // Controller tracking and callback check
        assertTrue("Callback onCrateThrown must fire", callbackInvoked)
        assertEquals(Vec2(210f, -70f), recordedVelocity)
        assertFalse(recordedAimUp)
        assertEquals(1, controller.activeAirborneCrates.size)
        assertEquals(crate, controller.activeAirborneCrates[0])
    }

    @Test
    fun testThrowUpwardArcWhenAimingUp() {
        val player = PlatformerPlayer(position = Vec2(100f, 80f))
            .withFacing(PlayerFacing.LEFT)
        val crate = ThrowableCrate(position = Vec2(100f, 66f))
        player.pickUpCrate(crate)

        val controller = CrateThrowController()
        var aimedUpParam = false
        controller.onCrateThrown = { _, _, aimedUp ->
            aimedUpParam = aimedUp
        }

        val thrown = controller.throwCarriedCrate(player, isAimingUp = true)

        assertNotNull(thrown)
        assertTrue(aimedUpParam)
        assertEquals(CrateState.THROWN_AIRBORNE, crate.state)

        // Facing left + Aimed Up -> vx = -40f, vy = -280f
        assertEquals(-40f, crate.velocity.x, 0.001f)
        assertEquals(-280f, crate.velocity.y, 0.001f)
    }

    @Test
    fun testAirborneGravityIntegrationOverTime() {
        val player = PlatformerPlayer(position = Vec2(50f, 50f))
            .withFacing(PlayerFacing.RIGHT)
        val crate = ThrowableCrate(position = Vec2(50f, 36f))
        player.pickUpCrate(crate)

        val controller = CrateThrowController()
        controller.throwCarriedCrate(player, isAimingUp = false)

        val context = GameContext()
        val startX = crate.position.x
        val startY = crate.position.y

        // Initial velocity is (210f, -70f)
        assertEquals(210f, crate.velocity.x, 0.001f)
        assertEquals(-70f, crate.velocity.y, 0.001f)

        // Step 1: 0.1s
        controller.update(0.1f, context)

        // vx stays constant 210f
        assertEquals(210f, crate.velocity.x, 0.001f)
        // vy = -70 + 650 * 0.1 = -5f (climbing near apex)
        assertEquals(-5f, crate.velocity.y, 0.001f)
        assertTrue("X advanced forward", crate.position.x > startX)
        assertTrue("Y ascended", crate.position.y < startY)

        // Step 2: another 0.1s -> vy = -5 + 650 * 0.1 = +60f (downward arc)
        controller.update(0.1f, context)
        assertEquals(60f, crate.velocity.y, 0.001f)
    }

    @Test
    fun testAudioTriggerOnThrow() {
        var playedSoundKey: String? = null
        var playedAudioEvent: AudioEvent? = null

        val mockAudioPort = object : AudioPort {
            override var isMuted: Boolean = false
            override fun playSound(event: AudioEvent) {
                playedAudioEvent = event
            }
        }

        val controller = CrateThrowController()
            .withAudioPort(mockAudioPort)
            .onPlaySound { key -> playedSoundKey = key }

        val player = PlatformerPlayer(position = Vec2(100f, 80f))
        val crate = ThrowableCrate(position = Vec2(100f, 66f))
        player.pickUpCrate(crate)

        controller.throwCarriedCrate(player, isAimingUp = false)

        assertEquals("crate_throw", playedSoundKey)
        assertNotNull("Audio port should be called", playedAudioEvent)
    }

    @Test
    fun testHandleActionInputPickUpOrThrow() {
        val player = PlatformerPlayer(position = Vec2(100f, 80f))
            .withFacing(PlayerFacing.RIGHT)
        val groundCrate = ThrowableCrate(position = Vec2(112f, 80f))
        val crates = listOf(groundCrate)

        val controller = CrateThrowController()

        // 1. When empty handed, action press picks up adjacent crate
        val pickedUp = controller.handleActionInput(player, isAimingUp = false, crates = crates)
        assertTrue(pickedUp)
        assertTrue(player.isCarrying)
        assertEquals(groundCrate, player.carriedObject)

        // 2. When carrying, action press throws crate
        val thrown = controller.handleActionInput(player, isAimingUp = true, crates = crates)
        assertTrue(thrown)
        assertFalse(player.isCarrying)
        assertNull(player.carriedObject)
        assertEquals(CrateState.THROWN_AIRBORNE, groundCrate.state)
        assertEquals(40f, groundCrate.velocity.x, 0.001f)
        assertEquals(-280f, groundCrate.velocity.y, 0.001f)
    }

    @Test
    fun testHandleActionInputWithInputPortAimUp() {
        val player = PlatformerPlayer(position = Vec2(100f, 80f))
            .withFacing(PlayerFacing.RIGHT)
        val crate = ThrowableCrate(position = Vec2(100f, 66f))
        player.pickUpCrate(crate)

        val controller = CrateThrowController()

        val mockInput = object : InputPort {
            override val moveX: Float = 0f
            override val moveY: Float = -1f // Upward aim
            override val isJumpPressed: Boolean = false
            override val isDuckPressed: Boolean = false
            override val isActionPressed: Boolean = true
        }

        val success = controller.handleActionInput(player, mockInput)
        assertTrue(success)
        assertEquals(CrateState.THROWN_AIRBORNE, crate.state)
        assertEquals(40f, crate.velocity.x, 0.001f)
        assertEquals(-280f, crate.velocity.y, 0.001f)
    }

    @Test
    fun testPlayerOnActionPressSupportsAimingUp() {
        val player = PlatformerPlayer(position = Vec2(100f, 80f))
            .withFacing(PlayerFacing.RIGHT)
        val crate = ThrowableCrate(position = Vec2(100f, 66f))
        player.pickUpCrate(crate)

        // Aim up throw
        val success = player.onActionPress(isAimingUp = true)
        assertTrue(success)
        assertEquals(CrateState.THROWN_AIRBORNE, crate.state)
        assertEquals(40f, crate.velocity.x, 0.001f)
        assertEquals(-280f, crate.velocity.y, 0.001f)
    }

    @Test
    fun testActiveAirborneCratesPruningOnShatter() {
        val player = PlatformerPlayer(position = Vec2(100f, 80f))
        val crate = ThrowableCrate(position = Vec2(100f, 66f))
        player.pickUpCrate(crate)

        val controller = CrateThrowController()
        controller.throwCarriedCrate(player, isAimingUp = false)

        assertEquals(1, controller.activeAirborneCrates.size)

        // Shatter the crate upon impact
        crate.shatter()

        // Update simulation -> shattered crate pruned from active airborne list
        controller.update(0.016f, GameContext())
        assertEquals(0, controller.activeAirborneCrates.size)
    }

    @Test
    fun testFluentBuilders() {
        val controller = CrateThrowController()
            .withForwardVelocity(250f, -80f)
            .withUpwardVelocity(50f, -300f)
            .withGravity(700f)

        assertEquals(250f, controller.forwardThrowVx, 0.001f)
        assertEquals(-80f, controller.forwardThrowVy, 0.001f)
        assertEquals(50f, controller.upwardThrowVx, 0.001f)
        assertEquals(-300f, controller.upwardThrowVy, 0.001f)
        assertEquals(700f, controller.throwGravity, 0.001f)
    }
}
