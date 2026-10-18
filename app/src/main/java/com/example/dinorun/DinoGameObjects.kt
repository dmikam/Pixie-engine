package com.example.dinorun

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.entity.MotionBlurConfig
import com.pixie.engine.core.entity.ObjectColorMode
import com.pixie.engine.core.entity.PixieObject
import kotlin.math.sin

/**
 * Player dinosaur entity with running, ducking, jumping, and dynamic physics.
 */
class DinoPlayer(
    initialPosition: Vec2,
    val groundY: Float
) : PixieObject(
    id = "player_dino",
    position = initialPosition,
    size = Vec2(16f, 16f),
    initialClasses = setOf("player")
) {
    var isGrounded: Boolean = true
    var isDucking: Boolean = false

    private val gravity: Float = 620f
    private var baseJumpForce: Float = -245f
    private var baseSpeed: Float = 140f

    init {
        setAnimation("run", DinoSprites.dinoRunAnim)
        setAnimation("duck", DinoSprites.dinoDuckAnim)
        setAnimation("jump", DinoSprites.dinoJumpAnim)
        playAnimation("run")

        // Dynamic parameter calculation based on GameContext
        parameterCalculator = { obj, ctx ->
            val difficultyScale = ctx.calculateDifficultyMultiplier(1.0f, 0.08f)
            // Running speed scales smoothly with game progression
            obj.parameters["runSpeed"] = baseSpeed * difficultyScale
            obj.parameters["jumpForce"] = baseJumpForce
        }
    }

    fun jump() {
        if (isGrounded && !isDucking) {
            val jumpPower = parameters["jumpForce"] ?: baseJumpForce
            velocity = Vec2(velocity.x, jumpPower)
            isGrounded = false
            playAnimation("jump")
            // Enable speed trail motion blur during leaping
            motionBlur = MotionBlurConfig(enabled = true, strength = 0.5f, maxTrailSamples = 3)
        }
    }

    fun setDuckingState(ducking: Boolean) {
        if (isDucking == ducking) return
        isDucking = ducking
        if (isDucking) {
            size = Vec2(20f, 12f)
            playAnimation("duck")
        } else {
            size = Vec2(16f, 16f)
            playAnimation(if (isGrounded) "run" else "jump")
        }
    }

    override fun update(dt: Float, context: GameContext) {
        super.update(dt, context)

        // Horizontal velocity driven by context speed
        val currentSpeed = parameters["runSpeed"] ?: baseSpeed
        velocity = Vec2(currentSpeed, velocity.y)

        // Apply gravity if airborne
        if (!isGrounded) {
            velocity = Vec2(velocity.x, velocity.y + gravity * dt)
        }

        // Ground collision & landing
        val standingY = groundY - size.y
        if (position.y >= standingY) {
            position = Vec2(position.x, standingY)
            velocity = Vec2(velocity.x, 0f)
            if (!isGrounded) {
                isGrounded = true
                motionBlur = MotionBlurConfig(enabled = false)
                playAnimation(if (isDucking) "duck" else "run")
            }
        } else {
            isGrounded = false
        }
    }
}

/**
 * Prehistoric cactus ground obstacle.
 */
class CactusObstacle(
    id: String,
    position: Vec2,
    isDouble: Boolean = false
) : PixieObject(
    id = id,
    position = position,
    size = if (isDouble) Vec2(18f, 18f) else Vec2(12f, 18f),
    initialClasses = setOf("hazard", "ground_hazard", "obstacle")
) {
    init {
        val sprite = if (isDouble) DinoSprites.CACTUS_DOUBLE else DinoSprites.CACTUS_SMALL
        setAnimation("default", com.pixie.engine.core.sprite.AnimationSequence("default", listOf(com.pixie.engine.core.sprite.AnimationFrame(sprite))))
    }
}

/**
 * Flying Pterodactyl aerial hazard requiring jumping or ducking.
 */
class PterodactylHazard(
    id: String,
    position: Vec2,
    val speed: Float = 160f
) : PixieObject(
    id = id,
    position = position,
    velocity = Vec2(-speed, 0f),
    size = Vec2(18f, 14f),
    initialClasses = setOf("hazard", "flying_hazard", "obstacle")
) {
    init {
        setAnimation("fly", DinoSprites.pterodactylAnim)
        playAnimation("fly")
    }

    override fun update(dt: Float, context: GameContext) {
        super.update(dt, context)
        // Extra flap hover wave
        val wave = sin((context.timeElapsedSeconds * 6f).toDouble()).toFloat() * 8f * dt
        position += Vec2(0f, wave)
    }
}

/**
 * Primal Color Energy Spark.
 *
 * Configured with ObjectColorMode.AlwaysColor so it is always vividly colored
 * (cyan/gold) even when the surrounding game world is in full grayscale.
 */
class ColorSparkPickup(
    id: String,
    position: Vec2,
    val restoreAmount: Float = 0.15f
) : PixieObject(
    id = id,
    position = position,
    size = Vec2(10f, 10f),
    initialClasses = setOf("pickup", "color_spark", "coin")
) {
    private val initialY = position.y

    init {
        colorMode = ObjectColorMode.AlwaysColor // Always vivid glowing cyan!
        setAnimation(
            "default",
            com.pixie.engine.core.sprite.AnimationSequence(
                "default",
                listOf(com.pixie.engine.core.sprite.AnimationFrame(DinoSprites.COLOR_SPARK))
            )
        )
    }

    override fun update(dt: Float, context: GameContext) {
        super.update(dt, context)
        // Gentle bobbing motion
        val bob = sin((context.timeElapsedSeconds * 5f + position.x).toDouble()).toFloat() * 3f
        position = Vec2(position.x, initialY + bob)
    }
}

/**
 * Falling volcanic meteor hazard with motion blur trail.
 */
class VolcanicMeteor(
    id: String,
    position: Vec2
) : PixieObject(
    id = id,
    position = position,
    velocity = Vec2(-35f, 90f),
    size = Vec2(12f, 12f),
    initialClasses = setOf("hazard", "meteor")
) {
    init {
        colorMode = ObjectColorMode.AlwaysColor // Fiery orange/red
        motionBlur = MotionBlurConfig(enabled = true, strength = 0.75f, maxTrailSamples = 4)
        setAnimation(
            "default",
            com.pixie.engine.core.sprite.AnimationSequence(
                "default",
                listOf(com.pixie.engine.core.sprite.AnimationFrame(DinoSprites.METEOR))
            )
        )
    }
}
