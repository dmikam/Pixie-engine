package com.example.raptor.entities

import com.pixie.engine.core.context.GameContext
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

enum class DestructibleType(
    val baseHealth: Float,
    val scoreValue: Long,
    val width: Int,
    val height: Int,
    val isTurret: Boolean = false
) {
    GUN_TURRET(45f, 400L, 16, 16, isTurret = true),
    FUEL_SILO(35f, 500L, 18, 22),
    RADAR_TOWER(60f, 800L, 16, 20),
    SUPPLY_BUNKER(50f, 600L, 20, 16)
}

/**
 * Destructible surface installations, military buildings, fuel tanks, and ground turrets.
 *
 * Sits on the terrain layer, absorbs player fire, leaves scorched crater graphics when destroyed,
 * and triggers chained explosions or drops power-ups.
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

    // Optional weapon system for ground gun turrets
    val weaponSystem = if (type.isTurret) {
        WeaponSystem(
            weapon = StandardWeapons.ENEMY_RED_LASER,
            pool = projectilePool,
            isPlayer = false,
            audio = audio
        ).apply {
            shotCount = 1
        }
    } else null

    init {
        val sprite = when (type) {
            DestructibleType.GUN_TURRET -> SPRITE_TURRET
            DestructibleType.FUEL_SILO -> SPRITE_SILO
            DestructibleType.RADAR_TOWER -> SPRITE_RADAR
            DestructibleType.SUPPLY_BUNKER -> SPRITE_BUNKER
        }
        setAnimation("active", AnimationSequence.singleFrame(sprite))
        setAnimation("destroyed", AnimationSequence.singleFrame(SPRITE_CRATER))
        playAnimation("active")

        parameterCalculator = { target, ctx ->
            val scaledHp = ctx.calculateDynamicProperty(
                baseValue = type.baseHealth,
                difficultyWeight = 0.4f,
                levelWeight = 0.2f
            )
            target.parameters["maxHealth"] = scaledHp
            if (health == maxHealth) {
                maxHealth = scaledHp
                health = scaledHp
            }
        }
    }

    override fun update(dt: Float, context: GameContext) {
        if (isDestroyed) return
        weaponSystem?.update(dt)
        super.update(dt, context)
    }

    fun tryShoot(playerTarget: PixieObject?, context: GameContext, outList: MutableList<Projectile>): Boolean {
        if (isDestroyed || weaponSystem == null || playerTarget == null || !playerTarget.isAlive) return false

        // Only shoot when player is within firing range ahead of turret
        val dy = playerTarget.position.y - position.y
        if (dy > -220f && dy < 60f) {
            val center = position + (size * 0.5f)
            return weaponSystem.fire(
                origin = center,
                headingRad = 1.5707963f, // fires upward/forward toward player elevation
                context = context,
                outList = outList
            )
        }
        return false
    }

    fun takeDamage(amount: Float, particleLayer: ParticleLayer?): Boolean {
        if (isDestroyed) return false

        health -= amount
        particleLayer?.spawnSparks(position + (size * 0.5f), count = 4, color = 0xFFFFD54F.toInt())

        if (health <= 0f) {
            health = 0f
            isDestroyed = true
            isSolid = false // No longer blocks bullets after destruction
            playAnimation("destroyed")

            // Procedural heavy ground explosion
            particleLayer?.spawnExplosion(ExplosionType.GROUND_BLAST, position + (size * 0.5f), scale = 1.4f)
            audio?.playSound(AudioEvent.EXPLOSION_LARGE)

            // Fuel silos trigger area-of-effect chain reaction destroying nearby enemies!
            if (type == DestructibleType.FUEL_SILO) {
                particleLayer?.spawnExplosion(ExplosionType.SHOCKWAVE, position + (size * 0.5f), scale = 1.8f)
                onChainExplosion?.invoke(position + (size * 0.5f), 70f, 60f)
            }

            // Power-up drops
            onDeathDrop?.invoke(position + (size * 0.5f))
            return true
        }
        return false
    }

    companion object {
        val SPRITE_TURRET = PixelSprite.fromAscii(
            listOf(
                "....########....",
                "..############..",
                ".##############.",
                "################",
                "####...##...####",
                "####...##...####",
                "######.##.######",
                "######.##.######",
                "################",
                "################",
                "################",
                "################",
                ".##############.",
                "..############..",
                "....########....",
                "......####......"
            ),
            mapOf('.' to 0x00000000, '#' to 0xFF546E7A.toInt()) // Steel blue turret
        )

        val SPRITE_SILO = PixelSprite.fromAscii(
            listOf(
                "....##########....",
                "..##############..",
                ".################.",
                "##################",
                "###..########..###",
                "###..########..###",
                "##################",
                "##################",
                "##################",
                "##################",
                "##################",
                "##################",
                "##################",
                "##################",
                "##################",
                "##################",
                "##################",
                "##################",
                "##################",
                "##################",
                ".################.",
                "..##############.."
            ),
            mapOf('.' to 0x00000000, '#' to 0xFFFF7043.toInt()) // Volatile orange silo
        )

        val SPRITE_RADAR = PixelSprite.fromAscii(
            listOf(
                "....########....",
                "..############..",
                ".##############.",
                "###...####...###",
                "###...####...###",
                "################",
                "################",
                "################",
                ".##############.",
                "..############..",
                "....########....",
                "......####......",
                "......####......",
                "......####......",
                ".....######.....",
                ".....######.....",
                "....########....",
                "...##########...",
                "..############..",
                ".##############."
            ),
            mapOf('.' to 0x00000000, '#' to 0xFF78909C.toInt()) // Radar Tower
        )

        val SPRITE_BUNKER = PixelSprite.fromAscii(
            listOf(
                "....############....",
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
                ".##################.",
                "..################..",
                "....############....",
                "......########......"
            ),
            mapOf('.' to 0x00000000, '#' to 0xFF3E2723.toInt()) // Heavy bunker
        )

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
            mapOf('.' to 0x00000000, '#' to 0xFF212121.toInt()) // Scorched black crater
        )
    }
}
