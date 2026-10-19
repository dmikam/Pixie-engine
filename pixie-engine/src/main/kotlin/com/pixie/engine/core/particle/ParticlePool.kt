package com.pixie.engine.core.particle

import com.pixie.engine.core.pool.PixiePool

/**
 * Dedicated pool for particles in Pixie Engine.
 *
 * Pre-warms 256 particles (expands up to 2048) to guarantee zero GC stutter during intense bullet hell
 * and multi-stage procedural explosions.
 */
class ParticlePool(
    initialCapacity: Int = 256,
    maxCapacity: Int = 2048
) {
    private val pool = PixiePool(initialCapacity, maxCapacity) { Particle() }

    val freeCount: Int
        get() = pool.freeCount

    fun obtain(): Particle = pool.obtain()

    fun recycle(particle: Particle) {
        pool.recycle(particle)
    }

    fun recycleAll(particles: Iterable<Particle>) {
        pool.recycleAll(particles)
    }

    fun clear() {
        pool.clear()
    }
}
