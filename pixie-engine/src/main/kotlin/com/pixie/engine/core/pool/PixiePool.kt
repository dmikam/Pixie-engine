package com.pixie.engine.core.pool

/**
 * Interface implemented by objects managed in zero-allocation pools (projectiles, particles, damage popups).
 */
interface Poolable {
    /**
     * Resets internal state to neutral/default values before returning to the pool.
     */
    fun reset()
}

/**
 * High-performance, zero-allocation object pool engineered for 60 FPS mobile arcade gameplay.
 *
 * Avoids Java/Kotlin garbage collector churn during high-frequency spawning and destruction
 * of bullets, sparks, explosions, and debris.
 */
class PixiePool<T : Poolable>(
    val initialCapacity: Int = 64,
    val maxCapacity: Int = 2048,
    private val factory: () -> T
) {
    // ArrayDeque provides efficient O(1) push/pop with minimal overhead
    private val freeList = ArrayDeque<T>(initialCapacity)

    init {
        // Pre-warm the pool
        for (i in 0 until initialCapacity) {
            freeList.addLast(factory())
        }
    }

    val freeCount: Int
        get() = freeList.size

    /**
     * Obtains a recycled object from the pool, or instantiates a new one if the pool is exhausted.
     */
    fun obtain(): T {
        return if (freeList.isNotEmpty()) {
            freeList.removeLast()
        } else {
            factory()
        }
    }

    /**
     * Recycles an object back into the pool after resetting its state.
     */
    fun recycle(item: T) {
        item.reset()
        if (freeList.size < maxCapacity) {
            freeList.addLast(item)
        }
    }

    /**
     * Batch recycles a collection of objects.
     */
    fun recycleAll(items: Iterable<T>) {
        for (item in items) {
            recycle(item)
        }
    }

    /**
     * Clears all free pooled instances.
     */
    fun clear() {
        freeList.clear()
    }
}
