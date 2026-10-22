package com.example.dinorun.entities

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.ObjectColorMode
import com.pixie.engine.core.entity.PixieCollectible
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.sprite.AnimationFrame
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import kotlin.math.sin

/**
 * Primal Color Energy Spark collectible.
 * Inherits from engine core [PixieCollectible] base class to eliminate boilerplate.
 */
class ColorSparkPickup(
    id: String,
    position: Vec2,
    val restoreAmount: Float = 0.15f
) : PixieCollectible(
    id = id,
    position = position,
    size = Vec2(10f, 10f),
    initialClasses = setOf("pickup", "color_spark", "coin")
) {
    private val initialY = position.y

    init {
        colorMode = ObjectColorMode.AlwaysColor
        setAnimation(
            "default",
            AnimationSequence("default", listOf(AnimationFrame(SPARK_SPRITE, 1.0f)))
        )
    }

    override fun update(dt: Float, context: GameContext) {
        super.update(dt, context)
        // Gentle vertical levitation
        val bob = sin((context.timeElapsedSeconds * 5f + position.x).toDouble()).toFloat() * 3f
        position = Vec2(position.x, initialY + bob)
    }

    override fun onCollected(collector: PixieObject, context: GameContext) {
        context.recoverColor(restoreAmount)
    }

    companion object {
        private val PALETTE = mapOf(
            '.' to 0x00000000,
            'S' to 0xFF00E5FF.toInt(), // Cyan primal spark
            's' to 0xFF00B0FF.toInt()  // Cyan spark shade
        )

        val SPARK_SPRITE = PixelSprite.fromAscii(
            listOf(
                "....SS....",
                "...SSSS...",
                "..SSSSSS..",
                ".SSSSSSSS.",
                "SSSSSSSSSS",
                "SSSSSSSSSS",
                ".SSSSSSSS.",
                "..SSSSSS..",
                "...SSSS...",
                "....SS...."
            ),
            PALETTE
        )
    }
}
