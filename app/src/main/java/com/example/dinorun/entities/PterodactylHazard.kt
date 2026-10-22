package com.example.dinorun.entities

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieEnemy
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.sprite.AnimationFrame
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import kotlin.math.sin

/**
 * Self-encapsulated Flying Pterodactyl aerial hazard.
 * Inherits from engine core [PixieEnemy] base class to eliminate boilerplate.
 */
class PterodactylHazard(
    id: String,
    position: Vec2,
    val speed: Float = 40f
) : PixieEnemy(
    id = id,
    position = position,
    size = Vec2(18f, 14f),
    initialHealth = 15f,
    scoreAward = 250L,
    contactDamage = 100f,
    initialClasses = setOf("hazard", "flying_hazard", "obstacle")
) {
    init {
        velocity = Vec2(-speed, 0f)
        setAnimation("fly", FLY_ANIMATION)
        playAnimation("fly")
    }

    override fun update(dt: Float, context: GameContext) {
        super.update(dt, context)
        // Natural flapping hover oscillation
        val wave = sin((context.timeElapsedSeconds * 6f).toDouble()).toFloat() * 8f * dt
        position += Vec2(0f, wave)
    }

    companion object {
        private val PALETTE = mapOf(
            '.' to 0x00000000,
            'P' to 0xFFD84315.toInt(), // Wing red
            'p' to 0xFFBF360C.toInt(), // Body shade
            'E' to 0xFFFFFFFF.toInt(), // Eye white
            'B' to 0xFF000000.toInt()  // Eye pupil
        )

        val WING_UP_SPRITE = PixelSprite.fromAscii(
            listOf(
                "...PPP............",
                "..PPPPP...........",
                ".PPPPPPP..........",
                "PPPPPPPPP.........",
                "...PPPPPPP...ppp..",
                "....PPPPPPPPppppp.",
                ".....PPPPPPPpEBpp.",
                "......PPPPPPppppp.",
                ".......PPPP.......",
                "........PP........",
                "..................",
                "..................",
                "..................",
                ".................."
            ),
            PALETTE
        )

        val WING_DOWN_SPRITE = PixelSprite.fromAscii(
            listOf(
                "..................",
                "..................",
                "..................",
                "..................",
                ".......PP....ppp..",
                "......PPPP..ppppp.",
                ".....PPPPPPPpEBpp.",
                "....PPPPPPPPppppp.",
                "...PPPPPPP........",
                "PPPPPPPPP.........",
                ".PPPPPPP..........",
                "..PPPPP...........",
                "...PPP............",
                ".................."
            ),
            PALETTE
        )

        val FLY_ANIMATION = AnimationSequence(
            name = "fly",
            frames = listOf(
                AnimationFrame(WING_UP_SPRITE, 0.18f),
                AnimationFrame(WING_DOWN_SPRITE, 0.18f)
            )
        )
    }
}
