package com.example.raptor.entities

import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.weapon.ProjectilePool
import com.pixie.engine.core.weapon.StandardWeapons
import com.pixie.engine.core.weapon.WeaponSystem

/**
 * Stealth Bomber: Heavy flying wing with massive hull plating and relentless advance.
 */
class StealthBomberEnemyShip(
    id: String,
    initialPos: Vec2,
    projectilePool: ProjectilePool,
    audio: AudioPort? = null,
    onDeathDrop: ((dropPos: Vec2) -> Unit)? = null
) : EnemyShip(
    id = id,
    type = EnemyType.STEALTH_BOMBER,
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
    override val explosionScale: Float = 1.8f

    init {
        setAnimation("default", AnimationSequence.singleFrame(SPRITE_BOMBER))
        playAnimation("default")
    }

    override fun calculateFlightVelocity(speed: Float): Vec2 {
        // Slow, heavy armored advance
        return Vec2(0f, speed)
    }

    override fun getNextShootCooldown(): Float = 5.5f

    companion object {
        val SPRITE_BOMBER = PixelSprite.fromAscii(
            listOf(
                "............##............",
                "...........####...........",
                "..........######..........",
                ".........########.........",
                "........##########........",
                ".......############.......",
                "......##############......",
                ".....################.....",
                "....##################....",
                "...###...########...###...",
                "..###....########....###..",
                ".###.....########.....###.",
                "####.....########.....####",
                "###......########......###",
                "##.......########.......##",
                "#........########........#",
                ".........########.........",
                "..........######..........",
                "...........####...........",
                "............##............",
                "..........................",
                ".........................."
            ),
            mapOf('.' to 0x00000000, '#' to 0xFF37474F.toInt())
        )
    }
}
