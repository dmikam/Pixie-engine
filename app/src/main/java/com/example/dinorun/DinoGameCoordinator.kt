package com.example.dinorun

import com.example.dinorun.entities.DinoPlayer
import com.example.dinorun.entities.VolcanicMeteor
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
import com.pixie.engine.core.entity.PixieCollectible
import com.pixie.engine.core.entity.PixieEnemy
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.entity.PixieObstacle
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.particle.ProceduralExplosion
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
        engine.skyLayer.apply {
            dayNightCycleSeconds = world.dayNightCycleSeconds
            transitMode = world.transitMode
            celestialCenter = world.celestialCenter
            celestialRadiusX = world.celestialRadiusX
            celestialRadiusY = world.celestialRadiusY
            onCelestialShift = { _ ->
                audioAdapter.playSound(AudioEvent.DAY_NIGHT_SHIFT)
            }
        }
        engine.cloudLayer.windVelocity = world.windVelocity

        // Wire chunk procedural generation to the world definition
        engine.chunkManager.chunkSpawner = { chunkIndex, startX, width, ctx ->
            val chunkObjects = world.spawnChunkObjects(chunkIndex, startX, width, world.horizonY, ctx)
            for (obj in chunkObjects) {
                if (obj is VolcanicMeteor) {
                    obj.particleLayer = engine.particleLayer
                    obj.audioPort = audioAdapter
                }
            }
            chunkObjects
        }
    }

    fun setupGame() {
        lastMilestoneHundred = 0
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
        val wasDucking = player.isDucking
        player.setDuckingState(inputAdapter.isDuckPressed)
        if (!wasDucking && player.isDucking) {
            audioAdapter.playSound(AudioEvent.DUCK)
        }
    }

    fun checkCustomCollisions() {
        if (engine.context.isGameOver) return

        val playerBox = player.boundingBox
        val items = engine.itemsLayer.objects

        for (i in items.indices) {
            val obj = items[i]
            if (!obj.isAlive || obj === player) continue

            if (playerBox.intersects(obj.boundingBox)) {
                if (obj is PixieCollectible) {
                    handleCollectibleCollision(obj)
                } else if (obj is PixieObstacle || obj is PixieEnemy || obj.isInClass("hazard")) {
                    handleHazardCollision(obj)
                }
            }
        }

        updateDistanceAndScore()
    }

    private fun handleCollectibleCollision(collectible: PixieCollectible) {
        val collected = collectible.collect(player, engine.context)
        if (collected) {
            ProceduralExplosion.trigger(
                type = ExplosionType.SPARK_HIT,
                position = collectible.position,
                pool = engine.particleLayer.pool,
                activeList = engine.particleLayer.activeParticles,
                scale = 1.2f
            )
            hapticAdapter.trigger(HapticFeedbackType.LIGHT)
            val wasFull = engine.colorRecovery.progress >= 1.0f
            engine.colorRecovery.addProgress(0.15f)
            if (!wasFull && engine.colorRecovery.progress >= 1.0f) {
                audioAdapter.playSound(AudioEvent.SCORE_MILESTONE)
            } else {
                audioAdapter.playSound(AudioEvent.COLLECT)
            }
            engine.context.score += 50L
            onStateChanged?.invoke()
        }
    }

    private fun handleHazardCollision(hazard: PixieObject) {
        ProceduralExplosion.trigger(
            type = ExplosionType.SPARK_HIT,
            position = player.position,
            pool = engine.particleLayer.pool,
            activeList = engine.particleLayer.activeParticles,
            scale = 1.5f
        )
        hapticAdapter.trigger(HapticFeedbackType.HEAVY)
        if (hazard is VolcanicMeteor) {
            audioAdapter.playSound(AudioEvent.EXPLOSION_LARGE)
        } else {
            audioAdapter.playSound(AudioEvent.HIT)
        }
        player.takeDamage(100f, engine.context)
        engine.context.livesLeft--
        hazard.isAlive = false

        if (engine.context.livesLeft <= 0 || !player.isAlive) {
            engine.context.isGameOver = true
            audioAdapter.playSound(AudioEvent.GAME_OVER)
        }
        onStateChanged?.invoke()
    }

    private fun updateDistanceAndScore() {
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
