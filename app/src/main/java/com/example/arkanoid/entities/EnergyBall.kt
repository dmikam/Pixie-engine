package com.example.arkanoid.entities

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.matrix.BrickCell
import com.pixie.engine.core.matrix.BrickHitResult
import com.pixie.engine.core.matrix.BrickType
import com.pixie.engine.core.matrix.DestructibleMatrixLayer
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.physics.ContinuousCollision
import com.pixie.engine.core.physics.ContinuousCollisionResult
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.ports.RenderBatchPort
import kotlin.math.abs
import kotlin.math.min

/**
 * High-velocity energy ball entity using Continuous Collision Detection (CCD),
 * motion blur trail rendering, and corner tunneling prevention.
 */
class EnergyBall(
    var position: Vec2 = Vec2(160f, 155f),
    var velocity: Vec2 = Vec2.ZERO,
    val radius: Float = 2.5f
) {
    var isDocked: Boolean = true
    var dockOffset: Float = 0f
    var speed: Float = 130f
    var isPlasma: Boolean = false
    var plasmaTimer: Float = 0f
    var isAlive: Boolean = true

    // Motion blur history samples (most recent at index 0)
    val trailHistory = ArrayDeque<Vec2>()
    private val maxTrailSamples = 5

    fun launch(baseSpeed: Float, headingAngle: Float = -1.57f) {
        isDocked = false
        speed = baseSpeed
        velocity = Vec2(
            kotlin.math.cos(headingAngle),
            kotlin.math.sin(headingAngle)
        ).normalized() * speed
        trailHistory.clear()
    }

    fun makePlasma(duration: Float = 8f) {
        isPlasma = true
        plasmaTimer = duration
    }

    private data class BrickCandidate(
        val row: Int,
        val col: Int,
        val cell: BrickCell,
        val hit: ContinuousCollisionResult
    )

    fun update(
        dt: Float,
        paddle: ArkanoidPaddle,
        matrix: DestructibleMatrixLayer,
        arenaBounds: RectF,
        audioPort: AudioPort,
        particleLayer: ParticleLayer,
        onPaddleBounce: () -> Unit,
        onBrickHit: (BrickHitResult) -> Unit,
        onBallLost: (EnergyBall) -> Unit
    ) {
        if (!isAlive) return

        if (isDocked) {
            position = Vec2(paddle.x + dockOffset, paddle.y - radius - 1f)
            trailHistory.clear()
            return
        }

        // Record motion blur trail sample
        trailHistory.addFirst(position)
        if (trailHistory.size > maxTrailSamples) {
            trailHistory.removeLast()
        }

        if (isPlasma) {
            plasmaTimer -= dt
            if (plasmaTimer <= 0f) isPlasma = false
        }

        // Sub-stepped physics integration
        val steps = 4
        val stepDt = dt / steps

        for (step in 0 until steps) {
            if (!isAlive) break

            // 1. Check Collision with Arena Walls
            // Left Wall
            if (position.x - radius <= arenaBounds.left && velocity.x < 0f) {
                position = Vec2(arenaBounds.left + radius + 0.5f, position.y)
                velocity = Vec2(abs(velocity.x), velocity.y)
                audioPort.playSound(AudioEvent.SHIELD_HIT)
                particleLayer.spawnExplosion(ExplosionType.SPARK_HIT, position, scale = 0.6f)
            }
            // Right Wall
            if (position.x + radius >= arenaBounds.right && velocity.x > 0f) {
                position = Vec2(arenaBounds.right - radius - 0.5f, position.y)
                velocity = Vec2(-abs(velocity.x), velocity.y)
                audioPort.playSound(AudioEvent.SHIELD_HIT)
                particleLayer.spawnExplosion(ExplosionType.SPARK_HIT, position, scale = 0.6f)
            }
            // Top Ceiling
            if (position.y - radius <= arenaBounds.top && velocity.y < 0f) {
                position = Vec2(position.x, arenaBounds.top + radius + 0.5f)
                velocity = Vec2(velocity.x, abs(velocity.y))
                audioPort.playSound(AudioEvent.SHIELD_HIT)
                particleLayer.spawnExplosion(ExplosionType.SPARK_HIT, position, scale = 0.6f)
            }
            // Bottom Hazard / Barrier
            if (position.y + radius >= arenaBounds.bottom) {
                if (paddle.hasBarrier) {
                    paddle.hasBarrier = false
                    position = Vec2(position.x, arenaBounds.bottom - radius - 2f)
                    velocity = Vec2(velocity.x, -abs(velocity.y))
                    audioPort.playSound(AudioEvent.SHIELD_HIT)
                    particleLayer.spawnExplosion(ExplosionType.PLASMA_BURST, position, scale = 1.0f)
                } else {
                    isAlive = false
                    onBallLost(this)
                    return
                }
            }

            // 2. Check Collision with Paddle (Swept Circle vs Paddle AABB)
            if (velocity.y > 0f) {
                val paddleHit = ContinuousCollision.sweepCircleVsAABB(
                    center = position,
                    radius = radius,
                    velocity = velocity,
                    dt = stepDt,
                    box = paddle.boundingBox
                )

                if (paddleHit.hasHit && velocity.dot(paddleHit.normal) < 0f) {
                    // Position at point of impact + nudge above paddle
                    position = position + velocity * (stepDt * paddleHit.timeOfImpact) + Vec2(0f, -0.6f)

                    // Modulated rebound angle
                    val newDir = paddle.calculateBounceAngle(position.x)
                    // Slightly increase ball speed on each paddle rally
                    speed = min(speed + 3.0f, 260f)
                    velocity = newDir * speed

                    audioPort.playSound(AudioEvent.JUMP)
                    particleLayer.spawnExplosion(ExplosionType.SPARK_HIT, position, scale = 0.8f)
                    onPaddleBounce()
                    continue
                }
            }

            // 3. Check Collision with Destructible Bricks
            val gridCoords = matrix.worldToGrid(position.x, position.y)
            val centerRow = gridCoords?.first ?: ((position.y - matrix.originY) / (matrix.brickHeight + matrix.gap)).toInt()
            val centerCol = gridCoords?.second ?: ((position.x - matrix.originX) / (matrix.brickWidth + matrix.gap)).toInt()

            // Collect all candidate bricks that intersect trajectory during this sub-step
            val candidates = mutableListOf<BrickCandidate>()
            for (dr in -1..1) {
                for (dc in -1..1) {
                    val r = centerRow + dr
                    val c = centerCol + dc
                    val cell = matrix.getCell(r, c) ?: continue
                    if (cell.isDestroyed) continue

                    val brickBounds = matrix.getCellBounds(r, c)
                    val sweepRes = ContinuousCollision.sweepCircleVsAABB(
                        center = position,
                        radius = radius,
                        velocity = velocity,
                        dt = stepDt,
                        box = brickBounds
                    )

                    // Must hit AND velocity must be pointing towards the impact face normal
                    if (sweepRes.hasHit && velocity.dot(sweepRes.normal) < -0.001f) {
                        candidates.add(BrickCandidate(r, c, cell, sweepRes))
                    }
                }
            }

            if (candidates.isNotEmpty()) {
                // Find earliest collision in this sub-step
                candidates.sortBy { it.hit.timeOfImpact }
                val earliest = candidates.first()

                // Corner resolution: if another brick is hit at the exact same moment
                val simultaneous = candidates.filter {
                    abs(it.hit.timeOfImpact - earliest.hit.timeOfImpact) < 0.05f
                }

                var combinedNormal = earliest.hit.normal
                for (s in simultaneous) {
                    if (s != earliest) {
                        combinedNormal = combinedNormal + s.hit.normal
                    }
                }
                val finalNormal = combinedNormal.normalized()

                // Advance to exact impact point
                val hitTime = earliest.hit.timeOfImpact
                position = position + velocity * (stepDt * hitTime)

                // Reflect velocity if not piercing plasma or if unbreakable
                if (!isPlasma || earliest.cell.type == BrickType.UNBREAKABLE) {
                    velocity = velocity.reflect(finalNormal)
                }

                // Positional separation nudge: strictly eject the ball outside the brick!
                position = position + finalNormal * 0.8f

                // Damage brick
                val hitResult = matrix.hitCell(earliest.row, earliest.col, damage = if (isPlasma) 2 else 1)
                if (hitResult.wasDestroyed) {
                    if (earliest.cell.type == BrickType.EXPLOSIVE) {
                        audioPort.playSound(AudioEvent.EXPLOSION_LARGE)
                        particleLayer.spawnExplosion(ExplosionType.GROUND_BLAST, hitResult.hitPosition, scale = 1.3f)
                    } else {
                        audioPort.playSound(AudioEvent.EXPLOSION_SMALL)
                        particleLayer.spawnExplosion(ExplosionType.SPARK_HIT, hitResult.hitPosition, scale = 1.0f)
                    }
                } else {
                    audioPort.playSound(AudioEvent.HIT)
                    particleLayer.spawnExplosion(ExplosionType.SPARK_HIT, hitResult.hitPosition, scale = 0.5f)
                }

                onBrickHit(hitResult)

                // Advance remainder of sub-step with the newly reflected velocity moving away
                val remDt = stepDt * (1f - hitTime)
                if (remDt > 0.0001f) {
                    position = position + velocity * remDt
                }
            } else {
                // Free travel
                position = position + velocity * stepDt
            }
        }
    }

    fun render(batch: RenderBatchPort, camera: PixieCamera) {
        if (!isAlive) return

        // 1. Subtle Motion Blur Trail
        if (!isDocked && trailHistory.isNotEmpty()) {
            val trailCount = trailHistory.size
            for (i in trailHistory.indices) {
                val pastPos = trailHistory[i]
                val ghostScreen = camera.worldToScreen(pastPos)
                val ageRatio = (i + 1).toFloat() / (trailCount + 1)
                val alpha = (1.0f - ageRatio * 0.70f).coerceIn(0.12f, 0.60f)
                val trailColor = if (isPlasma) 0xFFFF9100.toInt() else 0xFF00E5FF.toInt()

                if (i <= 1) {
                    // Nearest ghosts: 2x2 px with soft glow
                    batch.drawPixelRect(ghostScreen.x - 1f, ghostScreen.y - 1f, 2f, 2f, trailColor, alpha = alpha)
                } else {
                    // Older ghosts: 1x1 px subtle trail spark
                    batch.drawPixelRect(ghostScreen.x, ghostScreen.y, 1f, 1f, trailColor, alpha = alpha * 0.75f)
                }
            }
        }

        // 2. Main Glowing Energy Ball
        val screenPos = camera.worldToScreen(position)
        val ballColor = if (isPlasma) 0xFFFF9100.toInt() else 0xFFFFFFFF.toInt()

        // Core 3x3 pixel circle
        batch.drawPixelRect(screenPos.x - 1f, screenPos.y - 1f, 3f, 3f, ballColor)
        // Outer glow cross
        batch.drawPixelRect(screenPos.x - 2f, screenPos.y, 1f, 1f, if (isPlasma) 0xFFFF3D00.toInt() else 0xFF00E5FF.toInt())
        batch.drawPixelRect(screenPos.x + 2f, screenPos.y, 1f, 1f, if (isPlasma) 0xFFFF3D00.toInt() else 0xFF00E5FF.toInt())
        batch.drawPixelRect(screenPos.x, screenPos.y - 2f, 1f, 1f, if (isPlasma) 0xFFFF3D00.toInt() else 0xFF00E5FF.toInt())
        batch.drawPixelRect(screenPos.x, screenPos.y + 2f, 1f, 1f, if (isPlasma) 0xFFFF3D00.toInt() else 0xFF00E5FF.toInt())
    }
}
