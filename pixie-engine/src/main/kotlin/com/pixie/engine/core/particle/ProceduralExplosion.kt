package com.pixie.engine.core.particle

import com.pixie.engine.core.math.Vec2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural explosion archetypes supported by Pixie Engine.
 */
enum class ExplosionType {
    SPARK_HIT,
    PLASMA_BURST,
    AERIAL_EXPLOSION,
    GROUND_BLAST,
    SHOCKWAVE,
    TITAN_CHAIN
}

/**
 * Factory for procedural particle effects and explosions.
 *
 * Spawns dynamic particle clusters directly from [ParticlePool] without runtime allocations.
 */
object ProceduralExplosion {

    /**
     * Spawns a procedural explosion of the specified [type] at [position].
     */
    fun trigger(
        type: ExplosionType,
        position: Vec2,
        pool: ParticlePool,
        activeList: MutableList<Particle>,
        scale: Float = 1.0f
    ) {
        when (type) {
            ExplosionType.SPARK_HIT -> spawnSparkHit(position, pool, activeList, (6 * scale).toInt().coerceAtLeast(3))
            ExplosionType.PLASMA_BURST -> spawnPlasmaBurst(position, pool, activeList, (14 * scale).toInt().coerceAtLeast(6))
            ExplosionType.AERIAL_EXPLOSION -> spawnAerialExplosion(position, pool, activeList, (22 * scale).toInt().coerceAtLeast(10))
            ExplosionType.GROUND_BLAST -> spawnGroundBlast(position, pool, activeList, (26 * scale).toInt().coerceAtLeast(12))
            ExplosionType.SHOCKWAVE -> spawnShockwave(position, pool, activeList, (16 * scale).toInt().coerceAtLeast(8))
            ExplosionType.TITAN_CHAIN -> spawnTitanChain(position, pool, activeList, scale)
        }
    }

