package com.example.raptor.entities

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.MotionBlurConfig
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.ports.HapticFeedbackType
import com.pixie.engine.core.ports.HapticPort
import com.pixie.engine.core.sprite.AnimationFrame
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.weapon.Projectile
import com.pixie.engine.core.weapon.ProjectilePool
import com.pixie.engine.core.weapon.StandardWeapons
import com.pixie.engine.core.weapon.WeaponDefinition
import com.pixie.engine.core.weapon.WeaponSystem

/**
 * Player Interceptor craft in Raptor.
 *
 * Encapsulates:
 * - Pixel-art directional bank sprites (neutral, bank left, bank right) with animated thruster plume
 * - Upgradable Primary Cannon (Vulcan -> Twin Vulcan -> Twin Plasma -> Quad Ion Blaster)
 * - Secondary Homing Missile Pods
 * - Super Orbital Mega-Bomb
 * - Regenerative Plasma Shield + Hull Integrity system
 * - Screen boundary clamping and responsive touch motion blur
 */
class RaptorShip(
    initialPosition: Vec2,
    val projectilePool: ProjectilePool,
    var audio: AudioPort? = null,
    var haptics: HapticPort? = null
) : PixieObject(
    id = "player_raptor",
    position = initialPosition,
    size = Vec2(18f, 22f),
    initialClasses = setOf("player", "aircraft")
) {
    // Health & Shield Systems
    var maxHull: Float = 100f
    var hull: Float = 100f
    var maxShield: Float = 100f
    var shield: Float = 100f
    private var shieldRechargeCooldown: Float = 0f
    private val shieldRechargeDelay: Float = 2.5f

    // Weapon Inventory & Tiers
    var weaponTier: Int = 1 // 1 = Single Vulcan, 2 = Twin Vulcan, 3 = Twin Plasma, 4 = Quad Ion
    var missileSalvoes: Int = 6
    var megaBombs: Int = 2

    // Primary & Secondary Weapon Controllers
    val primaryWeaponSystem = WeaponSystem(
        weapon = StandardWeapons.VULCAN_CANNON,
        pool = projectilePool,
        isPlayer = true,
        audio = audio,
        haptics = haptics
    )

    val missileWeaponSystem = WeaponSystem(
        weapon = StandardWeapons.HOMING_MISSILES,
        pool = projectilePool,
        isPlayer = true,
        audio = audio,
        haptics = haptics
    )

    // Touch steering smoothing
    var targetPosition: Vec2 = initialPosition
    private val baseSpeed: Float = 360f
    var isInvulnerable: Boolean = false
    private var invulnerableTimer: Float = 0f

    init {
        // Register directional flight animations
        setAnimation("neutral", AnimationSequence("neutral", listOf(AnimationFrame(SPRITE_NEUTRAL, 0.15f))))
        setAnimation("bank_left", AnimationSequence("bank_left", listOf(AnimationFrame(SPRITE_BANK_LEFT, 0.15f))))
        setAnimation("bank_right", AnimationSequence("bank_right", listOf(AnimationFrame(SPRITE_BANK_RIGHT, 0.15f))))
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

            // Configure primary weapon tier
            updateWeaponTier(ctx)
        }
    }

    private fun updateWeaponTier(context: GameContext) {
        when (weaponTier) {
            1 -> {
                primaryWeaponSystem.weapon = StandardWeapons.VULCAN_CANNON
                primaryWeaponSystem.shotCount = 1
            }
            2 -> {
                primaryWeaponSystem.weapon = StandardWeapons.VULCAN_CANNON
                primaryWeaponSystem.shotCount = 2
            }
            3 -> {
                primaryWeaponSystem.weapon = StandardWeapons.TWIN_PLASMA
                primaryWeaponSystem.shotCount = 2
            }
            else -> {
                primaryWeaponSystem.weapon = StandardWeapons.TWIN_PLASMA
                primaryWeaponSystem.shotCount = 4
            }
        }
    }

    fun upgradeWeapon() {
        if (weaponTier < 4) {
            weaponTier++
            audio?.playSound(AudioEvent.POWERUP_PICKUP)
            haptics?.trigger(HapticFeedbackType.DOUBLE_PULSE)
        }
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

        audio?.playSound(AudioEvent.EXPLOSION_LARGE)
        haptics?.trigger(HapticFeedbackType.HEAVY)

        // Clear all active incoming enemy bullets!
        for (bullet in activeEnemyProjectiles) {
            bullet.isAlive = false
            particleLayer?.spawnSparks(bullet.position, count = 4, color = 0xFF00E5FF.toInt())
        }

        // Damage all visible enemies on screen
        for (enemy in visibleEnemies) {
            if (enemy.isAlive) {
                particleLayer?.spawnExplosion(ExplosionType.AERIAL_EXPLOSION, enemy.position, scale = 1.2f)
                enemy.onCollision(this, GameContext())
            }
        }

        // Screen-wide shockwave at ship center
        particleLayer?.spawnExplosion(ExplosionType.SHOCKWAVE, position + (size * 0.5f), scale = 2.0f)
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
        val nosePos = position + Vec2(size.x * 0.5f, 0f)
        return primaryWeaponSystem.fire(
            origin = nosePos,
            headingRad = -1.5707963f, // straight up
            context = context,
            outList = outList
        )
    }

    fun fireMissiles(context: GameContext, target: PixieObject?, outList: MutableList<Projectile>): Boolean {
        if (missileSalvoes <= 0) return false

        missileWeaponSystem.targetGetter = { target }
        val wingLeft = position + Vec2(2f, 4f)
        val wingRight = position + Vec2(size.x - 2f, 4f)

        val fired = missileWeaponSystem.fire(
            origin = wingLeft,
            headingRad = -1.5707963f,
            context = context,
            outList = outList
        )
        if (fired) {
            // Also fire right wing missile
            missileWeaponSystem.fire(
                origin = wingRight,
                headingRad = -1.5707963f,
                context = context,
                outList = outList
            )
            missileSalvoes--
        }
        return fired
    }

    fun takeDamage(amount: Float, particleLayer: ParticleLayer?) {
        if (isInvulnerable) return

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

        // Brief invulnerability flash
        isInvulnerable = true
        invulnerableTimer = 0.6f

        if (hull <= 0f) {
            hull = 0f
            isAlive = false
            particleLayer?.spawnExplosion(ExplosionType.AERIAL_EXPLOSION, position, scale = 1.5f)
            audio?.playSound(AudioEvent.GAME_OVER)
        }
    }

    override fun update(dt: Float, context: GameContext) {
        primaryWeaponSystem.update(dt)
        missileWeaponSystem.update(dt)

        // Shield regeneration logic
        if (shieldRechargeCooldown > 0f) {
            shieldRechargeCooldown -= dt
        } else if (shield < maxShield) {
            val regenRate = if (context.hasPerk("TITANIUM_HULL")) 28f else 18f
            shield = (shield + regenRate * dt).coerceAtMost(maxShield)
        }

        // Invulnerability countdown
        if (isInvulnerable) {
            invulnerableTimer -= dt
            if (invulnerableTimer <= 0f) {
                isInvulnerable = false
            }
        }

        super.update(dt, context)
    }

    companion object {
        // 18x22 Retro Jet Sprites
        val SPRITE_NEUTRAL = PixelSprite.fromAscii(
            listOf(
                "........##........",
                ".......#..#.......",
                ".......#..#.......",
                "......#....#......",
                "......#....#......",
                ".....#..##..#.....",
                "....#..#..#..#....",
                "...#...#..#...#...",
                "..#....#..#....#..",
                "..#...#....#...#..",
                ".#....#....#....#.",
                "#.....#....#.....#",
                "#....#......#....#",
                "#....#..##..#....#",
                "##..#..#..#..#..##",
                ".#..#..#..#..#..#.",
                "..##...####...##..",
                ".......####.......",
                ".......#..#.......",
                ".......#..#.......",
                "........##........",
                "........##........"
            ),
            mapOf(
                '.' to 0x00000000,
                '#' to 0xFF37474F.toInt(), // Dark hull metal
                ' ' to 0x00000000
            )
        )

        val SPRITE_BANK_LEFT = PixelSprite.fromAscii(
            listOf(
                ".......##.........",
                "......#..#........",
                "......#..#........",
                ".....#....#.......",
                ".....#....#.......",
                "....#..##..#......",
                "...#..#..#..#.....",
                "..#...#..#...#....",
                ".#....#..#....#...",
                ".#...#....#...#...",
                "#....#....#....#..",
                "#....#....#.....#.",
                "#...#......#....#.",
                "#...#..##..#....#.",
                "##.#..#..#..#..##.",
                ".#.#..#..#..#..#..",
                ".##...####...##...",
                "......####........",
                "......#..#........",
                "......#..#........",
                ".......##.........",
                ".......##........."
            ),
            mapOf('.' to 0x00000000, '#' to 0xFF455A64.toInt())
        )

        val SPRITE_BANK_RIGHT = PixelSprite.fromAscii(
            listOf(
                ".........##.......",
                "........#..#......",
                "........#..#......",
                ".......#....#.....",
                ".......#....#.....",
                "......#..##..#....",
                ".....#..#..#..#...",
                "....#...#..#...#..",
                "...#....#..#....#.",
                "...#...#....#...#.",
                "..#....#....#....#",
                ".#.....#....#....#",
                ".#....#......#...#",
                ".#....#..##..#...#",
                ".##..#..#..#..#.##",
                "..#..#..#..#..#.#.",
                "...##...####...##.",
                "........####......",
                "........#..#......",
                "........#..#......",
                ".........##.......",
                ".........##......."
            ),
            mapOf('.' to 0x00000000, '#' to 0xFF455A64.toInt())
        )
    }
}
