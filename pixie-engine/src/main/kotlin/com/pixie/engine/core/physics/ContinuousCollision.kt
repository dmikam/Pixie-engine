package com.pixie.engine.core.physics

import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Result data contract for continuous collision queries.
 */
data class ContinuousCollisionResult(
    val hasHit: Boolean,
    val timeOfImpact: Float = 1f, // Normalized 0f..1f within current dt
    val normal: Vec2 = Vec2.ZERO,
    val contactPoint: Vec2 = Vec2.ZERO
) {
    companion object {
        val NO_HIT = ContinuousCollisionResult(hasHit = false)
    }
}

/**
 * High-performance Continuous Collision Detection (CCD) physics engine for Pixie Engine.
 *
 * Implements swept circle-to-AABB raycasting and sub-stepped trajectory integration
 * to completely eliminate tunneling artifacts at high velocities.
 */
object ContinuousCollision {

    /**
     * Continuous ray-swept collision query between a moving circle and an Axis-Aligned Bounding Box (AABB).
     *
     * @param center Start position of circle center
     * @param radius Circle radius
     * @param velocity Movement velocity in pixels per second
     * @param dt Time step duration in seconds
     * @param box Target AABB obstacle (e.g. brick or paddle)
     */
    fun sweepCircleVsAABB(
        center: Vec2,
        radius: Float,
        velocity: Vec2,
        dt: Float,
        box: RectF
    ): ContinuousCollisionResult {
        val movement = velocity * dt
        val moveLenSq = movement.lengthSquared()

        // 1. Initial Overlap Check (if already penetrating)
        val expandedBox = RectF(
            box.left - radius,
            box.top - radius,
            box.right + radius,
            box.bottom + radius
        )

        if (expandedBox.contains(center.x, center.y)) {
            // Find minimum penetration axis to resolve normal
            val distLeft = abs(center.x - expandedBox.left)
            val distRight = abs(expandedBox.right - center.x)
            val distTop = abs(center.y - expandedBox.top)
            val distBottom = abs(expandedBox.bottom - center.y)

            val minDist = min(min(distLeft, distRight), min(distTop, distBottom))
            val normal = when (minDist) {
                distLeft -> Vec2.LEFT
                distRight -> Vec2.RIGHT
                distTop -> Vec2.UP
                else -> Vec2.DOWN
            }
            return ContinuousCollisionResult(
                hasHit = true,
                timeOfImpact = 0f,
                normal = normal,
                contactPoint = center
            )
        }

        if (moveLenSq < 0.00001f) {
            return ContinuousCollisionResult.NO_HIT
        }

        // 2. Raycast from center to center + movement against expandedBox
        var tMinX = 0f
        var tMaxX = 1f
        var normalX = Vec2.ZERO

        if (abs(movement.x) > 0.0001f) {
            val invDx = 1f / movement.x
            var t1 = (expandedBox.left - center.x) * invDx
            var t2 = (expandedBox.right - center.x) * invDx
            var signX = -1f

            if (t1 > t2) {
                val temp = t1
                t1 = t2
                t2 = temp
                signX = 1f
            }

            tMinX = t1
            tMaxX = t2
            normalX = Vec2(signX, 0f)
        } else {
            if (center.x < expandedBox.left || center.x > expandedBox.right) {
                return ContinuousCollisionResult.NO_HIT
            }
            tMinX = Float.NEGATIVE_INFINITY
            tMaxX = Float.POSITIVE_INFINITY
        }

        var tMinY = 0f
        var tMaxY = 1f
        var normalY = Vec2.ZERO

        if (abs(movement.y) > 0.0001f) {
            val invDy = 1f / movement.y
            var t1 = (expandedBox.top - center.y) * invDy
            var t2 = (expandedBox.bottom - center.y) * invDy
            var signY = -1f

            if (t1 > t2) {
                val temp = t1
                t1 = t2
                t2 = temp
                signY = 1f
            }

            tMinY = t1
            tMaxY = t2
            normalY = Vec2(0f, signY)
        } else {
            if (center.y < expandedBox.top || center.y > expandedBox.bottom) {
                return ContinuousCollisionResult.NO_HIT
            }
            tMinY = Float.NEGATIVE_INFINITY
            tMaxY = Float.POSITIVE_INFINITY
        }

        val tNear = max(tMinX, tMinY)
        val tFar = min(tMaxX, tMaxY)

        if (tNear > tFar || tFar < 0f || tNear > 1f) {
            return ContinuousCollisionResult.NO_HIT
        }

        val hitTime = tNear.coerceIn(0f, 1f)
        val hitNormal = if (tMinX > tMinY) normalX else normalY
        val contactPos = center + movement * hitTime

        return ContinuousCollisionResult(
            hasHit = true,
            timeOfImpact = hitTime,
            normal = hitNormal,
            contactPoint = contactPos
        )
    }

    /**
     * Executes sub-stepped trajectory integration for high-speed physics objects.
     */
    fun integrateSubSteps(
        startPos: Vec2,
        velocity: Vec2,
        radius: Float,
        dt: Float,
        steps: Int = 4,
        collisionQuery: (pos: Vec2, vel: Vec2, stepDt: Float) -> ContinuousCollisionResult?
    ): Pair<Vec2, Vec2> {
        var currentPos = startPos
        var currentVel = velocity
        val stepDt = dt / steps.coerceAtLeast(1)

        for (i in 0 until steps) {
            val hit = collisionQuery(currentPos, currentVel, stepDt)
            if (hit != null && hit.hasHit) {
                // Advance to impact point
                currentPos = currentPos + currentVel * (stepDt * hit.timeOfImpact)
                // Reflect velocity
                currentVel = currentVel.reflect(hit.normal)
                // Small push along normal to prevent re-collision
                currentPos = currentPos + hit.normal * 0.1f
                // Remaining step time
                val remainingDt = stepDt * (1f - hit.timeOfImpact)
                currentPos = currentPos + currentVel * remainingDt
            } else {
                currentPos = currentPos + currentVel * stepDt
            }
        }

        return Pair(currentPos, currentVel)
    }
}
