package com.example.raptor.entities

import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.weapon.ProjectilePool
import com.pixie.engine.core.weapon.StandardWeapons
import com.pixie.engine.core.weapon.WeaponSystem
import kotlin.math.sin

/**
 * Interceptor Craft: Swept-wing fighter with dynamic sine curve flight maneuvers.
 */
class InterceptorEnemyShip(
    id: String,
    initialPos: Vec2,
    projectilePool: ProjectilePool,
    audio: AudioPort? = null,
    onDeathDrop: ((dropPos: Vec2) -> Unit)? = null
) : EnemyShip(
    id = id,
    type = EnemyType.INTERCEPTOR,
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
    override val explosionScale: Float = 1.2f

    init {
        setAnimation("default", AnimationSequence.singleFrame(SPRITE_INTERCEPTOR))
        playAnimation("default")
    }

    override fun calculateFlightVelocity(speed: Float): Vec2 {
        val vx = sin((flightTime + flightSeed) * 2.2f) * 42f
        return Vec2(vx, speed)
    }

    override fun getNextShootCooldown(): Float = 4.5f

    companion object {
        val SPRITE_INTERCEPTOR = PixelSprite.fromAscii(
            listOf(
                "........##........",
                ".......####.......",
                ".......####.......",
                "......######......",
                ".....########.....",
                "....##########....",
                "...###..##..###...",
                "..###...##...###..",
                ".####...##...####.",
                "#####...##...#####",
                "####....##....####",
                "###.....##.....###",
                "##......##......##",
                "#.......##.......#",
                "........##........",
                ".................."
            ),
            mapOf('.' to 0x00000000, '#' to 0xFFD81B60.toInt())
        )
    }
}
