package com.example.raptor.entities

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.MotionBlurConfig
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.entity.PixiePlayer
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.ports.HapticFeedbackType
import com.pixie.engine.core.ports.HapticPort
import com.pixie.engine.core.sprite.AnimationFrame
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.weapon.Projectile
import com.pixie.engine.core.weapon.ProjectilePool
import com.pixie.engine.core.weapon.WeaponSystem

/**
 * Player Interceptor craft in Raptor.
 * Inherits from engine core [PixiePlayer] base class to eliminate entity boilerplate.
 * Delegates weapons and projectile firing math to [RaptorWeaponController].
 */
class RaptorShip(
    initialPosition: Vec2,
    val projectilePool: ProjectilePool,
    var audio: AudioPort? = null,
    var haptics: HapticPort? = null
) : PixiePlayer(
    id = "player_raptor",
    position = initialPosition,
    size = Vec2(18f, 22f),
    initialHealth = 100f,
    initialLives = 3,
    initialClasses = setOf("aircraft")
) {
    // Health & Shield Systems (linked to PixiePlayer.health)
    var maxHull: Float
        get() = maxHealth
        set(value) { maxHealth = value }

    var hull: Float
        get() = health
        set(value) { health = value }

    var maxShield: Float = 100f
    var shield: Float = 100f
    private var shieldRechargeCooldown: Float = 0f
    private val shieldRechargeDelay: Float = 2.5f

    // Weapon Inventory & Tiers delegated to RaptorWeaponController
    val weaponController = RaptorWeaponController(projectilePool, audio, haptics)

    var weaponTier: Int
        get() = weaponController.weaponTier
        set(value) { weaponController.weaponTier = value }

    var activeWeaponId: String
        get() = weaponController.activeWeaponId
        set(value) { weaponController.equipWeapon(value) }

    val primaryWeaponSystem: WeaponSystem
        get() = weaponController.primaryWeaponSystem

    val missileWeaponSystem: WeaponSystem
        get() = weaponController.missileWeaponSystem

    var missileSalvoes: Int = 6
    var megaBombs: Int = 2

    // Touch steering smoothing
    var targetPosition: Vec2 = initialPosition
    private val baseSpeed: Float = 360f

    init {
        // Register directional flight animations
        setAnimation("neutral", AnimationSequence("neutral", listOf(AnimationFrame(RaptorShipSprites.SPRITE_NEUTRAL, 0.15f))))
        setAnimation("bank_left", AnimationSequence("bank_left", listOf(AnimationFrame(RaptorShipSprites.SPRITE_BANK_LEFT, 0.15f))))
        setAnimation("bank_right", AnimationSequence("bank_right", listOf(AnimationFrame(RaptorShipSprites.SPRITE_BANK_RIGHT, 0.15f))))
        playAnimation("neutral")

        // Dynamic parameter calculation based on continuous difficulty, active perks, and context
        parameterCalculator = { ship, ctx ->
            val perkSpeedMult = if (ctx.hasPerk("OVERCLOCK")) 1.25f else 1.0f
            val calculatedSpeed = ctx.calculateDynamicProperty(
                baseValue = baseSpeed,
                difficultyWeight = 0.0f,
                levelWeight = 0.05f,
                perkBonus = perkSpeedMult
            )
            ship.parameters["speed"] = calculatedSpeed
            weaponController.updateWeaponTier(ctx)
        }
    }

    fun equipWeapon(weaponId: String) {
        weaponController.equipWeapon(weaponId)
    }

    fun upgradeWeapon() {
        weaponController.upgradeWeapon()
    }

    fun addMissiles(count: Int = 4) {
        missileSalvoes += count
        audio?.playSound(AudioEvent.POWERUP_PICKUP)
    }

    fun addBomb(count: Int = 1) {
        megaBombs += count
        audio?.playSound(AudioEvent.POWERUP_PICKUP)
    }

    fun rechargeShield(amount: Float = 40f) {
        shield = (shield + amount).coerceAtMost(maxShield)
        audio?.playSound(AudioEvent.COLLECT)
    }

    fun repairHull(amount: Float = 30f) {
        hull = (hull + amount).coerceAtMost(maxHull)
        audio?.playSound(AudioEvent.COLLECT)
    }

    fun triggerMegaBomb(
        visibleEnemies: List<PixieObject>,
        activeEnemyProjectiles: List<Projectile>,
        particleLayer: ParticleLayer?
    ): Boolean {
        if (megaBombs <= 0) return false
        megaBombs--
        weaponController.triggerMegaBomb(
            shipPos = position,
            shipSize = size,
            sourcePlayer = this,
            visibleEnemies = visibleEnemies,
            activeEnemyProjectiles = activeEnemyProjectiles,
            particleLayer = particleLayer
        )
        return true
    }

    /**
     * Steer towards target touch coordinate with velocity smoothing and banking animation.
     */
    fun moveTo(target: Vec2, dt: Float, boundsXMin: Float, boundsXMax: Float, boundsYMin: Float, boundsYMax: Float) {
        targetPosition = Vec2(
            target.x.coerceIn(boundsXMin, boundsXMax - size.x),
            target.y.coerceIn(boundsYMin, boundsYMax - size.y)
        )

        val delta = targetPosition - position
        val speed = parameters["speed"] ?: baseSpeed
        val step = speed * dt

        if (delta.length() > step) {
            velocity = delta.normalized() * speed
            position += velocity * dt
        } else {
            position = targetPosition
            velocity = Vec2.ZERO
        }

        // Set directional banking animation based on lateral movement
        when {
            velocity.x < -20f -> playAnimation("bank_left")
            velocity.x > 20f -> playAnimation("bank_right")
            else -> playAnimation("neutral")
        }

        // Speed trail motion blur when moving fast
        if (velocity.length() > 100f) {
            motionBlur = MotionBlurConfig(enabled = true, strength = 0.5f, maxTrailSamples = 3)
        } else {
            motionBlur = MotionBlurConfig(enabled = false)
        }
    }

    fun firePrimary(context: GameContext, outList: MutableList<Projectile>): Boolean {
        return weaponController.firePrimary(position, size, context, outList)
    }

    fun fireMissiles(context: GameContext, target: PixieObject?, outList: MutableList<Projectile>): Boolean {
        if (missileSalvoes <= 0) return false
        val fired = weaponController.fireMissiles(position, size, context, target, outList)
        if (fired) {
            missileSalvoes--
        }
        return fired
    }

    override fun takeDamage(amount: Float, context: GameContext?): Boolean {
        if (!isAlive || isInvulnerable) return false
        takeDamage(amount, particleLayer = null)
        return true
    }

    fun takeDamage(amount: Float, particleLayer: ParticleLayer?) {
        if (isInvulnerable || !isAlive) return

        shieldRechargeCooldown = shieldRechargeDelay
        audio?.playSound(AudioEvent.HIT)
        haptics?.trigger(HapticFeedbackType.MEDIUM)

        if (shield > 0f) {
            shield -= amount
            particleLayer?.spawnSparks(position + (size * 0.5f), count = 8, color = 0xFF00E5FF.toInt())
            if (shield < 0f) {
                // Shield breached, remaining damage bleeds to hull
                hull += shield
                shield = 0f
            }
        } else {
            hull -= amount
            particleLayer?.spawnSparks(position + (size * 0.5f), count = 10, color = 0xFFFF5722.toInt())
        }

        grantInvulnerability(0.8f)

        if (hull <= 0f) {
            hull = 0f
            isAlive = false
            particleLayer?.spawnExplosion(ExplosionType.AERIAL_EXPLOSION, position, scale = 1.5f)
            audio?.playSound(AudioEvent.GAME_OVER)
        }
    }

    override fun onCollision(other: PixieObject, context: GameContext) {
        if (!isAlive || isInvulnerable) return

        if (other is EnemyShip && other.isAlive && !other.isWreck) {
            val playerDmg = when (other.type) {
                EnemyType.SCOUT -> 25f
                EnemyType.INTERCEPTOR -> 35f
                EnemyType.GUNSHIP -> 45f
                EnemyType.STEALTH_BOMBER -> 55f
            }
            takeDamage(playerDmg, particleLayer = null)
            grantInvulnerability(1.0f)
            other.takeDamage(50f, particleLayer = null)
        } else if (other is RaptorBoss && other.isAlive && !other.isDefeated) {
            takeDamage(40f, particleLayer = null)
            grantInvulnerability(1.2f)
            other.takeDamage(35f, position, particleLayer = null)
        }
    }

    override fun update(dt: Float, context: GameContext) {
        weaponController.update(dt)

        // Shield regeneration logic
        if (shieldRechargeCooldown > 0f) {
            shieldRechargeCooldown -= dt
        } else if (shield < maxShield) {
            val regenRate = if (context.hasPerk("TITANIUM_HULL")) 28f else 18f
            shield = (shield + regenRate * dt).coerceAtMost(maxShield)
        }

        super.update(dt, context)
    }

    companion object {
        val SPRITE_NEUTRAL get() = RaptorShipSprites.SPRITE_NEUTRAL
        val SPRITE_BANK_LEFT get() = RaptorShipSprites.SPRITE_BANK_LEFT
        val SPRITE_BANK_RIGHT get() = RaptorShipSprites.SPRITE_BANK_RIGHT
    }
}
