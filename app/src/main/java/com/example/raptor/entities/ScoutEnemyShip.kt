package com.example.raptor.entities

import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.weapon.ProjectilePool
import com.pixie.engine.core.weapon.StandardWeapons
import com.pixie.engine.core.weapon.Trajectory
import com.pixie.engine.core.weapon.WeaponSystem

/**
 * Scout Craft: Swift delta-wing reconnaissance fighter.
 * Features rapid downward speed, subtle evasive drifts, and single laser salvos.
 */
class ScoutEnemyShip(
    id: String,
    initialPos: Vec2,
    projectilePool: ProjectilePool,
    audio: AudioPort? = null,
    onDeathDrop: ((dropPos: Vec2) -> Unit)? = null
) : EnemyShip(
    id = id,
    type = EnemyType.SCOUT,
    initialPos = initialPos,
    projectilePool = projectilePool,
    audio = audio,
    onDeathDrop = onDeathDrop
) {
    override val weaponSystem: WeaponSystem = WeaponSystem(
        weapon = StandardWeapons.ENEMY_RED_LASER,
        pool = projectilePool,
        isPlayer = false,
        audio = audio
    ).apply {
        shotCount = 1
    }

    override val explosionType: ExplosionType = ExplosionType.AERIAL_EXPLOSION
    override val explosionScale: Float = 1.0f

    val trajectory: Trajectory = Trajectory.SineWave(
        baseAngleRad = 1.5707963f,
        frequency = 1.8f,
        amplitude = 18f
    )

    init {
        setAnimation("default", AnimationSequence.singleFrame(SPRITE_SCOUT))
        playAnimation("default")
    }

    override fun calculateFlightVelocity(speed: Float): Vec2 {
        return trajectory.computeVelocity(
            currentPosition = position,
            currentVelocity = velocity,
            flightTime = flightTime + flightSeed,
            speed = speed,
            dt = 0f
        )
    }

    override fun getNextShootCooldown(): Float = 5.0f

    companion object {
        val SPRITE_SCOUT = PixelSprite.fromAscii(
            listOf(
                "......##......",
                ".....####.....",
                ".....####.....",
                "....######....",
                "....######....",
                "...########...",
                "..###.##.###..",
                "..##..##..##..",
                ".###..##..###.",
                ".##...##...##.",
                "##....##....##",
                "##....##....##",
                "#............#",
                ".............."
            ),
            mapOf('.' to 0x00000000, '#' to 0xFFE53935.toInt())
        )
    }
}
