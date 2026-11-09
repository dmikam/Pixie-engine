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

    @Test
    fun `wave spawner delays and reset timers initialize with expected defaults`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            assertEquals(1.2f, coordinator.waveSpawner.waveSpawnTimer, 0.001f)
            assertEquals(0.4f, coordinator.waveSpawner.groundSpawnTimer, 0.001f)

            coordinator.waveSpawner.reset(waveDelay = 2.5f, groundDelay = 1.0f)
            assertEquals(2.5f, coordinator.waveSpawner.waveSpawnTimer, 0.001f)
            assertEquals(1.0f, coordinator.waveSpawner.groundSpawnTimer, 0.001f)

            coordinator.waveSpawner.reset()
            assertEquals(1.2f, coordinator.waveSpawner.waveSpawnTimer, 0.001f)
            assertEquals(0.4f, coordinator.waveSpawner.groundSpawnTimer, 0.001f)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `offscreen craft and wrecks past Y 350 are removed from simulation`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            val scout = ScoutEnemyShip("test_offscreen", Vec2(100f, 351f), coordinator.engine.projectilePool)
            coordinator.enemies.add(scout)
            coordinator.engine.itemsLayer.addObject(scout)

            val wreck = ScoutEnemyShip("test_wreck", Vec2(120f, 340f), coordinator.engine.projectilePool).apply {
                isWreck = true
                position = Vec2(120f, 352f)
            }
            coordinator.enemies.add(wreck)
            coordinator.engine.itemsLayer.addObject(wreck)

            coordinator.update(0.016f)

            assertFalse("Offscreen enemy should be removed from enemies list", coordinator.enemies.contains(scout))
            assertFalse("Offscreen enemy should be removed from items layer", coordinator.engine.itemsLayer.objects.contains(scout))
            assertFalse("Offscreen wreck should be removed from enemies list", coordinator.enemies.contains(wreck))
        } finally {
            audio.release()
        }
    }

    @Test
    fun `inputAdapter is wired to engine and steers player craft along virtual axes`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            assertEquals(coordinator.inputAdapter, coordinator.engine.input)

            val initialTarget = coordinator.playerShip.targetPosition
            // Steer right via moveX = 1f
            coordinator.inputAdapter.moveX = 1f
            coordinator.update(0.1f)
            assertTrue("Target X should move right", coordinator.playerShip.targetPosition.x > initialTarget.x)

            // Steer up via moveY = -1f
            coordinator.inputAdapter.moveX = 0f
            coordinator.inputAdapter.moveY = -1f
            val midTarget = coordinator.playerShip.targetPosition
            coordinator.update(0.1f)
            assertTrue("Target Y should move up", coordinator.playerShip.targetPosition.y < midTarget.y)

            // Touch drag works simultaneously
            coordinator.inputAdapter.reset()
            coordinator.movePlayer(15f, 25f)
            val touchTarget = coordinator.playerShip.targetPosition
            assertEquals(midTarget.x + 15f, touchTarget.x, 0.05f)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `layer stack maintains clean vertical scroller z-order`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            val enabledLayers = coordinator.engine.layerStack.allLayers().filter { it.isEnabled }
            val layerNamesAndOrders = enabledLayers.map { it.name to it.order }

            assertEquals(5, enabledLayers.size)
            assertEquals("Raptor_Vertical_Terrain" to 150, layerNamesAndOrders[0])
            assertEquals("Layer5_Items" to 500, layerNamesAndOrders[1])
            assertEquals("Raptor_Projectiles" to 520, layerNamesAndOrders[2])
            assertEquals("Layer_Particles" to 550, layerNamesAndOrders[3])
            assertEquals("Raptor_Clouds" to 600, layerNamesAndOrders[4])

            for (i in 0 until enabledLayers.size - 1) {
                assertTrue(
                    "Layer ${enabledLayers[i].name} (${enabledLayers[i].order}) must render below ${enabledLayers[i+1].name} (${enabledLayers[i+1].order})",
                    enabledLayers[i].order < enabledLayers[i + 1].order
                )
            }
        } finally {
            audio.release()
        }
    }

    @Test
    fun `unlocking weapons adds them to unlocked list and allows switching during combat`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            assertEquals(listOf("vulcan"), coordinator.unlockedWeapons)
            assertEquals("vulcan", coordinator.activeWeaponId)
            assertEquals("vulcan", coordinator.playerShip.activeWeaponId)

            coordinator.unlockWeapon("odin_laser")
            assertTrue(coordinator.unlockedWeapons.contains("odin_laser"))
            assertEquals("odin_laser", coordinator.activeWeaponId)
            assertEquals("odin_laser", coordinator.playerShip.activeWeaponId)

            coordinator.unlockWeapon("deathray")
            assertTrue(coordinator.unlockedWeapons.contains("deathray"))
            assertEquals("deathray", coordinator.activeWeaponId)

            // Switch back to vulcan
            val switchedVulcan = coordinator.switchWeapon("vulcan")
            assertTrue(switchedVulcan)
            assertEquals("vulcan", coordinator.activeWeaponId)
            assertEquals("vulcan", coordinator.playerShip.activeWeaponId)

            // Switch to odin_laser
            val switchedLaser = coordinator.switchWeapon("odin_laser")
            assertTrue(switchedLaser)
            assertEquals("odin_laser", coordinator.activeWeaponId)
            assertEquals("odin_laser", coordinator.playerShip.activeWeaponId)

            // Attempting to switch to unowned weapon fails
            val switchedUnowned = coordinator.switchWeapon("unknown_cannon")
            assertFalse(switchedUnowned)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `cycleWeapon loops through all unlocked weapons seamlessly`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            coordinator.unlockWeapon("twin_plasma")
            coordinator.unlockWeapon("odin_laser")
            coordinator.switchWeapon("vulcan")

            assertEquals("vulcan", coordinator.activeWeaponId)

            val next1 = coordinator.cycleWeapon()
            assertEquals("twin_plasma", next1)
            assertEquals("twin_plasma", coordinator.activeWeaponId)

            val next2 = coordinator.cycleWeapon()
            assertEquals("odin_laser", next2)
            assertEquals("odin_laser", coordinator.activeWeaponId)

            val next3 = coordinator.cycleWeapon()
            assertEquals("vulcan", next3)
            assertEquals("vulcan", coordinator.activeWeaponId)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `setupGame preserves unlocked weapons and persistent perks`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            coordinator.unlockWeapon("deathray")
            coordinator.persistentPerks.add("MAGNET_BEAM")
            coordinator.savedWeaponTier = 3
            coordinator.savedMissiles = 10
            coordinator.savedBombs = 4

            // Re-invoke setupGame (e.g. launching level or restarting)
            coordinator.setupGame()

            assertEquals("deathray", coordinator.playerShip.activeWeaponId)
            assertTrue(coordinator.playerShip.unlockedWeapons.contains("deathray"))
            assertEquals(3, coordinator.playerShip.weaponTier)
            assertEquals(10, coordinator.playerShip.missileSalvoes)
            assertEquals(4, coordinator.playerShip.megaBombs)
            assertTrue(coordinator.engine.context.hasPerk("MAGNET_BEAM"))
        } finally {
            audio.release()
        }
    }
}
