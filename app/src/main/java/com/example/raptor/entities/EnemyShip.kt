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
    SCOUT(18f, 130f, 150L, 14, 14),
    INTERCEPTOR(40f, 110f, 350L, 18, 16),
    GUNSHIP(90f, 65f, 800L, 22, 20),
    STEALTH_BOMBER(140f, 50f, 1500L, 26, 22)
}

/**
 * Hostile combat craft in Raptor.
 *
 * Encapsulates:
 * - Dynamic HP & flight trajectory (sine swoop, straight charge, hover strafe)
 * - Self-encapsulated hostile weapon system (Red Lasers, Plasma Fans)
 * - Procedural drops (weapon upgrade, shield, repair, score gems)
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
            EnemyType.INTERCEPTOR -> 2
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
            // Health scales with difficulty (0..100) and progression level
            val scaledHp = ctx.calculateDynamicProperty(
                baseValue = type.baseHealth,
                difficultyWeight = 0.5f,
                levelWeight = 0.2f
            )
            enemy.parameters["maxHealth"] = scaledHp
            if (health == maxHealth) {
                maxHealth = scaledHp
                health = scaledHp
            }

            // Flight speed
            val scaledSpeed = ctx.calculateDynamicProperty(
                baseValue = type.baseSpeed,
                difficultyWeight = 0.3f,
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

        // Trajectory by archetype
        when (type) {
            EnemyType.SCOUT -> {
                // Swift straight downward run with slight horizontal drift
                velocity = Vec2(sin(flightTime * 3f) * 30f, speed)
            }
            EnemyType.INTERCEPTOR -> {
                // Wide sweeping sine wave swoop
                val vx = sin(flightTime * 4.5f) * 70f
                velocity = Vec2(vx, speed)
            }
            EnemyType.GUNSHIP -> {
                // Steady advance downward with hover deceleration
                val vy = if (position.y < 90f) speed else speed * 0.6f
                velocity = Vec2(sin(flightTime * 2f) * 20f, vy)
            }
            EnemyType.STEALTH_BOMBER -> {
                // Slow armored advance
                velocity = Vec2(0f, speed)
            }
        }

        super.update(dt, context)
    }

    /**
     * Checks if enemy can shoot at the player.
     */
    fun tryShoot(playerTarget: PixieObject?, context: GameContext, outList: MutableList<Projectile>): Boolean {
        if (!isAlive || position.y < 10f) return false
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

            // Spawns procedural explosion
            val explosion = if (type == EnemyType.GUNSHIP || type == EnemyType.STEALTH_BOMBER) {
                ExplosionType.TITAN_CHAIN
            } else {
                ExplosionType.AERIAL_EXPLOSION
            }
            particleLayer?.spawnExplosion(explosion, position + (size * 0.5f), scale = 1.2f)
            audio?.playSound(AudioEvent.EXPLOSION_SMALL)

            // Trigger drop
            onDeathDrop?.invoke(position + (size * 0.5f))
            return true
        }
        return false
    }

    companion object {
        val SPRITE_SCOUT = PixelSprite.fromAscii(
            listOf(
                "....######....",
                "..##########..",
                ".############.",
                "##############",
                "###...##...###",
                "###...##...###",
                "##############",
                ".############.",
                "..##########..",
                "...########...",
                "....######....",
                ".....####.....",
                "......##......",
                "......##......"
            ),
            mapOf('.' to 0x00000000, '#' to 0xFFD32F2F.toInt()) // Crimson Red
        )

        val SPRITE_INTERCEPTOR = PixelSprite.fromAscii(
            listOf(
                "#...######...#",
                "##.########.##",
                "##############",
                ".############.",
                "..##########..",
                "...########...",
                "....######....",
                ".....####.....",
                "....######....",
                "...########...",
                "..##########..",
                ".############.",
                "##############",
                "##.########.##",
                "#...######...#",
                ".....####....."
            ),
            mapOf('.' to 0x00000000, '#' to 0xFFE65100.toInt()) // Amber Orange
        )

        val SPRITE_GUNSHIP = PixelSprite.fromAscii(
            listOf(
                "......##########......",
                "....##############....",
                "..##################..",
                "######################",
                "######################",
                "###..############..###",
                "###..############..###",
                "######################",
                "######################",
                "..##################..",
                "....##############....",
                "......##########......",
                "........######........",
                ".........####.........",
                ".........####.........",
                "........######........",
                "......##########......",
                "....##############....",
                "....##############....",
                "......##########......"
            ),
            mapOf('.' to 0x00000000, '#' to 0xFF5D4037.toInt()) // Armored Brown
        )

        val SPRITE_BOMBER = PixelSprite.fromAscii(
            listOf(
                "........##########........",
                "......##############......",
                "....##################....",
                "..######################..",
                "##########################",
                "##########################",
                "###..################..###",
                "###..################..###",
                "##########################",
                "##########################",
                "..######################..",
                "....##################....",
                "......##############......",
                "........##########........",
                "..........######..........",
                "...........####...........",
                "..........######..........",
                "........##########........",
                "......##############......",
                "....##################....",
                "....##################....",
                "......##############......"
            ),
            mapOf('.' to 0x00000000, '#' to 0xFF263238.toInt()) // Stealth Slate
        )
    }
}
