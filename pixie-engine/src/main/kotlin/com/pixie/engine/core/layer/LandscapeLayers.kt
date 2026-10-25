package com.pixie.engine.core.layer

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.ports.RenderBatchPort

/**
 * Layer 2: Clouds
 *
 * Atmospheric drift layer independent of celestial transit and ground velocity.
 */
data class Cloud(var x: Float, val y: Float, val width: Float, val height: Float, val speed: Float)

class CloudLayer(
    override val order: Int = 200,
    override var isEnabled: Boolean = true,
    override var isGridEnabled: Boolean = false, // Default: solid for clouds
    var windVelocity: Float = 12f
) : PixieLayer {
    override val name: String = "Layer2_Clouds"

    private val clouds = mutableListOf(
        Cloud(x = 20f, y = 25f, width = 45f, height = 12f, speed = 0.8f),
        Cloud(x = 110f, y = 45f, width = 36f, height = 10f, speed = 1.1f),
        Cloud(x = 220f, y = 18f, width = 52f, height = 14f, speed = 0.6f),
        Cloud(x = 310f, y = 35f, width = 40f, height = 11f, speed = 0.9f)
    )

    override fun update(dt: Float, context: GameContext) {
        for (cloud in clouds) {
            cloud.x += (windVelocity * cloud.speed) * dt
            if (cloud.x > 380f) {
                cloud.x = -cloud.width - 20f
            }
        }
    }

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        val cloudColor = 0xFFECEFF1.toInt()
        val shadowColor = 0xFFCFD8DC.toInt()

        for (cloud in clouds) {
            // Draw cloud body
            batch.drawPixelRect(cloud.x, cloud.y, cloud.width, cloud.height, cloudColor, context.colorRecoveryProgress, 0.85f)
            // Cloud top puff
            batch.drawPixelRect(cloud.x + 8f, cloud.y - 4f, cloud.width - 16f, 4f, cloudColor, context.colorRecoveryProgress, 0.85f)
            // Cloud shadow base
            batch.drawPixelRect(cloud.x + 4f, cloud.y + cloud.height - 3f, cloud.width - 8f, 3f, shadowColor, context.colorRecoveryProgress, 0.85f)
        }
    }
}

/**
 * Layer 3: Background Parallax
 *
 * Mountains, forests, and distant silhouettes. Strict parallax effect based on z-index
 * where factor = 1 / (1 + zIndex). Shares the exact same [horizon.horizonY].
 */
data class ParallaxSilhouette(
    val xOffset: Float,
    val peakWidth: Float,
    val peakHeight: Float,
    val zIndex: Float,
    val color: Int
)

class BackgroundParallaxLayer(
    val horizon: HorizonCoordinator,
    override val order: Int = 300,
    override var isEnabled: Boolean = true,
    override var isGridEnabled: Boolean = true // Default: grid enabled for background
) : PixieLayer {
    override val name: String = "Layer3_Background"

    private val mountains = listOf(
        ParallaxSilhouette(xOffset = 0f, peakWidth = 80f, peakHeight = 45f, zIndex = 3.0f, color = 0xFF455A64.toInt()),
        ParallaxSilhouette(xOffset = 90f, peakWidth = 110f, peakHeight = 60f, zIndex = 3.0f, color = 0xFF37474F.toInt()),
        ParallaxSilhouette(xOffset = 210f, peakWidth = 95f, peakHeight = 50f, zIndex = 3.0f, color = 0xFF455A64.toInt()),
        // Mid-distance volcanic ridge
        ParallaxSilhouette(xOffset = 30f, peakWidth = 60f, peakHeight = 35f, zIndex = 1.5f, color = 0xFF5D4037.toInt()),
        ParallaxSilhouette(xOffset = 150f, peakWidth = 70f, peakHeight = 40f, zIndex = 1.5f, color = 0xFF4E342E.toInt()),
        ParallaxSilhouette(xOffset = 260f, peakWidth = 65f, peakHeight = 32f, zIndex = 1.5f, color = 0xFF5D4037.toInt())
    )

    override fun update(dt: Float, context: GameContext) {}

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        val screenWidth = batch.resolution.width.toFloat()
        val horizonY = horizon.horizonY - camera.position.y

        for (peak in mountains) {
            val parallaxFactor = 1.0f / (1.0f + peak.zIndex)
            val wrapWidth = screenWidth + peak.peakWidth
            val rawX = peak.xOffset - camera.position.x * parallaxFactor
            val drawX = ((rawX % wrapWidth) + wrapWidth) % wrapWidth - peak.peakWidth

            // Draw mountain peak anchored to horizonY
            val topY = horizonY - peak.peakHeight
            batch.drawPixelRect(drawX, topY, peak.peakWidth, peak.peakHeight, peak.color, context.colorRecoveryProgress)
            // Accent peak shadow
            batch.drawPixelRect(drawX + peak.peakWidth * 0.5f, topY, peak.peakWidth * 0.5f, peak.peakHeight, 0xFF263238.toInt(), context.colorRecoveryProgress, 0.4f)
        }
    }
}

/**
 * Optional Layer 6: Foreground Parallax
 *
 * Non-collidable foreground elements (hanging vines, foreground boulders, dust particles)
 * positioned between the camera and the playzone with forward parallax (z < 0).
 */
class ForegroundParallaxLayer(
    override val order: Int = 600,
    override var isEnabled: Boolean = true,
    override var isGridEnabled: Boolean = true // Default: grid enabled for foreground
) : PixieLayer {
    override val name: String = "Layer6_Foreground"

    data class ForegroundProp(var x: Float, val y: Float, val width: Float, val height: Float, val color: Int)

    private val props = mutableListOf(
        ForegroundProp(x = 50f, y = 160f, width = 28f, height = 24f, color = 0xAA1B5E20.toInt()),
        ForegroundProp(x = 180f, y = 165f, width = 34f, height = 20f, color = 0xAA2E7D32.toInt()),
        ForegroundProp(x = 300f, y = 158f, width = 40f, height = 26f, color = 0xAA1B5E20.toInt())
    )

    override fun update(dt: Float, context: GameContext) {}

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        val screenWidth = batch.resolution.width.toFloat()
        val forwardParallax = 1.25f // Moves faster than the ground plane for foreground depth!

        for (prop in props) {
            val wrapWidth = screenWidth + prop.width
            val rawX = prop.x - camera.position.x * forwardParallax
            val drawX = ((rawX % wrapWidth) + wrapWidth) % wrapWidth - prop.width

            batch.drawPixelRect(drawX, prop.y, prop.width, prop.height, prop.color, context.colorRecoveryProgress, 0.75f)
        }
    }
}
