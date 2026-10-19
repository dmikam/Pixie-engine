package com.example.raptor.world

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.layer.PixieLayer
import com.pixie.engine.core.ports.RenderBatchPort
import kotlin.random.Random

data class AtmosphericCloud(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val speedFactor: Float,
    val alpha: Float
)

/**
 * Atmospheric cloud layer for Raptor (order 600, above entities).
 *
 * Drifts downward faster than the terrain to generate a real sense of high-speed altitude flight.
 */
class RaptorCloudLayer(
    override val order: Int = 600,
    override var isEnabled: Boolean = true,
    override var isGridEnabled: Boolean = false
) : PixieLayer {
    override val name: String = "Raptor_Clouds"

    private val clouds = mutableListOf<AtmosphericCloud>()
    private val baseCloudSpeed: Float = 75f

    init {
        initClouds()
    }

    private fun initClouds() {
        clouds.clear()
        val rng = Random(99)
        var y = -80f
        while (y < 360f) {
            val w = rng.nextFloat() * 50f + 40f
            val h = rng.nextFloat() * 30f + 20f
            val x = rng.nextFloat() * (240f - w)
            val speed = rng.nextFloat() * 0.4f + 1.2f
            clouds.add(
                AtmosphericCloud(
                    x = x,
                    y = y,
                    width = w,
                    height = h,
                    speedFactor = speed,
                    alpha = rng.nextFloat() * 0.15f + 0.18f
                )
            )
            y += rng.nextFloat() * 90f + 60f
        }
    }

    override fun update(dt: Float, context: GameContext) {
        if (!isEnabled) return

        for (cloud in clouds) {
            cloud.y += (baseCloudSpeed * cloud.speedFactor) * dt
            if (cloud.y > 340f) {
                cloud.y = -70f - Random.nextFloat() * 40f
                cloud.x = Random.nextFloat() * (240f - cloud.width)
            }
        }
    }

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        if (!isEnabled) return

        val cloudColor = 0xFFFFFFFF.toInt()
        for (cloud in clouds) {
            if (cloud.y + cloud.height >= 0f && cloud.y <= 320f) {
                // Semi-transparent cloud block with rounded corners
                batch.drawPixelRect(
                    cloud.x,
                    cloud.y,
                    cloud.width,
                    cloud.height,
                    cloudColor,
                    alpha = cloud.alpha
                )
                // Center denser fluff
                val padX = 6f
                val padY = 4f
                if (cloud.width > padX * 2 && cloud.height > padY * 2) {
                    batch.drawPixelRect(
                        cloud.x + padX,
                        cloud.y + padY,
                        cloud.width - (padX * 2),
                        cloud.height - (padY * 2),
                        cloudColor,
                        alpha = cloud.alpha * 1.4f
                    )
                }
            }
        }
    }

    fun reset() {
        initClouds()
    }
}
