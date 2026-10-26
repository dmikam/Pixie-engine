package com.example.arkanoid.entities

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.matrix.PowerUpType
import com.pixie.engine.core.ports.RenderBatchPort
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Player-controlled energy deflector paddle.
 */
class ArkanoidPaddle(
    initialX: Float = 160f,
    val y: Float = 163f
) {
    var x: Float = initialX
    val normalWidth: Float = 44f
    val wideWidth: Float = 66f
    val height: Float = 6f

    var isWide: Boolean = false
    var wideTimer: Float = 0f

    var isLaser: Boolean = false
    var laserTimer: Float = 0f
    private var laserCooldown: Float = 0f

    var hasBarrier: Boolean = false

    val currentWidth: Float get() = if (isWide) wideWidth else normalWidth

    val boundingBox: RectF
        get() = RectF.fromCenter(x, y + height * 0.5f, currentWidth, height)

    fun reset(centerX: Float = 160f) {
        x = centerX
        isWide = false
        wideTimer = 0f
        isLaser = false
        laserTimer = 0f
        laserCooldown = 0f
        hasBarrier = false
    }

    fun applyPowerUp(type: PowerUpType) {
        when (type) {
            PowerUpType.WIDE_PADDLE -> {
                isWide = true
                wideTimer = com.example.arkanoid.world.ArkanoidPowerupCatalog.DURATION_WIDE_PADDLE
            }
            PowerUpType.LASER -> {
                isLaser = true
                laserTimer = com.example.arkanoid.world.ArkanoidPowerupCatalog.DURATION_LASER
            }
            PowerUpType.SAFETY_BARRIER -> {
                hasBarrier = true
            }
            else -> {}
        }
    }

    fun update(dt: Float, targetX: Float, minX: Float, maxX: Float) {
        // Smooth responsive follow
        x += (targetX - x) * (24f * dt).coerceIn(0f, 1f)

        // Clamp inside arena walls
        val halfW = currentWidth * 0.5f
        if (x - halfW < minX) x = minX + halfW
        if (x + halfW > maxX) x = maxX - halfW

        // Update timers
        if (isWide) {
            wideTimer -= dt
            if (wideTimer <= 0f) isWide = false
        }

        if (isLaser) {
            laserTimer -= dt
            if (laserTimer <= 0f) isLaser = false
            if (laserCooldown > 0f) laserCooldown -= dt
        }
    }

    /**
     * Calculates modulated rebound angle based on where the ball impacts the paddle.
     * Center hit: -90° (straight up).
     * Extreme Left hit: -150°.
     * Extreme Right hit: -30°.
     */
    fun calculateBounceAngle(ballCenterX: Float): Vec2 {
        val halfW = currentWidth * 0.5f
        val offset = ((ballCenterX - x) / halfW).coerceIn(-1f, 1f) // -1.0 .. +1.0

        // Map offset to angle: center (0.0) -> -90 deg (-PI/2), left (-1.0) -> -150 deg, right (+1.0) -> -30 deg
        val angleRad = -PI.toFloat() * 0.5f + (offset * 1.05f) // +/- ~60 degrees
        return Vec2(cos(angleRad), sin(angleRad)).normalized()
    }

    fun canFireLaser(): Boolean = isLaser && laserCooldown <= 0f

    fun consumeLaserFire(): Boolean {
        return if (canFireLaser()) {
            laserCooldown = 0.28f // ~3.5 shots/sec
            true
        } else false
    }

    fun render(batch: RenderBatchPort, camera: PixieCamera) {
        val box = boundingBox
        val screenPos = camera.worldToScreen(Vec2(box.left, box.top))

        val paddleColor = if (isLaser) 0xFFFF1744.toInt() else 0xFF00E5FF.toInt()

        // 1. Paddle Main Body
        batch.drawPixelRect(
            screenPos.x,
            screenPos.y,
            box.width,
            box.height,
            paddleColor
        )

        // 2. Beveled Highlight on top
        batch.drawPixelRect(
            screenPos.x,
            screenPos.y,
            box.width,
            1f,
            0xFFFFFFFF.toInt()
        )

        // 3. Central Energy Cell (Glowing Blue/Amber)
        val cellW = 8f
        batch.drawPixelRect(
            screenPos.x + (box.width - cellW) * 0.5f,
            screenPos.y + 1f,
            cellW,
            box.height - 2f,
            if (isLaser) 0xFFFFEB3B.toInt() else 0xFF00B0FF.toInt()
        )

        // 4. Laser Cannons on Tips
        if (isLaser) {
            batch.drawPixelRect(screenPos.x, screenPos.y - 2f, 3f, 3f, 0xFFFFD600.toInt())
            batch.drawPixelRect(screenPos.x + box.width - 3f, screenPos.y - 2f, 3f, 3f, 0xFFFFD600.toInt())
        }

        // 5. Safety Barrier (if active)
        if (hasBarrier) {
            val barrierPos = camera.worldToScreen(Vec2(10f, 172f))
            batch.drawPixelRect(
                barrierPos.x,
                barrierPos.y,
                300f,
                2f,
                0xFFD500F9.toInt()
            )
        }
    }
}
