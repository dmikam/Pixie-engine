package com.pixie.engine.core.particle

import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.pool.Poolable

/**
 * High-performance, pooled particle representation for procedural retro explosions,
 * bullet sparks, smoke plumes, thruster trails, and plasma bursts.
 */
class Particle : Poolable {
    var position: Vec2 = Vec2.ZERO
    var velocity: Vec2 = Vec2.ZERO
    var acceleration: Vec2 = Vec2.ZERO
    var drag: Float = 0f // Friction damping per second (0 = none, 5 = heavy drag)
    var life: Float = 0f
    var maxLife: Float = 1.0f
    var size: Float = 2.0f
    var startColor: Int = 0xFFFFFFFF.toInt()
    var endColor: Int = 0x00FFFFFF
    var currentColor: Int = 0xFFFFFFFF.toInt()
    var alpha: Float = 1.0f
    var isAlive: Boolean = false

    fun spawn(
        pos: Vec2,
        vel: Vec2,
        accel: Vec2 = Vec2.ZERO,
        lifespan: Float,
        particleSize: Float,
        startClr: Int,
        endClr: Int,
        linearDrag: Float = 0f
    ) {
        position = pos
        velocity = vel
        acceleration = accel
        drag = linearDrag
        maxLife = lifespan.coerceAtLeast(0.01f)
        life = maxLife
        size = particleSize
        startColor = startClr
        endColor = endClr
        currentColor = startClr
        alpha = 1.0f
        isAlive = true
    }

    /**
     * Updates physics and life progression. Returns true if particle is still active.
     */
    fun update(dt: Float): Boolean {
        if (!isAlive) return false

        life -= dt
        if (life <= 0f) {
            isAlive = false
            return false
        }

        // Apply physics
        velocity += acceleration * dt
        if (drag > 0f) {
            val damping = (1f - drag * dt).coerceIn(0f, 1f)
            velocity *= damping
        }
        position += velocity * dt

        // Normalized progress [0f..1f], 0 = just born, 1 = dead
        val progress = 1.0f - (life / maxLife).coerceIn(0f, 1f)

        // Interpolate alpha and colors
        alpha = (1.0f - progress).coerceIn(0f, 1f)
        currentColor = interpolateColor(startColor, endColor, progress)

        return true
    }

    override fun reset() {
        position = Vec2.ZERO
        velocity = Vec2.ZERO
        acceleration = Vec2.ZERO
        drag = 0f
        life = 0f
        maxLife = 1.0f
        size = 2.0f
        startColor = 0xFFFFFFFF.toInt()
        endColor = 0x00FFFFFF
        currentColor = 0xFFFFFFFF.toInt()
        alpha = 1.0f
        isAlive = false
    }

    companion object {
        /**
         * Fast ARGB integer color blend.
         */
        fun interpolateColor(colorA: Int, colorB: Int, factor: Float): Int {
            val f = factor.coerceIn(0f, 1f)
            val aA = (colorA ushr 24) and 0xFF
            val rA = (colorA ushr 16) and 0xFF
            val gA = (colorA ushr 8) and 0xFF
            val bA = colorA and 0xFF

            val aB = (colorB ushr 24) and 0xFF
            val rB = (colorB ushr 16) and 0xFF
            val gB = (colorB ushr 8) and 0xFF
            val bB = colorB and 0xFF

            val a = (aA + ((aB - aA) * f)).toInt()
            val r = (rA + ((rB - rA) * f)).toInt()
            val g = (gA + ((gB - gA) * f)).toInt()
            val b = (bA + ((bB - bA) * f)).toInt()

            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
}
