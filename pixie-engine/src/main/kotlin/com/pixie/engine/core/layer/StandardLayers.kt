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

data class Star(
    val initialAngleRad: Float,
    val orbitRadius: Float,
    val staticX: Float,
    val staticY: Float,
    val size: Float,
    var twinklePhase: Float
)

/**
 * Layer 1: Sky
 *
 * Renders celestial bodies (Sun, Moon), star field, and gradual Day/Night atmospheric gradient.
 * Movement orbits smoothly around a configurable celestial center [celestialCenter]:
 * Sun and Moon rise near the upper top corner and set next to the right screen border.
 * The Moon shares the exact size and silhouette as the Sun with distinct crater texture.
 * Stars rotate around the EXACT SAME celestial center and in the EXACT SAME direction as the
 * Sun and Moon, at 95% speed (5% slower).
 */
class SkyLayer(
    val horizon: HorizonCoordinator,
    override val order: Int = 100,
    override var isEnabled: Boolean = true,
    override var isGridEnabled: Boolean = false, // Default: solid for sky
    var transitMode: CelestialTransitMode = CelestialTransitMode.CIRCULAR,
    var dayNightCycleSeconds: Float = 60f,
    var celestialCenter: Vec2 = Vec2(160f, 140f),
    var celestialRadiusX: Float = 145f,
    var celestialRadiusY: Float = 110f,
    var riseAngleDeg: Float = 150f, // Starts near upper top corner
    var setAngleDeg: Float = 8f,    // Sets next to right screen border
    var starsFollowCelestialOrbit: Boolean = true,
    var starsOrbitSpeedRatio: Float = 0.95f, // 5% slower than main celestial trajectory
    var starsMinRadius: Float = 25f,
    var starsMaxRadius: Float = 300f, // Extends all the way up to and beyond the farthest corners (~213px to corners (0,0) and (320,0))
    var starsVerticalScaling: Float = 0.96f
) : PixieLayer {
    override val name: String = "Layer1_Sky"

    var cycleProgress: Float = 0.15f // 0=dawn, 0.25=noon, 0.5=sunset, 0.75=midnight
    private val stars = mutableListOf<Star>()

    data class SkyKeyframe(val time: Float, val color: Int)

    private val skyKeyframes = listOf(
        SkyKeyframe(0.00f, 0xFFFFA07A.toInt()), // Dawn light salmon peach
        SkyKeyframe(0.12f, 0xFFFFCA28.toInt()), // Early morning warm golden
        SkyKeyframe(0.25f, 0xFF87CEEB.toInt()), // Midday clear sky blue
        SkyKeyframe(0.42f, 0xFF4FC3F7.toInt()), // Afternoon bright cyan-blue
        SkyKeyframe(0.50f, 0xFFFF7043.toInt()), // Sunset warm coral
        SkyKeyframe(0.58f, 0xFFD84315.toInt()), // Sunset deep amber crimson
        SkyKeyframe(0.66f, 0xFF4A148C.toInt()), // Twilight deep purple
        SkyKeyframe(0.75f, 0xFF0D1B2A.toInt()), // Midnight dark obsidian
        SkyKeyframe(0.88f, 0xFF1B263B.toInt()), // Pre-dawn deep navy
        SkyKeyframe(1.00f, 0xFFFFA07A.toInt())  // Smooth loop back to Dawn
    )

    init {
        // Generate procedural star field extending all the way to and beyond the farthest upper corners
        val random = java.util.Random(1337)
        for (i in 0 until 140) {
            val initAngle = random.nextFloat() * 6.2831855f
            // Uniform 2D circular disk distribution ensures outer radii and upper corners are richly populated
            val rad = starsMinRadius + kotlin.math.sqrt(random.nextFloat()) * (starsMaxRadius - starsMinRadius)
            stars.add(
                Star(
                    initialAngleRad = initAngle,
                    orbitRadius = rad,
                    staticX = random.nextFloat() * 320f,
                    staticY = random.nextFloat() * 140f,
                    size = if (random.nextFloat() > 0.80f) 2f else 1f,
                    twinklePhase = random.nextFloat() * 6.28f
                )
            )
        }
    }

    override fun update(dt: Float, context: GameContext) {
        if (dayNightCycleSeconds > 0f) {
            cycleProgress = (cycleProgress + (dt / dayNightCycleSeconds)) % 1.0f
        }
        for (star in stars) {
            star.twinklePhase += dt * 3.5f
        }
    }

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        val width = batch.resolution.width.toFloat()
        val horizonY = horizon.horizonY - camera.position.y

        // 1. Ultra-gradual day/night ambient color grading interpolation
        val skyColor = calculateSkyColor(cycleProgress)
        batch.drawPixelRect(0f, 0f, width, horizonY.coerceAtLeast(0f), skyColor, context.colorRecoveryProgress)

        // 2. Stars visibility (fades smoothly in during dusk, fades out at dawn)
        val starAlpha = when {
            cycleProgress in 0.65f..0.92f -> 1.0f
            cycleProgress in 0.55f..0.65f -> (cycleProgress - 0.55f) * 10f
            cycleProgress in 0.92f..1.0f -> (1.0f - cycleProgress) * 12.5f
            cycleProgress in 0.0f..0.05f -> (0.05f - cycleProgress) * 20f
            else -> 0f
        }

        if (starAlpha > 0.05f) {
            // Direction matches Sun and Moon: angle DECREASES (clockwise rotation from top-left to right-border)
            val starSweepAngle = cycleProgress * 6.2831855f * starsOrbitSpeedRatio

            for (star in stars) {
                val twinkle = (sin(star.twinklePhase.toDouble()) * 0.4 + 0.6).toFloat()
                val starColor = 0xFFFFFFFF.toInt()

                val (sx, sy) = if (starsFollowCelestialOrbit) {
                    // EXACT same center and EXACT same rotation direction (minus sign) as Sun & Moon!
                    val angle = star.initialAngleRad - starSweepAngle
                    val x = celestialCenter.x + cos(angle.toDouble()).toFloat() * star.orbitRadius
                    val y = celestialCenter.y - sin(angle.toDouble()).toFloat() * (star.orbitRadius * starsVerticalScaling)
                    x to y
                } else {
                    star.staticX to star.staticY
                }

                if (sy in -4f..horizonY && sx in -4f..(width + 4f)) {
                    batch.drawPixelRect(sx, sy, star.size, star.size, starColor, 1.0f, starAlpha * twinkle)
                }
            }
        }

        // 3. Celestial Movement around common center
        // Day window: [0.04 .. 0.54] (Sun traverses upper left corner -> right border)
        // Night window: [0.54 .. 1.04] (Moon traverses upper left corner -> right border)
        val dayWindowStart = 0.04f
        val dayWindowEnd = 0.54f
        val windowDuration = 0.50f

        val isDay = cycleProgress in dayWindowStart..dayWindowEnd

        if (isDay) {
            val t = (cycleProgress - dayWindowStart) / windowDuration
            val currentAngleDeg = riseAngleDeg + t * (setAngleDeg - riseAngleDeg)
            val rad = Math.toRadians(currentAngleDeg.toDouble())

            val sunX = celestialCenter.x + cos(rad).toFloat() * celestialRadiusX
            val sunY = celestialCenter.y - sin(rad).toFloat() * celestialRadiusY

            // Smooth fade-in near top corner and fade-out near right border
            val sunAlpha = when {
                t < 0.08f -> t / 0.08f
                t > 0.92f -> (1.0f - t) / 0.08f
                else -> 1.0f
            }

            if (sunAlpha > 0.02f) {
                // Sun: 20x20 round pixel-art silhouette (16x16 core + 20x12 and 12x20 corona)
                batch.drawPixelRect(sunX - 8f, sunY - 8f, 16f, 16f, 0xFFFFD700.toInt(), 1.0f, sunAlpha)
                batch.drawPixelRect(sunX - 10f, sunY - 6f, 20f, 12f, 0xFFFFE082.toInt(), 1.0f, sunAlpha * 0.75f)
                batch.drawPixelRect(sunX - 6f, sunY - 10f, 12f, 20f, 0xFFFFE082.toInt(), 1.0f, sunAlpha * 0.75f)
            }
        } else {
            // Night window
            val normalizedNightProgress = if (cycleProgress >= dayWindowEnd) {
                (cycleProgress - dayWindowEnd) / windowDuration
            } else {
                (cycleProgress + 1.0f - dayWindowEnd) / windowDuration
            }

            val t = normalizedNightProgress.coerceIn(0f, 1f)
            val currentAngleDeg = riseAngleDeg + t * (setAngleDeg - riseAngleDeg)
            val rad = Math.toRadians(currentAngleDeg.toDouble())

            val moonX = celestialCenter.x + cos(rad).toFloat() * celestialRadiusX
            val moonY = celestialCenter.y - sin(rad).toFloat() * celestialRadiusY

            val moonAlpha = when {
                t < 0.08f -> t / 0.08f
                t > 0.92f -> (1.0f - t) / 0.08f
                else -> 1.0f
            }

            if (moonAlpha > 0.02f) {
                // Moon: EXACT same size and form as the Sun (20x20 round silhouette: 16x16 core + 20x12 / 12x20 rim),
                // but with distinct silver-blue lunar color and crater texture!
                batch.drawPixelRect(moonX - 8f, moonY - 8f, 16f, 16f, 0xFFE0E6ED.toInt(), 1.0f, moonAlpha)
                batch.drawPixelRect(moonX - 10f, moonY - 6f, 20f, 12f, 0xFFCFD8DC.toInt(), 1.0f, moonAlpha * 0.8f)
                batch.drawPixelRect(moonX - 6f, moonY - 10f, 12f, 20f, 0xFFCFD8DC.toInt(), 1.0f, moonAlpha * 0.8f)

                // Distinct lunar crater texture (Maria & crater spots)
                batch.drawPixelRect(moonX - 4f, moonY - 4f, 4f, 4f, 0xFF90A4AE.toInt(), 1.0f, moonAlpha)
                batch.drawPixelRect(moonX + 2f, moonY - 2f, 3f, 3f, 0xFF78909C.toInt(), 1.0f, moonAlpha)
                batch.drawPixelRect(moonX - 3f, moonY + 2f, 5f, 3f, 0xFF90A4AE.toInt(), 1.0f, moonAlpha)
                batch.drawPixelRect(moonX - 6f, moonY - 6f, 3f, 3f, 0xFFECEFF1.toInt(), 1.0f, moonAlpha * 0.9f) // Bright rim highlight
            }
        }
    }

    private fun calculateSkyColor(progress: Float): Int {
        val p = progress % 1.0f
        for (i in 0 until skyKeyframes.size - 1) {
            val current = skyKeyframes[i]
            val next = skyKeyframes[i + 1]
            if (p in current.time..next.time) {
                val t = (p - current.time) / (next.time - current.time)
                return interpolateColor(current.color, next.color, t)
            }
        }
        return skyKeyframes.last().color
    }

    private fun interpolateColor(colorA: Int, colorB: Int, t: Float): Int {
        val factor = t.coerceIn(0f, 1f)
        val a = ((colorA ushr 24 and 0xFF) * (1f - factor) + (colorB ushr 24 and 0xFF) * factor).toInt()
        val r = ((colorA ushr 16 and 0xFF) * (1f - factor) + (colorB ushr 16 and 0xFF) * factor).toInt()
        val g = ((colorA ushr 8 and 0xFF) * (1f - factor) + (colorB ushr 8 and 0xFF) * factor).toInt()
        val b = ((colorA and 0xFF) * (1f - factor) + (colorB and 0xFF) * factor).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
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
 * Layer 4: Ground / Gamefield
 *
 * Horizontal ground with height variation, pits, or static bounded arena (Arkanoid mode).
 * Top collision surface exactly matches [horizon.horizonY].
 */
class GroundGamefieldLayer(
    val horizon: HorizonCoordinator,
    override val order: Int = 400,
    override var isEnabled: Boolean = true,
    override var isGridEnabled: Boolean = false, // Default: solid for ground
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
    override var isEnabled: Boolean = true,
    override var isGridEnabled: Boolean = true // Default: grid enabled for objects and player
) : PixieLayer {
    override val name: String = "Layer5_Items"

    val objects = mutableListOf<PixieObject>()

    fun addObject(obj: PixieObject) {
        objects.add(obj)
    }

    fun removeObject(obj: PixieObject) {
        objects.remove(obj)
    }

    fun removeObject(id: String) {
        objects.removeAll { it.id == id }
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

            // Render motion blur trail if active or naturally fading out
            if (obj.trailHistory.isNotEmpty()) {
                val effectiveStrength = obj.motionBlur.strength * obj.trailFadeAlpha
                if (effectiveStrength > 0.01f) {
                    val screenTrail = obj.trailHistory.map { camera.worldToScreen(it) }
                    batch.drawMotionBlur(sprite, screenTrail, effectiveStrength, saturation)
                }
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
