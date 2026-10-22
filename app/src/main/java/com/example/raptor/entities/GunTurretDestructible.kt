package com.example.raptor.entities

import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.weapon.ProjectilePool
import com.pixie.engine.core.weapon.StandardWeapons
import com.pixie.engine.core.weapon.WeaponSystem

/**
 * Coastal Gun Battery: Fixed ground defense installation.
 * Fires single red laser bolts at approaching fighters.
 */
class GunTurretDestructible(
    id: String,
    initialPos: Vec2,
    projectilePool: ProjectilePool,
    audio: AudioPort? = null,
    onDeathDrop: ((dropPos: Vec2) -> Unit)? = null,
    onChainExplosion: ((centerPos: Vec2, blastRadius: Float, blastDamage: Float) -> Unit)? = null
) : GroundDestructible(
    id = id,
    type = DestructibleType.GUN_TURRET,
    initialPos = initialPos,
    projectilePool = projectilePool,
    audio = audio,
    onDeathDrop = onDeathDrop,
    onChainExplosion = onChainExplosion
) {
    override val weaponSystem: WeaponSystem = WeaponSystem(
        weapon = StandardWeapons.ENEMY_RED_LASER,
        pool = projectilePool,
        isPlayer = false,
        audio = audio
    ).apply {
        shotCount = 1
    }

    init {
        setAnimation("active", AnimationSequence.singleFrame(SPRITE_TURRET))
        playAnimation("active")
    }

    companion object {
        val SPRITE_TURRET = PixelSprite.fromAscii(
            listOf(
                "...##......##...",
                "...##......##...",
                "..####....####..",
                "..####....####..",
                ".##############.",
                ".##############.",
                "################",
                "####..####..####",
                "####..####..####",
                "################",
                "################",
                ".##############.",
                ".##############.",
                "..############..",
                "...##########...",
                "....########...."
            ),
            mapOf('.' to 0x00000000, '#' to 0xFF546E7A.toInt())
        )
    }
}
