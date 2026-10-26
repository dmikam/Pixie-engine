package com.example.arkanoid

import com.example.arkanoid.entities.ArkanoidLaser
import com.example.arkanoid.entities.ArkanoidPaddle
import com.example.arkanoid.entities.EnergyBall
import com.example.arkanoid.entities.PowerCapsule
import com.example.arkanoid.world.ArkanoidLevelCatalog
import com.example.arkanoid.world.ArkanoidPowerupCatalog
import com.pixie.engine.core.PixieEngine
import com.pixie.engine.core.camera.GameViewMode
import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.DifficultyPreset
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.layer.PixieLayer
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.matrix.DestructibleMatrixLayer
import com.pixie.engine.core.matrix.PowerUpType
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.ports.HapticFeedbackType
import com.pixie.engine.core.ports.HapticPort
import com.pixie.engine.core.ports.RenderBatchPort
import com.pixie.engine.core.ports.VirtualResolution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

import com.pixie.engine.core.ports.InputPort

data class ArkanoidUiState(
    val score: Int = 0,
    val highScore: Int = 0,
    val lives: Int = 3,
    val levelNumber: Int = 1,
    val levelName: String = "Neon Perimeter",
    val combo: Int = 0,
    val comboMultiplier: Int = 1,
    val remainingBricks: Int = 0,
    val activePowerUp: String? = null,
    val isBallDocked: Boolean = true,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val isVictory: Boolean = false,
    val isLevelCleared: Boolean = false
)

