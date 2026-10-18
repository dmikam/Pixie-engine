package com.example.dinorun.entities

import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.sprite.AnimationFrame
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite

/**
 * Prehistoric cactus ground obstacle.
 *
 * Self-encapsulates its own pixel sprites (single and double variants), dimensions,
 * and obstacle hazard classification.
 */
class CactusObstacle(
    id: String,
    position: Vec2,
    val isDouble: Boolean = false
) : PixieObject(
    id = id,
    position = position,
    size = if (isDouble) Vec2(18f, 18f) else Vec2(12f, 18f),
    initialClasses = setOf("hazard", "ground_hazard", "obstacle")
) {
    init {
        val sprite = if (isDouble) CACTUS_DOUBLE_SPRITE else CACTUS_SMALL_SPRITE
        setAnimation(
            "default",
            AnimationSequence("default", listOf(AnimationFrame(sprite, 1.0f)))
        )
    }

    companion object {
        private val PALETTE = mapOf(
            '.' to 0x00000000,
            'C' to 0xFF43A047.toInt(), // Cactus bright green
            'c' to 0xFF2E7D32.toInt()  // Cactus outline shade
        )

        val CACTUS_SMALL_SPRITE = PixelSprite.fromAscii(
            listOf(
                "....CCCC....",
                "....CCCC....",
                "..C.CCCC....",
                "..C.CCCC.C..",
                "..C.CCCC.C..",
                "..CCCCCCCc..",
                "..CCCCCCCc..",
                "....CCCC....",
                "....CCCC....",
                "....CCCC....",
                "....CCCC....",
                "....CCCC....",
                "....CCCC....",
                "....CCCC....",
                "....CCCC....",
                "....CCCC....",
                "....CCCC....",
                "...CCCCCC..."
            ),
            PALETTE
        )

        val CACTUS_DOUBLE_SPRITE = PixelSprite.fromAscii(
            listOf(
                "..CCCC....CCCC....",
                "..CCCC....CCCC....",
                "..CCCC..C.CCCC....",
                "..CCCC..C.CCCC.C..",
                "C.CCCC..C.CCCC.C..",
                "C.CCCC..CCCCCCCc..",
                "CCCCCC....CCCC....",
                "..CCCC....CCCC....",
                "..CCCC....CCCC....",
                "..CCCC....CCCC....",
                "..CCCC....CCCC....",
                "..CCCC....CCCC....",
                "..CCCC....CCCC....",
                "..CCCC....CCCC....",
                "..CCCC....CCCC....",
                "..CCCC....CCCC....",
                "..CCCC....CCCC....",
                ".CCCCCC..CCCCCC..."
            ),
            PALETTE
        )
    }
}
