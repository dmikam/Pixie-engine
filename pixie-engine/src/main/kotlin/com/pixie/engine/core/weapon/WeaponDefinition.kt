package com.pixie.engine.core.weapon

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.MotionBlurConfig
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.HapticFeedbackType
import com.pixie.engine.core.sprite.PixelSprite

/**
 * Self-encapsulated weapon definition in Pixie Engine.
 *
 * Encapsulates:
 * - Bullet sprite & dimensions
 * - Motion blur trail configuration
 * - Trajectory generator (Linear, Spread, SineWave, Homing, Accelerating)
 * - Procedural explosion archetype on impact
 * - Base hit power, fire rate, bullet speed, and piercing rules
 * - Dynamic context scaling (difficulty, levels, perks, speed)
 * - Sound & haptic triggers
 *
 * Usable identically by player ships, enemy craft, ground turrets, and bosses.
 */
data class WeaponDefinition(
    val id: String,
    val name: String,
    val bulletSprite: PixelSprite,
    val bulletSize: Vec2 = Vec2(bulletSprite.width.toFloat(), bulletSprite.height.toFloat()),
    val motionBlur: MotionBlurConfig = MotionBlurConfig(enabled = true, strength = 0.5f, maxTrailSamples = 3),
    val explosionType: ExplosionType = ExplosionType.SPARK_HIT,
    val baseHitPower: Float = 10f,
    val baseFireRatePerSec: Float = 4f,
    val baseBulletSpeed: Float = 260f,
    val pierceCount: Int = 1,
    val maxLifespanSeconds: Float = 2.0f,
    val audioEvent: AudioEvent = AudioEvent.FIRE_PRIMARY,
    val hapticFeedback: HapticFeedbackType? = null,
    val trajectoryFactory: (shotIndex: Int, totalShots: Int, headingRad: Float, target: PixieObject?) -> Trajectory = { _, _, heading, _ ->
        Trajectory.Linear(heading)
    }
) {
    /**
     * Calculates effective hit power dynamically based on GameContext.
     */
    fun calculateDynamicHitPower(context: GameContext, isPlayerWeapon: Boolean = true): Float {
        val perkBonus = if (isPlayerWeapon && context.hasPerk("OVERCLOCK")) 1.35f else 1.0f
        return context.calculateDynamicProperty(
            baseValue = baseHitPower,
            difficultyWeight = if (isPlayerWeapon) -0.15f else 0.4f, // Higher difficulty makes enemies hit harder
            levelWeight = if (isPlayerWeapon) 0.15f else 0.2f,
            perkBonus = perkBonus
        )
    }

    /**
     * Calculates effective fire rate dynamically based on GameContext.
     */
    fun calculateDynamicFireRate(context: GameContext, isPlayerWeapon: Boolean = true): Float {
        val perkBonus = if (isPlayerWeapon && context.hasPerk("RAPID_FIRE")) 1.4f else 1.0f
        return context.calculateDynamicProperty(
            baseValue = baseFireRatePerSec,
            difficultyWeight = if (isPlayerWeapon) 0.0f else 0.35f, // Enemies shoot faster at higher difficulties
            levelWeight = 0.05f,
            perkBonus = perkBonus
        )
    }

    /**
     * Calculates effective projectile velocity dynamically based on GameContext.
     */
    fun calculateDynamicBulletSpeed(context: GameContext, isPlayerWeapon: Boolean = true): Float {
        return context.calculateDynamicProperty(
            baseValue = baseBulletSpeed,
            difficultyWeight = if (isPlayerWeapon) 0.0f else 0.25f,
            levelWeight = 0.05f
        )
    }
}
