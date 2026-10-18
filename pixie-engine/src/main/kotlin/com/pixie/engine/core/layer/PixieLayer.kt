package com.pixie.engine.core.layer

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.ports.RenderBatchPort

/**
 * Centralized coordinator ensuring all layers (Sky, Background, Ground, Foreground)
 * align seamlessly to the exact same world horizon line.
 */
class HorizonCoordinator(var horizonY: Float = 135f)

/**
 * Base interface for all rendering and simulation layers in Pixie Engine.
 * Every layer is optional and users can easily define custom layers.
 */
interface PixieLayer {
    val name: String
    val order: Int
    var isEnabled: Boolean
    var isGridEnabled: Boolean

    fun update(dt: Float, context: GameContext)
    fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext)
}

/**
 * Manages the ordered rendering stack.
 */
class LayerStack(val horizon: HorizonCoordinator = HorizonCoordinator()) {
    private val layers = mutableListOf<PixieLayer>()

    fun addLayer(layer: PixieLayer) {
        layers.add(layer)
        layers.sortBy { it.order }
    }

    fun removeLayer(name: String) {
        layers.removeAll { it.name == name }
    }

    fun getLayer(name: String): PixieLayer? {
        return layers.find { it.name == name }
    }

    fun setLayerEnabled(name: String, enabled: Boolean) {
        getLayer(name)?.isEnabled = enabled
    }

    fun setLayerGridEnabled(name: String, gridEnabled: Boolean) {
        getLayer(name)?.isGridEnabled = gridEnabled
    }

    fun update(dt: Float, context: GameContext) {
        for (i in 0 until layers.size) {
            val layer = layers[i]
            if (layer.isEnabled) {
                layer.update(dt, context)
            }
        }
    }

    fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        for (i in 0 until layers.size) {
            val layer = layers[i]
            if (layer.isEnabled) {
                batch.isCurrentLayerGridEnabled = layer.isGridEnabled
                layer.render(batch, camera, context)
            }
        }
    }

    fun allLayers(): List<PixieLayer> = layers.toList()
}
