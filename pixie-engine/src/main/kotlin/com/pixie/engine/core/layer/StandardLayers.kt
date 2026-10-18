package com.pixie.engine.core.layer

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.ports.RenderBatchPort
import kotlin.math.cos
import kotlin.math.sin

/**
 * Celestial movement strategy for the sun and moon.
 */
enum class CelestialTransitMode {
    CIRCULAR,       // Orbits smoothly around the horizon center
    UNIDIRECTIONAL  // Traverses east to west across the sky
}

data class Star(val x: Float, val y: Float, val size: Float, var twinklePhase: Float)

/**
 * Layer 1: Sky
 *
 * Renders celestial body (Sun, Moon), star field, and dynamic Day/Night atmospheric gradient.
 * Anchored to [horizon.horizonY].
 */
class SkyLayer(
    val horizon: HorizonCoordinator,
    override val order: Int = 100,
    override var isEnabled: Boolean = true,
    var transitMode: CelestialTransitMode = CelestialTransitMode.CIRCULAR,
    var dayNightCycleSeconds: Float = 60f
) : PixieLayer {
    override val name: String = "Layer1_Sky"

    var cycleProgress: Float = 0.25f // 0=dawn, 0.25=noon, 0.5=sunset, 0.75=midnight
    private val stars = mutableListOf<Star>()

    init {
        // Generate procedural star field
        val random = java.util.Random(1337)
        for (i in 0 until 40) {
            stars.add(
                Star(
                    x = random.nextFloat() * 320f,
                    y = random.nextFloat() * 100f,
                    size = if (random.nextFloat() > 0.8f) 2f else 1f,
                    twinklePhase = random.nextFloat() * 6.28f
                )
            )
        }
    }

    override fun update(dt: Float, context: GameContext) {
        if (dayNightCycleSeconds > 0f) {
            cycleProgress = (cycleProgress + (dt / dayNightCycleSeconds)) % 1.0f
        }
    }

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        val width = batch.resolution.width.toFloat()
        val horizonY = horizon.horizonY - camera.position.y

        // Day/night ambient color grading calculation
        val isDay = cycleProgress in 0.05f..0.55f
        val skyColor = when {
            cycleProgress in 0.2f..0.4f -> 0xFF87CEEB.toInt() // Daytime sky blue
            cycleProgress in 0.4f..0.55f -> 0xFFE65100.toInt() // Sunset fiery amber
            cycleProgress in 0.05f..0.2f -> 0xFFFF8A65.toInt() // Dawn peach
            else -> 0xFF0D1B2A.toInt() // Night dark obsidian
        }

        // Fill sky background down to shared horizon
        batch.drawPixelRect(0f, 0f, width, horizonY.coerceAtLeast(0f), skyColor, context.colorRecoveryProgress)

        // Render stars at night or twilight
        val starAlpha = when {
            cycleProgress in 0.6f..0.95f -> 1.0f
            cycleProgress in 0.5f..0.6f -> (cycleProgress - 0.5f) * 10f
            cycleProgress in 0.95f..1.0f -> (1.0f - cycleProgress) * 20f
            else -> 0f
        }

        if (starAlpha > 0.05f) {
            for (star in stars) {
                val twinkle = (sin(star.twinklePhase.toDouble()) * 0.4 + 0.6).toFloat()
                val starColor = 0xFFFFFFFF.toInt()
                batch.drawPixelRect(star.x, star.y, star.size, star.size, starColor, 1.0f, starAlpha * twinkle)
            }
        }

        // Render Sun or Moon (Always colored / glowing!)
        val celestialCenterX = width * 0.5f
        val celestialRadius = width * 0.42f
        val angle = cycleProgress * 6.2831855f - 1.5707963f

        if (isDay) {
            val sunX = celestialCenterX + cos(angle.toDouble()).toFloat() * celestialRadius
            val sunY = horizonY + sin(angle.toDouble()).toFloat() * celestialRadius * 0.7f
            if (sunY < horizonY) {
                // Sun is always in full brilliant color
                batch.drawPixelRect(sunX - 8f, sunY - 8f, 16f, 16f, 0xFFFFD700.toInt(), 1.0f)
                batch.drawPixelRect(sunX - 10f, sunY - 6f, 20f, 12f, 0xFFFFE082.toInt(), 1.0f, 0.7f)
                batch.drawPixelRect(sunX - 6f, sunY - 10f, 12f, 20f, 0xFFFFE082.toInt(), 1.0f, 0.7f)
            }
        } else {
            val moonAngle = angle + 3.1415927f
            val moonX = celestialCenterX + cos(moonAngle.toDouble()).toFloat() * celestialRadius
            val moonY = horizonY + sin(moonAngle.toDouble()).toFloat() * celestialRadius * 0.7f
            if (moonY < horizonY) {
                // Moon is silver-white
                batch.drawPixelRect(moonX - 6f, moonY - 6f, 12f, 12f, 0xFFE0E0E0.toInt(), 1.0f)
                batch.drawPixelRect(moonX - 4f, moonY - 4f, 4f, 4f, 0xFF9E9E9E.toInt(), 1.0f)
            }
        }
    }
}

/**
 * Layer 2: Clouds
 *
 * Atmospheric drift layer independent of celestial transit and ground velocity.
 */
data class Cloud(var x: Float, val y: Float, val width: Float, val height: Float, val speed: Float)

