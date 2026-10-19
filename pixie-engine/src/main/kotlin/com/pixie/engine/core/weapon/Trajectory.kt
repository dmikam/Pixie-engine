package com.pixie.engine.core.weapon

import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Trajectory strategy defining how a projectile moves through virtual world space.
 */
sealed interface Trajectory {

    /**
     * Updates velocity and/or position for this trajectory step.
     */
    fun computeVelocity(
        currentPosition: Vec2,
        currentVelocity: Vec2,
        flightTime: Float,
        speed: Float,
        dt: Float
    ): Vec2

    /**
     * Straight linear trajectory along [angleRad].
     */
    data class Linear(val angleRad: Float) : Trajectory {
        override fun computeVelocity(
            currentPosition: Vec2,
            currentVelocity: Vec2,
            flightTime: Float,
            speed: Float,
            dt: Float
        ): Vec2 {
            return Vec2(cos(angleRad) * speed, sin(angleRad) * speed)
        }
    }

    /**
     * Angled fan spread trajectory.
     */
    data class Spread(
        val baseAngleRad: Float,
        val spreadOffsetRad: Float
    ) : Trajectory {
        val effectiveAngle = baseAngleRad + spreadOffsetRad

        override fun computeVelocity(
            currentPosition: Vec2,
            currentVelocity: Vec2,
            flightTime: Float,
            speed: Float,
            dt: Float
        ): Vec2 {
            return Vec2(cos(effectiveAngle) * speed, sin(effectiveAngle) * speed)
        }
    }

    /**
     * Oscillating sine wave trajectory (great for plasma pulses or alien lasers).
     */
    data class SineWave(
        val baseAngleRad: Float,
        val frequency: Float = 18f,
        val amplitude: Float = 60f
    ) : Trajectory {
        override fun computeVelocity(
            currentPosition: Vec2,
            currentVelocity: Vec2,
            flightTime: Float,
            speed: Float,
            dt: Float
        ): Vec2 {
            // Forward component
            val fwdX = cos(baseAngleRad) * speed
            val fwdY = sin(baseAngleRad) * speed

            // Perpendicular wave oscillation
            val perpAngle = baseAngleRad + 1.5707963f
            val waveOffsetVel = cos(flightTime * frequency) * amplitude
            val waveX = cos(perpAngle) * waveOffsetVel
            val waveY = sin(perpAngle) * waveOffsetVel

            return Vec2(fwdX + waveX, fwdY + waveY)
        }
    }

    /**
     * Target-seeking homing missile trajectory with smooth turning rate clamp.
     */
    class Homing(
        var target: PixieObject? = null,
        val turnRateRadPerSec: Float = 4.5f,
        var currentHeadingRad: Float = -1.5707963f
    ) : Trajectory {
        override fun computeVelocity(
            currentPosition: Vec2,
            currentVelocity: Vec2,
            flightTime: Float,
            speed: Float,
            dt: Float
        ): Vec2 {
            val t = target
            if (t != null && t.isAlive) {
                val targetCenter = t.position + (t.size * 0.5f)
                val desiredAngle = atan2(targetCenter.y - currentPosition.y, targetCenter.x - currentPosition.x)

                // Compute shortest angle difference
                var angleDiff = desiredAngle - currentHeadingRad
                while (angleDiff < -Math.PI.toFloat()) angleDiff += 6.2831853f
                while (angleDiff > Math.PI.toFloat()) angleDiff -= 6.2831853f

                val maxTurn = turnRateRadPerSec * dt
                val step = angleDiff.coerceIn(-maxTurn, maxTurn)
                currentHeadingRad += step
            }

            return Vec2(cos(currentHeadingRad) * speed, sin(currentHeadingRad) * speed)
        }
    }

    /**
     * Accelerating linear trajectory (starts at initialSpeed, ramps up by acceleration * time).
     */
    data class Accelerating(
        val angleRad: Float,
        val acceleration: Float = 200f
    ) : Trajectory {
        override fun computeVelocity(
            currentPosition: Vec2,
            currentVelocity: Vec2,
            flightTime: Float,
            speed: Float,
            dt: Float
        ): Vec2 {
            val currentSpeed = speed + acceleration * flightTime
            return Vec2(cos(angleRad) * currentSpeed, sin(angleRad) * currentSpeed)
        }
    }
}
