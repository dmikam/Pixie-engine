package com.pixie.engine.core.particle

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.layer.PixieLayer
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.RenderBatchPort

/**
 * Standard Pixie Engine rendering and simulation layer for procedural particles.
 *
 * Sits at order 550 (rendering immediately above items and below atmospheric foreground/HUD),
 * driving zero-allocation particle lifecycles and batch drawing.
 */
class ParticleLayer(
    override val order: Int = 550,
    override var isEnabled: Boolean = true,
    override var isGridEnabled: Boolean = true,
    val pool: ParticlePool = ParticlePool(initialCapacity = 256, maxCapacity = 2048)
) : PixieLayer {
    override val name: String = "Layer_Particles"

    val activeParticles = mutableListOf<Particle>()

    fun spawnExplosion(type: ExplosionType, position: Vec2, scale: Float = 1.0f) {
        ProceduralExplosion.trigger(type, position, pool, activeParticles, scale)
    }

    fun spawnSparks(position: Vec2, count: Int = 6, color: Int = 0xFFFFEB3B.toInt()) {
        ProceduralExplosion.spawnSparkHit(position, pool, activeParticles, count, color)
    }

    fun spawnPlasmaBurst(position: Vec2, count: Int = 14) {
        ProceduralExplosion.spawnPlasmaBurst(position, pool, activeParticles, count)
    }

    fun spawnShockwave(position: Vec2, count: Int = 16) {
        ProceduralExplosion.spawnShockwave(position, pool, activeParticles, count)
    }

    override fun update(dt: Float, context: GameContext) {
        if (!isEnabled || activeParticles.isEmpty()) return

        var i = 0
        while (i < activeParticles.size) {
            val p = activeParticles[i]
            val stillAlive = p.update(dt)
            if (!stillAlive) {
                pool.recycle(p)
                // Fast remove at index (swap with last for O(1) or removeAt)
                val lastIdx = activeParticles.size - 1
                if (i != lastIdx) {
                    activeParticles[i] = activeParticles[lastIdx]
                }
                activeParticles.removeAt(lastIdx)
            } else {
                i++
            }
        }
    }

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        if (!isEnabled || activeParticles.isEmpty()) return

        val viewportWidth = camera.viewport.width.toFloat()
        val viewportHeight = camera.viewport.height.toFloat()

        for (i in 0 until activeParticles.size) {
            val p = activeParticles[i]
            val screenPos = camera.worldToScreen(p.position)

            // Viewport frustum culling
            if (screenPos.x >= -10f && screenPos.x <= viewportWidth + 10f &&
                screenPos.y >= -10f && screenPos.y <= viewportHeight + 10f
            ) {
                batch.drawPixelRect(
                    x = screenPos.x,
                    y = screenPos.y,
                    width = p.size,
                    height = p.size,
                    color = p.currentColor,
                    saturation = 1.0f,
                    alpha = p.alpha
                )
            }
        }
    }

    fun clear() {
        pool.recycleAll(activeParticles)
        activeParticles.clear()
    }
}
