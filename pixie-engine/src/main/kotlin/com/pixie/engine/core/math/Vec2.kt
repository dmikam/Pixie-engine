package com.pixie.engine.core.math

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Immutable 2D vector for high-performance pure Kotlin physics and coordinate geometry.
 */
data class Vec2(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float): Vec2 = Vec2(x * scalar, y * scalar)
    operator fun div(scalar: Float): Vec2 = if (scalar != 0f) Vec2(x / scalar, y / scalar) else ZERO

    fun length(): Float = sqrt(x * x + y * y)
    fun lengthSquared(): Float = x * x + y * y

    fun normalized(): Vec2 {
        val len = length()
        return if (len > 0.0001f) Vec2(x / len, y / len) else ZERO
    }

    fun lerp(target: Vec2, factor: Float): Vec2 {
        val clamped = factor.coerceIn(0f, 1f)
        return Vec2(x + (target.x - x) * clamped, y + (target.y - y) * clamped)
    }

    companion object {
        val ZERO = Vec2(0f, 0f)
        val ONE = Vec2(1f, 1f)
        val UP = Vec2(0f, -1f)
        val DOWN = Vec2(0f, 1f)
        val LEFT = Vec2(-1f, 0f)
        val RIGHT = Vec2(1f, 0f)
    }
}

/**
 * Axis-Aligned Bounding Box (AABB) for 2D collision detection and trigger volumes.
 */
data class RectF(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = max(0f, right - left)
    val height: Float get() = max(0f, bottom - top)
    val centerX: Float get() = (left + right) * 0.5f
    val centerY: Float get() = (top + bottom) * 0.5f

    fun intersects(other: RectF): Boolean {
        return left < other.right && right > other.left && top < other.bottom && bottom > other.top
    }

    fun contains(x: Float, y: Float): Boolean {
        return x in left..right && y in top..bottom
    }

    fun offset(dx: Float, dy: Float): RectF {
        return RectF(left + dx, top + dy, right + dx, bottom + dy)
    }

    companion object {
        fun fromCenter(centerX: Float, centerY: Float, width: Float, height: Float): RectF {
            val hw = width * 0.5f
            val hh = height * 0.5f
            return RectF(centerX - hw, centerY - hh, centerX + hw, centerY + hh)
        }

        fun fromTopLeft(x: Float, y: Float, width: Float, height: Float): RectF {
            return RectF(x, y, x + width, y + height)
        }
    }
}