class ArkanoidGameCoordinator(
    private val audioPort: AudioPort,
    private val hapticPort: HapticPort,
    val inputPort: InputPort? = null
) {
    val engine: PixieEngine = PixieEngine(
        virtualResolution = VirtualResolution(320, 180),
        viewMode = GameViewMode.FixedScreen(Vec2.ZERO)
    )

    val arenaBounds = RectF(14f, 12f, 306f, 175f)

    private val matrixLayer = DestructibleMatrixLayer(
        originX = 18f,
        originY = 22f,
        brickWidth = 22f,
        brickHeight = 9f,
        gap = 2f
    )

    val paddle = ArkanoidPaddle(initialX = 160f, y = 162f)
    private val balls = mutableListOf<EnergyBall>()
    private val capsules = mutableListOf<PowerCapsule>()
    private val lasers = mutableListOf<ArkanoidLaser>()

    private var targetPaddleX: Float = 160f
    private var baseBallSpeed: Float = 135f

    private var currentLevelIndex: Int = 1
    private var score: Int = 0
    private var highScore: Int = 0
    private var lives: Int = 3
    private var combo: Int = 0

    private var isGameOver: Boolean = false
    private var isVictory: Boolean = false
    private var isLevelCleared: Boolean = false
    private var isPaused: Boolean = false
    private var wasActionPressedLastFrame: Boolean = false

    private val _uiState = MutableStateFlow(ArkanoidUiState())
    val uiState: StateFlow<ArkanoidUiState> = _uiState.asStateFlow()

    init {
        // Clear background scroller layers from default engine
        engine.skyLayer.isEnabled = false
        engine.cloudLayer.isEnabled = false
        engine.backgroundLayer.isEnabled = false
        engine.groundLayer.isEnabled = false
        engine.itemsLayer.isEnabled = false
        engine.foregroundLayer.isEnabled = false

        // 1. Register arena background & borders layer (order = 50)
        val arenaBackgroundLayer = object : PixieLayer {
            override val name: String = "Layer_ArkanoidBackground"
            override val order: Int = 50
            override var isEnabled: Boolean = true
            override var isGridEnabled: Boolean = false

            override fun update(dt: Float, context: GameContext) {}

            override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
                renderArena(batch, camera)
            }
        }
        engine.layerStack.addLayer(arenaBackgroundLayer)

        // 2. Register destructible matrix layer (order = 450)
        engine.layerStack.addLayer(matrixLayer)

        // 3. Register custom gamefield layer for paddle, balls, capsules, lasers (order = 500)
        val arkanoidPlayfieldLayer = object : PixieLayer {
            override val name: String = "Layer_ArkanoidPlayfield"
            override val order: Int = 500
            override var isEnabled: Boolean = true
            override var isGridEnabled: Boolean = false

            override fun update(dt: Float, context: GameContext) {}

            override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
                paddle.render(batch, camera)
                for (b in balls) b.render(batch, camera)
                for (c in capsules) c.render(batch, camera)
                for (l in lasers) l.render(batch, camera)
            }
        }
        engine.layerStack.addLayer(arkanoidPlayfieldLayer)

        loadLevel(1)
    }

    fun setDifficulty(preset: DifficultyPreset) {
        engine.context.difficulty = preset.difficultyValue
        baseBallSpeed = when (preset) {
            DifficultyPreset.EASY -> 110f
            DifficultyPreset.MEDIUM -> 135f
            DifficultyPreset.DIFFICULT -> 160f
            DifficultyPreset.HEAVY_METAL -> 195f
            else -> 135f
        }
    }

    fun loadLevel(levelIndex: Int) {
        currentLevelIndex = levelIndex
        isLevelCleared = false
        balls.clear()
        capsules.clear()
        lasers.clear()
        paddle.reset(centerX = 160f)
        targetPaddleX = 160f
        combo = 0

        val level = ArkanoidLevelCatalog.getLevel(levelIndex)
        matrixLayer.loadLayout(level.rows, level.cols, level.bricks)

        // Spawn docked ball
        val initialBall = EnergyBall(
            position = Vec2(paddle.x, paddle.y - 4f),
            radius = 2.5f
        ).apply {
            isDocked = true
            dockOffset = 0f
            speed = baseBallSpeed
        }
        balls.add(initialBall)

        updateUiState()
    }

    fun restartGame() {
        lives = 3
        score = 0
        combo = 0
        isGameOver = false
        isVictory = false
        isPaused = false
        loadLevel(1)
    }

    fun nextLevel() {
        if (currentLevelIndex < 3) {
            loadLevel(currentLevelIndex + 1)
        } else {
            isVictory = true
            audioPort.playSound(AudioEvent.SCORE_MILESTONE)
            updateUiState()
        }
    }

    fun togglePause() {
        isPaused = !isPaused
        updateUiState()
    }

    fun updatePaddleTouch(relativeScreenX: Float) {
        val clamped = relativeScreenX.coerceIn(0f, 1f)
        targetPaddleX = arenaBounds.left + paddle.currentWidth * 0.5f +
                clamped * (arenaBounds.width - paddle.currentWidth)
    }

    fun movePaddleByDelta(deltaX: Float) {
        targetPaddleX = (targetPaddleX + deltaX).coerceIn(
            arenaBounds.left + paddle.currentWidth * 0.5f,
            arenaBounds.right - paddle.currentWidth * 0.5f
        )
    }

    fun fireLasers(): Boolean {
        if (isGameOver || isVictory || isPaused) return false
        if (paddle.consumeLaserFire()) {
            val halfW = paddle.currentWidth * 0.5f
            lasers.add(ArkanoidLaser(Vec2(paddle.x - halfW + 1f, paddle.y - 3f)))
            lasers.add(ArkanoidLaser(Vec2(paddle.x + halfW - 1f, paddle.y - 3f)))
            audioPort.playSound(AudioEvent.FIRE_PRIMARY)
            hapticPort.trigger(HapticFeedbackType.LIGHT)
            return true
        }
        return false
    }

    fun onActionTriggered() {
        if (isGameOver || isVictory || isPaused) return

        // 1. If any ball is docked, launch it!
        var launched = false
        for (b in balls) {
            if (b.isDocked) {
                b.launch(baseBallSpeed, headingAngle = -1.57f)
                audioPort.playSound(AudioEvent.JUMP)
                hapticPort.trigger(HapticFeedbackType.LIGHT)
                launched = true
            }
        }

        // 2. If laser active, fire twin lasers
        if (!launched) {
            fireLasers()
        }

        updateUiState()
    }

    fun update(dt: Float) {
        if (isPaused || isGameOver || isVictory || isLevelCleared) return

        // 0. Continuous movement & immediate action from InputPort
        inputPort?.let { input ->
            if (input.moveX != 0f) {
                val paddleSpeed = 220f
                targetPaddleX = (targetPaddleX + input.moveX * paddleSpeed * dt).coerceIn(
                    arenaBounds.left + paddle.currentWidth * 0.5f,
                    arenaBounds.right - paddle.currentWidth * 0.5f
                )
            }
            if (input.isActionPressed) {
                if (!wasActionPressedLastFrame) {
                    onActionTriggered()
                } else if (paddle.isLaser) {
                    fireLasers()
                }
            }
            wasActionPressedLastFrame = input.isActionPressed
        }

        // 1. Update Paddle
        paddle.update(dt, targetPaddleX, arenaBounds.left, arenaBounds.right)

        // 2. Update Balls
        val activeBalls = balls.filter { it.isAlive }
        for (ball in activeBalls) {
            ball.update(
                dt = dt,
                paddle = paddle,
                matrix = matrixLayer,
                arenaBounds = arenaBounds,
                audioPort = audioPort,
                particleLayer = engine.particleLayer,
                onPaddleBounce = {
                    combo = 0 // Reset combo streak on paddle touch
                    hapticPort.trigger(HapticFeedbackType.LIGHT)
                    updateUiState()
                },
                onBrickHit = { result ->
                    combo++
                    val multiplier = 1 + (combo / 4).coerceAtMost(4)
                    score += result.scoreAwarded * multiplier
                    highScore = max(highScore, score)
                    hapticPort.trigger(HapticFeedbackType.MEDIUM)

                    // Check for power-up drops
                    result.droppedPowerUp?.let { pType ->
                        capsules.add(PowerCapsule(position = result.hitPosition, type = pType))
                    }

                    // Check if level cleared
                    if (matrixLayer.remainingDestructibles() <= 0) {
                        isLevelCleared = true
                        audioPort.playSound(AudioEvent.SCORE_MILESTONE)
                    }
                    updateUiState()
                },
                onBallLost = { lostBall ->
                    hapticPort.trigger(HapticFeedbackType.HEAVY)
                    checkBallLoss()
                }
            )
        }
        balls.removeAll { !it.isAlive }

        // 3. Update Power Capsules
        for (c in capsules) {
            c.update(dt, paddle) { pType ->
                applyPowerUp(pType)
            }
        }
        capsules.removeAll { !it.isAlive }

        // 4. Update Lasers
        for (l in lasers) {
            l.update(dt, matrixLayer, audioPort, engine.particleLayer) { result ->
                combo++
                score += result.scoreAwarded
                highScore = max(highScore, score)
                result.droppedPowerUp?.let { pType ->
                    capsules.add(PowerCapsule(position = result.hitPosition, type = pType))
                }
                if (matrixLayer.remainingDestructibles() <= 0) {
                    isLevelCleared = true
                    audioPort.playSound(AudioEvent.SCORE_MILESTONE)
                }
                updateUiState()
            }
        }
        lasers.removeAll { !it.isAlive }

        // 5. Update engine particles & context
        engine.particleLayer.update(dt, engine.context)
    }

    private fun checkBallLoss() {
        if (balls.none { it.isAlive }) {
            lives--
            combo = 0
            audioPort.playSound(AudioEvent.DUCK)
            if (lives <= 0) {
                isGameOver = true
                audioPort.playSound(AudioEvent.GAME_OVER)
            } else {
                // Dock fresh ball on paddle
                paddle.reset(centerX = 160f)
                targetPaddleX = 160f
                balls.clear()
                balls.add(
                    EnergyBall(
                        position = Vec2(paddle.x, paddle.y - 4f),
                        radius = 2.5f
                    ).apply {
                        isDocked = true
                        dockOffset = 0f
                        speed = baseBallSpeed
                    }
                )
            }
            updateUiState()
        }
    }

    private fun applyPowerUp(type: PowerUpType) {
        audioPort.playSound(AudioEvent.POWERUP_PICKUP)
        hapticPort.trigger(HapticFeedbackType.MEDIUM)
        score += ArkanoidPowerupCatalog.getBonusScore(type)
        highScore = max(highScore, score)

        ArkanoidPowerupCatalog.applyEffect(
            type = type,
            paddle = paddle,
            balls = balls,
            baseBallSpeed = baseBallSpeed
        )
        updateUiState()
    }

    private fun renderArena(batch: RenderBatchPort, camera: PixieCamera) {
        val darkBg = 0xFF0D1117.toInt()
        batch.fillBackground(darkBg)

        // Draw Arena Enclosing Border (Left, Top, Right)
        val borderColor = 0xFF00E5FF.toInt()
        val borderHighlight = 0xFFFFFFFF.toInt()

        // Left Border
        batch.drawPixelRect(arenaBounds.left - 4f, arenaBounds.top - 4f, 4f, arenaBounds.height + 4f, borderColor)
        batch.drawPixelRect(arenaBounds.left - 4f, arenaBounds.top - 4f, 1f, arenaBounds.height + 4f, borderHighlight)

        // Right Border
        batch.drawPixelRect(arenaBounds.right, arenaBounds.top - 4f, 4f, arenaBounds.height + 4f, borderColor)
        batch.drawPixelRect(arenaBounds.right + 3f, arenaBounds.top - 4f, 1f, arenaBounds.height + 4f, borderHighlight)

        // Top Border
        batch.drawPixelRect(arenaBounds.left - 4f, arenaBounds.top - 4f, arenaBounds.width + 8f, 4f, borderColor)
        batch.drawPixelRect(arenaBounds.left - 4f, arenaBounds.top - 4f, arenaBounds.width + 8f, 1f, borderHighlight)

        // Bottom Hazard Grid (Red / Crimson warning stripes)
        val bottomY = arenaBounds.bottom
        batch.drawPixelRect(arenaBounds.left, bottomY + 1f, arenaBounds.width, 2f, 0xFFFF1744.toInt())
        for (gx in arenaBounds.left.toInt()..arenaBounds.right.toInt() step 12) {
            batch.drawPixelRect(gx.toFloat(), bottomY + 1f, 4f, 2f, 0xFFFFD600.toInt())
        }
    }

    private fun updateUiState() {
        val level = ArkanoidLevelCatalog.getLevel(currentLevelIndex)
        val activePill = ArkanoidPowerupCatalog.formatActiveStatus(paddle, balls)

        _uiState.value = ArkanoidUiState(
            score = score,
            highScore = highScore,
            lives = lives,
            levelNumber = currentLevelIndex,
            levelName = level.name,
            combo = combo,
            comboMultiplier = 1 + (combo / 4).coerceAtMost(4),
            remainingBricks = matrixLayer.remainingDestructibles(),
            activePowerUp = activePill,
            isBallDocked = balls.any { it.isDocked },
            isPaused = isPaused,
            isGameOver = isGameOver,
            isVictory = isVictory,
            isLevelCleared = isLevelCleared
        )
    }
}
