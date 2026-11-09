package com.pixie.engine.tilemap

import com.pixie.engine.core.camera.GameViewMode
import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.RenderBatchPort
import com.pixie.engine.core.ports.VirtualResolution
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.tilemap.TileType
import com.pixie.engine.core.tilemap.TilemapChunkLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TilemapChunkLayerTest {

    private class RecordingBatchPort : RenderBatchPort {
        override val resolution: VirtualResolution = VirtualResolution(320, 180)
        override var pixelsGap: Int = 0
        override var isCurrentLayerGridEnabled: Boolean = false

        var drawCount: Int = 0
        val drawnRects = mutableListOf<FloatArray>()

        override fun fillBackground(color: Int) {}
        override fun drawPixelRect(
            x: Float,
            y: Float,
            width: Float,
            height: Float,
            color: Int,
            saturation: Float,
            alpha: Float
        ) {
            drawCount++
            drawnRects.add(floatArrayOf(x, y, width, height))
        }

        override fun drawSprite(sprite: PixelSprite, x: Float, y: Float, saturation: Float, alpha: Float) {}
        override fun drawMotionBlur(sprite: PixelSprite, trail: List<Vec2>, strength: Float, saturation: Float) {}
        override fun drawText(text: String, x: Float, y: Float, color: Int, sizePx: Float) {}
    }

    @Test
    fun testLoadAsciiAndDimensions() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        val ascii = """
            ..........
            ..??BB....
            .[].......
            .|/..-..^.
            ##########
        """.trimIndent()

        layer.loadAscii(ascii)

        assertEquals(10, layer.cols)
        assertEquals(5, layer.rows)
        assertEquals(160f, layer.widthPx, 0.001f)
        assertEquals(80f, layer.heightPx, 0.001f)

        // Check specific tile coordinates
        assertNull(layer.getTile(0, 0)) // empty
        assertEquals(TileType.QUESTION_BLOCK, layer.getTile(2, 1)?.type)
        assertEquals(TileType.BRICK, layer.getTile(4, 1)?.type)
        assertEquals(TileType.PIPE_TOP_LEFT, layer.getTile(1, 2)?.type)
        assertEquals(TileType.PIPE_BODY_LEFT, layer.getTile(1, 3)?.type)
        assertEquals(TileType.ONE_WAY_LEDGE, layer.getTile(5, 3)?.type)
        assertEquals(TileType.SPIKE_HAZARD, layer.getTile(8, 3)?.type)
        assertEquals(TileType.GROUND, layer.getTile(0, 4)?.type)
        assertEquals(TileType.GROUND, layer.getTile(9, 4)?.type)

        // World coordinate conversion
        assertEquals(2, layer.worldToTileCol(35f))
        assertEquals(1, layer.worldToTileRow(20f))
        assertEquals(TileType.QUESTION_BLOCK, layer.getTileAtWorld(35f, 20f)?.type)
    }

    @Test
    fun testSetTileAndClear() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(listOf("...", "..."))

        assertEquals(3, layer.cols)
        assertEquals(2, layer.rows)

        // Set tile to brick
        val brick = layer.setTile(1, 1, TileType.BRICK)
        assertNotNull(brick)
        assertEquals(TileType.BRICK, layer.getTile(1, 1)?.type)

        // Mutate existing tile to used block
        val used = layer.setTile(1, 1, TileType.USED_BLOCK)
        assertEquals(TileType.USED_BLOCK, used?.type)

        // Set to empty clears it
        val emptied = layer.setTile(1, 1, TileType.EMPTY)
        assertNull(emptied)
        assertNull(layer.getTile(1, 1))

        // Out of bounds checks
        assertNull(layer.getTile(-1, 0))
        assertNull(layer.getTile(10, 0))
        assertNull(layer.setTile(-1, 0, TileType.GROUND))

        // Clear
        layer.clear()
        assertEquals(0, layer.cols)
        assertEquals(0, layer.rows)
        assertNull(layer.getTile(0, 0))
    }

    @Test
    fun testLoadIntArray() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        val grid = arrayOf(
            intArrayOf(0, 1, 2),
            intArrayOf(1, 0, 1)
        )

        layer.loadIntArray(grid) { id ->
            when (id) {
                1 -> TileType.GROUND
                2 -> TileType.QUESTION_BLOCK
                else -> TileType.EMPTY
            }
        }

        assertEquals(3, layer.cols)
        assertEquals(2, layer.rows)
        assertNull(layer.getTile(0, 0))
        assertEquals(TileType.GROUND, layer.getTile(1, 0)?.type)
        assertEquals(TileType.QUESTION_BLOCK, layer.getTile(2, 0)?.type)
        assertEquals(TileType.GROUND, layer.getTile(0, 1)?.type)
    }

    @Test
    fun testTriggerBumpAndPhysicsSpringUpdate() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(listOf("?#"))

        // Bump non-bumpable ground -> fails
        val groundBumped = layer.triggerBump(1, 0)
        assertFalse(groundBumped)

        // Bump question block -> succeeds
        val questionBumped = layer.triggerBump(0, 0, velocity = -90f)
        assertTrue(questionBumped)

        val tile = layer.getTile(0, 0)
        assertNotNull(tile)
        assertEquals(-90f, tile!!.bumpVelocityY, 0.001f)

        // Update bump motion
        layer.update(0.05f, GameContext())
        assertTrue("Tile should have recoiled upwards", tile.bumpOffsetY < 0f)

        // Advance until spring returns to rest
        for (i in 0..20) {
            layer.update(0.05f, GameContext())
        }
        assertEquals(0f, tile.bumpOffsetY, 0.001f)
        assertEquals(0f, tile.bumpVelocityY, 0.001f)
    }

    @Test
    fun testZeroAllocationViewportCulling() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        // 20 columns wide map: 320px
        val row1 = "####################" // 20 ground tiles
        layer.loadAscii(listOf(row1))

        val camera = PixieCamera(
            viewport = VirtualResolution(64, 32),
            viewMode = GameViewMode.FixedScreen(screenOrigin = Vec2(0f, 0f))
        )
        // Camera at (0, 0) with viewport (64, 32) sees col 0..4 (5 tiles out of 20)
        val batch = RecordingBatchPort()
        layer.render(batch, camera, GameContext())

        // Ensure rendering occurred for visible tiles
        assertTrue("Should render visible tiles", batch.drawCount > 0)
        val initialCount = batch.drawCount

        // Move camera far to the right beyond the map bounds
        camera.setPositionImmediate(Vec2(500f, 0f))
        val batchOutside = RecordingBatchPort()
        layer.render(batchOutside, camera, GameContext())
        assertEquals(0, batchOutside.drawCount)
    }
}
