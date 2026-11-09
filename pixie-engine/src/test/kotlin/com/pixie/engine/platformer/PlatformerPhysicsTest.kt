package com.pixie.engine.platformer

import com.pixie.engine.core.platformer.PlatformerCollisionResolver
import com.pixie.engine.core.platformer.PlatformerPhysicsConfig
import com.pixie.engine.core.platformer.PlatformerPhysicsController
import com.pixie.engine.core.platformer.PlayerAnimationState
import com.pixie.engine.core.platformer.PlayerFacing
import com.pixie.engine.core.tilemap.TileType
import com.pixie.engine.core.tilemap.TilemapChunkLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformerPhysicsTest {

    @Test
    fun testReversingDirectionWhileSprintingEntersSkidMode() {
        var skidStarted = false
        var skidEnded = false

        val controller = PlatformerPhysicsController()
            .withGrounded(true)
            .withVelocity(vx = 170f, vy = 0f)
            .withFacing(PlayerFacing.RIGHT)

        controller.onSkidStart = { skidStarted = true }
        controller.onSkidEnd = { skidEnded = true }

        // Reverse input while sprinting right
        controller.setInput(moveX = -1f, run = true)
        controller.update(1f / 60f)

        assertTrue("Reversing direction at high speed should trigger skid", skidStarted)
        assertTrue(controller.isSkidding)
        assertEquals(PlayerAnimationState.SKID, controller.state)

        // Step until skidding stops
        for (i in 0 until 15) {
            controller.update(1f / 60f)
        }

        assertFalse("Skid mode should finish once velocity reverses", controller.isSkidding)
        assertTrue(skidEnded)
        assertEquals(PlayerFacing.LEFT, controller.facing)
    }

    @Test
    fun testTappingJumpProducesLowerJumpApexThanHoldingJump() {
        // Full jump (jump button held continuously)
        val fullController = PlatformerPhysicsController()
            .withPosition(0f, 200f)
            .withGrounded(true)
        fullController.pressJump()

        var fullApexY = 200f
        for (i in 0 until 30) {
            fullController.update(1f / 60f)
            if (fullController.position.y < fullApexY) {
                fullApexY = fullController.position.y
            }
        }
        val fullApexHeight = 200f - fullApexY

        // Cut jump (jump tapped and immediately released)
        val cutController = PlatformerPhysicsController()
            .withPosition(0f, 200f)
            .withGrounded(true)
        cutController.pressJump()
        cutController.releaseJump()

        var cutApexY = 200f
        for (i in 0 until 30) {
            cutController.update(1f / 60f)
            if (cutController.position.y < cutApexY) {
                cutApexY = cutController.position.y
            }
        }
        val cutApexHeight = 200f - cutApexY

        assertTrue(
            "Full jump apex ($fullApexHeight) should be significantly higher than cut jump apex ($cutApexHeight)",
            fullApexHeight > cutApexHeight * 1.7f
        )
    }

    @Test
    fun testSlippedLedgeAllowsJumpWithinCoyoteWindow() {
        var jumpTriggered = false
        val controller = PlatformerPhysicsController().withGrounded(true)
        controller.onJump = { jumpTriggered = true }

        // Player walks off ledge
        controller.leaveGround()
        assertFalse(controller.isGrounded)

        // Advance 3 frames (50ms, within 120ms coyote window)
        for (i in 0 until 3) {
            controller.update(1f / 60f)
        }

        assertTrue(controller.timeSinceGrounded <= controller.config.coyoteTimeDuration)

        controller.pressJump()
        assertTrue("Jump should execute during coyote window", jumpTriggered)
        assertEquals(controller.config.jumpImpulse, controller.velocityY, 0.01f)
    }

    @Test
    fun testHittingCeilingZeroesVerticalVelocityAndTriggersHeadBonkHook() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        val ascii = """
            .?.
            ...
            ...
        """.trimIndent()
        layer.loadAscii(ascii)

        var bonkedCol = -1
        var bonkedRow = -1

        val resolver = PlatformerCollisionResolver()
            .withHitbox(width = 14f, height = 16f, offsetX = 1f, offsetY = 0f)
            .onHeadBonk { col, row ->
                bonkedCol = col
                bonkedRow = row
            }

        // Question block is at col 1, row 0 (x: 16..32, y: 0..16)
        // Player positioned at x = 16f, y = 20f moving upward with vy = -200f
        val controller = PlatformerPhysicsController()
            .withPosition(16f, 20f)
            .withGrounded(false)
            .withVelocity(0f, -200f)

        // Update with collision resolution: target y penetrates ceiling (y = 20 - 200 * (1/60) = 16.67f)
        controller.update(1f / 30f, layer, resolver)

        assertEquals("Head bonk hook should trigger on question block column", 1, bonkedCol)
        assertEquals("Head bonk hook should trigger on question block row", 0, bonkedRow)
        assertEquals("Vertical velocity should be zeroed upon bonking ceiling", 0f, controller.velocityY, 0.001f)
        assertEquals("Player y should be clamped to block bottom (16f)", 16f, controller.position.y, 0.001f)

        // Verify tile bump spring animation was initiated on the layer
        val questionTile = layer.getTile(1, 0)
        assertTrue("Collided tile should have bump velocity initialized", (questionTile?.bumpVelocityY ?: 0f) < 0f)
    }

    @Test
    fun testHorizontalWallCollisionClampsPositionAndZeroesVelocityX() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        val ascii = """
            ....
            .#..
            ####
        """.trimIndent()
        layer.loadAscii(ascii)

        var hitWall = false
        val resolver = PlatformerCollisionResolver()
            .withHitbox(width = 14f, height = 16f, offsetX = 1f, offsetY = 0f)
            .onWallHit { col, row, isRightWall ->
                hitWall = isRightWall
            }

        // Brick wall at col 1, row 1 (x: 16..32, y: 16..32)
        // Player at x = 0f, y = 16f moving right towards wall with vx = 120f
        val controller = PlatformerPhysicsController()
            .withPosition(0f, 16f)
            .withGrounded(true)
            .withVelocity(vx = 120f, vy = 0f)

        // 6 frames of movement: 0 + 120 * 0.1s = 12f displacement -> would penetrate wall (right edge 12 + 15 = 27f > 16f)
        controller.update(0.1f, layer, resolver)

        assertTrue("Wall hit should be detected on right-facing motion", hitWall)
        assertEquals("Horizontal velocity must be zeroed upon hitting wall", 0f, controller.velocityX, 0.001f)
        // Player right edge (position.x + offsetX + width = clampedX + 1 + 14 = clampedX + 15) must touch 16f
        // Therefore clampedX = 16 - 14 - 1 = 1f
        assertEquals(1f, controller.position.x, 0.001f)
    }

    @Test
    fun testLandingOnFloorZeroesVelocityYSetsGroundedAndResetsCoyoteTimer() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        val ascii = """
            ....
            ....
            ####
        """.trimIndent()
        layer.loadAscii(ascii)

        var landed = false
        val resolver = PlatformerCollisionResolver()
            .withHitbox(width = 14f, height = 16f, offsetX = 1f, offsetY = 0f)
            .onLand { _, _ -> landed = true }

        // Ground is at row 2 (y = 32..48)
        // Player falling from mid-air: y = 20f, vy = 200f
        val controller = PlatformerPhysicsController()
            .withPosition(8f, 20f)
            .withGrounded(false)
            .withVelocity(0f, 200f)

        controller.update(1f / 30f, layer, resolver)

        assertTrue("Landing callback should fire", landed)
        assertTrue("Player should be grounded after landing", controller.isGrounded)
        assertEquals("Vertical velocity should be zeroed on landing", 0f, controller.velocityY, 0.001f)
        assertEquals("Coyote timer should reset to 0 upon landing", 0f, controller.timeSinceGrounded, 0.001f)
        // Player feet (position.y + 16) clamped to ground top (32f) -> position.y = 16f
        assertEquals(16f, controller.position.y, 0.001f)
    }

    @Test
    fun testWalkingOffLedgeStartsCoyoteWindowAndAppliesGravity() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        val ascii = """
            ....
            ##..
            ....
        """.trimIndent()
        layer.loadAscii(ascii)

        val resolver = PlatformerCollisionResolver()
            .withHitbox(width = 14f, height = 16f, offsetX = 1f, offsetY = 0f)

        // Player standing on ground at col 1, row 1 (y: 16..32, feet at 16f) -> position.y = 0f
        val controller = PlatformerPhysicsController()
            .withPosition(16f, 0f)
            .withGrounded(true)
            .setInput(moveX = 1f, run = false)

        // Walk right past the ledge (col 2 is empty)
        for (i in 0 until 25) {
            controller.update(1f / 60f, layer, resolver)
        }

        assertTrue("Player should no longer be grounded after walking off cliff", !controller.isGrounded)
        assertTrue("Coyote timer should have started ticking", controller.timeSinceGrounded > 0f)
        assertTrue("Downward vertical velocity should begin accumulating from gravity", controller.velocityY > 0f)
    }

    @Test
    fun testSubSteppedResolutionPreventsTunnelingAtHighVelocity() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        val ascii = """
            ....
            ....
            ####
        """.trimIndent()
        layer.loadAscii(ascii)

        val resolver = PlatformerCollisionResolver()
            .withHitbox(width = 14f, height = 16f, offsetX = 1f, offsetY = 0f)
            .withSubStepping(maxDt = 1f / 60f, maxSize = 8f)

        // Player falling with terminal velocity (360 px/s) over a large dt (0.1s -> 36 px displacement!)
        // Without sub-stepping, 36px displacement would jump straight through the 16px ground block!
        val controller = PlatformerPhysicsController()
            .withPosition(8f, 10f)
            .withGrounded(false)
            .withVelocity(0f, 360f)

        controller.update(0.1f, layer, resolver)

        assertTrue("Sub-stepping must catch ground collision and prevent tunneling", controller.isGrounded)
        assertEquals(16f, controller.position.y, 0.001f)
        assertEquals(0f, controller.velocityY, 0.001f)
    }
}
