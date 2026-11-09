package com.example.raptor

import com.example.raptor.entities.PickupType
import com.example.raptor.world.RaptorDropCatalog
import com.pixie.engine.core.math.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RaptorDropCatalogTest {

    @Test
    fun `rollDropType maps boundary percentiles to expected pickup types`() {
        val testRng = object : Random() {
            var nextVal = 0
            override fun nextBits(bitCount: Int): Int = nextVal
            override fun nextInt(until: Int): Int = nextVal
        }

        // Test boundary values
        testRng.nextVal = 0
        assertEquals(PickupType.WEAPON_UPGRADE, RaptorDropCatalog.rollDropType(testRng))
        testRng.nextVal = 19
        assertEquals(PickupType.WEAPON_UPGRADE, RaptorDropCatalog.rollDropType(testRng))

        testRng.nextVal = 20
        assertEquals(PickupType.HOMING_MISSILES, RaptorDropCatalog.rollDropType(testRng))
        testRng.nextVal = 35
        assertEquals(PickupType.HOMING_MISSILES, RaptorDropCatalog.rollDropType(testRng))

        testRng.nextVal = 36
        assertEquals(PickupType.SHIELD_CELL, RaptorDropCatalog.rollDropType(testRng))
        testRng.nextVal = 53
        assertEquals(PickupType.SHIELD_CELL, RaptorDropCatalog.rollDropType(testRng))

        testRng.nextVal = 54
        assertEquals(PickupType.REPAIR_KIT, RaptorDropCatalog.rollDropType(testRng))
        testRng.nextVal = 69
        assertEquals(PickupType.REPAIR_KIT, RaptorDropCatalog.rollDropType(testRng))

        testRng.nextVal = 70
        assertEquals(PickupType.MEGA_BOMB, RaptorDropCatalog.rollDropType(testRng))
        testRng.nextVal = 79
        assertEquals(PickupType.MEGA_BOMB, RaptorDropCatalog.rollDropType(testRng))

        testRng.nextVal = 80
        assertEquals(PickupType.SCORE_GEM, RaptorDropCatalog.rollDropType(testRng))
        testRng.nextVal = 91
        assertEquals(PickupType.SCORE_GEM, RaptorDropCatalog.rollDropType(testRng))

        testRng.nextVal = 92
        assertEquals(PickupType.PERK_CHIP, RaptorDropCatalog.rollDropType(testRng))
        testRng.nextVal = 99
        assertEquals(PickupType.PERK_CHIP, RaptorDropCatalog.rollDropType(testRng))
    }

    @Test
    fun `spawnPickup initializes valid entity with downward velocity`() {
        val spawnPos = Vec2(120f, 80f)
        val pickup = RaptorDropCatalog.spawnPickup(spawnPos)

        assertNotNull(pickup)
        assertEquals(spawnPos.x, pickup.position.x, 0.001f)
        assertEquals(spawnPos.y, pickup.position.y, 0.001f)
        assertEquals(0f, pickup.velocity.x, 0.001f)
        assertEquals(36f, pickup.velocity.y, 0.001f)
        assertTrue(pickup.isAlive)
    }
}
