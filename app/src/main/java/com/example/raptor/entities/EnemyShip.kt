package com.example.raptor.entities

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieEnemy
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.entity.PixiePlayer
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.weapon.Projectile
import com.pixie.engine.core.weapon.ProjectilePool
import com.pixie.engine.core.weapon.WeaponSystem
import kotlin.random.Random

enum class EnemyType(
    val baseHealth: Float,
    val baseSpeed: Float,
    val scoreValue: Long,
    val width: Int,
    val height: Int
) {
    SCOUT(18f, 52f, 150L, 14, 14),
    INTERCEPTOR(38f, 44f, 350L, 18, 16),
    GUNSHIP(80f, 28f, 800L, 22, 20),
    STEALTH_BOMBER(120f, 22f, 1500L, 26, 22)
}

/**
 * Common abstract parent class for hostile combat craft in Raptor.
 * Inherits from engine core [PixieEnemy] base class to eliminate entity boilerplate.
 */
abstract class EnemyShip(
    id: String,
    val type: EnemyType,
    initialPos: Vec2,
    val projectilePool: ProjectilePool,
    var audio: AudioPort? = null,
    val onDeathDrop: ((dropPos: Vec2) -> Unit)? = null
) : PixieEnemy(
    id = id,
    position = initialPos,
    size = Vec2(type.width.toFloat(), type.height.toFloat()),
    initialHealth = type.baseHealth,
    scoreAward = type.scoreValue,
    contactDamage = 15f,
    initialClasses = setOf("aircraft")
) {
    var flightTime: Float = 0f
    protected val flightSeed: Float = Random.nextFloat() * 10f

    abstract val weaponSystem: WeaponSystem
    abstract val explosionType: ExplosionType
    abstract val explosionScale: Float

    var isWreck: Boolean = false
    protected var shootCooldown: Float = Random.nextFloat() * 2.5f + 3.0f

    init {
        // Dynamic parameter calculations based on continuous difficulty & level
        parameterCalculator = { enemy, ctx ->
            val scaledHp = ctx.calculateDynamicProperty(
                baseValue = type.baseHealth,
                difficultyWeight = 0.4f,
                levelWeight = 0.15f
            )
            enemy.parameters["maxHealth"] = scaledHp
            if (health == maxHealth) {
                maxHealth = scaledHp
                health = scaledHp
            }

            val scaledSpeed = ctx.calculateDynamicProperty(
                baseValue = type.baseSpeed,
                difficultyWeight = 0.25f,
                levelWeight = 0.1f
            )
            enemy.parameters["speed"] = scaledSpeed
        }
    }

    abstract fun calculateFlightVelocity(speed: Float): Vec2

    abstract fun getNextShootCooldown(): Float

    override fun update(dt: Float, context: GameContext) {
        if (!isAlive) return

        if (isWreck) {
            colorMode = com.pixie.engine.core.entity.ObjectColorMode.AlwaysGrayscale
            // Wrecks drift downward with the scrolling terrain at 36 px/s until leaving screen
            velocity = Vec2(velocity.x * 0.92f, 36f)
            super.update(dt, context)

            // When the wreck drifts below the bottom of the virtual screen with the ground, clean it up
            if (position.y > 350f) {
                isAlive = false
            }
            return
        }

        flightTime += dt
        shootCooldown -= dt
        weaponSystem.update(dt)

        val speed = parameters["speed"] ?: type.baseSpeed
        velocity = calculateFlightVelocity(speed)

        super.update(dt, context)
    }

    /**
     * Checks if enemy can shoot at the player. Throttled to lower ratio for readable tactical combat.
     */
    fun tryShoot(playerTarget: PixieObject?, context: GameContext, outList: MutableList<Projectile>): Boolean {
        if (!isAlive || isWreck || shootCooldown > 0f || position.y < 25f || position.y > 240f) return false
        val muzzlePos = position + Vec2(size.x * 0.5f, size.y)
        val fired = weaponSystem.fire(
            origin = muzzlePos,
            headingRad = 1.5707963f, // straight down
            context = context,
            outList = outList
        )
        if (fired) {
            shootCooldown = getNextShootCooldown()
        }
        return fired
    }

    override fun takeDamage(amount: Float, context: GameContext?): Boolean {
        return takeDamage(amount, particleLayer = null)
    }

    override fun onCollision(other: PixieObject, context: GameContext) {
        if (!isAlive || isWreck || isDefeated) return

        if (other is PixiePlayer && other.isAlive && !other.isInvulnerable) {
            val playerDmg = when (type) {
                EnemyType.SCOUT -> 25f
                EnemyType.INTERCEPTOR -> 35f
                EnemyType.GUNSHIP -> 45f
                EnemyType.STEALTH_BOMBER -> 55f
            }
            other.takeDamage(playerDmg, context)
            takeDamage(50f, particleLayer = null)
        }
    }

    fun takeDamage(amount: Float, particleLayer: ParticleLayer?): Boolean {
        if (!isAlive || isWreck) return false

        health -= amount
        particleLayer?.spawnSparks(position + (size * 0.5f), count = 5, color = 0xFFFFEB3B.toInt())

        if (health <= 0f) {
            health = 0f
            isWreck = true
            isSolid = false // bullets pass through burning wrecks

            particleLayer?.spawnExplosion(explosionType, position + (size * 0.5f), explosionScale)
            audio?.playSound(
                if (type == EnemyType.GUNSHIP || type == EnemyType.STEALTH_BOMBER)
                    AudioEvent.EXPLOSION_LARGE else AudioEvent.EXPLOSION_SMALL
            )

            // Spawn random reward drop
            onDeathDrop?.invoke(position + (size * 0.5f))
            return true
        }
        return false
    }

    companion object {
        /**
         * Factory method to create appropriate subclass by EnemyType.
         */
        fun create(
            type: EnemyType,
            id: String,
            initialPos: Vec2,
            projectilePool: ProjectilePool,
            audio: AudioPort? = null,
            onDeathDrop: ((dropPos: Vec2) -> Unit)? = null
        ): EnemyShip = when (type) {
            EnemyType.SCOUT -> ScoutEnemyShip(id, initialPos, projectilePool, audio, onDeathDrop)
            EnemyType.INTERCEPTOR -> InterceptorEnemyShip(id, initialPos, projectilePool, audio, onDeathDrop)
            EnemyType.GUNSHIP -> GunshipEnemyShip(id, initialPos, projectilePool, audio, onDeathDrop)
            EnemyType.STEALTH_BOMBER -> StealthBomberEnemyShip(id, initialPos, projectilePool, audio, onDeathDrop)
        }

        /**
         * Invoke operator allowing EnemyShip(...) constructor-like syntax for polymorphic creation.
         */
        operator fun invoke(
            id: String,
            type: EnemyType,
            initialPos: Vec2,
            projectilePool: ProjectilePool,
            audio: AudioPort? = null,
            onDeathDrop: ((dropPos: Vec2) -> Unit)? = null
        ): EnemyShip = create(type, id, initialPos, projectilePool, audio, onDeathDrop)
    }
}