    /**
     * High-speed directional or omnidirectional sparks on bullet impact.
     */
    fun spawnSparkHit(
        pos: Vec2,
        pool: ParticlePool,
        activeList: MutableList<Particle>,
        count: Int = 6,
        color: Int = 0xFFFFEB3B.toInt()
    ) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 6.283185f
            val speed = Random.nextFloat() * 60f + 30f
            val vel = Vec2(cos(angle) * speed, sin(angle) * speed)
            val p = pool.obtain()
            p.spawn(
                pos = pos,
                vel = vel,
                accel = Vec2(0f, 40f), // light gravity
                lifespan = Random.nextFloat() * 0.15f + 0.1f,
                particleSize = 1.5f,
                startClr = color,
                endClr = 0x00FF9800,
                linearDrag = 4.0f
            )
            activeList.add(p)
        }
    }

    /**
     * High-energy plasma dispersion ring (cyan, magenta, electric blue).
     */
    fun spawnPlasmaBurst(
        pos: Vec2,
        pool: ParticlePool,
        activeList: MutableList<Particle>,
        count: Int = 14
    ) {
        val startColors = intArrayOf(
            0xFF00E5FF.toInt(), // Cyan
            0xFFD500F9.toInt(), // Magenta
            0xFF7C4DFF.toInt()  // Deep Violet
        )
        for (i in 0 until count) {
            val angle = (i.toFloat() / count.toFloat()) * 6.283185f + (Random.nextFloat() * 0.2f)
            val speed = Random.nextFloat() * 70f + 40f
            val vel = Vec2(cos(angle) * speed, sin(angle) * speed)
            val startClr = startColors[i % startColors.size]
            val p = pool.obtain()
            p.spawn(
                pos = pos,
                vel = vel,
                accel = Vec2.ZERO,
                lifespan = Random.nextFloat() * 0.25f + 0.15f,
                particleSize = 2.0f,
                startClr = startClr,
                endClr = 0x0018FFFF,
                linearDrag = 3.0f
            )
            activeList.add(p)
        }
    }

    /**
     * Multi-stage aerial combat explosion: yellow/orange fireball core + smoke + shrapnel.
     */
    fun spawnAerialExplosion(
        pos: Vec2,
        pool: ParticlePool,
        activeList: MutableList<Particle>,
        count: Int = 22
    ) {
        // 1. Hot core fire particles
        val fireCount = (count * 0.6f).toInt()
        for (i in 0 until fireCount) {
            val angle = Random.nextFloat() * 6.283185f
            val speed = Random.nextFloat() * 80f + 20f
            val vel = Vec2(cos(angle) * speed, sin(angle) * speed)
            val p = pool.obtain()
            p.spawn(
                pos = pos + Vec2((Random.nextFloat() - 0.5f) * 4f, (Random.nextFloat() - 0.5f) * 4f),
                vel = vel,
                accel = Vec2(0f, 20f),
                lifespan = Random.nextFloat() * 0.35f + 0.2f,
                particleSize = Random.nextFloat() * 2f + 2f,
                startClr = if (Random.nextBoolean()) 0xFFFFFF00.toInt() else 0xFFFF5722.toInt(),
                endClr = 0x00D50000,
                linearDrag = 2.5f
            )
            activeList.add(p)
        }

        // 2. Trailing dark smoke puffs
        val smokeCount = count - fireCount
        for (i in 0 until smokeCount) {
            val angle = Random.nextFloat() * 6.283185f
            val speed = Random.nextFloat() * 30f + 10f
            val vel = Vec2(cos(angle) * speed, sin(angle) * speed - 15f) // Upward buoyancy
            val p = pool.obtain()
            p.spawn(
                pos = pos,
                vel = vel,
                accel = Vec2(0f, -10f),
                lifespan = Random.nextFloat() * 0.5f + 0.3f,
                particleSize = 3.0f,
                startClr = 0xFF546E7A.toInt(),
                endClr = 0x00263238,
                linearDrag = 1.5f
            )
            activeList.add(p)
        }
    }

    /**
     * Heavy ground explosion with upward crater debris and fiery embers.
     */
    fun spawnGroundBlast(
        pos: Vec2,
        pool: ParticlePool,
        activeList: MutableList<Particle>,
        count: Int = 26
    ) {
        for (i in 0 until count) {
            // Predominantly upward arc (-PI to 0)
            val angle = -Random.nextFloat() * 3.14159f
            val speed = Random.nextFloat() * 90f + 30f
            val vel = Vec2(cos(angle) * speed, sin(angle) * speed)
            val isDebris = Random.nextBoolean()

            val p = pool.obtain()
            p.spawn(
                pos = pos,
                vel = vel,
                accel = Vec2(0f, 120f), // Heavy gravity pulling back down
                lifespan = Random.nextFloat() * 0.45f + 0.25f,
                particleSize = if (isDebris) 2.5f else 1.5f,
                startClr = if (isDebris) 0xFF8D6E63.toInt() else 0xFFFF9800.toInt(),
                endClr = 0x003E2723,
                linearDrag = 1.0f
            )
            activeList.add(p)
        }
    }

    /**
     * Expanding circular shockwave perimeter.
     */
    fun spawnShockwave(
        pos: Vec2,
        pool: ParticlePool,
        activeList: MutableList<Particle>,
        count: Int = 16,
        color: Int = 0xFF80D8FF.toInt()
    ) {
        val speed = 90f
        for (i in 0 until count) {
            val angle = (i.toFloat() / count.toFloat()) * 6.283185f
            val vel = Vec2(cos(angle) * speed, sin(angle) * speed)
            val p = pool.obtain()
            p.spawn(
                pos = pos,
                vel = vel,
                accel = Vec2.ZERO,
                lifespan = 0.2f,
                particleSize = 2.0f,
                startClr = color,
                endClr = 0x0000B0FF,
                linearDrag = 1.0f
            )
            activeList.add(p)
        }
    }

    /**
     * Multi-point cascading explosion sequence for bosses and large buildings.
     */
    private fun spawnTitanChain(
        pos: Vec2,
        pool: ParticlePool,
        activeList: MutableList<Particle>,
        scale: Float
    ) {
        // Central burst
        spawnAerialExplosion(pos, pool, activeList, (20 * scale).toInt())
        // Off-center secondary bursts
        val offsets = arrayOf(
            Vec2(-12f * scale, -8f * scale),
            Vec2(10f * scale, 12f * scale),
            Vec2(-6f * scale, 14f * scale)
        )
        for (offset in offsets) {
            spawnPlasmaBurst(pos + offset, pool, activeList, 8)
        }
    }
}
