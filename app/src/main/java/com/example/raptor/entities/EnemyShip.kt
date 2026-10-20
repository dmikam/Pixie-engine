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
import kotlin.math.sin
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
 * Hostile combat craft in Raptor.
 *
 * Encapsulates:
 * - Dynamic HP & flight trajectory (graceful sine curves, steady strafe, heavy gunship advance)
 * - Self-encapsulated hostile weapon system (Red Lasers, Plasma Fans)
 * - Procedural drops (weapon upgrade, missiles, shield, repair, score gems)
 * - Context-driven property scaling (difficulty 0..100 & current level)
 */
class EnemyShip(
    id: String,
    val type: EnemyType,
    initialPos: Vec2,
    val projectilePool: ProjectilePool,
    var audio: AudioPort? = null,
    val onDeathDrop: ((dropPos: Vec2) -> Unit)? = null
) : PixieObject(
    id = id,
    position = initialPos,
    size = Vec2(type.width.toFloat(), type.height.toFloat()),
    initialClasses = setOf("enemy", "aircraft")
) {
    var health: Float = type.baseHealth
    var maxHealth: Float = type.baseHealth
    var scoreAward: Long = type.scoreValue
    var flightTime: Float = 0f
    private val flightSeed: Float = Random.nextFloat() * 10f

    // Weapon system configured per enemy type
    val weaponSystem = WeaponSystem(
        weapon = when (type) {
            EnemyType.SCOUT -> StandardWeapons.ENEMY_RED_LASER
            EnemyType.INTERCEPTOR -> StandardWeapons.ENEMY_RED_LASER
            EnemyType.GUNSHIP -> StandardWeapons.ENEMY_PLASMA_FAN
            EnemyType.STEALTH_BOMBER -> StandardWeapons.ENEMY_PLASMA_FAN
        },
        pool = projectilePool,
        isPlayer = false,
        audio = audio
    ).apply {
        shotCount = when (type) {
            EnemyType.SCOUT -> 1
            EnemyType.INTERCEPTOR -> 1
            EnemyType.GUNSHIP -> 3
            EnemyType.STEALTH_BOMBER -> 3
        }
    }

    init {
        val sprite = when (type) {
            EnemyType.SCOUT -> SPRITE_SCOUT
            EnemyType.INTERCEPTOR -> SPRITE_INTERCEPTOR
            EnemyType.GUNSHIP -> SPRITE_GUNSHIP
            EnemyType.STEALTH_BOMBER -> SPRITE_BOMBER
        }
        setAnimation("default", AnimationSequence.singleFrame(sprite))
        playAnimation("default")

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

    override fun update(dt: Float, context: GameContext) {
        if (!isAlive) return
        flightTime += dt
        weaponSystem.update(dt)

        val speed = parameters["speed"] ?: type.baseSpeed

        // Graceful tactical arcade flight paths
        when (type) {
            EnemyType.SCOUT -> {
                // Swift straight downward run with slight gentle drift
                velocity = Vec2(sin((flightTime + flightSeed) * 1.8f) * 18f, speed)
            }
            EnemyType.INTERCEPTOR -> {
                // Sweeping sine wave curves
                val vx = sin((flightTime + flightSeed) * 2.2f) * 42f
                velocity = Vec2(vx, speed)
            }
            EnemyType.GUNSHIP -> {
                // Steady advance downward with hover deceleration
                val vy = if (position.y < 85f) speed else speed * 0.7f
                velocity = Vec2(sin((flightTime + flightSeed) * 1.2f) * 14f, vy)
            }
            EnemyType.STEALTH_BOMBER -> {
                // Slow, heavy armored advance
                velocity = Vec2(0f, speed)
            }
        }

        super.update(dt, context)
    }

    /**
     * Checks if enemy can shoot at the player.
     */
    fun tryShoot(playerTarget: PixieObject?, context: GameContext, outList: MutableList<Projectile>): Boolean {
        if (!isAlive || position.y < 15f || position.y > 270f) return false
        val muzzlePos = position + Vec2(size.x * 0.5f, size.y)
        return weaponSystem.fire(
            origin = muzzlePos,
            headingRad = 1.5707963f, // straight down
            context = context,
            outList = outList
        )
    }

    fun takeDamage(amount: Float, particleLayer: ParticleLayer?): Boolean {
        if (!isAlive) return false

        health -= amount
        particleLayer?.spawnSparks(position + (size * 0.5f), count = 5, color = 0xFFFFEB3B.toInt())

        if (health <= 0f) {
            health = 0f
            isAlive = false

            // Death explosion based on craft size
            val explosionType = when (type) {
                EnemyType.SCOUT -> ExplosionType.AERIAL_EXPLOSION
                EnemyType.INTERCEPTOR -> ExplosionType.AERIAL_EXPLOSION
                EnemyType.GUNSHIP -> ExplosionType.GROUND_BLAST
                EnemyType.STEALTH_BOMBER -> ExplosionType.GROUND_BLAST
            }
            val scale = when (type) {
                EnemyType.SCOUT -> 1.0f
                EnemyType.INTERCEPTOR -> 1.2f
                EnemyType.GUNSHIP -> 1.6f
                EnemyType.STEALTH_BOMBER -> 1.8f
            }

            particleLayer?.spawnExplosion(explosionType, position + (size * 0.5f), scale)
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
        // Scout Craft: 14x14 Swift Delta Wing
        val SPRITE_SCOUT = PixelSprite.fromAscii(
            listOf(
                "......##......",
                ".....####.....",
                ".....####.....",
                "....######....",
                "....######....",
                "...########...",
                "..###.##.###..",
                "..##..##..##..",
                ".###..##..###.",
                ".##...##...##.",
                "##....##....##",
                "##....##....##",
                "#............#",
                ".............."
            ),
            mapOf('.' to 0x00000000, '#' to 0xFFE53935.toInt())
        )

        // Interceptor Craft: 18x16 Swept Wing Fighter
        val SPRITE_INTERCEPTOR = PixelSprite.fromAscii(
            listOf(
                "........##........",
                ".......####.......",
                ".......####.......",
                "......######......",
                ".....########.....",
                "....##########....",
                "...###..##..###...",
                "..###...##...###..",
                ".####...##...####.",
                "#####...##...#####",
                "####....##....####",
                "###.....##.....###",
                "##......##......##",
                "#.......##.......#",
                "........##........",
                ".................."
            ),
            mapOf('.' to 0x00000000, '#' to 0xFFD81B60.toInt())
        )

        // Gunship: 22x20 Heavy Armored Dreadnought
        val SPRITE_GUNSHIP = PixelSprite.fromAscii(
            listOf(
                ".........####.........",
                "........######........",
                ".......########.......",
                "......##########......",
                ".....############.....",
                "....##############....",
                "...####...##...####...",
                "..#####...##...#####..",
                ".######...##...######.",
                "#######...##...#######",
                "#######...##...#######",
                "#######...##...#######",
                ".######...##...######.",
                "..#####...##...#####..",
                "...####...##...####...",
                "....##############....",
                ".....############.....",
                "......##########......",
                ".......########.......",
                "........######........"
            ),
            mapOf('.' to 0x00000000, '#' to 0xFF8E24AA.toInt())
        )

        // Stealth Bomber: 26x22 Heavy Flying Wing
        val SPRITE_BOMBER = PixelSprite.fromAscii(
            listOf(
                "............##............",
                "...........####...........",
                "..........######..........",
                ".........########.........",
                "........##########........",
                ".......############.......",
                "......##############......",
                ".....################.....",
                "....##################....",
                "...###...########...###...",
                "..###....########....###..",
                ".###.....########.....###.",
                "####.....########.....####",
                "###......########......###",
                "##.......########.......##",
                "#........########........#",
                ".........########.........",
                "..........######..........",
                "...........####...........",
                "............##............",
                "..........................",
                ".........................."
            ),
            mapOf('.' to 0x00000000, '#' to 0xFF37474F.toInt())
        )
    }
}
