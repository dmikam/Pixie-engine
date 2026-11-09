package com.pixie.engine.platformer

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.platformer.MushroomState
import com.pixie.engine.core.platformer.PlatformerPhysicsController
import com.pixie.engine.core.platformer.PlayerTier
import com.pixie.engine.core.platformer.SuperMushroom
import com.pixie.engine.core.tilemap.TilemapChunkLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuperMushroomTest {

    @Test
    fun testMushroomWallBounceReversesDirection() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(
            listOf(
                "....#",
                "....#",
                "#####"
            )
        )

        // Mushroom starts at x=32, y=16 (above row 2 floor) moving right toward wall at col 4 (x=64)
        val mushroom = SuperMushroom(
            position = Vec2(32f, 16f),
            state = MushroomState.MOVING,
            direction = 1f,
            speed = 45f,
            tilemap = layer
        )
        mushroom.isGrounded = true

        var directionChangeCount = 0
        var newDirection = 0f
        mushroom.onDirectionChanged { dir ->
            directionChangeCount++
            newDirection = dir
        }

        // Advance simulation toward wall
        val dt = 1f / 60f
        for (i in 0 until 60) {
            mushroom.updatePhysics(dt, layer)
        }

        // Mushroom hit the wall at x=64, clamped to x=48 (64 - 16), and reversed direction to -1f
        assertEquals(-1f, mushroom.direction, 0.001f)
        assertEquals(-45f, mushroom.velocity.x, 0.001f)
        assertTrue("Direction change callback should fire", directionChangeCount >= 1)
        assertEquals(-1f, newDirection, 0.001f)
        assertTrue("Mushroom should now be moving left", mushroom.position.x <= 48f)
    }

    @Test
    fun testMushroomFallsOffLedgeAndLandsOnGround() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(
            listOf(
                "........",
                "###.....",
                "........",
                "########"
            )
        )

        // Mushroom starts on upper ledge at row 1 (x=16, y=0)
        val mushroom = SuperMushroom(
            position = Vec2(16f, 0f),
            state = MushroomState.MOVING,
            direction = 1f,
            speed = 45f,
            tilemap = layer
        )
        mushroom.isGrounded = true

        val dt = 1f / 60f
        // Walk right past x = 32 (ledge end)
        var fellOffLedge = false
        var landedOnLowerGround = false

        for (i in 0 until 100) {
            mushroom.updatePhysics(dt, layer)
            if (!mushroom.isGrounded && mushroom.position.y > 0f) {
                fellOffLedge = true
            }
            if (mushroom.isGrounded && mushroom.position.y >= 32f) {
                landedOnLowerGround = true
                break
            }
        }

        assertTrue("Mushroom should fall off ledge", fellOffLedge)
        assertTrue("Mushroom should land on lower ground at y=32", landedOnLowerGround)
        assertEquals(32f, mushroom.position.y, 0.001f)
        assertEquals(0f, mushroom.velocity.y, 0.001f)
        assertTrue(mushroom.isGrounded)
    }

    @Test
    fun testCollectionWithPlatformerPhysicsControllerUpgradesTier() {
        val mushroom = SuperMushroom(
            position = Vec2(20f, 20f),
            state = MushroomState.MOVING
        )

        val controller = PlatformerPhysicsController()
        controller.withPosition(20f, 32f)
        controller.landOnGround(32f)
        assertEquals(PlayerTier.SMALL, controller.tier)
        assertEquals(16f, controller.hitboxHeight, 0.001f)

        val player = PixieObject(id = "player_hero", position = Vec2(20f, 32f), size = Vec2(16f, 16f))
        val context = GameContext(score = 500L)

        var powerupCallbackTriggered = false
        mushroom.onPowerupCollected { powerupCallbackTriggered = true }

        val result = mushroom.collect(player, context, controller)
        assertTrue("Collection should succeed", result)
        assertTrue(mushroom.isCollected)
        assertFalse(mushroom.isAlive)
        assertEquals(MushroomState.COLLECTED, mushroom.state)

        // Score awarded (+1000)
        assertEquals(1500L, context.score)
        assertEquals("SUPER", context.getAttribute("player_tier", ""))
        assertTrue(powerupCallbackTriggered)

        // Controller upgraded to SUPER
        assertEquals(PlayerTier.SUPER, controller.tier)
        assertEquals(24f, controller.hitboxHeight, 0.001f)
        assertTrue(controller.tier.canBreakBricks)
        // Controller position adjusted upward by 8px so bottom remains aligned with ground
        assertEquals(24f, controller.position.y, 0.001f)

        // Player object size upgraded to 14x24 and position adjusted
        assertEquals(14f, player.size.x, 0.001f)
        assertEquals(24f, player.size.y, 0.001f)
        assertEquals(24f, player.position.y, 0.001f)
    }

    @Test
    fun testEmergingMushroomCannotBeCollectedUntilEmerged() {
        val mushroom = SuperMushroom.spawnEmerging(col = 2, row = 2, tileSize = 16f)
        assertEquals(MushroomState.EMERGING, mushroom.state)

        val playerBox = com.pixie.engine.core.math.RectF.fromTopLeft(32f, 32f, 16f, 16f)
        assertFalse("Emerging mushroom should not trigger collection overlap", mushroom.checkPlayerOverlap(playerBox))

        // Step emergence to completion
        val context = GameContext()
        while (mushroom.state == MushroomState.EMERGING) {
            mushroom.update(1f / 60f, context)
        }

        assertEquals(MushroomState.MOVING, mushroom.state)
        val movingBox = com.pixie.engine.core.math.RectF.fromTopLeft(mushroom.position.x, mushroom.position.y, 16f, 16f)
        assertTrue("Emerged mushroom should now overlap with player", mushroom.checkPlayerOverlap(movingBox))
    }
}
