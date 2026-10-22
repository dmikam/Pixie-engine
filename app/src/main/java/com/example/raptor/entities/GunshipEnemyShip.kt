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
 * Gunship: Heavy dreadnought craft.
 * Features hover deceleration, durable shielding, and 3-way plasma fan barrages.
 */
class GunshipEnemyShip(
    id: String,
    initialPos: Vec2,
    projectilePool: ProjectilePool,
    audio: AudioPort? = null,
    onDeathDrop: ((dropPos: Vec2) -> Unit)? = null
) : EnemyShip(
    id = id,
    type = EnemyType.GUNSHIP,
    initialPos = initialPos,
    projectilePool = projectilePool,
    audio = audio,
    onDeathDrop = onDeathDrop
) {
    override val weaponSystem: WeaponSystem = WeaponSystem(
        weapon = StandardWeapons.ENEMY_PLASMA_FAN,
        pool = projectilePool,
        isPlayer = false,
        audio = audio
    ).apply {
        shotCount = 3
    }

    override val explosionType: ExplosionType = ExplosionType.GROUND_BLAST
    override val explosionScale: Float = 1.6f

    init {
        setAnimation("default", AnimationSequence.singleFrame(SPRITE_GUNSHIP))
        playAnimation("default")
    }

    override fun calculateFlightVelocity(speed: Float): Vec2 {
        val vy = if (position.y < 85f) speed else speed * 0.7f
        return Vec2(sin((flightTime + flightSeed) * 1.2f) * 14f, vy)
    }

    override fun getNextShootCooldown(): Float = 4.8f

    companion object {
        val SPRITE_GUNSHIP = PixelSprite.fromAscii(
            listOf(
                ".........####.........",
                "........######........",
                ".......########.......",
                "......##########......",
                ".....############.....",
                "....##############....",
                "...####...##...####...",
                "..#####...##...#####..",
                ".######...##...######.",
                "#######...##...#######",
                "#######...##...#######",
                "#######...##...#######",
                ".######...##...######.",
                "..#####...##...#####..",
                "...####...##...####...",
                "....##############....",
                ".....############.....",
                "......##########......",
                ".......########.......",
                "........######........"
            ),
            mapOf('.' to 0x00000000, '#' to 0xFF8E24AA.toInt())
        )
    }
}
