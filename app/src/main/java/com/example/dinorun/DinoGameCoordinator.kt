package com.example.dinorun

import com.example.dinorun.entities.DinoPlayer
import com.example.dinorun.world.PrehistoricWorldDefinition
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.android.input.DualModeInputAdapter
import com.pixie.android.render.AndroidCanvasRenderer
import com.pixie.engine.core.PixieEngine
import com.pixie.engine.core.camera.GameViewMode
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.HapticFeedbackType
import com.pixie.engine.core.ports.VirtualResolution
import com.pixie.engine.core.world.WorldDefinition

/**
 * Coordinates the Dinorun proof-of-concept game on top of the Pixie Engine,
 * driven by modular [WorldDefinition] configurations and self-encapsulated entities.
 */
class DinoGameCoordinator(
    val audioAdapter: AndroidSoundPoolAudioAdapter,
    val hapticAdapter: AndroidHapticAdapter,
    val inputAdapter: DualModeInputAdapter,
    var currentWorld: WorldDefinition = PrehistoricWorldDefinition()
) {
    val resolution = VirtualResolution(360, 200)
    val renderer = AndroidCanvasRenderer(resolution = resolution, pixelsGap = 1)

    val engine = PixieEngine(
        virtualResolution = resolution,
        viewMode = GameViewMode.HorizontalScroller(lookAheadX = 65f, lockY = true, fixedY = 0f),
        pixelsGap = 1,
        audio = audioAdapter,
        haptics = hapticAdapter,
        input = inputAdapter
    )

    lateinit var player: DinoPlayer
    var onStateChanged: (() -> Unit)? = null

    init {
        applyWorldConfiguration(currentWorld)
        setupGame()
    }

    fun applyWorldConfiguration(world: WorldDefinition) {
        currentWorld = world
        engine.horizon.horizonY = world.horizonY
        engine.skyLayer.dayNightCycleSeconds = world.dayNightCycleSeconds
        engine.cloudLayer.windVelocity = world.windVelocity

        // Wire chunk procedural generation to the world definition
        engine.chunkManager.chunkSpawner = { chunkIndex, startX, width, ctx ->
            world.spawnChunkObjects(chunkIndex, startX, width, world.horizonY, ctx)
        }
    }

    fun setupGame() {
        val groundY = currentWorld.horizonY

        // Create player from self-encapsulated entity
        player = DinoPlayer(
            initialPosition = Vec2(40f, groundY - 16f),
            groundY = groundY
        )
        engine.player = player
        engine.itemsLayer.addObject(player)

        // Initialize world chunk generator
        engine.chunkManager.reset(0f)
    }

    private var lastMilestoneHundred: Int = 0

    fun updateInputs() {
        if (inputAdapter.isJumpPressed) {
            val wasGrounded = player.isGrounded
            player.jump()
            if (wasGrounded && !player.isGrounded) {
                audioAdapter.playSound(AudioEvent.JUMP)
            }
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
                    // Collected color energy spark
                    hapticAdapter.trigger(HapticFeedbackType.LIGHT)
                    audioAdapter.playSound(AudioEvent.COLLECT)
                    engine.colorRecovery.addProgress(0.15f)
                    engine.context.score += 50L
                    obj.isAlive = false
                    onStateChanged?.invoke()
                }
            }
        }

        // Distance and continuous score accumulation
        engine.context.distance = player.position.x / 10f
        engine.context.score = (engine.context.distance * 2f).toLong() + (engine.colorRecovery.progress * 500f).toLong()

        val hundred = (engine.context.distance / 100f).toInt()
        if (hundred > lastMilestoneHundred) {
            lastMilestoneHundred = hundred
            audioAdapter.playSound(AudioEvent.SCORE_MILESTONE)
        }
    }

    fun switchWorld(world: WorldDefinition) {
        restart(world)
    }

    fun restart(world: WorldDefinition = currentWorld) {
        engine.resetGame()
        applyWorldConfiguration(world)
        setupGame()
        onStateChanged?.invoke()
    }

    fun setPixelGap(gap: Int) {
        engine.pixelsGap = gap
        renderer.pixelsGap = gap
    }
}
