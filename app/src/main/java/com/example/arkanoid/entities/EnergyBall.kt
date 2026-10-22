package com.example.arkanoid.entities

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.matrix.BrickHitResult
import com.pixie.engine.core.matrix.BrickType
import com.pixie.engine.core.matrix.DestructibleMatrixLayer
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.physics.ContinuousCollision
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.ports.RenderBatchPort
import kotlin.math.min

/**
 * High-velocity energy ball entity using Continuous Collision Detection (CCD).
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

    fun launch(baseSpeed: Float, headingAngle: Float = -1.57f) {
        isDocked = false
        speed = baseSpeed
        velocity = Vec2(
            kotlin.math.cos(headingAngle),
            kotlin.math.sin(headingAngle)
        ).normalized() * speed
    }

    fun makePlasma(duration: Float = 8f) {
        isPlasma = true
        plasmaTimer = duration
    }

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
            return
        }

        if (isPlasma) {
            plasmaTimer -= dt
            if (plasmaTimer <= 0f) isPlasma = false
        }

        // Sub-stepped physics integration
        val steps = 3
        val stepDt = dt / steps

        for (step in 0 until steps) {
            if (!isAlive) break

            // 1. Check Collision with Arena Walls
            // Left Wall
            if (position.x - radius <= arenaBounds.left && velocity.x < 0f) {
                position = Vec2(arenaBounds.left + radius, position.y)
                velocity = Vec2(-velocity.x, velocity.y)
                audioPort.playSound(AudioEvent.SHIELD_HIT)
                particleLayer.spawnExplosion(ExplosionType.SPARK_HIT, position, scale = 0.6f)
            }
            // Right Wall
            if (position.x + radius >= arenaBounds.right && velocity.x > 0f) {
                position = Vec2(arenaBounds.right - radius, position.y)
                velocity = Vec2(-velocity.x, velocity.y)
                audioPort.playSound(AudioEvent.SHIELD_HIT)
                particleLayer.spawnExplosion(ExplosionType.SPARK_HIT, position, scale = 0.6f)
            }
            // Top Ceiling
            if (position.y - radius <= arenaBounds.top && velocity.y < 0f) {
                position = Vec2(position.x, arenaBounds.top + radius)
                velocity = Vec2(velocity.x, -velocity.y)
                audioPort.playSound(AudioEvent.SHIELD_HIT)
                particleLayer.spawnExplosion(ExplosionType.SPARK_HIT, position, scale = 0.6f)
            }
            // Bottom Hazard / Barrier
            if (position.y + radius >= arenaBounds.bottom) {
                if (paddle.hasBarrier) {
                    paddle.hasBarrier = false
                    position = Vec2(position.x, arenaBounds.bottom - radius - 2f)
                    velocity = Vec2(velocity.x, -kotlin.math.abs(velocity.y))
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

                if (paddleHit.hasHit) {
                    // Position at point of impact
                    position = position + velocity * (stepDt * paddleHit.timeOfImpact)

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
            var brickHitOccurred = false
            val gridCoords = matrix.worldToGrid(position.x, position.y)
            val centerRow = gridCoords?.first ?: ((position.y - matrix.originY) / (matrix.brickHeight + matrix.gap)).toInt()
            val centerCol = gridCoords?.second ?: ((position.x - matrix.originX) / (matrix.brickWidth + matrix.gap)).toInt()

            // Sweep nearby 3x3 brick neighborhood
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

                    if (sweepRes.hasHit) {
                        // Impact brick
                        position = position + velocity * (stepDt * sweepRes.timeOfImpact)
                        if (!isPlasma || cell.type == BrickType.UNBREAKABLE) {
                            velocity = velocity.reflect(sweepRes.normal)
                        }

                        val hitResult = matrix.hitCell(r, c, damage = if (isPlasma) 2 else 1)
                        if (hitResult.wasDestroyed) {
                            if (cell.type == BrickType.EXPLOSIVE) {
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
                        brickHitOccurred = true
                        break
                    }
                }
                if (brickHitOccurred) break
            }

            if (!brickHitOccurred) {
                // Free travel
                position = position + velocity * stepDt
            }
        }
    }

    fun render(batch: RenderBatchPort, camera: PixieCamera) {
        if (!isAlive) return

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
