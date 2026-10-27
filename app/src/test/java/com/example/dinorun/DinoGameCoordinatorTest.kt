package com.example.dinorun

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dinorun.entities.CactusObstacle
import com.example.dinorun.entities.ColorSparkPickup
import com.example.dinorun.entities.PterodactylHazard
import com.example.dinorun.entities.VolcanicMeteor
import com.example.dinorun.world.PrehistoricWorldDefinition
import com.example.dinorun.world.VolcanicCraterWorldDefinition
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.android.input.ControlMode
import com.pixie.android.input.DualModeInputAdapter
import com.pixie.engine.core.layer.CelestialTransitMode
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioEvent
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
            assertTrue("Spark collection triggers burst particles", coordinator.engine.particleLayer.activeParticles.isNotEmpty())
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
            assertTrue("Hazard collision triggers spark particles", coordinator.engine.particleLayer.activeParticles.isNotEmpty())
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
            assertTrue("Hazard collision triggers spark particles", coordinator.engine.particleLayer.activeParticles.isNotEmpty())
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
            val playedAudio = mutableListOf<AudioEvent>()
            audio.onSoundPlayed = { playedAudio.add(it) }

            // Step before 100m (e.g. 80m -> position.x = 800f)
            coordinator.player.position = Vec2(800f, coordinator.player.position.y)
            coordinator.checkCustomCollisions()
            assertEquals(80f, coordinator.engine.context.distance, 0.001f)
            assertFalse("No milestone before 100m", playedAudio.contains(AudioEvent.SCORE_MILESTONE))

            // Step across 100m milestone (e.g. 105m -> position.x = 1050f)
            coordinator.player.position = Vec2(1050f, coordinator.player.position.y)
            coordinator.checkCustomCollisions()

            assertEquals(105f, coordinator.engine.context.distance, 0.001f)
            assertTrue("Score accumulates from distance", coordinator.engine.context.score >= 210L)
            assertEquals("SCORE_MILESTONE plays once at 100m", 1, playedAudio.count { it == AudioEvent.SCORE_MILESTONE })

            // Advance before 200m (e.g. 180m)
            coordinator.player.position = Vec2(1800f, coordinator.player.position.y)
            coordinator.checkCustomCollisions()
            assertEquals("SCORE_MILESTONE not played between 100m and 200m", 1, playedAudio.count { it == AudioEvent.SCORE_MILESTONE })

            // Cross 200m milestone (e.g. 210m)
            coordinator.player.position = Vec2(2100f, coordinator.player.position.y)
            coordinator.checkCustomCollisions()
            assertEquals("SCORE_MILESTONE plays again at 200m", 2, playedAudio.count { it == AudioEvent.SCORE_MILESTONE })

            // Restart resets milestone tracking
            coordinator.restart()
            playedAudio.clear()
            coordinator.player.position = Vec2(1050f, coordinator.player.position.y)
            coordinator.checkCustomCollisions()
            assertEquals("SCORE_MILESTONE triggers after restart across 100m", 1, playedAudio.count { it == AudioEvent.SCORE_MILESTONE })
        } finally {
            audio.release()
        }
    }

    @Test
    fun `full color restoration triggers victory fanfare audio event`() {
        val (audio, _, coordinator) = createContextAndCoordinator()
        try {
            val playedAudio = mutableListOf<AudioEvent>()
            audio.onSoundPlayed = { playedAudio.add(it) }

            // Set progress to 90%
            coordinator.engine.colorRecovery.setProgress(0.90f)

            val spark = ColorSparkPickup(
                id = "victory_spark",
                position = coordinator.player.position,
                restoreAmount = 0.15f
            )
            coordinator.engine.itemsLayer.addObject(spark)
            coordinator.checkCustomCollisions()

            assertEquals(1.0f, coordinator.engine.colorRecovery.progress, 0.001f)
            assertTrue("100% color recovery triggers SCORE_MILESTONE victory fanfare", playedAudio.contains(AudioEvent.SCORE_MILESTONE))
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
            assertEquals(volcanicWorld.transitMode, coordinator.engine.skyLayer.transitMode)
            assertEquals(volcanicWorld.celestialCenter, coordinator.engine.skyLayer.celestialCenter)
            assertEquals(volcanicWorld.celestialRadiusX, coordinator.engine.skyLayer.celestialRadiusX, 0.001f)
            assertEquals(volcanicWorld.celestialRadiusY, coordinator.engine.skyLayer.celestialRadiusY, 0.001f)
            assertEquals(volcanicWorld.dayNightCycleSeconds, coordinator.engine.skyLayer.dayNightCycleSeconds, 0.001f)
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

    @Test
    fun `touch gestures in touch mode trigger jump and duck on coordinator player`() {
        val (audio, input, coordinator) = createContextAndCoordinator()
        try {
            input.currentMode = ControlMode.TOUCH_GESTURES

            // Tap or swipe-up triggers jump
            input.triggerGestureJump()
            coordinator.updateInputs()

            val player = coordinator.player
            assertFalse("Jump gesture should make player airborne", player.isGrounded)
            assertTrue("Jump should give negative y velocity", player.velocity.y < 0f)

            input.releaseGestureJump()
            coordinator.updateInputs()

            // Step player until landed
            repeat(20) {
                player.update(0.05f, coordinator.engine.context)
            }
            assertTrue("Player should land back on ground", player.isGrounded)

            // Swipe-down triggers duck
            input.triggerGestureDuck(true)
            coordinator.updateInputs()

            assertTrue("Duck gesture should make player duck", player.isDucking)
            assertEquals(Vec2(20f, 12f), player.size)

            // Releasing duck gesture restores running posture
            input.triggerGestureDuck(false)
            coordinator.updateInputs()

            assertFalse("Releasing duck gesture should restore running", player.isDucking)
            assertEquals(Vec2(16f, 16f), player.size)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `pterodactyl hazard updates position along sine wave trajectory`() {
        val (audio, _, coordinator) = createContextAndCoordinator()
        try {
            val startPos = Vec2(300f, 100f)
            val ptero = PterodactylHazard(
                id = "sine_ptero",
                position = startPos,
                speed = 40f
            )

            // Step 1: 0.1s
            ptero.update(0.1f, coordinator.engine.context)
            assertTrue("Pterodactyl moves leftwards", ptero.position.x < startPos.x)
            assertEquals(0.1f, ptero.flightTime, 0.001f)
            assertTrue("Trajectory velocity is non-zero in X", ptero.velocity.x < 0f)

            // Step over several frames
            val prevY = ptero.position.y
            ptero.update(0.2f, coordinator.engine.context)
            assertTrue("Pterodactyl continues moving left", ptero.position.x < startPos.x - 4f)
            assertTrue("Pterodactyl oscillates vertically along trajectory", ptero.position.y != prevY)
            assertEquals("fly", ptero.currentAnimationName)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `volcanic meteor triggers when player approaches and falls along linear trajectory to ground impact`() {
        val (audio, _, coordinator) = createContextAndCoordinator()
        try {
            val groundY = coordinator.currentWorld.horizonY
            val targetX = 200f
            val meteor = VolcanicMeteor(
                id = "test_meteor",
                targetGroundX = targetX,
                groundY = groundY,
                triggerDistance = 150f
            )

            // Initially before trigger distance (distance = 0, playerX = 0)
            meteor.update(0.016f, coordinator.engine.context)
            assertFalse("Meteor should not start falling when player is far away", meteor.hasStartedFalling)
            assertEquals(-20f, meteor.position.y, 0.001f)

            // Player moves close enough to trigger (playerX = 60f -> 200 - 60 = 140 <= 150)
            coordinator.engine.context.distance = 6f
            meteor.update(0.016f, coordinator.engine.context)
            assertTrue("Meteor triggers and starts falling", meteor.hasStartedFalling)
            assertTrue("Velocity in X is negative (falling forward-left)", meteor.velocity.x < 0f)
            assertTrue("Velocity in Y is positive (downward)", meteor.velocity.y > 100f)

            // Advance frames until impact with ground
            repeat(30) {
                if (!meteor.hasImpactedGround) {
                    meteor.update(0.05f, coordinator.engine.context)
                }
            }

            assertTrue("Meteor should impact ground surface", meteor.hasImpactedGround)
            val expectedGroundContact = groundY - meteor.size.y
            assertEquals(expectedGroundContact, meteor.position.y, 0.001f)
            assertEquals(Vec2.ZERO, meteor.velocity)
            assertEquals("impact", meteor.currentAnimationName)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `volcanic meteor ground impact spawns particles into engine particleLayer`() {
        val (audio, _, coordinator) = createContextAndCoordinator()
        try {
            val groundY = coordinator.currentWorld.horizonY
            val targetX = 200f
            val meteor = VolcanicMeteor(
                id = "particle_meteor",
                targetGroundX = targetX,
                groundY = groundY,
                triggerDistance = 150f,
                particleLayer = coordinator.engine.particleLayer
            )

            coordinator.engine.context.distance = 6f
            meteor.update(0.016f, coordinator.engine.context)
            repeat(30) {
                if (!meteor.hasImpactedGround) {
                    meteor.update(0.05f, coordinator.engine.context)
                }
            }

            assertTrue("Meteor should impact ground", meteor.hasImpactedGround)
            assertTrue("Ground blast particles must be spawned in particleLayer", coordinator.engine.particleLayer.activeParticles.isNotEmpty())
        } finally {
            audio.release()
        }
    }

    @Test
    fun `pterodactyl hazards spawn strictly after 500m and maintain 140px safe clearance from cacti`() {
        val earlyCtx = com.pixie.engine.core.context.GameContext().apply { distance = 450f }
        val lateCtx = com.pixie.engine.core.context.GameContext().apply { distance = 650f }
        for (world in listOf(PrehistoricWorldDefinition(1234L), VolcanicCraterWorldDefinition(5678L))) {
            var earlyCount = 0
            var lateCount = 0
            for (idx in 2..20) {
                earlyCount += world.spawnChunkObjects(idx, idx * 320f, 320f, 135f, earlyCtx).count { it is PterodactylHazard }
                val late = world.spawnChunkObjects(idx, idx * 320f, 320f, 135f, lateCtx)
                val pteros = late.filterIsInstance<PterodactylHazard>()
                val cacti = late.filterIsInstance<CactusObstacle>()
                lateCount += pteros.size
                pteros.forEach { p -> cacti.forEach { c -> assertTrue(kotlin.math.abs(p.position.x - c.position.x) >= 140f) } }
            }
            assertEquals(0, earlyCount)
            assertTrue(lateCount > 0)
        }
    }

    @Test
    fun `volcanic meteors spawn strictly after 2000m and maintain 150px clearance from all hazards`() {
        val earlyCtx = com.pixie.engine.core.context.GameContext().apply { distance = 1800f }
        val lateCtx = com.pixie.engine.core.context.GameContext().apply { distance = 2500f }
        for (world in listOf(PrehistoricWorldDefinition(9999L), VolcanicCraterWorldDefinition(8888L))) {
            var earlyCount = 0
            var lateCount = 0
            for (idx in 2..30) {
                earlyCount += world.spawnChunkObjects(idx, idx * 320f, 320f, 135f, earlyCtx).count { it is VolcanicMeteor }
                val late = world.spawnChunkObjects(idx, idx * 320f, 320f, 135f, lateCtx)
                val meteors = late.filterIsInstance<VolcanicMeteor>()
                lateCount += meteors.size
                meteors.forEach { m ->
                    late.filter { it !== m && (it is CactusObstacle || it is PterodactylHazard || it is VolcanicMeteor) }.forEach { h ->
                        val hX = if (h is VolcanicMeteor) h.targetGroundX else h.position.x
                        assertTrue(kotlin.math.abs(m.targetGroundX - hX) >= 150f)
                    }
                }
            }
            assertEquals(0, earlyCount)
            assertTrue(lateCount > 0)
        }
    }

    @Test
    fun `world definitions configure celestial orbit parameters`() {
        val prehistoric = PrehistoricWorldDefinition()
        assertEquals(CelestialTransitMode.CIRCULAR, prehistoric.transitMode)
        assertEquals(Vec2(160f, 140f), prehistoric.celestialCenter)
        assertEquals(145f, prehistoric.celestialRadiusX, 0.001f)
        assertEquals(110f, prehistoric.celestialRadiusY, 0.001f)
        assertEquals(60f, prehistoric.dayNightCycleSeconds, 0.001f)

        val volcanic = VolcanicCraterWorldDefinition()
        assertEquals(CelestialTransitMode.CIRCULAR, volcanic.transitMode)
        assertEquals(Vec2(160f, 155f), volcanic.celestialCenter)
        assertEquals(150f, volcanic.celestialRadiusX, 0.001f)
        assertEquals(65f, volcanic.celestialRadiusY, 0.001f)
        assertEquals(40f, volcanic.dayNightCycleSeconds, 0.001f)
    }

    @Test
    fun `celestial shift triggers audio event on day-to-night and night-to-day transitions`() {
        val (audio, _, coordinator) = createContextAndCoordinator()
        try {
            var shiftCount = 0
            var lastShiftWasDay: Boolean? = null
            val originalShift = coordinator.engine.skyLayer.onCelestialShift
            coordinator.engine.skyLayer.onCelestialShift = { isDay ->
                originalShift?.invoke(isDay)
                shiftCount++
                lastShiftWasDay = isDay
            }

            coordinator.engine.skyLayer.cycleProgress = 0.535f
            coordinator.engine.skyLayer.dayNightCycleSeconds = 10f
            assertTrue(coordinator.engine.skyLayer.isDay)

            // Step across day-to-night threshold (0.54f)
            coordinator.engine.skyLayer.update(0.1f, coordinator.engine.context)
            assertFalse(coordinator.engine.skyLayer.isDay)
            assertEquals(1, shiftCount)
            assertEquals(false, lastShiftWasDay)

            // Move near night-to-day threshold (0.04f)
            coordinator.engine.skyLayer.cycleProgress = 0.035f
            assertFalse(coordinator.engine.skyLayer.isDay)

            // Step across night-to-day threshold
            coordinator.engine.skyLayer.update(0.1f, coordinator.engine.context)
            assertTrue(coordinator.engine.skyLayer.isDay)
            assertEquals(2, shiftCount)
            assertEquals(true, lastShiftWasDay)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `volcanic meteor ground impact plays EXPLOSION_LARGE audio event`() {
        val (audio, _, coordinator) = createContextAndCoordinator()
        try {
            val playedEvents = mutableListOf<AudioEvent>()
            audio.onSoundPlayed = { playedEvents.add(it) }

            val groundY = coordinator.currentWorld.horizonY
            val targetX = 200f
            val meteor = VolcanicMeteor(
                id = "audio_meteor",
                targetGroundX = targetX,
                groundY = groundY,
                triggerDistance = 150f,
                particleLayer = coordinator.engine.particleLayer,
                audioPort = audio
            )

            coordinator.engine.context.distance = 6f
            meteor.update(0.016f, coordinator.engine.context)
            repeat(30) {
                if (!meteor.hasImpactedGround) {
                    meteor.update(0.05f, coordinator.engine.context)
                }
            }

            assertTrue("Meteor should have impacted ground", meteor.hasImpactedGround)
            assertTrue("EXPLOSION_LARGE audio event must be triggered on impact", playedEvents.contains(AudioEvent.EXPLOSION_LARGE))
        } finally {
            audio.release()
        }
    }

    @Test
    fun `volcanic meteor player collision plays EXPLOSION_LARGE audio event`() {
        val (audio, _, coordinator) = createContextAndCoordinator()
        try {
            val playedEvents = mutableListOf<AudioEvent>()
            audio.onSoundPlayed = { playedEvents.add(it) }

            val meteor = VolcanicMeteor(
                id = "collision_meteor",
                targetGroundX = coordinator.player.position.x,
                groundY = coordinator.currentWorld.horizonY
            )
            meteor.position = coordinator.player.position
            coordinator.engine.itemsLayer.addObject(meteor)

            coordinator.checkCustomCollisions()

            assertFalse("Meteor should be deactivated after collision", meteor.isAlive)
            assertTrue("Collision with meteor plays EXPLOSION_LARGE audio event", playedEvents.contains(AudioEvent.EXPLOSION_LARGE))
        } finally {
            audio.release()
        }
    }
}


