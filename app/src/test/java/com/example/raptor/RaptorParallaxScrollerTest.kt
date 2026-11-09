package com.example.raptor

import com.example.raptor.world.RaptorCloudLayer
import com.example.raptor.world.RaptorLevelCatalog
import com.example.raptor.world.RaptorTerrainLayer
import com.pixie.engine.core.context.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RaptorParallaxScrollerTest {

    @Test
    fun `terrain layer speed scales dynamically with forward moving velocity`() {
        var activeLevel = RaptorLevelCatalog.getLevel(1)
        val terrainLayer = RaptorTerrainLayer(currentLevelGetter = { activeLevel })

        // Stationary velocity: speed equals base 36f
        val baseSpeed = terrainLayer.calculateEffectiveSpeed(movingSpeed = 0f)
        assertEquals(36f, baseSpeed, 0.001f)

        // Half thrust (180 px/s): speed scales by 1 + (0.5 * 0.4) = 1.2x
        val midSpeed = terrainLayer.calculateEffectiveSpeed(movingSpeed = 180f)
        assertEquals(36f * 1.2f, midSpeed, 0.001f)

        // Maximum thrust (360 px/s): speed scales by 1 + (1.0 * 0.4) = 1.4x
        val fullSpeed = terrainLayer.calculateEffectiveSpeed(movingSpeed = 360f)
        assertEquals(36f * 1.4f, fullSpeed, 0.001f)

        // Higher speeds remain capped at max multiplier
        val overSpeed = terrainLayer.calculateEffectiveSpeed(movingSpeed = 500f)
        assertEquals(36f * 1.4f, overSpeed, 0.001f)
    }

    @Test
    fun `cloud layer speed scales dynamically with forward moving velocity`() {
        val cloudLayer = RaptorCloudLayer()

        // Stationary velocity: speed equals base 75f
        val baseSpeed = cloudLayer.calculateEffectiveSpeed(movingSpeed = 0f)
        assertEquals(75f, baseSpeed, 0.001f)

        // Maximum thrust (360 px/s): speed scales by 1.4x (105f)
        val fullSpeed = cloudLayer.calculateEffectiveSpeed(movingSpeed = 360f)
        assertEquals(75f * 1.4f, fullSpeed, 0.001f)
        assertTrue("Cloud layer speed should always exceed terrain speed for altitude depth", fullSpeed > 36f * 1.4f)
    }

    @Test
    fun `terrain biomes update accurately on level transitions`() {
        var currentLevelIndex = 1
        val terrainLayer = RaptorTerrainLayer(currentLevelGetter = { RaptorLevelCatalog.getLevel(currentLevelIndex) })

        assertEquals("Level 1: Archipelago Infiltration", terrainLayer.currentLevelGetter().name)
        assertEquals(1, terrainLayer.currentLevelGetter().levelIndex)

        // Transition to Level 2
        currentLevelIndex = 2
        assertEquals("Level 2: Sulfur Canyon & Refinery", terrainLayer.currentLevelGetter().name)
        assertEquals(2, terrainLayer.currentLevelGetter().levelIndex)

        // Transition to Level 3
        currentLevelIndex = 3
        assertEquals("Level 3: Orbital Command Citadel", terrainLayer.currentLevelGetter().name)
        assertEquals(3, terrainLayer.currentLevelGetter().levelIndex)
    }
}
