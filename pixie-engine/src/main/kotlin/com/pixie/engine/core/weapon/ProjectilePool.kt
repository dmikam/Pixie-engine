package com.pixie.engine.core.weapon

import com.pixie.engine.core.pool.PixiePool

/**
 * Dedicated pool for high-speed projectiles in Pixie Engine.
 *
 * Pre-warms 128 projectiles (expandable up to 2048) ensuring 100% zero runtime GC allocation.
 */
class ProjectilePool(
    initialCapacity: Int = 128,
    maxCapacity: Int = 2048
) {
    private val pool = PixiePool(initialCapacity, maxCapacity) { Projectile() }

    val freeCount: Int
        get() = pool.freeCount

    fun obtain(): Projectile = pool.obtain()

    fun recycle(projectile: Projectile) {
        pool.recycle(projectile)
    }

    fun recycleAll(projectiles: Iterable<Projectile>) {
        pool.recycleAll(projectiles)
    }

    fun clear() {
        pool.clear()
    }
}
