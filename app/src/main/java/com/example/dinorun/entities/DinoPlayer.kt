package com.example.dinorun.entities

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.MotionBlurConfig
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.sprite.AnimationFrame
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite

/**
 * Self-encapsulated Player Dinosaur entity.
 *
 * Encapsulates its own sprites, run/jump/duck animations, physics parameters,
 * dynamic context-scaling formulas, and motion blur settings.
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
        private set
    var isDucking: Boolean = false
        private set

    // Internal physics properties
    private val gravity: Float = 620f
    private val baseJumpForce: Float = -245f
    private val baseSpeed: Float = 140f

    init {
        // Register internally encapsulated animations
        setAnimation("run", RUN_ANIMATION)
        setAnimation("duck", DUCK_ANIMATION)
        setAnimation("jump", JUMP_ANIMATION)
        playAnimation("run")

        // Dynamic parameter calculation based on GameContext (progression, difficulty, distance)
        parameterCalculator = { obj, ctx ->
            val difficultyScale = ctx.calculateDifficultyMultiplier(1.0f, 0.08f)
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
            // Leaping activates speed trail motion blur with natural dissolution
            motionBlur = MotionBlurConfig(enabled = true, strength = 0.65f, maxTrailSamples = 4)
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

        // Running speed evaluated dynamically from context
        val currentSpeed = parameters["runSpeed"] ?: baseSpeed
        velocity = Vec2(currentSpeed, velocity.y)

        // Gravity integration when airborne
        if (!isGrounded) {
            velocity = Vec2(velocity.x, velocity.y + gravity * dt)
        }

        // Snap to ground surface
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

    companion object {
        // Palette mapping private to Dino
        private val PALETTE = mapOf(
            '.' to 0x00000000,
            'D' to 0xFF2E7D32.toInt(), // Dino forest green
            'd' to 0xFF1B5E20.toInt(), // Dark shade
            'E' to 0xFFFFFFFF.toInt(), // Eye white
            'B' to 0xFF000000.toInt()  // Eye pupil
        )

        private val DINO_RUN_1 = PixelSprite.fromAscii(
            listOf(
                ".......DDDD.....",
                "......DDDDDD....",
                "......DDEBBD....",
                "......DDDDDD....",
                "......DDDD......",
                ".....DDDDDD.....",
                "..D.DDDDDD......",
                "..DDDDDDDDDD....",
                "..DDDDDDD.......",
                "...DDDDDD.......",
                "....DDDDD.......",
                ".....DDD........",
                ".....D.D........",
                ".....D..DD......",
                "....DD..........",
                "................"
            ),
            PALETTE
        )

        private val DINO_RUN_2 = PixelSprite.fromAscii(
            listOf(
                ".......DDDD.....",
                "......DDDDDD....",
                "......DDEBBD....",
                "......DDDDDD....",
                "......DDDD......",
                ".....DDDDDD.....",
                "..D.DDDDDD......",
                "..DDDDDDDDDD....",
                "..DDDDDDD.......",
                "...DDDDDD.......",
                "....DDDDD.......",
                ".....DDD........",
                ".....D.D........",
                "....DD..D.......",
                "........DD......",
                "................"
            ),
            PALETTE
        )

        private val DINO_JUMP = PixelSprite.fromAscii(
            listOf(
                ".......DDDD.....",
                "......DDDDDD....",
                "......DDEBBD....",
                "......DDDDDD....",
                "......DDDD......",
                ".....DDDDDD.....",
                "..D.DDDDDD......",
                "..DDDDDDDDDD....",
                "..DDDDDDD.......",
                "...DDDDDD.......",
                "....DDDDD.......",
                ".....DDD........",
                "....D...D.......",
                "...DD...DD......",
                "................",
                "................"
            ),
            PALETTE
        )

        private val DINO_DUCK_1 = PixelSprite.fromAscii(
            listOf(
                "....................",
                ".........DDDDDD.....",
                "........DDDEBBD.....",
                "..DD...DDDDDDDD.....",
                ".DDDDDDDDDDDD.......",
                "..DDDDDDDDDD........",
                "...DDDDDDDDD........",
                ".....DDDDDDD........",
                "......DD..DD........",
                ".....DD....DD.......",
                "....................",
                "...................."
            ),
            PALETTE
        )

        private val DINO_DUCK_2 = PixelSprite.fromAscii(
            listOf(
                "....................",
                ".........DDDDDD.....",
                "........DDDEBBD.....",
                "..DD...DDDDDDDD.....",
                ".DDDDDDDDDDDD.......",
                "..DDDDDDDDDD........",
                "...DDDDDDDDD........",
                ".....DDDDDDD........",
                "......D...D.........",
                ".....DD....D........",
                "............DD......",
                "...................."
            ),
            PALETTE
        )

        val RUN_ANIMATION = AnimationSequence(
            name = "run",
            frames = listOf(
                AnimationFrame(DINO_RUN_1, 0.12f),
                AnimationFrame(DINO_RUN_2, 0.12f)
            )
        )

        val DUCK_ANIMATION = AnimationSequence(
            name = "duck",
            frames = listOf(
                AnimationFrame(DINO_DUCK_1, 0.12f),
                AnimationFrame(DINO_DUCK_2, 0.12f)
            )
        )

        val JUMP_ANIMATION = AnimationSequence(
            name = "jump",
            frames = listOf(
                AnimationFrame(DINO_JUMP, 1.0f)
            )
        )
    }
}
