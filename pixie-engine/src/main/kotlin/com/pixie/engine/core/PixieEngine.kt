package com.pixie.engine.core

import com.pixie.engine.core.camera.GameViewMode
import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.chunk.ChunkManager
import com.pixie.engine.core.color.ColorRecoveryPipeline
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.layer.BackgroundParallaxLayer
import com.pixie.engine.core.layer.CloudLayer
import com.pixie.engine.core.layer.ForegroundParallaxLayer
import com.pixie.engine.core.layer.GameItemsLayer
import com.pixie.engine.core.layer.GroundGamefieldLayer
import com.pixie.engine.core.layer.HorizonCoordinator
import com.pixie.engine.core.layer.LayerStack
import com.pixie.engine.core.layer.SkyLayer
import com.pixie.engine.core.loop.PixieGameLoop
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.ports.HapticPort
import com.pixie.engine.core.ports.InputPort
import com.pixie.engine.core.ports.RenderBatchPort
import com.pixie.engine.core.ports.VirtualResolution

/**
 * Main entrance and facade of the Pixie 2D Game Engine.
 *
 * Coordinates the fixed-time step loop, 5+ layer rendering stack, shared horizon line,
 * AABB collision physics, chunk streaming, and color restoration.
 */
class PixieEngine(
    var virtualResolution: VirtualResolution = VirtualResolution(320, 180),
    viewMode: GameViewMode = GameViewMode.HorizontalScroller(),
    var pixelsGap: Int = 0, // 0 = solid pixel art mode, > 0 = retro pixel gap
    var audio: AudioPort? = null,
    var haptics: HapticPort? = null,
    var input: InputPort? = null
) {
    val context = GameContext()
    val horizon = HorizonCoordinator(horizonY = virtualResolution.height * 0.75f)
    val camera = PixieCamera(viewport = virtualResolution, viewMode = viewMode)
    val layerStack = LayerStack(horizon = horizon)
    val chunkManager = ChunkManager()
    val colorRecovery = ColorRecoveryPipeline()

    // Pre-configured standard layers
    val skyLayer = SkyLayer(horizon = horizon)
    val cloudLayer = CloudLayer()
    val backgroundLayer = BackgroundParallaxLayer(horizon = horizon)
    val groundLayer = GroundGamefieldLayer(horizon = horizon)
    val itemsLayer = GameItemsLayer()
    val particleLayer = com.pixie.engine.core.particle.ParticleLayer()
    val projectilePool = com.pixie.engine.core.weapon.ProjectilePool()
    val foregroundLayer = ForegroundParallaxLayer()

    var player: PixieObject? = null
    var onScoreChanged: ((Long) -> Unit)? = null
    var onGameOver: (() -> Unit)? = null

    val loop = PixieGameLoop(
        targetUpdatesPerSecond = 60,
        onFixedUpdate = { dt -> updateFixed(dt) },
        onRender = { alpha -> /* Ready for interpolation if needed */ }
    )

    init {
        // Register default layers: environment -> ground -> items -> particles -> foreground
        layerStack.addLayer(skyLayer)
        layerStack.addLayer(cloudLayer)
        layerStack.addLayer(backgroundLayer)
        layerStack.addLayer(groundLayer)
        layerStack.addLayer(itemsLayer)
        layerStack.addLayer(particleLayer)
        layerStack.addLayer(foregroundLayer)
    }

    fun start() {
        loop.start()
    }

    fun pause() {
        loop.pause()
    }

    fun resume() {
        loop.resume()
    }

    fun stop() {
        loop.stop()
    }

    fun tick(rawDeltaSeconds: Float) {
        loop.tick(rawDeltaSeconds)
    }

    private fun updateFixed(dt: Float) {
        if (context.isGameOver) return

        context.timeElapsedSeconds += dt
        colorRecovery.update(dt)
        context.colorRecoveryProgress = colorRecovery.progress

        // Update layers and active game objects
        layerStack.update(dt, context)

        // Update procedural chunk generation based on camera position
        chunkManager.update(camera.position.x, itemsLayer, context)

        // Physics & AABB Collisions resolution
        checkCollisions()

        // Update camera position tracking target
        camera.update(player, dt, context)
    }

    private fun checkCollisions() {
        val solidObjects = itemsLayer.objects.filter { it.isAlive && it.isSolid }
        val size = solidObjects.size

        for (i in 0 until size) {
            val a = solidObjects[i]
            val boxA = a.boundingBox
            for (j in i + 1 until size) {
                val b = solidObjects[j]
                if (boxA.intersects(b.boundingBox)) {
                    a.onCollision(b, context)
                    b.onCollision(a, context)
                }
            }
        }
    }

    /**
     * Renders the complete frame through the provided [RenderBatchPort].
     */
    fun render(batch: RenderBatchPort) {
        batch.pixelsGap = pixelsGap
        layerStack.render(batch, camera, context)
        batch.endFrame()
    }

    fun resetGame() {
        context.isGameOver = false
        context.isVictory = false
        context.score = 0L
        context.distance = 0f
        context.timeElapsedSeconds = 0f
        context.livesLeft = 3
        colorRecovery.reset(0f)
        itemsLayer.clear()
        particleLayer.clear()
        projectilePool.clear()
        chunkManager.reset(0f)
        camera.setPositionImmediate(com.pixie.engine.core.math.Vec2.ZERO)
        loop.reset()
    }
}
