package com.pixie.engine.core.entity

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2

/**
 * Core engine base class for power-ups, coins, cargo pods, gems, and ammo packs.
 *
 * Encapsulates:
 * - Non-solid trigger collection mechanics
 * - Magnetic attraction towards target entities (player, drone)
 * - Safe collection lifecycle hooks
 */
abstract class PixieCollectible(
    id: String,
    position: Vec2 = Vec2.ZERO,
    size: Vec2 = Vec2(12f, 12f),
    var defaultMagnetRange: Float = 60f,
    var defaultPullSpeed: Float = 140f,
    initialClasses: Set<String> = emptySet(),
    initialGroups: Set<String> = emptySet()
) : PixieObject(
    id = id,
    position = position,
    size = size,
    initialClasses = initialClasses + setOf("collectible", "pickup"),
    initialGroups = initialGroups
) {
    var isCollected: Boolean = false

    init {
        // Collectibles are gathered via trigger overlap, not physics blocking
        isSolid = false
    }

    /**
     * Magnet attraction towards an entity's position.
     */
    open fun attractTowards(
        targetPos: Vec2,
        dt: Float,
        range: Float = defaultMagnetRange,
        pullSpeed: Float = defaultPullSpeed
    ) {
        if (!isAlive || isCollected) return

        val delta = targetPos - position
        val distSq = delta.lengthSquared()
        val rangeSq = range * range

        if (distSq < rangeSq && distSq > 4f) {
            val pull = delta.normalized() * pullSpeed
            position += pull * dt
        }
    }

    /**
     * Attempts collection by a player entity.
     * @return true if collection succeeded, false if already collected or invalid.
     */
    open fun collect(collector: PixieObject, context: GameContext): Boolean {
        if (!isAlive || isCollected) return false

        isCollected = true
        isAlive = false
        onCollected(collector, context)
        return true
    }

    /**
     * Subclasses implement the exact gameplay benefit applied to the collector.
     */
    abstract fun onCollected(collector: PixieObject, context: GameContext)
}
