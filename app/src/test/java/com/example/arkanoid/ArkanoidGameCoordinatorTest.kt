package com.example.arkanoid

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.arkanoid.entities.ArkanoidLaser
import com.example.arkanoid.entities.EnergyBall
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.engine.core.context.DifficultyPreset
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.matrix.PowerUpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ArkanoidGameCoordinatorTest {

    private fun createContextAndCoordinator(
        preset: DifficultyPreset = DifficultyPreset.MEDIUM
    ): Pair<AndroidSoundPoolAudioAdapter, ArkanoidGameCoordinator> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioAdapter = AndroidSoundPoolAudioAdapter(context)
        val hapticAdapter = AndroidHapticAdapter(context)
        val coordinator = ArkanoidGameCoordinator(
            audioPort = audioAdapter,
            hapticPort = hapticAdapter
        ).apply {
            setDifficulty(preset)
        }
        return Pair(audioAdapter, coordinator)
    }

    @Test
    fun `initial state configures level 1 three lives and docked energy ball`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            val state = coordinator.uiState.value
            assertEquals(1, state.levelNumber)
            assertEquals("Neon Perimeter", state.levelName)
            assertEquals(3, state.lives)
            assertEquals(0, state.score)
            assertTrue(state.isBallDocked)
            assertFalse(state.isGameOver)
            assertFalse(state.isVictory)
            assertFalse(state.isLevelCleared)
            assertTrue(state.remainingBricks > 0)

            assertEquals(1, coordinator.balls.size)
            assertTrue(coordinator.balls.first().isDocked)
            assertEquals(44f, coordinator.paddle.currentWidth, 0.001f)
            assertTrue(coordinator.lasers.isEmpty())
            assertTrue(coordinator.capsules.isEmpty())
        } finally {
            audio.release()
        }
    }

    @Test
    fun `difficulty preset adjusts ball speed and engine context`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            coordinator.setDifficulty(DifficultyPreset.EASY)
            assertEquals(110f, coordinator.baseBallSpeed, 0.001f)
            assertEquals(10, coordinator.engine.context.difficulty)

            coordinator.setDifficulty(DifficultyPreset.MEDIUM)
            assertEquals(135f, coordinator.baseBallSpeed, 0.001f)
            assertEquals(25, coordinator.engine.context.difficulty)

            coordinator.setDifficulty(DifficultyPreset.DIFFICULT)
            assertEquals(160f, coordinator.baseBallSpeed, 0.001f)
            assertEquals(50, coordinator.engine.context.difficulty)

            coordinator.setDifficulty(DifficultyPreset.HEAVY_METAL)
            assertEquals(195f, coordinator.baseBallSpeed, 0.001f)
            assertEquals(100, coordinator.engine.context.difficulty)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `action trigger launches docked ball with upward trajectory`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            assertTrue(coordinator.balls.first().isDocked)
            coordinator.onActionTriggered()

            val ball = coordinator.balls.first()
            assertFalse(ball.isDocked)
            assertTrue("Velocity Y must point upward", ball.velocity.y < -50f)
            assertFalse(coordinator.uiState.value.isBallDocked)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `paddle touch and delta input moves and clamps paddle within arena`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            val arena = coordinator.arenaBounds

            // Extreme left touch clamping
            coordinator.updatePaddleTouch(0f)
            coordinator.paddle.update(0.5f, coordinator.targetPaddleX, arena.left, arena.right)
            val minX = arena.left + coordinator.paddle.currentWidth * 0.5f
            assertEquals(minX, coordinator.paddle.x, 0.5f)

            // Extreme right touch clamping
            coordinator.updatePaddleTouch(1f)
            coordinator.paddle.update(0.5f, coordinator.targetPaddleX, arena.left, arena.right)
            val maxX = arena.right - coordinator.paddle.currentWidth * 0.5f
            assertEquals(maxX, coordinator.paddle.x, 0.5f)

            // Movement delta clamp
            coordinator.movePaddleByDelta(-1000f)
            assertEquals(minX, coordinator.targetPaddleX, 0.001f)
            coordinator.movePaddleByDelta(2000f)
            assertEquals(maxX, coordinator.targetPaddleX, 0.001f)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `paddle deflection angle modulates heading across paddle surface`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            val paddle = coordinator.paddle
            paddle.reset(centerX = 160f)

            // 1. Center hit: deflects straight upward
            val centerBounce = paddle.calculateBounceAngle(160f)
            assertTrue("Center bounce X should be near 0", abs(centerBounce.x) < 0.05f)
            assertTrue("Center bounce Y should be nearly -1.0", centerBounce.y < -0.95f)

            // 2. Left side hit: deflects to upper-left
            val leftBounce = paddle.calculateBounceAngle(160f - paddle.currentWidth * 0.4f)
            assertTrue("Left bounce X should be negative", leftBounce.x < -0.4f)
            assertTrue("Left bounce Y should point up", leftBounce.y < -0.5f)

            // 3. Right side hit: deflects to upper-right
            val rightBounce = paddle.calculateBounceAngle(160f + paddle.currentWidth * 0.4f)
            assertTrue("Right bounce X should be positive", rightBounce.x > 0.4f)
            assertTrue("Right bounce Y should point up", rightBounce.y < -0.5f)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `paddle collision physically bounces downward ball upward and rallies speed`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            val ball = coordinator.balls.first().apply {
                isDocked = false
                speed = 100f
                position = Vec2(coordinator.paddle.x, coordinator.paddle.y - 3f)
                velocity = Vec2(0f, 100f)
            }

            coordinator.update(0.02f)

            assertTrue("Ball velocity Y should reverse upward", ball.velocity.y < 0f)
            assertTrue("Ball speed should increment on paddle rally", ball.speed > 100f)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `powerup activation applies wide paddle, laser, barrier, multiball and plasma`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            val paddle = coordinator.paddle

            // Wide Paddle
            coordinator.applyPowerUp(PowerUpType.WIDE_PADDLE)
            assertTrue(paddle.isWide)
            assertEquals(66f, paddle.currentWidth, 0.001f)
            assertTrue(coordinator.uiState.value.score > 0)

            // Laser Cannons
            coordinator.applyPowerUp(PowerUpType.LASER)
            assertTrue(paddle.isLaser)

            // Safety Barrier
            coordinator.applyPowerUp(PowerUpType.SAFETY_BARRIER)
            assertTrue(paddle.hasBarrier)

            // Multi-ball splits active in-flight balls to 3 balls
            coordinator.balls.first().isDocked = false
            coordinator.applyPowerUp(PowerUpType.MULTI_BALL)
            assertEquals(3, coordinator.balls.size)

            // Plasma transformation
            coordinator.applyPowerUp(PowerUpType.PLASMA_BALL)
            assertTrue(coordinator.balls.all { it.isPlasma })
        } finally {
            audio.release()
        }
    }

    @Test
    fun `laser firing spawns twin lasers and obeys firing cooldown`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            // Cannot fire laser without laser powerup
            assertFalse(coordinator.fireLasers())
            assertTrue(coordinator.lasers.isEmpty())

            // Activate laser mode with valid duration
            coordinator.applyPowerUp(PowerUpType.LASER)
            assertTrue(coordinator.paddle.isLaser)
            val fired = coordinator.fireLasers()
            assertTrue(fired)
            assertEquals(2, coordinator.lasers.size)

            // Cooldown prevents immediate spamming
            assertFalse(coordinator.fireLasers())
            assertEquals(2, coordinator.lasers.size)

            // Update paddle to expire cooldown
            coordinator.paddle.update(0.35f, 160f, 14f, 306f)
            assertTrue(coordinator.fireLasers())
            assertEquals(4, coordinator.lasers.size)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `laser projectiles travel upward and damage matrix brick`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            val targetBrickBounds = coordinator.matrixLayer.getCellBounds(1, 5)
            val laser = ArkanoidLaser(
                position = Vec2(targetBrickBounds.centerX, targetBrickBounds.bottom + 4f),
                speed = 200f
            )
            coordinator.lasers.add(laser)

            val initialScore = coordinator.uiState.value.score
            // Step physics forward so laser impacts brick
            coordinator.update(0.04f)

            assertTrue(coordinator.uiState.value.score > initialScore)
            assertTrue("Laser should be consumed on impact", coordinator.lasers.isEmpty() || !laser.isAlive)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `safety barrier absorbs hazard line impact and saves energy ball`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            val ball = coordinator.balls.first().apply {
                isDocked = false
                position = Vec2(160f, coordinator.arenaBounds.bottom - 1f)
                velocity = Vec2(0f, 80f)
            }
            coordinator.paddle.hasBarrier = true

            coordinator.update(0.02f)

            assertFalse("Barrier should be consumed after saving ball", coordinator.paddle.hasBarrier)
            assertEquals(3, coordinator.uiState.value.lives)
            assertTrue("Ball should be redirected upward", ball.velocity.y < 0f)
            assertTrue(ball.isAlive)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `ball loss without barrier decrements lives and resets docked ball`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            coordinator.balls.first().apply {
                isDocked = false
                position = Vec2(160f, coordinator.arenaBounds.bottom + 5f)
                velocity = Vec2(0f, 100f)
            }
            coordinator.paddle.hasBarrier = false

            coordinator.update(0.02f)

            assertEquals(2, coordinator.uiState.value.lives)
            assertEquals(1, coordinator.balls.size)
            assertTrue("Docked fresh ball should be provided", coordinator.balls.first().isDocked)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `depleting all lives triggers game over state`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            repeat(3) {
                coordinator.balls.first().apply {
                    isDocked = false
                    position = Vec2(160f, coordinator.arenaBounds.bottom + 5f)
                    velocity = Vec2(0f, 100f)
                }
                coordinator.paddle.hasBarrier = false
                coordinator.update(0.02f)
            }

            assertEquals(0, coordinator.uiState.value.lives)
            assertTrue(coordinator.uiState.value.isGameOver)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `level progression advances levels and triggers victory on final level`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            assertEquals(1, coordinator.uiState.value.levelNumber)

            coordinator.nextLevel()
            assertEquals(2, coordinator.uiState.value.levelNumber)
            assertEquals(3, coordinator.uiState.value.lives)

            coordinator.nextLevel()
            assertEquals(3, coordinator.uiState.value.levelNumber)
            assertFalse(coordinator.uiState.value.isVictory)

            // Completing final level triggers victory
            coordinator.nextLevel()
            assertTrue(coordinator.uiState.value.isVictory)

            // Restart game resets state cleanly
            coordinator.restartGame()
            assertEquals(1, coordinator.uiState.value.levelNumber)
            assertEquals(3, coordinator.uiState.value.lives)
            assertEquals(0, coordinator.uiState.value.score)
            assertFalse(coordinator.uiState.value.isVictory)
            assertFalse(coordinator.uiState.value.isGameOver)
        } finally {
            audio.release()
        }
    }

    @Test
    fun `toggle pause pauses gameplay updates`() {
        val (audio, coordinator) = createContextAndCoordinator()
        try {
            assertFalse(coordinator.uiState.value.isPaused)
            coordinator.togglePause()
            assertTrue(coordinator.uiState.value.isPaused)

            val ball = coordinator.balls.first().apply {
                isDocked = false
                position = Vec2(160f, 100f)
                velocity = Vec2(50f, 50f)
            }
            coordinator.update(0.1f)
            assertEquals(160f, ball.position.x, 0.001f)
            assertEquals(100f, ball.position.y, 0.001f)

            coordinator.togglePause()
            assertFalse(coordinator.uiState.value.isPaused)
        } finally {
            audio.release()
        }
    }
}
