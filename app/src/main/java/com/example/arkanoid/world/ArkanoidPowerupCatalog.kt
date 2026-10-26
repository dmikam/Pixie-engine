package com.example.arkanoid.world

import com.example.arkanoid.entities.ArkanoidPaddle
import com.example.arkanoid.entities.EnergyBall
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.matrix.PowerUpType
import kotlin.math.abs
import kotlin.random.Random

data class PowerupDefinition(
    val type: PowerUpType,
    val displayName: String,
    val statusLabel: String,
    val glyph: String,
    val durationSeconds: Float = 0f,
    val scoreBonus: Int = 500,
    val dropWeight: Float = 20f,
    val primaryColor: Int,
    val glyphColor: Int = 0xFFFFFFFF.toInt()
)

/**
 * Catalog for Arkanoid powerups, defining probability tables,
 * durations, level assignment matrices, and effect executions.
 */
object ArkanoidPowerupCatalog {

    const val DURATION_LASER: Float = 15f
    const val DURATION_WIDE_PADDLE: Float = 18f
    const val DURATION_PLASMA_BALL: Float = 9f
    const val DEFAULT_BONUS_SCORE: Int = 500

    private val catalog: Map<PowerUpType, PowerupDefinition> = mapOf(
        PowerUpType.LASER to PowerupDefinition(
            type = PowerUpType.LASER,
            displayName = "Laser Cannon",
            statusLabel = "LASER CANNON",
            glyph = "L",
            durationSeconds = DURATION_LASER,
            scoreBonus = DEFAULT_BONUS_SCORE,
            dropWeight = 20f,
            primaryColor = 0xFFFF1744.toInt()
        ),
        PowerUpType.WIDE_PADDLE to PowerupDefinition(
            type = PowerUpType.WIDE_PADDLE,
            displayName = "Wide Paddle",
            statusLabel = "WIDE PADDLE",
            glyph = "W",
            durationSeconds = DURATION_WIDE_PADDLE,
            scoreBonus = DEFAULT_BONUS_SCORE,
            dropWeight = 25f,
            primaryColor = 0xFF00E5FF.toInt()
        ),
        PowerUpType.MULTI_BALL to PowerupDefinition(
            type = PowerUpType.MULTI_BALL,
            displayName = "Multi-Ball",
            statusLabel = "MULTI-BALL",
            glyph = "M",
            durationSeconds = 0f,
            scoreBonus = DEFAULT_BONUS_SCORE,
            dropWeight = 20f,
            primaryColor = 0xFF00E676.toInt()
        ),
        PowerUpType.PLASMA_BALL to PowerupDefinition(
            type = PowerUpType.PLASMA_BALL,
            displayName = "Plasma Sphere",
            statusLabel = "PLASMA SPHERE",
            glyph = "P",
            durationSeconds = DURATION_PLASMA_BALL,
            scoreBonus = DEFAULT_BONUS_SCORE,
            dropWeight = 15f,
            primaryColor = 0xFFFF9100.toInt()
        ),
        PowerUpType.SLOW_BALL to PowerupDefinition(
            type = PowerUpType.SLOW_BALL,
            displayName = "Slow Ball",
            statusLabel = "SLOW SPEED",
            glyph = "S",
            durationSeconds = 0f,
            scoreBonus = DEFAULT_BONUS_SCORE,
            dropWeight = 10f,
            primaryColor = 0xFF2979FF.toInt()
        ),
        PowerUpType.SAFETY_BARRIER to PowerupDefinition(
            type = PowerUpType.SAFETY_BARRIER,
            displayName = "Safety Barrier",
            statusLabel = "SAFETY BARRIER ACTIVE",
            glyph = "B",
            durationSeconds = 0f,
            scoreBonus = DEFAULT_BONUS_SCORE,
            dropWeight = 10f,
            primaryColor = 0xFFD500F9.toInt()
        )
    )

    fun getDefinition(type: PowerUpType): PowerupDefinition {
        return catalog[type] ?: catalog.values.first()
    }

    fun getBonusScore(type: PowerUpType): Int = getDefinition(type).scoreBonus

