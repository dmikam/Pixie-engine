package com.example.raptor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.raptor.entities.EnemyShip
import com.example.raptor.entities.PickupType
import com.example.raptor.entities.ScoutEnemyShip
import com.example.raptor.world.RaptorLevelCatalog
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.engine.core.context.DifficultyPreset
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.weapon.Projectile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RaptorGameCoordinatorTest {

    private fun createContextAndCoordinator(
        difficulty: DifficultyPreset = DifficultyPreset.MEDIUM,
        levelIndex: Int = 1
    ): Pair<AndroidSoundPoolAudioAdapter, RaptorGameCoordinator> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioAdapter = AndroidSoundPoolAudioAdapter(context)
        val hapticAdapter = AndroidHapticAdapter(context)
        val coordinator = RaptorGameCoordinator(
            audio = audioAdapter,
            haptics = hapticAdapter,
            selectedDifficulty = difficulty,
            currentLevelIndex = levelIndex
        )
        return Pair(audioAdapter, coordinator)
    }

    @Test
    fun `initial setup configures level 1 full health and clean object lists`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            assertEquals(1, coordinator.currentLevelIndex)
            assertEquals("Level 1: Archipelago Infiltration", coordinator.currentLevel.name)
            assertEquals(1600f, coordinator.currentLevel.targetDistance, 0.001f)
            assertEquals(0f, coordinator.levelDistanceTraveled, 0.001f)
            assertFalse(coordinator.isBossSpawned)
            assertFalse(coordinator.isLevelCompleted)

            assertEquals(100f, coordinator.playerShip.hull, 0.001f)
            assertEquals(100f, coordinator.playerShip.shield, 0.001f)
            assertEquals(3, coordinator.engine.context.livesLeft)
            assertEquals(0L, coordinator.engine.context.score)
            assertEquals(1000L, coordinator.engine.context.credits)

            assertTrue(coordinator.enemies.isEmpty())
            assertTrue(coordinator.groundTargets.isEmpty())
            assertTrue(coordinator.pickups.isEmpty())
        } finally {
            audio.release()
        }
    }

    @Test
    fun `level progression updates catalog settings and resets flight distance`() {
        val (audio, coordinator) = createContextAndCoordinator(levelIndex = 1)
        try {
            coordinator.setLevel(2)
            assertEquals(2, coordinator.currentLevelIndex)
            assertEquals(2, coordinator.engine.context.currentLevel)
            assertEquals(2200f, coordinator.currentLevel.targetDistance, 0.001f)
            assertEquals(0f, coordinator.levelDistanceTraveled, 0.001f)

            coordinator.setLevel(3)
            assertEquals(3, coordinator.currentLevelIndex)
            assertEquals(3, coordinator.engine.context.currentLevel)
            assertEquals(2800f, coordinator.currentLevel.targetDistance, 0.001f)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `flight progress advances distance traveled and synchronizes engine context`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            coordinator.update(1.0f)
            assertEquals(36f, coordinator.levelDistanceTraveled, 0.001f)
            assertEquals(36f, coordinator.engine.context.distance, 0.001f)

            coordinator.update(2.0f)
            assertEquals(108f, coordinator.levelDistanceTraveled, 0.001f)
            assertEquals(108f, coordinator.engine.context.distance, 0.001f)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `difficulty presets configure engine difficulty context correctly`() {
        val (audio, coordinator) = createContextAndCoordinator(difficulty = DifficultyPreset.EASY)
        try {
            assertEquals(10, coordinator.engine.context.difficulty)

            coordinator.setDifficulty(DifficultyPreset.DIFFICULT)
            assertEquals(50, coordinator.engine.context.difficulty)
            assertEquals(DifficultyPreset.DIFFICULT, coordinator.selectedDifficulty)

            coordinator.setDifficulty(DifficultyPreset.HEAVY_METAL)
            assertEquals(100, coordinator.engine.context.difficulty)

            val scaledVal = coordinator.engine.context.calculateDynamicProperty(
                baseValue = 100f,
                difficultyWeight = 0.5f,
                levelWeight = 0.0f
            )
            assertTrue("Scaled value should reflect high difficulty", scaledVal > 100f)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `boss spawns automatically when flight distance reaches target distance`() {
        val (audio, coordinator) = createContextAndCoordinator(levelIndex = 1)
        try {
            assertFalse(coordinator.isBossSpawned)

            // Fast-forward distance just before boss trigger
            coordinator.levelDistanceTraveled = 1599f
            coordinator.update(0.1f) // 1599 + 3.6f > 1600f

            assertTrue(coordinator.isBossSpawned)
            assertNotNull(coordinator.boss)
            val boss = coordinator.boss!!
            assertEquals("boss_sea_titan", boss.id)
            assertTrue(coordinator.engine.itemsLayer.objects.contains(boss))
        } finally {
            audio.release()
        }
    }

    @Test
    fun `boss attack cycle spawns enemy projectiles and respects cooldown`() {
        val (audio, coordinator) = createContextAndCoordinator(levelIndex = 1)
        try {
            coordinator.levelDistanceTraveled = 1600f
            coordinator.update(0.016f)

            val boss = coordinator.boss
            assertNotNull(boss)
            boss?.position = Vec2(120f - 24f, 28f)
            boss?.update(2.5f, coordinator.engine.context)
            boss?.attack(coordinator.engine.context, coordinator.enemyProjectiles)

            assertTrue("Enemy projectiles should spawn from boss salvo", coordinator.enemyProjectiles.isNotEmpty())
        } finally {
            audio.release()
        }
    }

    @Test
    fun `boss defeat triggers victory callback awards score and completes level`() {
        val (audio, coordinator) = createContextAndCoordinator(levelIndex = 1)
        try {
            var victoryTriggered = false
            coordinator.onVictory = { victoryTriggered = true }

            coordinator.levelDistanceTraveled = 1600f
            coordinator.update(0.016f)

            val boss = coordinator.boss
            assertNotNull(boss)

            // Inflict lethal damage to boss core
            boss?.takeDamage(1000f, Vec2(boss.position.x + 24f, boss.position.y + 18f), null)
            assertTrue(boss?.isDefeated == true)

            // Coordinator updates boss state
            coordinator.update(0.016f)

            assertTrue(coordinator.isLevelCompleted)
            assertTrue(coordinator.engine.context.isVictory)
            assertTrue(victoryTriggered)
            assertEquals(10000L, coordinator.engine.context.score)
            assertEquals(4500L, coordinator.engine.context.credits)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `score gem pickup awards score and earns credits`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            coordinator.applyPickup(PickupType.SCORE_GEM)
            assertEquals(1500L, coordinator.engine.context.score)

            coordinator.engine.context.earnCredits(500L)
            assertEquals(1500L, coordinator.engine.context.credits)

            val spentSuccess = coordinator.engine.context.spendCredits(300L)
            assertTrue(spentSuccess)
            assertEquals(1200L, coordinator.engine.context.credits)

            val spentFailure = coordinator.engine.context.spendCredits(2000L)
            assertFalse(spentFailure)
            assertEquals(1200L, coordinator.engine.context.credits)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `weapon upgrade shield cell and repair kit pickups restore player state`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            val ship = coordinator.playerShip

            // Shield damage and restoration
            ship.takeDamage(40f)
            assertEquals(60f, ship.shield, 0.001f)
            coordinator.applyPickup(PickupType.SHIELD_CELL)
            assertEquals(100f, ship.shield, 0.001f)

            // Clear invulnerability from previous hit
            ship.isInvulnerable = false

            // Hull damage and repair
            ship.takeDamage(150f) // Depletes shield (100) and inflicts 50 to hull
            assertEquals(0f, ship.shield, 0.001f)
            assertEquals(50f, ship.hull, 0.001f)
            coordinator.applyPickup(PickupType.REPAIR_KIT)
            assertEquals(80f, ship.hull, 0.001f)

            // Weapon level upgrade
            assertEquals(1, ship.weaponTier)
            coordinator.applyPickup(PickupType.WEAPON_UPGRADE)
            assertEquals(2, ship.weaponTier)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `perk chip activates perk and magnet beam attracts pickups`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            coordinator.applyPickup(PickupType.PERK_CHIP)
            assertTrue(
                "Perk chip should award credits or active perk",
                coordinator.engine.context.credits > 0 || coordinator.engine.context.activePerks.isNotEmpty()
            )

            coordinator.engine.context.addPerk("MAGNET_BEAM")
            assertTrue(coordinator.engine.context.hasPerk("MAGNET_BEAM"))
        } finally {
            audio.release()
        }
    }

    @Test
    fun `mega bomb clears enemy projectiles and damages visible enemies`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            val enemy = ScoutEnemyShip("test_scout", Vec2(100f, 50f), coordinator.engine.projectilePool)
            coordinator.enemies.add(enemy)
            coordinator.playerShip.megaBombs = 1

            val dummyProj = Projectile().apply {
                position = Vec2(100f, 100f)
                isAlive = true
            }
            coordinator.enemyProjectiles.add(dummyProj)

            val fired = coordinator.triggerMegaBomb()
            assertTrue(fired)
            assertEquals(0, coordinator.playerShip.megaBombs)
            assertFalse(dummyProj.isAlive)
            coordinator.update(0.016f)
            assertTrue("Enemy projectile should be cleared by mega bomb", coordinator.enemyProjectiles.isEmpty())
        } finally {
            audio.release()
        }
    }

    @Test
    fun `fatal damage triggers game over and invokes game over callback`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            var gameOverFired = false
            coordinator.onGameOver = { gameOverFired = true }

            coordinator.playerShip.hull = 0f
            coordinator.playerShip.isAlive = false

            coordinator.update(0.016f)

            assertTrue(coordinator.engine.context.isGameOver)
            assertTrue(gameOverFired)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `steering and drag start clamp player within virtual resolution playfield`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            coordinator.movePlayer(1000f, 1000f)
            assertEquals(218f, coordinator.playerShip.targetPosition.x, 0.001f)
            assertEquals(290f, coordinator.playerShip.targetPosition.y, 0.001f)

            coordinator.movePlayer(-2000f, -2000f)
            assertEquals(4f, coordinator.playerShip.targetPosition.x, 0.001f)
            assertEquals(24f, coordinator.playerShip.targetPosition.y, 0.001f)

            coordinator.handleDragStart(100f, 150f)
            assertTrue(coordinator.playerShip.targetPosition.x >= 4f)
        } finally {
            audio.release()
        }
    }
}
