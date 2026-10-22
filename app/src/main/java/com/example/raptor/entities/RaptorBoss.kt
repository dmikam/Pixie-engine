package com.example.raptor.entities

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieEnemy
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
import com.pixie.engine.core.weapon.StandardWeapons
import com.pixie.engine.core.weapon.WeaponSystem
import kotlin.math.sin

/**
 * Multi-phase boss battle in Raptor.
 * Inherits from engine core [PixieEnemy] base class to eliminate entity boilerplate.
 */
class RaptorBoss(
    initialPos: Vec2,
    val projectilePool: ProjectilePool,
    var audio: AudioPort? = null,
    val onBossDefeated: (() -> Unit)? = null
) : PixieEnemy(
    id = "boss_sea_titan",
    position = initialPos,
    size = Vec2(48f, 36f),
    initialHealth = 220f,
    scoreAward = 10000L,
    contactDamage = 35f,
    initialClasses = setOf("boss")
) {
    var maxCoreHealth: Float
        get() = maxHealth
        set(value) { maxHealth = value }

    var coreHealth: Float
        get() = health
        set(value) { health = value }

    var leftCannonHp: Float = 60f
    var rightCannonHp: Float = 60f

    val isLeftCannonDestroyed: Boolean get() = leftCannonHp <= 0f
    val isRightCannonDestroyed: Boolean get() = rightCannonHp <= 0f
    val isRagePhase: Boolean get() = isLeftCannonDestroyed && isRightCannonDestroyed

    private var battleTime: Float = 0f
    private var attackCycleTimer: Float = 0f

    // Boss weapon systems
    val primaryWeapons = WeaponSystem(
        weapon = StandardWeapons.ENEMY_PLASMA_FAN,
        pool = projectilePool,
        isPlayer = false,
        audio = audio
    ).apply {
        shotCount = 3
    }

    val laserCannon = WeaponSystem(
        weapon = StandardWeapons.ENEMY_RED_LASER,
        pool = projectilePool,
        isPlayer = false,
        audio = audio
    ).apply {
        shotCount = 2
    }

    init {
        setAnimation("phase1", AnimationSequence.singleFrame(SPRITE_PHASE1))
        setAnimation("rage", AnimationSequence.singleFrame(SPRITE_RAGE))
        playAnimation("phase1")

        parameterCalculator = { boss, ctx ->
            val scaledHp = ctx.calculateDynamicProperty(
                baseValue = 220f,
                difficultyWeight = 0.6f,
                levelWeight = 0.3f
            )
            boss.parameters["maxHp"] = scaledHp
            if (coreHealth == maxCoreHealth) {
                maxCoreHealth = scaledHp
                coreHealth = scaledHp
            }
        }
    }

    override fun update(dt: Float, context: GameContext) {
        if (!isAlive || isDefeated) return

        battleTime += dt
        attackCycleTimer += dt
        bossSalvoCooldown -= dt
        primaryWeapons.update(dt)
        laserCannon.update(dt)

        // Floating movement pattern: hover down to top screen, then strafe horizontally
        val targetY = 28f
        val currentY = if (position.y < targetY) position.y + 18f * dt else targetY
        val currentX = 120f - (size.x * 0.5f) + sin(battleTime * 1.4f) * 65f
        position = Vec2(currentX, currentY)

        // Switch to rage animation when both cannons are eliminated
        if (isRagePhase) {
            playAnimation("rage")
        }

        super.update(dt, context)
    }

    private var bossSalvoCooldown: Float = 2.0f

    fun attack(context: GameContext, outList: MutableList<Projectile>) {
        if (!isAlive || isDefeated || position.y < 20f) return
        if (bossSalvoCooldown > 0f) return

        if (!isRagePhase) {
            // Phase 1: Left and right cannons fire alternating spreads
            var fired = false
            if (!isLeftCannonDestroyed) {
                fired = primaryWeapons.fire(
                    origin = position + Vec2(6f, size.y - 4f),
                    headingRad = 1.57f,
                    context = context,
                    outList = outList
                ) || fired
            }
            if (!isRightCannonDestroyed) {
                fired = primaryWeapons.fire(
                    origin = position + Vec2(size.x - 6f, size.y - 4f),
                    headingRad = 1.57f,
                    context = context,
                    outList = outList
                ) || fired
            }
            if (fired) {
                bossSalvoCooldown = 3.5f
            }
        } else {
            // Phase 2 (Rage): Center core laser and plasma burst
            val centerMuzzle = position + Vec2(size.x * 0.5f, size.y)
            val fired1 = laserCannon.fire(centerMuzzle, 1.57f, context, outList)
            val fired2 = primaryWeapons.fire(centerMuzzle, 1.57f, context, outList)
            if (fired1 || fired2) {
                bossSalvoCooldown = 2.4f
            }
        }
    }

    fun takeDamage(damage: Float, hitPos: Vec2, particleLayer: ParticleLayer?): Boolean {
        if (!isAlive || isDefeated) return false

        // Determine if hit landed on left wing, right wing, or core
        val localX = hitPos.x - position.x
        particleLayer?.spawnSparks(hitPos, count = 6, color = 0xFFFFD54F.toInt())

        when {
            localX < 14f && !isLeftCannonDestroyed -> {
                leftCannonHp -= damage
                if (isLeftCannonDestroyed) {
                    leftCannonHp = 0f
                    particleLayer?.spawnExplosion(ExplosionType.AERIAL_EXPLOSION, position + Vec2(8f, 16f))
                    audio?.playSound(AudioEvent.EXPLOSION_SMALL)
                }
            }
            localX > size.x - 14f && !isRightCannonDestroyed -> {
                rightCannonHp -= damage
                if (isRightCannonDestroyed) {
                    rightCannonHp = 0f
                    particleLayer?.spawnExplosion(ExplosionType.AERIAL_EXPLOSION, position + Vec2(size.x - 8f, 16f))
                    audio?.playSound(AudioEvent.EXPLOSION_SMALL)
                }
            }
            else -> {
                // Core takes direct damage (reinforced if cannons still active)
                val effectiveDamage = if (isRagePhase) damage else damage * 0.65f
                coreHealth -= effectiveDamage
                if (coreHealth <= 0f) {
                    coreHealth = 0f
                    isDefeated = true
                    isAlive = false

                    // Massive cascading explosion!
                    particleLayer?.spawnExplosion(ExplosionType.TITAN_CHAIN, position + (size * 0.5f), scale = 2.2f)
                    audio?.playSound(AudioEvent.EXPLOSION_LARGE)
                    onBossDefeated?.invoke()
                    return true
                }
            }
        }
        return false
    }

    companion object {
        val SPRITE_PHASE1 = PixelSprite.createSolid(48, 36, 0xFF37474F.toInt()) // Heavy Warship Gray
        val SPRITE_RAGE = PixelSprite.createSolid(48, 36, 0xFFD84315.toInt())   // Glowing Overheated Core
    }
}