    /**
     * Selects a random powerup according to weighted drop probabilities.
     */
    fun rollRandomPowerup(random: Random = Random.Default): PowerUpType {
        val totalWeight = catalog.values.sumOf { it.dropWeight.toDouble() }
        var roll = random.nextDouble() * totalWeight
        for (item in catalog.values) {
            roll -= item.dropWeight
            if (roll <= 0.0) return item.type
        }
        return PowerUpType.WIDE_PADDLE
    }

    /**
     * Determines whether a destroyed brick should drop a capsule.
     */
    fun rollDropFromHit(
        wasDestroyed: Boolean,
        dropChance: Float = 0.22f,
        random: Random = Random.Default
    ): PowerUpType? {
        if (!wasDestroyed) return null
        return if (random.nextFloat() < dropChance) rollRandomPowerup(random) else null
    }

    /**
     * Deterministic powerup assignment table for hand-crafted level layouts.
     */
    fun assignPowerupForLayout(levelNumber: Int, row: Int, col: Int): PowerUpType? {
        return when (levelNumber) {
            1 -> when {
                row == 0 && col == 5 -> PowerUpType.LASER
                row == 1 && col == 2 -> PowerUpType.WIDE_PADDLE
                row == 2 && col == 9 -> PowerUpType.MULTI_BALL
                row == 3 && col == 6 -> PowerUpType.SLOW_BALL
                row == 4 && col == 6 -> PowerUpType.SAFETY_BARRIER
                else -> null
            }
            2 -> when {
                row == 1 && col == 5 -> PowerUpType.PLASMA_BALL
                row == 2 && col == 4 -> PowerUpType.WIDE_PADDLE
                row == 4 && col == 7 -> PowerUpType.LASER
                row == 4 && col == 4 -> PowerUpType.MULTI_BALL
                else -> null
            }
            3 -> when {
                row == 0 && col == 5 -> PowerUpType.SAFETY_BARRIER
                row == 3 && col == 5 -> PowerUpType.PLASMA_BALL
                row == 5 && col == 2 -> PowerUpType.MULTI_BALL
                row == 6 && col == 6 -> PowerUpType.LASER
                else -> null
            }
            else -> null
        }
    }

    /**
     * Applies powerup mechanics directly to the paddle and balls.
     */
    fun applyEffect(
        type: PowerUpType,
        paddle: ArkanoidPaddle,
        balls: MutableList<EnergyBall>,
        baseBallSpeed: Float
    ) {
        when (type) {
            PowerUpType.WIDE_PADDLE,
            PowerUpType.LASER,
            PowerUpType.SAFETY_BARRIER -> {
                paddle.applyPowerUp(type)
            }
            PowerUpType.MULTI_BALL -> {
                splitBalls(balls)
            }
            PowerUpType.PLASMA_BALL -> {
                val duration = getDefinition(PowerUpType.PLASMA_BALL).durationSeconds
                for (b in balls) b.makePlasma(duration = duration)
            }
            PowerUpType.SLOW_BALL -> {
                for (b in balls) b.speed = baseBallSpeed
            }
        }
    }

    private fun splitBalls(balls: MutableList<EnergyBall>) {
        val newBalls = mutableListOf<EnergyBall>()
        for (b in balls) {
            if (b.isAlive && !b.isDocked) {
                newBalls.add(EnergyBall(b.position, Vec2(-b.velocity.x, b.velocity.y), b.radius).apply {
                    isDocked = false
                    speed = b.speed
                })
                newBalls.add(EnergyBall(b.position, Vec2(b.velocity.x * 0.7f, -abs(b.velocity.y)), b.radius).apply {
                    isDocked = false
                    speed = b.speed
                })
            }
        }
        balls.addAll(newBalls)
    }

    /**
     * Generates HUD active status pill label with remaining timer where applicable.
     */
    fun formatActiveStatus(paddle: ArkanoidPaddle, balls: List<EnergyBall>): String? {
        return when {
            paddle.isLaser -> "${getDefinition(PowerUpType.LASER).statusLabel} (${paddle.laserTimer.toInt()}s)"
            paddle.isWide -> "${getDefinition(PowerUpType.WIDE_PADDLE).statusLabel} (${paddle.wideTimer.toInt()}s)"
            paddle.hasBarrier -> getDefinition(PowerUpType.SAFETY_BARRIER).statusLabel
            balls.any { it.isPlasma } -> getDefinition(PowerUpType.PLASMA_BALL).statusLabel
            else -> null
        }
    }
}
