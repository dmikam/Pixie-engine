package com.pixie.engine.core.entity

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2

/**
 * Core engine base class for static or scrolling barriers, destructible walls, turrets, and hazards.
 *
 * Encapsulates:
 * - Destructibility flags and health
 * - Collision damage inflicted upon players
 * - Destruction lifecycle hook
 */
open class PixieObstacle(
    id: String,
    position: Vec2 = Vec2.ZERO,
    size: Vec2 = Vec2(16f, 16f),
    val isDestructible: Boolean = true,
    initialHealth: Float = 30f,
    var collisionDamage: Float = 15f,
    initialClasses: Set<String> = emptySet(),
    initialGroups: Set<String> = emptySet()
) : PixieObject(
    id = id,
    position = position,
    size = size,
    initialClasses = initialClasses + setOf("obstacle", "hazard"),
    initialGroups = initialGroups
) {
    var maxHealth: Float = initialHealth
    var health: Float = initialHealth
    var isDestroyed: Boolean = false

    /**
     * Applies damage if the obstacle is destructible.
     * @return true if the obstacle was destroyed by this hit, false otherwise.
     */
    open fun takeDamage(amount: Float, context: GameContext? = null): Boolean {
        if (!isDestructible || !isAlive || isDestroyed || amount <= 0f) return false

        health = (health - amount).coerceAtLeast(0f)
        if (health <= 0f) {
            isDestroyed = true
            onDestroyed(context)
            return true
        }
        return false
    }

    /**
     * Hook triggered when a destructible obstacle is destroyed.
     */
    open fun onDestroyed(context: GameContext? = null) {
        isSolid = false
    }

    override fun onCollision(other: PixieObject, context: GameContext) {
        if (!isAlive || isDestroyed) return

        if (other is PixiePlayer && other.isAlive && !other.isInvulnerable) {
            other.takeDamage(collisionDamage, context)
        }
    }
}
