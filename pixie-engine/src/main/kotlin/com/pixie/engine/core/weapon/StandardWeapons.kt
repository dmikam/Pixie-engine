package com.pixie.engine.core.weapon

import com.pixie.engine.core.entity.MotionBlurConfig
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.HapticFeedbackType
import com.pixie.engine.core.sprite.PixelSprite

/**
 * Pre-configured weapons catalog providing retro arcade combat weaponry for player and enemies.
 */
object StandardWeapons {

    // 1. Vulcan Autocannon: Standard rapid-fire military kinetic cannon
    val VULCAN_CANNON = WeaponDefinition(
        id = "vulcan_cannon",
        name = "Vulcan Autocannon",
        bulletSprite = PixelSprite(
            width = 3,
            height = 8,
            pixels = intArrayOf(
                0x00000000, 0xFFFFFFFF.toInt(), 0x00000000,
                0x00000000, 0xFFFFEB3B.toInt(), 0x00000000,
                0xFFFFEB3B.toInt(), 0xFFFFC107.toInt(), 0xFFFFEB3B.toInt(),
                0xFFFFEB3B.toInt(), 0xFFFF9800.toInt(), 0xFFFFEB3B.toInt(),
                0xFFFF9800.toInt(), 0xFFFF5722.toInt(), 0xFFFF9800.toInt(),
                0xFFFF9800.toInt(), 0xFFFF5722.toInt(), 0xFFFF9800.toInt(),
                0x00000000, 0xFFFF5722.toInt(), 0x00000000,
                0x00000000, 0x00000000, 0x00000000
            )
        ),
        motionBlur = MotionBlurConfig(enabled = true, strength = 0.6f, maxTrailSamples = 3),
        explosionType = ExplosionType.SPARK_HIT,
        baseHitPower = 12f,
        baseFireRatePerSec = 7f,
        baseBulletSpeed = 230f,
        pierceCount = 1,
        audioEvent = AudioEvent.FIRE_PRIMARY,
        hapticFeedback = HapticFeedbackType.LIGHT
    )

    // 2. Twin Plasma Spreader: Dual high-energy plasma bolts
    val TWIN_PLASMA = WeaponDefinition(
        id = "twin_plasma",
        name = "Twin Plasma Spreader",
        bulletSprite = PixelSprite(
            width = 5,
            height = 7,
            pixels = intArrayOf(
                0x00000000, 0x00000000, 0xFFFFFFFF.toInt(), 0x00000000, 0x00000000,
                0x00000000, 0xFF00E5FF.toInt(), 0xFFFFFFFF.toInt(), 0xFF00E5FF.toInt(), 0x00000000,
                0xFF00E5FF.toInt(), 0xFF00B0FF.toInt(), 0xFFFFFFFF.toInt(), 0xFF00B0FF.toInt(), 0xFF00E5FF.toInt(),
                0xFF00E5FF.toInt(), 0xFF00B0FF.toInt(), 0xFF00B0FF.toInt(), 0xFF00B0FF.toInt(), 0xFF00E5FF.toInt(),
                0x00000000, 0xFF0091EA.toInt(), 0xFF00B0FF.toInt(), 0xFF0091EA.toInt(), 0x00000000,
                0x00000000, 0x00000000, 0xFF0091EA.toInt(), 0x00000000, 0x00000000,
                0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000
            )
        ),
        motionBlur = MotionBlurConfig(enabled = true, strength = 0.7f, maxTrailSamples = 4),
        explosionType = ExplosionType.PLASMA_BURST,
        baseHitPower = 18f,
        baseFireRatePerSec = 4.5f,
        baseBulletSpeed = 200f,
        pierceCount = 1,
        audioEvent = AudioEvent.FIRE_PRIMARY,
        hapticFeedback = HapticFeedbackType.MEDIUM,
        trajectoryFactory = { shotIndex, totalShots, headingRad, _ ->
            val spreadStep = 0.14f
            val offset = (shotIndex - (totalShots - 1) / 2.0f) * spreadStep
            Trajectory.Spread(headingRad, offset)
        }
    )

