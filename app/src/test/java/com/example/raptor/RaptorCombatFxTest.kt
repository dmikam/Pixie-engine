package com.example.raptor

import com.example.raptor.entities.GunTurretDestructible
import com.example.raptor.entities.RaptorBoss
import com.example.raptor.entities.RaptorShip
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.particle.ProceduralExplosion
import com.pixie.engine.core.weapon.ProjectilePool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RaptorCombatFxTest {

    @Test
    fun `boss destruction sequence spawns titan chain explosion particles`() {
        val pool = ProjectilePool(32)
        val particleLayer = ParticleLayer()
        var defeatedCallback = false
        val boss = RaptorBoss(
            initialPos = Vec2(100f, 28f),
            projectilePool = pool,
            onBossDefeated = { defeatedCallback = true }
        )

        assertEquals(0, particleLayer.activeParticles.size)

        // Inflict lethal damage to the boss core
        val killed = boss.takeDamage(1000f, boss.position + Vec2(24f, 18f), particleLayer)

        assertTrue(killed)
        assertTrue(boss.isDefeated)
        assertTrue(defeatedCallback)
        // TITAN_CHAIN triggers a central aerial explosion + 3 secondary plasma bursts
        assertTrue("Expected active particles from TITAN_CHAIN", particleLayer.activeParticles.size >= 40)
    }

    @Test
    fun `mega bomb detonation triggers shockwave and aerial explosion at blast epicenter`() {
        val pool = ProjectilePool(32)
        val particleLayer = ParticleLayer()
        val ship = RaptorShip(
            initialPosition = Vec2(120f, 200f),
            projectilePool = pool
        )
        ship.megaBombs = 1

        assertEquals(0, particleLayer.activeParticles.size)

        val triggered = ship.triggerMegaBomb(
            visibleEnemies = emptyList(),
            activeEnemyProjectiles = emptyList(),
            particleLayer = particleLayer
        )

        assertTrue(triggered)
        assertEquals(0, ship.megaBombs)
        // Epicenter triggers SHOCKWAVE (16 * scale = 32) and AERIAL_EXPLOSION (22 * scale = 33)
        assertTrue("Expected active particles from SHOCKWAVE + AERIAL_EXPLOSION", particleLayer.activeParticles.size >= 40)
    }

    @Test
    fun `ground building destruction triggers ground blast particles`() {
        val pool = ProjectilePool(32)
        val particleLayer = ParticleLayer()
        val turret = GunTurretDestructible(
            id = "turret_1",
            initialPos = Vec2(80f, 120f),
            projectilePool = pool
        )

        assertEquals(0, particleLayer.activeParticles.size)

        val destroyed = turret.takeDamage(1000f, particleLayer)

        assertTrue(destroyed)
        assertTrue(turret.isDestroyed)
        // GROUND_BLAST triggers 26 * 1.5 = 39 particles
        assertTrue("Expected active particles from GROUND_BLAST", particleLayer.activeParticles.size >= 30)
    }

    @Test
    fun `procedural explosion triggers all explosion types without error`() {
        val particleLayer = ParticleLayer()
        val pos = Vec2(50f, 50f)

        for (type in ExplosionType.values()) {
            particleLayer.spawnExplosion(type, pos, scale = 1.0f)
        }

        assertTrue("Expected particles from all procedural explosion types", particleLayer.activeParticles.size > 80)
    }
}
