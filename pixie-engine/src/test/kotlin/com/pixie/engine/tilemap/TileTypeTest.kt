package com.pixie.engine.tilemap

import com.pixie.engine.core.tilemap.TileData
import com.pixie.engine.core.tilemap.TileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileTypeTest {

    @Test
    fun testTileTypePropertiesAndClassification() {
        // Solid physical blocks
        assertTrue(TileType.GROUND.isSolid)
        assertTrue(TileType.BRICK.isSolid)
        assertTrue(TileType.QUESTION_BLOCK.isSolid)
        assertTrue(TileType.USED_BLOCK.isSolid)
        assertTrue(TileType.PIPE_TOP_LEFT.isSolid)
        assertTrue(TileType.PIPE_TOP_RIGHT.isSolid)
        assertTrue(TileType.PIPE_BODY_LEFT.isSolid)
        assertTrue(TileType.PIPE_BODY_RIGHT.isSolid)
        assertTrue(TileType.CASTLE_BRICK.isSolid)

        // Non-solid tiles
        assertFalse(TileType.EMPTY.isSolid)
        assertFalse(TileType.ONE_WAY_LEDGE.isSolid)
        assertFalse(TileType.SPIKE_HAZARD.isSolid)
        assertFalse(TileType.FLAG_POLE.isSolid)
        assertFalse(TileType.FLAG_TOP.isSolid)

        // Bumpable from underneath
        assertTrue(TileType.QUESTION_BLOCK.isBumpable)
        assertTrue(TileType.BRICK.isBumpable)
        assertFalse(TileType.GROUND.isBumpable)
        assertFalse(TileType.USED_BLOCK.isBumpable)

        // Destructible
        assertTrue(TileType.BRICK.isDestructible)
        assertFalse(TileType.QUESTION_BLOCK.isDestructible)
        assertFalse(TileType.GROUND.isDestructible)

        // One-way and hazards
        assertTrue(TileType.ONE_WAY_LEDGE.isOneWay)
        assertFalse(TileType.GROUND.isOneWay)
        assertTrue(TileType.SPIKE_HAZARD.isHazard)
        assertFalse(TileType.EMPTY.isHazard)

        // Level exit markers
        assertTrue(TileType.FLAG_POLE.isExit)
        assertTrue(TileType.FLAG_TOP.isExit)
        assertTrue(TileType.CASTLE_BRICK.isExit)
        assertFalse(TileType.BRICK.isExit)

        // Warp pipe classifications
        assertTrue(TileType.PIPE_TOP_LEFT.isPipe)
        assertTrue(TileType.PIPE_TOP_RIGHT.isPipe)
        assertTrue(TileType.PIPE_BODY_LEFT.isPipe)
        assertTrue(TileType.PIPE_BODY_RIGHT.isPipe)
        assertFalse(TileType.GROUND.isPipe)
    }

    @Test
    fun testTileTypeFromChar() {
        assertEquals(TileType.GROUND, TileType.fromChar('#'))
        assertEquals(TileType.QUESTION_BLOCK, TileType.fromChar('?'))
        assertEquals(TileType.BRICK, TileType.fromChar('B'))
        assertEquals(TileType.BRICK, TileType.fromChar('b'))
        assertEquals(TileType.USED_BLOCK, TileType.fromChar('U'))
        assertEquals(TileType.USED_BLOCK, TileType.fromChar('u'))
        assertEquals(TileType.PIPE_TOP_LEFT, TileType.fromChar('['))
        assertEquals(TileType.PIPE_TOP_RIGHT, TileType.fromChar(']'))
        assertEquals(TileType.PIPE_BODY_LEFT, TileType.fromChar('|'))
        assertEquals(TileType.PIPE_BODY_RIGHT, TileType.fromChar('/'))
        assertEquals(TileType.PIPE_BODY_RIGHT, TileType.fromChar('}'))
        assertEquals(TileType.ONE_WAY_LEDGE, TileType.fromChar('-'))
        assertEquals(TileType.SPIKE_HAZARD, TileType.fromChar('^'))
        assertEquals(TileType.FLAG_POLE, TileType.fromChar('F'))
        assertEquals(TileType.FLAG_TOP, TileType.fromChar('*'))
        assertEquals(TileType.CASTLE_BRICK, TileType.fromChar('C'))
        assertEquals(TileType.EMPTY, TileType.fromChar('.'))
        assertEquals(TileType.EMPTY, TileType.fromChar(' '))
    }

    @Test
    fun testTileDataStateAndSpringRecoil() {
        val cell = TileData(TileType.QUESTION_BLOCK)
        assertTrue(cell.isSolid)
        assertTrue(cell.isBumpable)
        assertFalse(cell.isDestructible)
        assertEquals(0f, cell.bumpOffsetY, 0.001f)

        // Trigger bump
        cell.triggerBump(velocity = -80f)
        assertEquals(-80f, cell.bumpVelocityY, 0.001f)

        // Update bump physics with gravity 600f, dt = 0.05s
        // velocity = -80 + 30 = -50
        // offset = 0 + (-50 * 0.05) = -2.5
        cell.updateBump(dt = 0.05f, gravity = 600f)
        assertTrue("Bump offset should recoil upwards (negative Y)", cell.bumpOffsetY < 0f)

        // Mutate block type upon hit
        cell.mutateTo(TileType.USED_BLOCK)
        assertEquals(TileType.USED_BLOCK, cell.type)
        assertFalse(cell.isBumpable)
        assertTrue(cell.isSolid)

        // Reset bump
        cell.resetBump()
        assertEquals(0f, cell.bumpOffsetY, 0.001f)
        assertEquals(0f, cell.bumpVelocityY, 0.001f)
        assertEquals(0f, cell.bumpTimer, 0.001f)
    }
}
