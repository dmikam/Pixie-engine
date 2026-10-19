package com.pixie.engine.core.weapon

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.MotionBlurConfig
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.pool.Poolable
import com.pixie.engine.core.sprite.AnimationSequence

/**
 * High-performance, pooled projectile object.
 *
 * Managed by [ProjectilePool] to provide zero garbage collection during intense bullet hell
 * and multi-shot barrages.
 */
class Projectile : PixieObject(
    id = "projectile",
    position = Vec2.ZERO,
    size = Vec2(4f, 8f),
    initialClasses = setOf("projectile")
), Poolable {

    var weaponDef: WeaponDefinition? = null
        private set

    var isPlayerProjectile: Boolean = true
    var hitPower: Float = 10f
    var remainingPierce: Int = 1
    var trajectory: Trajectory? = null
    var flightTime: Float = 0f
    var maxLifespan: Float = 2.0f
    var effectiveSpeed: Float = 260f

    fun spawn(
        pos: Vec2,
        definition: WeaponDefinition,
        isPlayer: Boolean,
        traj: Trajectory,
        speed: Float,
        damage: Float,
        pierce: Int = definition.pierceCount
    ) {
        position = pos
        weaponDef = definition
        isPlayerProjectile = isPlayer
        trajectory = traj
        effectiveSpeed = speed
        hitPower = damage
        remainingPierce = pierce
        flightTime = 0f
        maxLifespan = definition.maxLifespanSeconds

        size = definition.bulletSize
        motionBlur = definition.motionBlur
        trailHistory.clear()
        trailFadeAlpha = 1.0f

        // Set sprite animation
        setAnimation("default", AnimationSequence.singleFrame(definition.bulletSprite))
        playAnimation("default", resetTime = true)

        isAlive = true
        isVisible = true
        isSolid = true

        if (isPlayer) {
            addClass("player_projectile")
            removeClass("enemy_projectile")
        } else {
            addClass("enemy_projectile")
            removeClass("player_projectile")
        }
    }

    override fun update(dt: Float, context: GameContext) {
        if (!isAlive) return

        flightTime += dt
        if (flightTime >= maxLifespan) {
            isAlive = false
            return
        }

        // Compute velocity from trajectory
        val traj = trajectory
        if (traj != null) {
            velocity = traj.computeVelocity(position, velocity, flightTime, effectiveSpeed, dt)
        }

        super.update(dt, context)
    }

    /**
     * Called when this projectile impacts a target.
     * Decrements pierce and triggers the weapon's encapsulated procedural explosion.
     */
    fun onImpact(impactPos: Vec2 = position, particleLayer: ParticleLayer? = null) {
        val def = weaponDef
        val explosionType = def?.explosionType ?: ExplosionType.SPARK_HIT
        particleLayer?.spawnExplosion(explosionType, impactPos)

        remainingPierce--
        if (remainingPierce <= 0) {
            isAlive = false
        }
    }

    override fun reset() {
        isAlive = false
        isVisible = false
        isSolid = false
        weaponDef = null
        trajectory = null
        flightTime = 0f
        remainingPierce = 1
        trailHistory.clear()
        trailFadeAlpha = 1.0f
        velocity = Vec2.ZERO
        position = Vec2.ZERO
    }
}
