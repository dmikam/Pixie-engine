package com.example.dinorun

import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.android.input.DualModeInputAdapter
import com.pixie.android.render.AndroidCanvasRenderer
import com.pixie.engine.core.PixieEngine
import com.pixie.engine.core.camera.GameViewMode
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.HapticFeedbackType
import com.pixie.engine.core.ports.VirtualResolution
import java.util.Random

/**
 * Coordinates the Dinorun proof-of-concept game on top of the Pixie Engine.
 */
class DinoGameCoordinator(
    val audioAdapter: AndroidSoundPoolAudioAdapter,
    val hapticAdapter: AndroidHapticAdapter,
    val inputAdapter: DualModeInputAdapter
) {
    val resolution = VirtualResolution(320, 180)
    val renderer = AndroidCanvasRenderer(resolution = resolution, pixelsGap = 0)

    val engine = PixieEngine(
        virtualResolution = resolution,
        viewMode = GameViewMode.HorizontalScroller(lookAheadX = 65f, lockY = true, fixedY = 0f),
        pixelsGap = 0,
        audio = audioAdapter,
        haptics = hapticAdapter,
        input = inputAdapter
    )

    private val random = Random(42)
    lateinit var player: DinoPlayer

    var onStateChanged: (() -> Unit)? = null

    init {
        setupGame()
    }

    fun setupGame() {
        val groundY = engine.horizon.horizonY

        // Create player
        player = DinoPlayer(
            initialPosition = Vec2(40f, groundY - 16f),
            groundY = groundY
        )
        engine.player = player
        engine.itemsLayer.addObject(player)

        // Setup procedural chunk generation
        engine.chunkManager.chunkSpawner = { chunkIndex, startX, width, ctx ->
            generateChunkObjects(chunkIndex, startX, width, groundY, ctx)
        }

        // Initialize first chunks
        engine.chunkManager.reset(0f)
    }

    private fun generateChunkObjects(
        chunkIndex: Int,
        startX: Float,
        width: Float,
        groundY: Float,
        ctx: com.pixie.engine.core.context.GameContext
    ): List<PixieObject> {
        val items = mutableListOf<PixieObject>()
        if (chunkIndex <= 1) return items // Leave starting zone clear

        val r = random
        val density = ctx.calculateDifficultyMultiplier(1.0f, 0.05f)

        // Ground Cactus Obstacle
        val cactusCount = if (density > 1.4f && r.nextBoolean()) 2 else 1
        var obstacleX = startX + 60f + r.nextFloat() * 80f
        for (i in 0 until cactusCount) {
            val isDouble = r.nextBoolean()
            val cactus = CactusObstacle(
                id = "cactus_${chunkIndex}_$i",
                position = Vec2(obstacleX, groundY - 18f),
                isDouble = isDouble
            )
            items.add(cactus)
            obstacleX += 130f
        }

        // Flying Pterodactyl (at low altitude to duck under, or high to run under)
        if (chunkIndex > 2 && r.nextFloat() < 0.65f) {
            val isLow = r.nextBoolean()
            val pteroY = if (isLow) groundY - 24f else groundY - 45f
            val ptero = PterodactylHazard(
                id = "ptero_$chunkIndex",
                position = Vec2(startX + 180f + r.nextFloat() * 60f, pteroY),
                speed = 40f
            )
            items.add(ptero)
        }

        // Color Energy Sparks (Pickups that restore color)
        val sparkCount = if (r.nextFloat() < 0.8f) 2 else 1
        for (i in 0 until sparkCount) {
            val sparkY = groundY - 25f - r.nextFloat() * 25f
            val spark = ColorSparkPickup(
                id = "spark_${chunkIndex}_$i",
                position = Vec2(startX + 40f + i * 90f, sparkY)
            )
            items.add(spark)
        }

        // Rare falling Volcanic Meteor
        if (chunkIndex > 3 && r.nextFloat() < 0.35f) {
            val meteor = VolcanicMeteor(
                id = "meteor_$chunkIndex",
                position = Vec2(startX + 220f, -20f)
            )
            items.add(meteor)
        }

        return items
    }

    fun updateInputs() {
        if (inputAdapter.isJumpPressed) {
            player.jump()
        }
        player.setDuckingState(inputAdapter.isDuckPressed)
    }

    fun checkCustomCollisions() {
        if (engine.context.isGameOver) return

        val playerBox = player.boundingBox
        val items = engine.itemsLayer.objects

        for (i in items.indices) {
            val obj = items[i]
            if (!obj.isAlive || obj === player) continue

            if (playerBox.intersects(obj.boundingBox)) {
                if (obj.isInClass("hazard")) {
                    // Player hit obstacle
                    hapticAdapter.trigger(HapticFeedbackType.HEAVY)
                    audioAdapter.playSound(AudioEvent.HIT)
                    engine.context.livesLeft--
                    obj.isAlive = false

                    if (engine.context.livesLeft <= 0) {
                        engine.context.isGameOver = true
                        audioAdapter.playSound(AudioEvent.GAME_OVER)
                    }
                    onStateChanged?.invoke()
                } else if (obj.isInClass("pickup")) {
                    // Collected color energy spark!
                    hapticAdapter.trigger(HapticFeedbackType.LIGHT)
                    audioAdapter.playSound(AudioEvent.COLLECT)
                    engine.colorRecovery.addProgress(0.12f)
                    engine.context.score += 50L
                    obj.isAlive = false
                    onStateChanged?.invoke()
                }
            }
        }

        // Update distance & continuous score
        engine.context.distance = player.position.x / 10f
        engine.context.score = (engine.context.distance * 2f).toLong() + (engine.colorRecovery.progress * 500f).toLong()
    }

    fun restart() {
        engine.resetGame()
        setupGame()
        onStateChanged?.invoke()
    }

    fun setPixelGap(gap: Int) {
        engine.pixelsGap = gap
        renderer.pixelsGap = gap
    }
}
