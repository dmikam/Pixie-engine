package com.example.raptor.entities

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObstacle
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.weapon.Projectile
import com.pixie.engine.core.weapon.ProjectilePool
import com.pixie.engine.core.weapon.WeaponSystem

enum class DestructibleType(
    val displayName: String,
    val baseHealth: Float,
    val scoreValue: Long,
    val width: Int,
    val height: Int,
    val isTurret: Boolean = false,
    val isNaval: Boolean = false
) {
    GUN_TURRET("Coastal Gun Battery", 35f, 400L, 16, 16, isTurret = true),
    FUEL_SILO("Volatile Fuel Silo", 30f, 600L, 18, 22),
    RADAR_TOWER("Radar Installation", 45f, 750L, 16, 20),
    SUPPLY_BUNKER("Reinforced Supply Bunker", 65f, 900L, 22, 18),
    MISSILE_SILO("SAM Launch Silo", 40f, 800L, 18, 18, isTurret = true),
    NAVAL_GUNBOAT("Naval Patrol Gunboat", 50f, 1000L, 16, 26, isTurret = true, isNaval = true),
    OIL_STORAGE_TANK("Crude Oil Reserve", 55f, 1200L, 20, 20)
}

/**
 * Common abstract parent class for destructible surface installations, military buildings,
 * fuel tanks, naval gunboats, and ground turrets in Raptor.
 * Inherits from engine core [PixieObstacle] base class to eliminate entity boilerplate.
 */
