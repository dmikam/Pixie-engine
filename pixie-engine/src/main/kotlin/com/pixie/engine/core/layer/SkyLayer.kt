package com.pixie.engine.core.layer

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
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
    var starsMaxRadius: Float = 300f, // Extends all the way up to and beyond the farthest corners
    var starsVerticalScaling: Float = 0.96f
) : PixieLayer {
    override val name: String = "Layer1_Sky"

    companion object {
        const val DAY_WINDOW_START = 0.04f
        const val DAY_WINDOW_END = 0.54f
    }

    val isDay: Boolean
        get() = cycleProgress in DAY_WINDOW_START..DAY_WINDOW_END

    var onCelestialShift: ((isDay: Boolean) -> Unit)? = null

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
        val random = java.util.Random(1337)
        for (i in 0 until 140) {
            val initAngle = random.nextFloat() * 6.2831855f
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
            val wasDay = isDay
            cycleProgress = (cycleProgress + (dt / dayNightCycleSeconds)) % 1.0f
            if (wasDay != isDay) {
                onCelestialShift?.invoke(isDay)
            }
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

        // 2. Stars
        renderStars(batch, horizonY, width)

        // 3. Celestial Movement around common center
        val dayWindowStart = DAY_WINDOW_START
        val dayWindowEnd = DAY_WINDOW_END
        val windowDuration = 0.50f
        val isDay = this.isDay

        if (isDay) {
            renderSun(batch, dayWindowStart, windowDuration)
        } else {
            renderMoon(batch, dayWindowEnd, windowDuration)
        }
    }

    private fun renderStars(batch: RenderBatchPort, horizonY: Float, width: Float) {
        val starAlpha = when {
            cycleProgress in 0.65f..0.92f -> 1.0f
            cycleProgress in 0.55f..0.65f -> (cycleProgress - 0.55f) * 10f
            cycleProgress in 0.92f..1.0f -> (1.0f - cycleProgress) * 12.5f
            cycleProgress in 0.0f..0.05f -> (0.05f - cycleProgress) * 20f
            else -> 0f
        }

        if (starAlpha <= 0.05f) return

        val starSweepAngle = cycleProgress * 6.2831855f * starsOrbitSpeedRatio

        for (star in stars) {
            val twinkle = (sin(star.twinklePhase.toDouble()) * 0.4 + 0.6).toFloat()
            val starColor = 0xFFFFFFFF.toInt()

            val (sx, sy) = if (starsFollowCelestialOrbit) {
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

    private fun renderSun(batch: RenderBatchPort, windowStart: Float, duration: Float) {
        val t = (cycleProgress - windowStart) / duration
        val currentAngleDeg = riseAngleDeg + t * (setAngleDeg - riseAngleDeg)
        val rad = Math.toRadians(currentAngleDeg.toDouble())

        val sunX = celestialCenter.x + cos(rad).toFloat() * celestialRadiusX
        val sunY = celestialCenter.y - sin(rad).toFloat() * celestialRadiusY

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
    }

    private fun renderMoon(batch: RenderBatchPort, dayWindowEnd: Float, duration: Float) {
        val normalizedNightProgress = if (cycleProgress >= dayWindowEnd) {
            (cycleProgress - dayWindowEnd) / duration
        } else {
            (cycleProgress + 1.0f - dayWindowEnd) / duration
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
            // Moon: EXACT same size and form as the Sun (20x20 round silhouette: 16x16 core + 20x12 / 12x20 rim)
            batch.drawPixelRect(moonX - 8f, moonY - 8f, 16f, 16f, 0xFFE0E6ED.toInt(), 1.0f, moonAlpha)
            batch.drawPixelRect(moonX - 10f, moonY - 6f, 20f, 12f, 0xFFCFD8DC.toInt(), 1.0f, moonAlpha * 0.8f)
            batch.drawPixelRect(moonX - 6f, moonY - 10f, 12f, 20f, 0xFFCFD8DC.toInt(), 1.0f, moonAlpha * 0.8f)

            // Distinct lunar crater texture (Maria & crater spots)
            batch.drawPixelRect(moonX - 4f, moonY - 4f, 4f, 4f, 0xFF90A4AE.toInt(), 1.0f, moonAlpha)
            batch.drawPixelRect(moonX + 2f, moonY - 2f, 3f, 3f, 0xFF78909C.toInt(), 1.0f, moonAlpha)
            batch.drawPixelRect(moonX - 3f, moonY + 2f, 5f, 3f, 0xFF90A4AE.toInt(), 1.0f, moonAlpha)
            batch.drawPixelRect(moonX - 6f, moonY - 6f, 3f, 3f, 0xFFECEFF1.toInt(), 1.0f, moonAlpha * 0.9f)
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
