package com.example.dinorun.entities

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.MotionBlurConfig
import com.pixie.engine.core.entity.ObjectColorMode
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.sprite.AnimationFrame
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite

/**
 * Falling volcanic meteor hazard.
 *
 * Encapsulates its molten flame sprite, proximity-activated falling trigger,
 * ground impact detection, and motion blur trail.
 *
 * Strictly prevents falling below [groundY] to ensure playability and visual realism.
 */
class VolcanicMeteor(
    id: String,
    val targetGroundX: Float,
    val groundY: Float,
    val triggerDistance: Float = 180f
) : PixieObject(
    id = id,
    position = Vec2(targetGroundX + 45f, -20f), // Starts above viewport
    velocity = Vec2(-40f, 130f),
    size = Vec2(12f, 12f),
    initialClasses = setOf("hazard", "meteor")
) {
    var hasStartedFalling: Boolean = false
        private set
    var hasImpactedGround: Boolean = false
        private set
    private var impactTimeRemaining: Float = 0.25f

    init {
        colorMode = ObjectColorMode.AlwaysColor
        motionBlur = MotionBlurConfig(enabled = false, strength = 0.75f, maxTrailSamples = 4)
        setAnimation(
            "default",
            AnimationSequence("default", listOf(AnimationFrame(METEOR_SPRITE, 1.0f)))
        )
        setAnimation(
            "impact",
            AnimationSequence("impact", listOf(AnimationFrame(IMPACT_SPRITE, 0.25f)))
        )
    }

    override fun update(dt: Float, context: GameContext) {
        // 1. Proximity trigger: only start falling when player is within warning distance
        if (!hasStartedFalling) {
            val playerX = context.distance * 10f // Convert back to world units
            if (targetGroundX - playerX <= triggerDistance) {
                hasStartedFalling = true
                motionBlur = MotionBlurConfig(enabled = true, strength = 0.75f, maxTrailSamples = 4)
            } else {
                // Keep above screen until triggered
                return
            }
        }

        // 2. Impact state handling
        if (hasImpactedGround) {
            impactTimeRemaining -= dt
            if (impactTimeRemaining <= 0f) {
                isAlive = false // Cleanly despawn after scorch effect
            }
            return
        }

        super.update(dt, context)

        // 3. Ground impact detection: clamp strictly at ground surface and trigger impact
        val groundContactY = groundY - size.y
        if (position.y >= groundContactY) {
            position = Vec2(position.x, groundContactY)
            velocity = Vec2.ZERO
            hasImpactedGround = true
            motionBlur = MotionBlurConfig(enabled = false)
            playAnimation("impact")
        }
    }

    companion object {
        private val PALETTE = mapOf(
            '.' to 0x00000000,
            'M' to 0xFFFF5722.toInt(), // Meteor outer magma
            'm' to 0xFFD32F2F.toInt(), // Meteor inner core
            'Y' to 0xFFFFEB3B.toInt(), // Impact yellow flash
            'O' to 0xFFFF9800.toInt()  // Impact orange flash
        )

        val METEOR_SPRITE = PixelSprite.fromAscii(
            listOf(
                "....MMMM....",
                "...MMMMMM...",
                "..MMmmmmMM..",
                ".MMmmmmmmMM.",
                "MMmmmmmmmmMM",
                "MMmmmmmmmmMM",
                "MMmmmmmmmmMM",
                "MMmmmmmmmmMM",
                ".MMmmmmmmMM.",
                "..MMmmmmMM..",
                "...MMMMMM...",
                "....MMMM...."
            ),
            PALETTE
        )

        val IMPACT_SPRITE = PixelSprite.fromAscii(
            listOf(
                "....YYYY....",
                "..YYOOOOYY..",
                ".YOOmmmmOOY.",
                "YOmmmmmmmmOY",
                "YOmmmmmmmmOY",
                "YOmmmmmmmmOY",
                "YOmmmmmmmmOY",
                "YOmmmmmmmmOY",
                ".YOOmmmmOOY.",
                "..YYOOOOYY..",
                "....YYYY....",
                "............"
            ),
            PALETTE
        )
    }
}
