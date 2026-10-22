package com.pixie.engine.core.entity

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2

/**
 * Core engine base class for hostile AI combatants, drones, minions, and bosses.
 *
 * Encapsulates common enemy lifecycle behaviors:
 * - Dynamic health scaling and damage reception
 * - Score awards upon defeat
 * - Contact damage to player characters
 * - Reward drop coordination hook
 */
open class PixieEnemy(
    id: String,
    position: Vec2 = Vec2.ZERO,
    size: Vec2 = Vec2(16f, 16f),
    initialHealth: Float = 20f,
    var scoreAward: Long = 100L,
    var contactDamage: Float = 10f,
    initialClasses: Set<String> = emptySet(),
    initialGroups: Set<String> = emptySet()
) : PixieObject(
    id = id,
    position = position,
    size = size,
    initialClasses = initialClasses + setOf("enemy", "hostile"),
    initialGroups = initialGroups
) {
    var maxHealth: Float = initialHealth
    var health: Float = initialHealth
    var isDefeated: Boolean = false

    /**
     * Applies damage to the enemy.
     * @return true if this damage resulted in the enemy's defeat, false otherwise.
     */
    open fun takeDamage(amount: Float, context: GameContext? = null): Boolean {
        if (!isAlive || isDefeated || amount <= 0f) return false

        health = (health - amount).coerceAtLeast(0f)
        if (health <= 0f) {
            isDefeated = true
            onDefeated(context)
            return true
        }
        return false
    }

    /**
     * Called when the enemy is defeated (health reaches 0).
     */
    open fun onDefeated(context: GameContext? = null) {
        isAlive = false
    }

    override fun onCollision(other: PixieObject, context: GameContext) {
        if (!isAlive || isDefeated) return

        if (other is PixiePlayer && other.isAlive && !other.isInvulnerable) {
            other.takeDamage(contactDamage, context)
        }
    }
}
