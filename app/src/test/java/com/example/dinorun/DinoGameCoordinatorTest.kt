package com.example.dinorun

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dinorun.entities.CactusObstacle
import com.example.dinorun.entities.ColorSparkPickup
import com.example.dinorun.entities.PterodactylHazard
import com.example.dinorun.world.PrehistoricWorldDefinition
import com.example.dinorun.world.VolcanicCraterWorldDefinition
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.android.input.DualModeInputAdapter
import com.pixie.engine.core.math.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DinoGameCoordinatorTest {

    private fun createContextAndCoordinator(): Triple<AndroidSoundPoolAudioAdapter, DualModeInputAdapter, DinoGameCoordinator> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioAdapter = AndroidSoundPoolAudioAdapter(context)
        val hapticAdapter = AndroidHapticAdapter(context)
        val inputAdapter = DualModeInputAdapter()
        val coordinator = DinoGameCoordinator(
            audioAdapter = audioAdapter,
            hapticAdapter = hapticAdapter,
            inputAdapter = inputAdapter
        )
        return Triple(audioAdapter, inputAdapter, coordinator)
    }

    @Test
    fun `initialization sets up prehistoric world, grounded player and default metrics`() {
        val (audio, _, coordinator) = createContextAndCoordinator()
        try {
            assertEquals("prehistoric_ash", coordinator.currentWorld.id)
            assertEquals(135f, coordinator.currentWorld.horizonY, 0.001f)
            assertEquals(360, coordinator.resolution.width)
            assertEquals(200, coordinator.resolution.height)

            val player = coordinator.player
            assertEquals(40f, player.position.x, 0.001f)
            assertEquals(119f, player.position.y, 0.001f)
            assertTrue(player.isGrounded)
            assertFalse(player.isDucking)
            assertEquals("run", player.currentAnimationName)

            assertFalse(coordinator.engine.context.isGameOver)
            assertEquals(3, coordinator.engine.context.livesLeft)
            assertEquals(0L, coordinator.engine.context.score)
            assertEquals(0f, coordinator.engine.colorRecovery.progress, 0.001f)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `jump input triggers vertical leap, airborne state and jump animation`() {
        val (audio, input, coordinator) = createContextAndCoordinator()
        try {
            assertTrue(coordinator.player.isGrounded)

            input.setButtonJump(true)
            coordinator.updateInputs()

            val player = coordinator.player
            assertFalse(player.isGrounded)
            assertTrue("Jump force should give upward velocity", player.velocity.y < -200f)
            assertEquals("jump", player.currentAnimationName)
            assertTrue(player.motionBlur.enabled)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `airborne jump physics integrates gravity and lands smoothly back on ground`() {
        val (audio, input, coordinator) = createContextAndCoordinator()
        try {
            val player = coordinator.player
            input.setButtonJump(true)
            coordinator.updateInputs()
            input.setButtonJump(false)

            val initialGroundY = coordinator.currentWorld.horizonY - player.size.y

            // Step forward through upward arc
            player.update(0.1f, coordinator.engine.context)
            assertTrue("Player ascends in air", player.position.y < initialGroundY)
            assertFalse(player.isGrounded)

            // Step until player reaches peak and falls back down to ground
            repeat(15) {
                player.update(0.05f, coordinator.engine.context)
            }

            assertTrue("Player should land back on ground", player.isGrounded)
            assertEquals(initialGroundY, player.position.y, 0.001f)
            assertEquals(0f, player.velocity.y, 0.001f)
            assertFalse("Motion blur disables on landing", player.motionBlur.enabled)
            assertEquals("run", player.currentAnimationName)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `ducking input shrinks bounding box and suppresses jumping`() {
        val (audio, input, coordinator) = createContextAndCoordinator()
        try {
            val player = coordinator.player

            input.setButtonDuck(true)
            coordinator.updateInputs()

            assertTrue(player.isDucking)
            assertEquals(Vec2(20f, 12f), player.size)
            assertEquals("duck", player.currentAnimationName)

            // Jumping while ducking must be ignored
            player.jump()
            assertTrue("Ducking player remains grounded", player.isGrounded)
            assertEquals(0f, player.velocity.y, 0.001f)

            // Releasing duck resets bounding box and animation
            input.setButtonDuck(false)
            coordinator.updateInputs()

            assertFalse(player.isDucking)
            assertEquals(Vec2(16f, 16f), player.size)
            assertEquals("run", player.currentAnimationName)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `collectible spark recovers color progression and awards score`() {
        val (audio, _, coordinator) = createContextAndCoordinator()
        try {
            assertEquals(0f, coordinator.engine.colorRecovery.progress, 0.001f)

            val spark = ColorSparkPickup(
                id = "test_spark",
                position = coordinator.player.position,
                restoreAmount = 0.15f
            )
            coordinator.engine.itemsLayer.addObject(spark)

            var stateChangedNotified = false
            coordinator.onStateChanged = { stateChangedNotified = true }

            coordinator.checkCustomCollisions()

            assertFalse("Spark should be collected", spark.isAlive)
            assertEquals(0.15f, coordinator.engine.colorRecovery.progress, 0.001f)
            assertTrue("Score should be awarded", coordinator.engine.context.score >= 50L)
            assertTrue("State change callback should be invoked", stateChangedNotified)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `obstacle collision inflicts lethal damage, deactivates obstacle and triggers game over`() {
        val (audio, _, coordinator) = createContextAndCoordinator()
        try {
            assertFalse(coordinator.engine.context.isGameOver)
            assertTrue(coordinator.player.isAlive)

            val cactus = CactusObstacle(
                id = "test_cactus",
                position = coordinator.player.position
            )
            coordinator.engine.itemsLayer.addObject(cactus)

            var stateChangedNotified = false
            coordinator.onStateChanged = { stateChangedNotified = true }

            coordinator.checkCustomCollisions()

            assertFalse("Obstacle should be deactivated on impact", cactus.isAlive)
            assertFalse("Player health is depleted", coordinator.player.isAlive)
            assertEquals(2, coordinator.engine.context.livesLeft)
            assertTrue("Lethal collision triggers game over", coordinator.engine.context.isGameOver)
            assertTrue("State change listener invoked", stateChangedNotified)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `pterodactyl hazard collision inflicts damage and deactivates hazard`() {
        val (audio, _, coordinator) = createContextAndCoordinator()
        try {
            val ptero = PterodactylHazard(
                id = "test_ptero",
                position = coordinator.player.position
            )
            coordinator.engine.itemsLayer.addObject(ptero)
            coordinator.checkCustomCollisions()

            assertFalse("Aerial hazard should be deactivated", ptero.isAlive)
            assertFalse(coordinator.player.isAlive)
            assertTrue(coordinator.engine.context.isGameOver)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `invulnerable player survives collision and decrements lives without ending game`() {
        val (audio, _, coordinator) = createContextAndCoordinator()
        try {
            assertEquals(3, coordinator.engine.context.livesLeft)
            coordinator.player.grantInvulnerability(10f)

            // Hit 1 with shield/i-frames
            val c1 = CactusObstacle("c1", coordinator.player.position)
            coordinator.engine.itemsLayer.addObject(c1)
            coordinator.checkCustomCollisions()

            assertFalse(c1.isAlive)
            assertTrue(coordinator.player.isAlive)
            assertEquals(2, coordinator.engine.context.livesLeft)
            assertFalse(coordinator.engine.context.isGameOver)

            // Hit 2 with shield/i-frames
            val c2 = CactusObstacle("c2", coordinator.player.position)
            coordinator.engine.itemsLayer.addObject(c2)
            coordinator.checkCustomCollisions()

            assertEquals(1, coordinator.engine.context.livesLeft)
            assertFalse(coordinator.engine.context.isGameOver)

            // Hit 3 exhausts remaining lives
            val c3 = CactusObstacle("c3", coordinator.player.position)
            coordinator.engine.itemsLayer.addObject(c3)
            coordinator.checkCustomCollisions()

            assertEquals(0, coordinator.engine.context.livesLeft)
            assertTrue("Depleting all lives triggers game over", coordinator.engine.context.isGameOver)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `distance progression updates score and tracks milestones`() {
        val (audio, _, coordinator) = createContextAndCoordinator()
        try {
            coordinator.player.position = Vec2(1050f, coordinator.player.position.y)
            coordinator.checkCustomCollisions()

            assertEquals(105f, coordinator.engine.context.distance, 0.001f)
            assertTrue("Score accumulates from distance", coordinator.engine.context.score >= 210L)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `world switching and restart resets game state and updates world configuration`() {
        val (audio, _, coordinator) = createContextAndCoordinator()
        try {
            coordinator.engine.context.livesLeft = 1
            coordinator.engine.context.isGameOver = true

            val volcanicWorld = VolcanicCraterWorldDefinition()
            coordinator.switchWorld(volcanicWorld)

            assertEquals("volcanic_ridge", coordinator.currentWorld.id)
            assertFalse(coordinator.engine.context.isGameOver)
            assertEquals(3, coordinator.engine.context.livesLeft)
            assertTrue(coordinator.player.isGrounded)
            assertEquals(volcanicWorld.horizonY, coordinator.engine.horizon.horizonY, 0.001f)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `pixel gap adjustment updates both engine and canvas renderer`() {
        val (audio, _, coordinator) = createContextAndCoordinator()
        try {
            coordinator.setPixelGap(2)
            assertEquals(2, coordinator.engine.pixelsGap)
            assertEquals(2, coordinator.renderer.pixelsGap)

            coordinator.setPixelGap(0)
            assertEquals(0, coordinator.engine.pixelsGap)
            assertEquals(0, coordinator.renderer.pixelsGap)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `dynamic difficulty scales player run speed as difficulty rises`() {
        val (audio, _, coordinator) = createContextAndCoordinator()
        try {
            val player = coordinator.player

            coordinator.engine.context.difficulty = 0
            player.update(0.016f, coordinator.engine.context)
            val baseRunSpeed = player.velocity.x

            coordinator.engine.context.difficulty = 80
            player.update(0.016f, coordinator.engine.context)
            val boostedRunSpeed = player.velocity.x

            assertTrue("Run speed should scale up with higher difficulty", boostedRunSpeed > baseRunSpeed)
        } finally {
            audio.release()
        }
    }
}
