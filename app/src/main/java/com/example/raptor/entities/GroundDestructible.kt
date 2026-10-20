package com.example.raptor.entities

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.ports.RenderBatchPort
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.weapon.Projectile
import com.pixie.engine.core.weapon.ProjectilePool
import com.pixie.engine.core.weapon.StandardWeapons
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
 * Destructible surface installations, military buildings, fuel tanks, naval gunboats, and ground turrets.
 *
 * Anchored to the scrolling terrain, absorbs player fire, flashes on hit, leaves scorched crater
 * graphics when destroyed, triggers chained area explosions, and drops power-ups.
 */
class GroundDestructible(
    id: String,
    val type: DestructibleType,
    initialPos: Vec2,
    val projectilePool: ProjectilePool,
    var audio: AudioPort? = null,
    val onDeathDrop: ((dropPos: Vec2) -> Unit)? = null,
    val onChainExplosion: ((centerPos: Vec2, blastRadius: Float, blastDamage: Float) -> Unit)? = null
) : PixieObject(
    id = id,
    position = initialPos,
    size = Vec2(type.width.toFloat(), type.height.toFloat()),
    initialClasses = setOf("ground_target", "destructible")
) {
    var health: Float = type.baseHealth
    var maxHealth: Float = type.baseHealth
    var isDestroyed: Boolean = false
    var hitFlashTimer: Float = 0f

    // Optional weapon system for ground turrets / SAM silos / gunboats
    val weaponSystem = if (type.isTurret) {
        WeaponSystem(
            weapon = StandardWeapons.ENEMY_RED_LASER,
            pool = projectilePool,
            isPlayer = false,
            audio = audio
        ).apply {
            shotCount = if (type == DestructibleType.GUN_TURRET) 1 else 2
        }
    } else null

    init {
        val sprite = when (type) {
            DestructibleType.GUN_TURRET -> SPRITE_TURRET
            DestructibleType.FUEL_SILO -> SPRITE_SILO
            DestructibleType.RADAR_TOWER -> SPRITE_RADAR
            DestructibleType.SUPPLY_BUNKER -> SPRITE_BUNKER
            DestructibleType.MISSILE_SILO -> SPRITE_MISSILE_SILO
            DestructibleType.NAVAL_GUNBOAT -> SPRITE_GUNBOAT
            DestructibleType.OIL_STORAGE_TANK -> SPRITE_OIL_TANK
        }
        setAnimation("active", AnimationSequence.singleFrame(sprite))
        setAnimation("destroyed", AnimationSequence.singleFrame(SPRITE_CRATER))
        playAnimation("active")

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
    }

    override fun update(dt: Float, context: GameContext) {
        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt
            colorMode = com.pixie.engine.core.entity.ObjectColorMode.AlwaysGrayscale
        } else {
            colorMode = com.pixie.engine.core.entity.ObjectColorMode.AlwaysColor
        }

        if (isDestroyed) return
        weaponSystem?.update(dt)
        super.update(dt, context)
    }

    fun tryShoot(playerTarget: PixieObject?, context: GameContext, outList: MutableList<Projectile>): Boolean {
        if (isDestroyed || weaponSystem == null || playerTarget == null || !playerTarget.isAlive) return false

        // Only shoot when player is within firing range ahead of ground target
        val dy = playerTarget.position.y - position.y
        if (dy > -220f && dy < 40f) {
            val center = position + (size * 0.5f)
            return weaponSystem.fire(
                origin = center,
                headingRad = 1.5707963f, // fires downward/forward
                context = context,
                outList = outList
            )
        }
        return false
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

            // Volatile tanks & silos trigger devastating chain explosions!
            if (type == DestructibleType.FUEL_SILO || type == DestructibleType.OIL_STORAGE_TANK) {
                particleLayer?.spawnExplosion(ExplosionType.SHOCKWAVE, position + (size * 0.5f), scale = 2.0f)
                onChainExplosion?.invoke(position + (size * 0.5f), 85f, 75f)
            }

            // Power-up drops
            onDeathDrop?.invoke(position + (size * 0.5f))
            return true
        }
        return false
    }

    companion object {
        // Detailed 16x16 Coastal Gun Turret with steel plating and dual barrels
        val SPRITE_TURRET = PixelSprite.fromAscii(
            listOf(
                "...##......##...",
                "...##......##...",
                "..####....####..",
                "..####....####..",
                ".##############.",
                ".##############.",
                "################",
                "####..####..####",
                "####..####..####",
                "################",
                "################",
                ".##############.",
                ".##############.",
                "..############..",
                "...##########...",
                "....########...."
            ),
            mapOf(
                '.' to 0x00000000,
                '#' to 0xFF546E7A.toInt() // Slate steel gun battery
            )
        )

        // 18x22 Volatile Fuel Silo with hazard stripes and dome top
        val SPRITE_SILO = PixelSprite.fromAscii(
            listOf(
                ".....########.....",
                "...############...",
                "..##############..",
                ".################.",
                "##################",
                "##################",
                "###..########..###",
                "###..########..###",
                "##################",
                "##..##..##..##..##",
                "##################",
                "##################",
                "##..##..##..##..##",
                "##################",
                "##################",
                "##..##..##..##..##",
                "##################",
                "##################",
                "##################",
                ".################.",
                "..##############..",
                "...############..."
            ),
            mapOf(
                '.' to 0x00000000,
                '#' to 0xFFFF7043.toInt() // High hazard orange silo
            )
        )

        // 16x20 Military Radar Installation with dish scanner & support struts
        val SPRITE_RADAR = PixelSprite.fromAscii(
            listOf(
                "....########....",
                "..############..",
                ".##############.",
                "###...####...###",
                "###...####...###",
                "################",
                ".##############.",
                "..############..",
                "....########....",
                "......####......",
                "......####......",
                "......####......",
                ".....######.....",
                "....##.##.##....",
                "...###.##.###...",
                "..####.##.####..",
                ".#####.##.#####.",
                "################",
                "################",
                "################"
            ),
            mapOf(
                '.' to 0x00000000,
                '#' to 0xFF00E5FF.toInt() // High-tech radar tower
            )
        )

        // 22x18 Heavy Concrete Military Bunker with reinforced blast doors
        val SPRITE_BUNKER = PixelSprite.fromAscii(
            listOf(
                "....##############....",
                "..##################..",
                ".####################.",
                "######################",
                "######################",
                "####...########...####",
                "####...########...####",
                "######################",
                "######################",
                "########......########",
                "########......########",
                "########......########",
                "######################",
                "######################",
                ".####################.",
                "..##################..",
                "...################...",
                ".....############....."
            ),
            mapOf(
                '.' to 0x00000000,
                '#' to 0xFF6D4C41.toInt() // Fortified earthen concrete bunker
            )
        )

        // 18x18 Hardened SAM Missile Silo with open launch tube
        val SPRITE_MISSILE_SILO = PixelSprite.fromAscii(
            listOf(
                "....##########....",
                "..##############..",
                ".################.",
                "##################",
                "###....####....###",
                "###....####....###",
                "##......##......##",
                "##......##......##",
                "##......##......##",
                "##......##......##",
                "###....####....###",
                "###....####....###",
                "##################",
                "##################",
                ".################.",
                "..##############..",
                "....##########....",
                "......######......"
            ),
            mapOf(
                '.' to 0x00000000,
                '#' to 0xFFE53935.toInt() // Crimson missile hatch
            )
        )

        // 16x26 Armed Naval Patrol Gunboat (navigates coastal waters)
        val SPRITE_GUNBOAT = PixelSprite.fromAscii(
            listOf(
                ".......##.......",
                "......####......",
                "......####......",
                ".....######.....",
                ".....######.....",
                "....########....",
                "....########....",
                "...##########...",
                "...##..##..##...",
                "..###..##..###..",
                "..############..",
                "..############..",
                ".##############.",
                ".##############.",
                "################",
                "################",
                "################",
                ".##############.",
                ".##############.",
                "..############..",
                "..############..",
                "...##########...",
                "...##########...",
                "....########....",
                ".....######.....",
                "......####......"
            ),
            mapOf(
                '.' to 0x00000000,
                '#' to 0xFF37474F.toInt() // Battleship gray naval boat
            )
        )

        // 20x20 Heavy Industrial Oil Storage Tank
        val SPRITE_OIL_TANK = PixelSprite.fromAscii(
            listOf(
                ".....##########.....",
                "...##############...",
                "..################..",
                ".##################.",
                "####################",
                "####################",
                "###..##########..###",
                "###..##########..###",
                "####################",
                "####################",
                "####################",
                "####################",
                "####################",
                "####################",
                "####################",
                ".##################.",
                "..################..",
                "...##############...",
                ".....##########.....",
                ".......######......."
            ),
            mapOf(
                '.' to 0x00000000,
                '#' to 0xFFFFB300.toInt() // Amber industrial tank
            )
        )

        // Scorched Rubble Crater
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
    }
}
