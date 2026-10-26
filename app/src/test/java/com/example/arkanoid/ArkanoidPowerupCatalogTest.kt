package com.example.arkanoid

import com.example.arkanoid.entities.ArkanoidPaddle
import com.example.arkanoid.entities.EnergyBall
import com.example.arkanoid.world.ArkanoidPowerupCatalog
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.matrix.PowerUpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ArkanoidPowerupCatalogTest {

    @Test
    fun `catalog definitions contain valid metadata for all powerup types`() {
        for (type in PowerUpType.values()) {
            val def = ArkanoidPowerupCatalog.getDefinition(type)
            assertEquals(type, def.type)
            assertTrue("Display name must not be blank", def.displayName.isNotBlank())
            assertTrue("Glyph must not be blank", def.glyph.isNotBlank())
            assertEquals(500, def.scoreBonus)
            assertTrue("Drop weight must be positive", def.dropWeight > 0f)
        }
    }

    @Test
    fun `duration constants match expected gameplay values`() {
        assertEquals(15f, ArkanoidPowerupCatalog.DURATION_LASER)
        assertEquals(18f, ArkanoidPowerupCatalog.DURATION_WIDE_PADDLE)
        assertEquals(9f, ArkanoidPowerupCatalog.DURATION_PLASMA_BALL)
    }

    @Test
    fun `rollRandomPowerup returns valid powerups within distribution`() {
        val seeded = Random(42)
        val sample = List(20) { ArkanoidPowerupCatalog.rollRandomPowerup(seeded) }
        assertTrue("Sample should contain varied powerups", sample.toSet().size > 1)
    }

    @Test
    fun `rollDropFromHit respects destroyed state and probability`() {
        val seeded = Random(42)
        assertNull(ArkanoidPowerupCatalog.rollDropFromHit(wasDestroyed = false, dropChance = 1.0f, random = seeded))
        val drop = ArkanoidPowerupCatalog.rollDropFromHit(wasDestroyed = true, dropChance = 1.0f, random = seeded)
        assertNotNull(drop)
    }

    @Test
    fun `assignPowerupForLayout maps exact level bricks`() {
        assertEquals(PowerUpType.LASER, ArkanoidPowerupCatalog.assignPowerupForLayout(levelNumber = 1, row = 0, col = 5))
        assertEquals(PowerUpType.WIDE_PADDLE, ArkanoidPowerupCatalog.assignPowerupForLayout(levelNumber = 1, row = 1, col = 2))
        assertEquals(PowerUpType.MULTI_BALL, ArkanoidPowerupCatalog.assignPowerupForLayout(levelNumber = 1, row = 2, col = 9))
        assertEquals(PowerUpType.PLASMA_BALL, ArkanoidPowerupCatalog.assignPowerupForLayout(levelNumber = 2, row = 1, col = 5))
        assertEquals(PowerUpType.SAFETY_BARRIER, ArkanoidPowerupCatalog.assignPowerupForLayout(levelNumber = 3, row = 0, col = 5))
        assertNull(ArkanoidPowerupCatalog.assignPowerupForLayout(levelNumber = 1, row = 0, col = 0))
    }

    @Test
    fun `applyEffect triggers wide paddle, laser, and barrier`() {
        val paddle = ArkanoidPaddle(initialX = 160f, y = 160f)
        val balls = mutableListOf<EnergyBall>()

        ArkanoidPowerupCatalog.applyEffect(PowerUpType.WIDE_PADDLE, paddle, balls, 135f)
        assertTrue(paddle.isWide)
        assertEquals(18f, paddle.wideTimer)

        ArkanoidPowerupCatalog.applyEffect(PowerUpType.LASER, paddle, balls, 135f)
        assertTrue(paddle.isLaser)
        assertEquals(15f, paddle.laserTimer)

        ArkanoidPowerupCatalog.applyEffect(PowerUpType.SAFETY_BARRIER, paddle, balls, 135f)
        assertTrue(paddle.hasBarrier)
    }

    @Test
    fun `applyEffect triggers multi-ball split and plasma transformation`() {
        val paddle = ArkanoidPaddle(initialX = 160f, y = 160f)
        val initialBall = EnergyBall(
            position = Vec2(100f, 100f),
            velocity = Vec2(50f, -80f),
            radius = 2.5f
        ).apply {
            isDocked = false
            speed = 135f
        }
        val balls = mutableListOf(initialBall)

        // Multi-ball splits 1 ball into 3 total
        ArkanoidPowerupCatalog.applyEffect(PowerUpType.MULTI_BALL, paddle, balls, 135f)
        assertEquals(3, balls.size)

        // Plasma ball makes all balls plasma
        ArkanoidPowerupCatalog.applyEffect(PowerUpType.PLASMA_BALL, paddle, balls, 135f)
        assertTrue(balls.all { it.isPlasma })
    }

    @Test
    fun `formatActiveStatus returns formatted labels or null`() {
        val paddle = ArkanoidPaddle(initialX = 160f, y = 160f)
        val balls = listOf(EnergyBall(position = Vec2(100f, 100f), radius = 2.5f))

        assertNull(ArkanoidPowerupCatalog.formatActiveStatus(paddle, balls))

        paddle.isLaser = true
        paddle.laserTimer = 12.4f
        assertEquals("LASER CANNON (12s)", ArkanoidPowerupCatalog.formatActiveStatus(paddle, balls))

        paddle.isLaser = false
        paddle.isWide = true
        paddle.wideTimer = 9.8f
        assertEquals("WIDE PADDLE (9s)", ArkanoidPowerupCatalog.formatActiveStatus(paddle, balls))

        paddle.isWide = false
        paddle.hasBarrier = true
        assertEquals("SAFETY BARRIER ACTIVE", ArkanoidPowerupCatalog.formatActiveStatus(paddle, balls))
    }
}
