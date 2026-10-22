package com.pixie.engine.core.screen

import com.pixie.engine.core.PixieEngine
import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.ports.RenderBatchPort

/**
 * Built-in gameplay screen for [PixieEngine] that drives standard layer stack updates,
 * collision detection, chunk generation, and camera movement.
 */
class StandardGameplayScreen(
    val engine: PixieEngine,
    override val screenId: String = StandardScreenIds.GAMEPLAY,
    val onTick: ((dt: Float, context: GameContext) -> Unit)? = null
) : PixieScreen {

    override fun update(dt: Float, context: GameContext) {
        engine.layerStack.update(dt, context)
        engine.chunkManager.update(engine.camera.position.x, engine.itemsLayer, context)
        engine.checkCollisions()
        engine.camera.update(engine.player, dt, context)
        onTick?.invoke(dt, context)
    }

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        engine.layerStack.render(batch, camera, context)
    }
}