abstract class GroundDestructible(
    id: String,
    val type: DestructibleType,
    initialPos: Vec2,
    val projectilePool: ProjectilePool,
    var audio: AudioPort? = null,
    val onDeathDrop: ((dropPos: Vec2) -> Unit)? = null,
    val onChainExplosion: ((centerPos: Vec2, blastRadius: Float, blastDamage: Float) -> Unit)? = null
) : PixieObstacle(
    id = id,
    position = initialPos,
    size = Vec2(type.width.toFloat(), type.height.toFloat()),
    isDestructible = true,
    initialHealth = type.baseHealth,
    collisionDamage = 25f,
    initialClasses = setOf("ground_target")
) {
    var hitFlashTimer: Float = 0f
    protected var turretShootCooldown: Float = kotlin.random.Random.nextFloat() * 2.5f + 2.5f

    // Optional weapon system for ground turrets / SAM silos / gunboats
    open val weaponSystem: WeaponSystem? = null

    init {
        setAnimation("destroyed", AnimationSequence.singleFrame(SPRITE_CRATER))

        parameterCalculator = { target, ctx ->
            val scaledHp = ctx.calculateDynamicProperty(
                baseValue = type.baseHealth,
                difficultyWeight = 0.35f,
                levelWeight = 0.15f
            )
            target.parameters["maxHealth"] = scaledHp
            if (health == maxHealth) {
                maxHealth = scaledHp
                health = scaledHp
            }
        }
        velocity = Vec2(0f, 36f)
    }

    override fun update(dt: Float, context: GameContext) {
        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt
            colorMode = com.pixie.engine.core.entity.ObjectColorMode.AlwaysGrayscale
        } else {
            colorMode = com.pixie.engine.core.entity.ObjectColorMode.AlwaysColor
        }

        turretShootCooldown -= dt

        if (!isDestroyed) {
            weaponSystem?.update(dt)
        }

        // Always integrate velocity (36 px/s) so craters and ruins drift downward with terrain until offscreen
        velocity = Vec2(0f, 36f)
        super.update(dt, context)
    }

    fun tryShoot(playerTarget: PixieObject?, context: GameContext, outList: MutableList<Projectile>): Boolean {
        if (isDestroyed || weaponSystem == null || playerTarget == null || !playerTarget.isAlive) return false
        if (turretShootCooldown > 0f) return false

        // Only shoot when player is within firing range ahead of ground target
        val dy = playerTarget.position.y - position.y
        if (dy > -220f && dy < 40f) {
            val center = position + (size * 0.5f)
            val fired = weaponSystem?.fire(
                origin = center,
                headingRad = 1.5707963f, // fires downward/forward
                context = context,
                outList = outList
            ) ?: false
            if (fired) {
                turretShootCooldown = 4.8f // reduced shooting ratio (once every 4.8s)
            }
            return fired
        }
        return false
    }

    /**
     * Subclasses can override to trigger secondary blast effects (e.g. shockwaves, chained explosions).
     */
    open fun onDeathEffect(particleLayer: ParticleLayer?) {
        // Default no-op
    }

    fun takeDamage(amount: Float, particleLayer: ParticleLayer?): Boolean {
        if (isDestroyed) return false

        health -= amount
        hitFlashTimer = 0.08f
        particleLayer?.spawnSparks(position + (size * 0.5f), count = 5, color = 0xFFFFD54F.toInt())

        if (health <= 0f) {
            health = 0f
            isDestroyed = true
            isSolid = false
            playAnimation("destroyed")

            // Procedural heavy ground explosion
            particleLayer?.spawnExplosion(ExplosionType.GROUND_BLAST, position + (size * 0.5f), scale = 1.5f)
            audio?.playSound(AudioEvent.EXPLOSION_LARGE)

            // Trigger subclass-specific death effect (shockwaves, chain reactions)
            onDeathEffect(particleLayer)

            // Power-up drops
            onDeathDrop?.invoke(position + (size * 0.5f))
            return true
        }
        return false
    }

    companion object {
        // Scorched Rubble Crater shared by all destroyed ground targets
        val SPRITE_CRATER = PixelSprite.fromAscii(
            listOf(
                "....########....",
                "..############..",
                ".##############.",
                "################",
                "################",
                "################",
                "################",
                "################",
                "################",
                "################",
                "################",
                "################",
                ".##############.",
                "..############..",
                "....########....",
                "......####......"
            ),
            mapOf(
                '.' to 0x00000000,
                '#' to 0xFF212121.toInt() // Scorched black crater
            )
        )

        /**
         * Polymorphic factory for GroundDestructible.
         */
        fun create(
            type: DestructibleType,
            id: String,
            initialPos: Vec2,
            projectilePool: ProjectilePool,
            audio: AudioPort? = null,
            onDeathDrop: ((dropPos: Vec2) -> Unit)? = null,
            onChainExplosion: ((centerPos: Vec2, blastRadius: Float, blastDamage: Float) -> Unit)? = null
        ): GroundDestructible = when (type) {
            DestructibleType.GUN_TURRET -> GunTurretDestructible(id, initialPos, projectilePool, audio, onDeathDrop, onChainExplosion)
            DestructibleType.MISSILE_SILO -> MissileSiloDestructible(id, initialPos, projectilePool, audio, onDeathDrop, onChainExplosion)
            DestructibleType.NAVAL_GUNBOAT -> NavalGunboatDestructible(id, initialPos, projectilePool, audio, onDeathDrop, onChainExplosion)
            DestructibleType.FUEL_SILO -> FuelSiloDestructible(id, initialPos, projectilePool, audio, onDeathDrop, onChainExplosion)
            DestructibleType.RADAR_TOWER -> RadarTowerDestructible(id, initialPos, projectilePool, audio, onDeathDrop, onChainExplosion)
            DestructibleType.SUPPLY_BUNKER -> SupplyBunkerDestructible(id, initialPos, projectilePool, audio, onDeathDrop, onChainExplosion)
            DestructibleType.OIL_STORAGE_TANK -> OilStorageTankDestructible(id, initialPos, projectilePool, audio, onDeathDrop, onChainExplosion)
        }

        /**
         * Invoke operator allowing constructor syntax GroundDestructible(...)
         */
        operator fun invoke(
            id: String,
            type: DestructibleType,
            initialPos: Vec2,
            projectilePool: ProjectilePool,
            audio: AudioPort? = null,
            onDeathDrop: ((dropPos: Vec2) -> Unit)? = null,
            onChainExplosion: ((centerPos: Vec2, blastRadius: Float, blastDamage: Float) -> Unit)? = null
        ): GroundDestructible = create(type, id, initialPos, projectilePool, audio, onDeathDrop, onChainExplosion)
    }
}