    // 3. Micro-Homing Missiles: Smart lock-on ordnance
    val HOMING_MISSILES = WeaponDefinition(
        id = "homing_missiles",
        name = "Micro-Homing Missiles",
        bulletSprite = PixelSprite(
            width = 4,
            height = 9,
            pixels = intArrayOf(
                0x00000000, 0xFFF44336.toInt(), 0xFFF44336.toInt(), 0x00000000,
                0xFFFFFFFF.toInt(), 0xFFCFD8DC.toInt(), 0xFFCFD8DC.toInt(), 0xFFFFFFFF.toInt(),
                0xFFB0BEC5.toInt(), 0xFF90A4AE.toInt(), 0xFF90A4AE.toInt(), 0xFFB0BEC5.toInt(),
                0xFFB0BEC5.toInt(), 0xFF78909C.toInt(), 0xFF78909C.toInt(), 0xFFB0BEC5.toInt(),
                0xFF90A4AE.toInt(), 0xFF607D8B.toInt(), 0xFF607D8B.toInt(), 0xFF90A4AE.toInt(),
                0xFF90A4AE.toInt(), 0xFF455A64.toInt(), 0xFF455A64.toInt(), 0xFF90A4AE.toInt(),
                0x00000000, 0xFFFF9800.toInt(), 0xFFFF9800.toInt(), 0x00000000,
                0x00000000, 0xFFFF5722.toInt(), 0xFFFF5722.toInt(), 0x00000000,
                0x00000000, 0x00000000, 0x00000000, 0x00000000
            )
        ),
        motionBlur = MotionBlurConfig(enabled = true, strength = 0.5f, maxTrailSamples = 5),
        explosionType = ExplosionType.AERIAL_EXPLOSION,
        baseHitPower = 35f,
        baseFireRatePerSec = 2.0f,
        baseBulletSpeed = 160f,
        pierceCount = 1,
        audioEvent = AudioEvent.FIRE_MISSILE,
        hapticFeedback = HapticFeedbackType.HEAVY,
        trajectoryFactory = { _, _, headingRad, target ->
            Trajectory.Homing(target = target, turnRateRadPerSec = 4.8f, currentHeadingRad = headingRad)
        }
    )

    // 4. Enemy Crimson Laser: Standard hostile pulse bolt
    val ENEMY_RED_LASER = WeaponDefinition(
        id = "enemy_red_laser",
        name = "Enemy Crimson Laser",
        bulletSprite = PixelSprite(
            width = 3,
            height = 6,
            pixels = intArrayOf(
                0x00000000, 0xFFFF8A80.toInt(), 0x00000000,
                0xFFFF5252.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF5252.toInt(),
                0xFFFF1744.toInt(), 0xFFFF5252.toInt(), 0xFFFF1744.toInt(),
                0xFFFF1744.toInt(), 0xFFFF1744.toInt(), 0xFFFF1744.toInt(),
                0x00000000, 0xFFD50000.toInt(), 0x00000000,
                0x00000000, 0x00000000, 0x00000000
            )
        ),
        motionBlur = MotionBlurConfig(enabled = true, strength = 0.4f, maxTrailSamples = 2),
        explosionType = ExplosionType.SPARK_HIT,
        baseHitPower = 8f,
        baseFireRatePerSec = 1.3f,
        baseBulletSpeed = 100f,
        pierceCount = 1,
        audioEvent = AudioEvent.FIRE_PRIMARY
    )

    // 5. Enemy Heavy Plasma Fan: 3-way spread fired by heavy gunships
    val ENEMY_PLASMA_FAN = WeaponDefinition(
        id = "enemy_plasma_fan",
        name = "Enemy Plasma Fan",
        bulletSprite = PixelSprite(
            width = 4,
            height = 5,
            pixels = intArrayOf(
                0x00000000, 0xFFFF80AB.toInt(), 0xFFFF80AB.toInt(), 0x00000000,
                0xFFFF4081.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF4081.toInt(),
                0xFFF50057.toInt(), 0xFFFF4081.toInt(), 0xFFFF4081.toInt(), 0xFFF50057.toInt(),
                0x00000000, 0xFFC51162.toInt(), 0xFFC51162.toInt(), 0x00000000,
                0x00000000, 0x00000000, 0x00000000, 0x00000000
            )
        ),
        motionBlur = MotionBlurConfig(enabled = true, strength = 0.5f, maxTrailSamples = 3),
        explosionType = ExplosionType.PLASMA_BURST,
        baseHitPower = 14f,
        baseFireRatePerSec = 0.7f,
        baseBulletSpeed = 85f,
        pierceCount = 1,
        audioEvent = AudioEvent.FIRE_PRIMARY,
        trajectoryFactory = { shotIndex, totalShots, headingRad, _ ->
            val spreadStep = 0.22f
            val offset = (shotIndex - (totalShots - 1) / 2.0f) * spreadStep
            Trajectory.Spread(headingRad, offset)
        }
    )
}
