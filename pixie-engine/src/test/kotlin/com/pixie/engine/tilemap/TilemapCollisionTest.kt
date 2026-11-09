package com.pixie.engine.tilemap

import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.tilemap.CollisionContactSide
import com.pixie.engine.core.tilemap.TileType
import com.pixie.engine.core.tilemap.TilemapChunkLayer
import com.pixie.engine.core.tilemap.TilemapCollisionQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TilemapCollisionTest {

    @Test
    fun testComputeGridRangeCoordinates() {
        // Tile size 16f
        // Box at (16, 16) with width 16, height 16 -> occupies column 1, row 1 exactly
        val box = RectF.fromTopLeft(16f, 16f, 16f, 16f)
        val range = TilemapCollisionQuery.computeGridRange(box, tileSize = 16f, maxCols = 10, maxRows = 10)

        assertTrue(range.isValid)
        assertEquals(1, range.minCol)
        assertEquals(1, range.maxCol)
        assertEquals(1, range.minRow)
        assertEquals(1, range.maxRow)

        // Box spanning columns 1 and 2
        val spanningBox = RectF.fromTopLeft(20f, 16f, 16f, 16f) // left 20, right 36 -> cols 1..2
        val spanningRange = TilemapCollisionQuery.computeGridRange(spanningBox, tileSize = 16f, maxCols = 10, maxRows = 10)
        assertEquals(1, spanningRange.minCol)
        assertEquals(2, spanningRange.maxCol)

        // Out of bounds box
        val outOfBounds = RectF.fromTopLeft(-30f, -30f, 10f, 10f)
        val outRange = TilemapCollisionQuery.computeGridRange(outOfBounds, tileSize = 16f, maxCols = 10, maxRows = 10)
        assertFalse(outRange.isValid)
    }

    @Test
    fun testEntityLandingOnGroundAndClamping() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        val ascii = """
            ....
            ....
            ####
        """.trimIndent()
        layer.loadAscii(ascii)

        // Entity of size 16x16 falling into ground row 2 (y = 32f)
        // Attempting to move to y = 35f (penetrating 3px into the floor)
        val targetBox = RectF.fromTopLeft(16f, 35f, 16f, 16f) // bottom = 51f, floor top = 32f
        val velocityY = 120f

        val resolution = TilemapCollisionQuery.resolveVertical(layer, targetBox, velocityY)

        assertTrue(resolution.isGrounded)
        assertFalse(resolution.hasBonkedCeiling)
        assertEquals(0f, resolution.resolvedVelocityY, 0.001f)
        // Entity should be clamped on top of the ground: 32f - 16f = 16f
        assertEquals(16f, resolution.resolvedY, 0.001f)
        assertEquals(1, resolution.hits.size)
        assertEquals(CollisionContactSide.GROUND, resolution.hits.first().side)
        assertEquals(TileType.GROUND, resolution.hits.first().tile.type)
    }

    @Test
    fun testUpwardJumpingCeilingHeadBonk() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        val ascii = """
            .?.
            ...
            ...
        """.trimIndent()
        layer.loadAscii(ascii)

        // Question block at col 1, row 0 (y: 0..16f)
        // Entity jumping up with velocityY = -250f
        // Position penetrating ceiling: y = 12f, top = 12f (penetrating into 0..16)
        val targetBox = RectF.fromTopLeft(16f, 12f, 16f, 16f)
        val velocityY = -250f

        val resolution = TilemapCollisionQuery.resolveVertical(layer, targetBox, velocityY)

        assertFalse(resolution.isGrounded)
        assertTrue(resolution.hasBonkedCeiling)
        assertEquals(0f, resolution.resolvedVelocityY, 0.001f)
        // Entity top clamped to bottom of block: 16f
        assertEquals(16f, resolution.resolvedY, 0.001f)
        assertEquals(1, resolution.hits.size)
        assertEquals(CollisionContactSide.CEILING, resolution.hits.first().side)
        assertEquals(TileType.QUESTION_BLOCK, resolution.hits.first().tile.type)
    }

    @Test
    fun testOneWayLedgePassThroughFromBelow() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        val ascii = """
            ...
            .-.
            ...
        """.trimIndent()
        layer.loadAscii(ascii)

        // One-way ledge at col 1, row 1 (y: 16..32f)
        // Jumping up through ledge with velocityY < 0
        val targetBox = RectF.fromTopLeft(16f, 20f, 16f, 16f)
        val velocityY = -200f

        val resolution = TilemapCollisionQuery.resolveVertical(layer, targetBox, velocityY)

        // Jumping up should not hit or clamp
        assertFalse(resolution.hasBonkedCeiling)
        assertFalse(resolution.isGrounded)
        assertEquals(0, resolution.hits.size)
        assertEquals(20f, resolution.resolvedY, 0.001f)
        assertEquals(velocityY, resolution.resolvedVelocityY, 0.001f)
    }

    @Test
    fun testOneWayLedgeLandingFromAbove() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        val ascii = """
            ...
            .-.
            ...
        """.trimIndent()
        layer.loadAscii(ascii)

        // One-way ledge at col 1, row 1 (top edge is at y = 16f)
        // Entity falling downwards from above: previousBottom was at 15f (above 16f)
        // TargetBox bottom is at 18f (penetrating ledge top by 2f)
        val targetBox = RectF.fromTopLeft(16f, 2f, 16f, 16f) // bottom = 18f
        val velocityY = 90f
        val previousBottom = 15f

        val resolution = TilemapCollisionQuery.resolveVertical(
            layer = layer,
            box = targetBox,
            velocityY = velocityY,
            previousBottom = previousBottom
        )

        assertTrue("Should land on one-way ledge", resolution.isGrounded)
        assertEquals(0f, resolution.resolvedVelocityY, 0.001f)
        // Top clamped to: ledgeTop(16f) - height(16f) = 0f
        assertEquals(0f, resolution.resolvedY, 0.001f)
        assertEquals(1, resolution.hits.size)
        assertEquals(CollisionContactSide.GROUND, resolution.hits.first().side)
        assertEquals(TileType.ONE_WAY_LEDGE, resolution.hits.first().tile.type)
    }

    @Test
    fun testOneWayLedgeWalkThroughHorizontally() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        val ascii = """
            ...
            .-.
            ...
        """.trimIndent()
        layer.loadAscii(ascii)

        // Walking horizontally through the one-way ledge
        val movingRightBox = RectF.fromTopLeft(12f, 16f, 16f, 16f)
        val rightRes = TilemapCollisionQuery.resolveHorizontal(layer, movingRightBox, velocityX = 80f)
        assertFalse("One-way ledge must not block horizontally moving right", rightRes.hasWallCollision)
        assertEquals(12f, rightRes.resolvedX, 0.001f)
        assertEquals(80f, rightRes.resolvedVelocityX, 0.001f)

        val movingLeftBox = RectF.fromTopLeft(20f, 16f, 16f, 16f)
        val leftRes = TilemapCollisionQuery.resolveHorizontal(layer, movingLeftBox, velocityX = -80f)
        assertFalse("One-way ledge must not block horizontally moving left", leftRes.hasWallCollision)
        assertEquals(20f, leftRes.resolvedX, 0.001f)
        assertEquals(-80f, leftRes.resolvedVelocityX, 0.001f)
    }

    @Test
    fun testWallCollisionsLeftAndRight() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        val ascii = """
            #..#
            #..#
        """.trimIndent()
        layer.loadAscii(ascii)

        // Right wall at col 3 (x: 48..64)
        // Entity at x = 35f, width = 16f (right = 51f, penetrating 3px)
        val rightBox = RectF.fromTopLeft(35f, 0f, 16f, 16f)
        val rightRes = TilemapCollisionQuery.resolveHorizontal(layer, rightBox, velocityX = 100f)

        assertTrue(rightRes.hasWallCollision)
        assertEquals(0f, rightRes.resolvedVelocityX, 0.001f)
        // Clamped to wall left (48f) - width (16f) = 32f
        assertEquals(32f, rightRes.resolvedX, 0.001f)
        assertEquals(CollisionContactSide.RIGHT_WALL, rightRes.hits.first().side)

        // Left wall at col 0 (x: 0..16)
        // Entity at x = 12f, width = 16f (left = 12f, penetrating 4px into 0..16)
        val leftBox = RectF.fromTopLeft(12f, 0f, 16f, 16f)
        val leftRes = TilemapCollisionQuery.resolveHorizontal(layer, leftBox, velocityX = -100f)

        assertTrue(leftRes.hasWallCollision)
        assertEquals(0f, leftRes.resolvedVelocityX, 0.001f)
        // Clamped to wall right (16f)
        assertEquals(16f, leftRes.resolvedX, 0.001f)
        assertEquals(CollisionContactSide.LEFT_WALL, leftRes.hits.first().side)
    }

    @Test
    fun testGeneralQueryCollisionsWithContactSideClassification() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(listOf("?#"))

        val prevBox = RectF.fromTopLeft(0f, 20f, 16f, 16f)
        val currentBox = RectF.fromTopLeft(0f, 14f, 16f, 16f)
        val vel = Vec2(0f, -60f)

        val hits = TilemapCollisionQuery.queryCollisions(layer, currentBox, prevBox, vel)
        assertEquals(1, hits.size)
        assertEquals(CollisionContactSide.CEILING, hits.first().side)
        assertEquals(TileType.QUESTION_BLOCK, hits.first().tile.type)
    }

    @Test
    fun testCameraFollowsTargetWithDeadzoneSmoothing() {
        val camera = com.pixie.engine.core.camera.PixieCamera()
        val mode = com.pixie.engine.core.camera.PlatformerFollowViewMode(
            deadzoneWidth = 48f,
            leadOffset = 0f,
            stageWidth = 3200f,
            smoothing = 10f
        )
        camera.viewMode = mode

        val player = com.pixie.engine.core.entity.PixieObject("mario", position = Vec2(160f, 90f))
        mode.snapToTarget(camera, player)
        assertEquals(0f, camera.position.x, 0.001f)

        // Inside deadzone: no movement
        player.position = Vec2(170f, 90f)
        camera.update(player, dt = 0.016f, context = com.pixie.engine.core.context.GameContext())
        assertEquals(0f, camera.position.x, 0.001f)

        // Exceed deadzone: camera smoothly tracks target
        player.position = Vec2(220f, 90f)
        camera.update(player, dt = 0.05f, context = com.pixie.engine.core.context.GameContext())
        assertTrue("Camera X should smoothly move right toward target", camera.position.x > 0f)
    }
}

