package com.pixie.engine.camera

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.camera.PlatformerFollowViewMode
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.VirtualResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformerFollowViewModeTest {

    @Test
    fun testCameraRemainsStaticInsideDeadzone() {
        val viewport = VirtualResolution(320, 180)
        val mode = PlatformerFollowViewMode(
            deadzoneWidth = 48f,
            leadOffset = 0f,
            stageWidth = 3200f,
            smoothing = 0f // Instantaneous when leaving deadzone
        )
        val camera = PixieCamera(viewport = viewport, viewMode = mode)
        val context = GameContext()

        // Place target at center of viewport (160f, 90f)
        var targetPos = Vec2(160f, 90f)
        mode.target { targetPos }
        mode.snapToTarget(camera)

        assertEquals(0f, camera.position.x, 0.001f)
        assertEquals(0f, camera.position.y, 0.001f)

        // Target moves right by +15px (within 48px deadzone: +/- 24px)
        targetPos = Vec2(175f, 90f)
        mode.update(camera, null, dt = 0.016f, context = context)
        assertEquals("Camera should remain static inside deadzone", 0f, camera.position.x, 0.001f)

        // Target moves left by -15px (within deadzone)
        targetPos = Vec2(145f, 90f)
        mode.update(camera, null, dt = 0.016f, context = context)
        assertEquals("Camera should remain static inside deadzone", 0f, camera.position.x, 0.001f)
    }

    @Test
    fun testCameraPushesWhenExceedingDeadzone() {
        val viewport = VirtualResolution(320, 180)
        val mode = PlatformerFollowViewMode(
            deadzoneWidth = 48f,
            leadOffset = 0f,
            stageWidth = 3200f,
            smoothing = 0f // Instantaneous push
        )
        val camera = PixieCamera(viewport = viewport, viewMode = mode)
        val context = GameContext()

        var targetPos = Vec2(160f, 90f)
        mode.target { targetPos }
        mode.snapToTarget(camera)
        assertEquals(0f, camera.position.x, 0.001f)

        // Push past right deadzone (center 160 + halfDeadzone 24 = 184f)
        // Moving to 204f (+20px beyond deadzone boundary)
        targetPos = Vec2(204f, 90f)
        mode.update(camera, null, dt = 0.016f, context = context)

        // Camera must shift right by exactly 20px
        assertEquals(20f, camera.position.x, 0.001f)
    }

    @Test
    fun testLookaheadBiasesCameraInFacingDirection() {
        val viewport = VirtualResolution(320, 180)
        val mode = PlatformerFollowViewMode(
            deadzoneWidth = 0f,
            leadOffset = 32f,
            leadSmoothing = 0f, // Instant lead update for testing
            smoothing = 0f,
            stageWidth = 3200f
        )
        val camera = PixieCamera(viewport = viewport, viewMode = mode)
        val context = GameContext()

        var targetPos = Vec2(500f, 90f)
        mode.target { targetPos }
        mode.facing(1f) // Facing right
        mode.snapToTarget(camera)

        // Screen center is 160f. Deadzone center screen = 160 - 32 = 128f.
        // Camera X = 500 - 128 = 372f
        assertEquals(372f, camera.position.x, 0.001f)

        // Switch facing left
        mode.facing(-1f)
        mode.snapToTarget(camera)

        // Screen center 160. Deadzone center screen = 160 - (-32) = 192f.
        // Camera X = 500 - 192 = 308f
        assertEquals(308f, camera.position.x, 0.001f)
    }

    @Test
    fun testStageBoundsClamping() {
        val viewport = VirtualResolution(320, 180)
        val mode = PlatformerFollowViewMode(
            deadzoneWidth = 0f,
            leadOffset = 0f,
            stageWidth = 1000f,
            minX = 0f,
            smoothing = 0f
        )
        val camera = PixieCamera(viewport = viewport, viewMode = mode)
        val context = GameContext()

        // Max camera X is stageWidth (1000) - viewportWidth (320) = 680f
        var targetPos = Vec2(1200f, 90f)
        mode.target { targetPos }
        mode.update(camera, null, dt = 0.016f, context = context)
        assertEquals("Camera X should clamp to max stage bound", 680f, camera.position.x, 0.001f)

        // Min camera X is 0f
        targetPos = Vec2(-50f, 90f)
        mode.update(camera, null, dt = 0.016f, context = context)
        assertEquals("Camera X should clamp to min stage bound", 0f, camera.position.x, 0.001f)
    }

    @Test
    fun testVerticalBaselineHorizonLock() {
        val viewport = VirtualResolution(320, 180)
        val mode = PlatformerFollowViewMode(
            fixedBaselineY = 0f,
            isVerticalLocked = true
        )
        val camera = PixieCamera(viewport = viewport, viewMode = mode)
        val context = GameContext()

        val player = PixieObject("player", position = Vec2(160f, 90f))

        // High jump: y goes up to -150f
        player.position = Vec2(160f, -150f)
        mode.update(camera, player, dt = 0.016f, context = context)
        assertEquals("Camera Y must remain locked at baseline horizon", 0f, camera.position.y, 0.001f)

        // Pit fall: y goes down to 300f
        player.position = Vec2(160f, 300f)
        mode.update(camera, player, dt = 0.016f, context = context)
        assertEquals("Camera Y must remain locked at baseline horizon", 0f, camera.position.y, 0.001f)
    }

    @Test
    fun testOneWayScrollBackwardLock() {
        val viewport = VirtualResolution(320, 180)
        val mode = PlatformerFollowViewMode(
            deadzoneWidth = 0f,
            leadOffset = 0f,
            stageWidth = 3200f,
            smoothing = 0f,
            allowScrollBackwards = false
        )
        val camera = PixieCamera(viewport = viewport, viewMode = mode)
        val context = GameContext()

        var targetPos = Vec2(360f, 90f) // Camera X = 360 - 160 = 200f
        mode.target { targetPos }
        mode.update(camera, null, dt = 0.016f, context = context)
        assertEquals(200f, camera.position.x, 0.001f)

        // Target moves back left (e.g. retreating)
        targetPos = Vec2(200f, 90f) // Desired camera X = 40f
        mode.update(camera, null, dt = 0.016f, context = context)

        // Camera must remain locked at 200f
        assertEquals("Camera should not scroll backwards when locked", 200f, camera.position.x, 0.001f)
    }

    @Test
    fun testFluentBuilderConfiguration() {
        val mode = PlatformerFollowViewMode()
            .deadzone(64f, 32f)
            .lookahead(40f, 8f)
            .stageBounds(width = 4000f, height = 360f, minX = 10f, minY = 5f)
            .horizonLock(baselineY = 12f, locked = true)
            .smoothing(horizontal = 15f, vertical = 5f)
            .facing(-1f)
            .allowBackwardsScroll(false)

        assertEquals(64f, mode.deadzoneWidth, 0.001f)
        assertEquals(32f, mode.verticalDeadzoneHeight, 0.001f)
        assertEquals(40f, mode.leadOffset, 0.001f)
        assertEquals(8f, mode.leadSmoothing, 0.001f)
        assertEquals(4000f, mode.stageWidth, 0.001f)
        assertEquals(360f, mode.stageHeight, 0.001f)
        assertEquals(10f, mode.minX, 0.001f)
        assertEquals(5f, mode.minY, 0.001f)
        assertEquals(12f, mode.fixedBaselineY, 0.001f)
        assertTrue(mode.isVerticalLocked)
        assertEquals(15f, mode.smoothing, 0.001f)
        assertEquals(5f, mode.verticalSmoothing, 0.001f)
        assertEquals(-1f, mode.facingDirection, 0.001f)
        assertFalse(mode.allowScrollBackwards)
    }
}