class CloudLayer(
    override val order: Int = 200,
    override var isEnabled: Boolean = true,
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
    override var isEnabled: Boolean = true
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
 * Layer 4: Ground / Gamefield
 *
 * Horizontal ground with height variation, pits, or static bounded arena (Arkanoid mode).
 * Top collision surface exactly matches [horizon.horizonY].
 */
class GroundGamefieldLayer(
    val horizon: HorizonCoordinator,
    override val order: Int = 400,
    override var isEnabled: Boolean = true,
    var isStaticArena: Boolean = false
) : PixieLayer {
    override val name: String = "Layer4_Ground"

    var groundHeightOffset: Float = 0f
    val pits = mutableListOf<FloatRange>() // X ranges of holes/pits

    data class FloatRange(val start: Float, val end: Float) {
        fun contains(x: Float): Boolean = x in start..end
    }

    override fun update(dt: Float, context: GameContext) {}

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        val screenWidth = batch.resolution.width.toFloat()
        val screenHeight = batch.resolution.height.toFloat()
        val horizonY = horizon.horizonY - camera.position.y + groundHeightOffset

        if (isStaticArena) {
            // Arkanoid fixed screen borders
            val wallColor = 0xFF607D8B.toInt()
            batch.drawPixelRect(0f, 0f, screenWidth, 8f, wallColor, context.colorRecoveryProgress) // Top wall
            batch.drawPixelRect(0f, 0f, 8f, screenHeight, wallColor, context.colorRecoveryProgress) // Left wall
            batch.drawPixelRect(screenWidth - 8f, 0f, 8f, screenHeight, wallColor, context.colorRecoveryProgress) // Right wall
            return
        }

        // Horizontal runner terrain
        val groundColor = 0xFF5D4037.toInt() // Dirt brown
        val grassColor = 0xFF388E3C.toInt()  // Top surface green
        val bedrockColor = 0xFF3E2723.toInt()

        val groundHeight = (screenHeight - horizonY).coerceAtLeast(0f)

        // Main ground block
        batch.drawPixelRect(0f, horizonY, screenWidth, groundHeight, groundColor, context.colorRecoveryProgress)
        // Top surface contour stripe (3px)
        batch.drawPixelRect(0f, horizonY, screenWidth, 3f, grassColor, context.colorRecoveryProgress)
        // Subsurface bedrock layer
        batch.drawPixelRect(0f, horizonY + 14f, screenWidth, groundHeight, bedrockColor, context.colorRecoveryProgress)

        // Draw ground details / texture spots
        val tileOffset = ((-camera.position.x % 32f) + 32f) % 32f
        var x = tileOffset - 32f
        while (x < screenWidth + 32f) {
            batch.drawPixelRect(x, horizonY + 6f, 6f, 2f, 0xFF795548.toInt(), context.colorRecoveryProgress)
            batch.drawPixelRect(x + 14f, horizonY + 10f, 4f, 2f, 0xFF4E342E.toInt(), context.colorRecoveryProgress)
            x += 32f
        }
    }
}

/**
 * Layer 5: Game Items & Entities
 *
 * Manages and renders all active interactive entities: player, hazards, pickups, particles.
 */
class GameItemsLayer(
    override val order: Int = 500,
    override var isEnabled: Boolean = true
) : PixieLayer {
    override val name: String = "Layer5_Items"

    val objects = mutableListOf<PixieObject>()

    fun addObject(obj: PixieObject) {
        objects.add(obj)
    }

    fun removeObject(obj: PixieObject) {
        objects.remove(obj)
    }

    fun clear() {
        objects.clear()
    }

    fun getObjectsInClass(className: String): List<PixieObject> {
        return objects.filter { it.isAlive && it.isInClass(className) }
    }

    fun getObjectsInGroup(groupName: String): List<PixieObject> {
        return objects.filter { it.isAlive && it.isInGroup(groupName) }
    }

    override fun update(dt: Float, context: GameContext) {
        val iterator = objects.iterator()
        while (iterator.hasNext()) {
            val obj = iterator.next()
            if (!obj.isAlive) {
                iterator.remove()
                continue
            }
            obj.update(dt, context)
        }
    }

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        // Sort objects by zIndex for proper depth layering
        val sorted = objects.filter { it.isVisible && it.isAlive }.sortedBy { it.zIndex }

        for (obj in sorted) {
            val screenPos = camera.worldToScreen(obj.position)
            val sprite = obj.getCurrentSprite()
            val saturation = obj.resolveSaturation(context.colorRecoveryProgress)

            // Render motion blur trail if enabled for this object or object group
            if (obj.motionBlur.enabled && obj.trailHistory.isNotEmpty()) {
                val screenTrail = obj.trailHistory.map { camera.worldToScreen(it) }
                batch.drawMotionBlur(sprite, screenTrail, obj.motionBlur.strength, saturation)
            }

            // Render current sprite
            if (sprite != com.pixie.engine.core.sprite.PixelSprite.EMPTY) {
                batch.drawSprite(sprite, screenPos.x, screenPos.y, saturation)
            } else {
                // Fallback bounding box pixel rect if no sprite set
                batch.drawPixelRect(screenPos.x, screenPos.y, obj.size.x, obj.size.y, 0xFFFF00FF.toInt(), saturation)
            }
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
    override var isEnabled: Boolean = true
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
