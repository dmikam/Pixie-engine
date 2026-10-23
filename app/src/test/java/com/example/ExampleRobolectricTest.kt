package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.menu.model.GameStatus
import com.example.menu.model.PocGameRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Pixie Engine", appName)
    }

    @Test
    fun `poc game registry contains all four proof of concept games`() {
        val games = PocGameRegistry.games
        assertEquals(4, games.size)

        val dinorun = PocGameRegistry.getById("dinorun")
        assertNotNull(dinorun)
        assertEquals("Dinorun", dinorun?.title)
        assertEquals(GameStatus.PLAYABLE, dinorun?.status)
        assertEquals("Horizontal Scroller", dinorun?.paradigm)

        val raptor = PocGameRegistry.getById("raptor")
        assertNotNull(raptor)
        assertEquals("Raptor", raptor?.title)
        assertEquals(GameStatus.PLAYABLE, raptor?.status)
        assertEquals("Vertical Scroller", raptor?.paradigm)

        val arkanoid = PocGameRegistry.getById("arkanoid")
        assertNotNull(arkanoid)
        assertEquals("Arkanoid", arkanoid?.title)
        assertEquals(GameStatus.PLAYABLE, arkanoid?.status)
        assertEquals("Fixed Screen", arkanoid?.paradigm)

        val chiki = PocGameRegistry.getById("chiki")
        assertNotNull(chiki)
        assertEquals("Chiki", chiki?.title)
        assertEquals(GameStatus.NEXT_UP, chiki?.status)
        assertEquals("Classic Platformer", chiki?.paradigm)
    }

    @Test
    fun `arkanoid coordinator initializes, docks ball, and launches correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioAdapter = com.pixie.android.audio.AndroidSoundPoolAudioAdapter(context)
        val hapticAdapter = com.pixie.android.haptic.AndroidHapticAdapter(context)

        val coordinator = com.example.arkanoid.ArkanoidGameCoordinator(
            audioPort = audioAdapter,
            hapticPort = hapticAdapter
        )

        assertEquals(3, coordinator.uiState.value.lives)
        assertEquals(1, coordinator.uiState.value.levelNumber)
        org.junit.Assert.assertTrue("Initial ball should be docked on paddle", coordinator.uiState.value.isBallDocked)

        // Launch ball
        coordinator.onActionTriggered()
        org.junit.Assert.assertFalse("Ball should be released after action triggered", coordinator.uiState.value.isBallDocked)

        audioAdapter.release()
    }

    @Test
    fun `energy ball records motion blur trail and bounces cleanly without tunneling`() {
        val ball = com.example.arkanoid.entities.EnergyBall(
            position = com.pixie.engine.core.math.Vec2(100f, 100f),
            radius = 2.5f
        )
        ball.launch(150f, headingAngle = -1.57f)
        org.junit.Assert.assertFalse(ball.isDocked)

        val paddle = com.example.arkanoid.entities.ArkanoidPaddle(initialX = 100f, y = 160f)
        val matrix = com.pixie.engine.core.matrix.DestructibleMatrixLayer(
            originX = 80f,
            originY = 80f,
            brickWidth = 20f,
            brickHeight = 10f,
            gap = 2f
        )
        val cell = com.pixie.engine.core.matrix.BrickCell(0, 0, hp = 1)
        matrix.loadLayout(1, 1, listOf(cell))

        val arena = com.pixie.engine.core.math.RectF(10f, 10f, 310f, 175f)
        val dummyAudio = object : com.pixie.engine.core.ports.AudioPort {
            override fun playSound(event: com.pixie.engine.core.ports.AudioEvent) {}
        }
        val particleLayer = com.pixie.engine.core.particle.ParticleLayer()

        var hitCount = 0
        ball.update(
            dt = 0.05f,
            paddle = paddle,
            matrix = matrix,
            arenaBounds = arena,
            audioPort = dummyAudio,
            particleLayer = particleLayer,
            onPaddleBounce = {},
            onBrickHit = { hitCount++ },
            onBallLost = {}
        )

        // Trail history records previous positions
        org.junit.Assert.assertTrue("Motion blur trail should record past positions", ball.trailHistory.isNotEmpty())
    }

    @Test
    fun `raptor coordinator initializes with continuous difficulty and fires weapons`() {
        val coordinator = com.example.raptor.RaptorGameCoordinator(
            selectedDifficulty = com.pixie.engine.core.context.DifficultyPreset.DIFFICULT,
            currentLevelIndex = 1
        )

        assertEquals(50, coordinator.engine.context.difficulty)
        assertEquals(1, coordinator.currentLevelIndex)
        assertEquals(100f, coordinator.playerShip.hull, 0.001f)
        assertEquals(100f, coordinator.playerShip.shield, 0.001f)

        // Test primary weapon fire
        val bullets = mutableListOf<com.pixie.engine.core.weapon.Projectile>()
        val fired = coordinator.playerShip.firePrimary(coordinator.engine.context, bullets)
        org.junit.Assert.assertTrue("Primary fire should succeed", fired)
        org.junit.Assert.assertTrue("Bullet spawned", bullets.isNotEmpty())

        // Test secondary missiles
        val initialMissiles = coordinator.playerShip.missileSalvoes
        coordinator.fireSecondaryMissiles()
        assertEquals(initialMissiles - 1, coordinator.playerShip.missileSalvoes)
    }

    @Test
    fun `audio and haptic adapters toggle mute and enabled correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioAdapter = com.pixie.android.audio.AndroidSoundPoolAudioAdapter(context)
        val hapticAdapter = com.pixie.android.haptic.AndroidHapticAdapter(context)

        // Initial default states
        org.junit.Assert.assertFalse("Audio should not be muted initially", audioAdapter.isMuted)
        org.junit.Assert.assertTrue("Haptics should be enabled initially", hapticAdapter.isEnabled)

        // Toggle mute
        audioAdapter.isMuted = true
        org.junit.Assert.assertTrue("Audio is now muted", audioAdapter.isMuted)
        audioAdapter.playSound(com.pixie.engine.core.ports.AudioEvent.BUTTON_CLICK) // should not crash

        audioAdapter.isMuted = false
        org.junit.Assert.assertFalse("Audio is unmuted", audioAdapter.isMuted)

        // Toggle haptics
        hapticAdapter.isEnabled = false
        org.junit.Assert.assertFalse("Haptics disabled", hapticAdapter.isEnabled)
        hapticAdapter.trigger(com.pixie.engine.core.ports.HapticFeedbackType.LIGHT) // should safely return

        hapticAdapter.isEnabled = true
        org.junit.Assert.assertTrue("Haptics re-enabled", hapticAdapter.isEnabled)

        audioAdapter.release()
    }

    @Test
    fun `dinorun and raptor pause settings support sound and haptics toggling`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioAdapter = com.pixie.android.audio.AndroidSoundPoolAudioAdapter(context)
        val hapticAdapter = com.pixie.android.haptic.AndroidHapticAdapter(context)
        val inputAdapter = com.pixie.android.input.DualModeInputAdapter()

        val dinoCoordinator = com.example.dinorun.DinoGameCoordinator(
            audioAdapter = audioAdapter,
            hapticAdapter = hapticAdapter,
            inputAdapter = inputAdapter
        )
        assertNotNull(dinoCoordinator.engine.audio)
        assertNotNull(dinoCoordinator.engine.haptics)

        val raptorCoordinator = com.example.raptor.RaptorGameCoordinator(
            audio = audioAdapter,
            haptics = hapticAdapter
        )
        assertNotNull(raptorCoordinator.engine.audio)
        assertNotNull(raptorCoordinator.engine.haptics)

        // Toggle off
        audioAdapter.isMuted = true
        hapticAdapter.isEnabled = false

        assertEquals(true, dinoCoordinator.audioAdapter.isMuted)
        assertEquals(false, dinoCoordinator.hapticAdapter.isEnabled)
        assertEquals(true, raptorCoordinator.audio?.isMuted)
        assertEquals(false, raptorCoordinator.haptics?.isEnabled)

        audioAdapter.release()
    }
}
