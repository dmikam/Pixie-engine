package com.pixie.engine.core.entity

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2

/**
 * Core engine base class for player characters, ships, or controllable heroes.
 *
 * Encapsulates common player mechanics to eliminate game-specific boilerplate:
 * - Health and max health management
 * - Life count and score tracking
 * - Invulnerability frames (i-frames) with blinking visibility modulation
 * - Healing and damage lifecycle hooks
 */
open class PixiePlayer(
    id: String,
    position: Vec2 = Vec2.ZERO,
    size: Vec2 = Vec2(16f, 16f),
    initialHealth: Float = 100f,
    initialLives: Int = 3,
    initialClasses: Set<String> = emptySet(),
    initialGroups: Set<String> = emptySet()
) : PixieObject(
    id = id,
    position = position,
    size = size,
    initialClasses = initialClasses + setOf("player", "controllable"),
    initialGroups = initialGroups
) {
    var maxHealth: Float = initialHealth
    var health: Float = initialHealth
    var lives: Int = initialLives

    var isInvulnerable: Boolean = false
    var invulnerabilityTimer: Float = 0f
    var blinkFrequency: Float = 16f // Blinks per second when invulnerable

    open fun grantInvulnerability(duration: Float) {
        invulnerabilityTimer = duration
        isInvulnerable = true
    }

    /**
     * Applies damage to the player, respecting invulnerability frames.
     * @return true if damage was taken, false if blocked by invulnerability or already dead.
     */
    open fun takeDamage(amount: Float, context: GameContext? = null): Boolean {
        if (!isAlive || isInvulnerable || amount <= 0f) return false

        health = (health - amount).coerceAtLeast(0f)
        if (health <= 0f) {
            onDeath(context)
        }
        return true
    }

    open fun heal(amount: Float) {
        if (!isAlive || amount <= 0f) return
        health = (health + amount).coerceAtMost(maxHealth)
    }

    open fun onDeath(context: GameContext? = null) {
        isAlive = false
    }

    override fun update(dt: Float, context: GameContext) {
        if (invulnerabilityTimer > 0f) {
            invulnerabilityTimer = (invulnerabilityTimer - dt).coerceAtLeast(0f)
            isInvulnerable = invulnerabilityTimer > 0f

            // Visual blink effect during invulnerability
            if (isInvulnerable) {
                val cycle = (invulnerabilityTimer * blinkFrequency).toInt() % 2
                isVisible = (cycle == 0)
            } else {
                isVisible = true
            }
        } else {
            isInvulnerable = false
            isVisible = true
        }

        super.update(dt, context)
    }
}
