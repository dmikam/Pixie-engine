package com.example.raptor.entities

import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.weapon.ProjectilePool
import com.pixie.engine.core.weapon.StandardWeapons
import com.pixie.engine.core.weapon.WeaponSystem

/**
 * SAM Launch Silo: Hardened missile fortification.
 * Fires dual projectile barrages at approaching player aircraft.
 */
class MissileSiloDestructible(
    id: String,
    initialPos: Vec2,
    projectilePool: ProjectilePool,
    audio: AudioPort? = null,
    onDeathDrop: ((dropPos: Vec2) -> Unit)? = null,
    onChainExplosion: ((centerPos: Vec2, blastRadius: Float, blastDamage: Float) -> Unit)? = null
) : GroundDestructible(
    id = id,
    type = DestructibleType.MISSILE_SILO,
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
        shotCount = 2
    }

    init {
        setAnimation("active", AnimationSequence.singleFrame(SPRITE_MISSILE_SILO))
        playAnimation("active")
    }

    companion object {
        val SPRITE_MISSILE_SILO = PixelSprite.fromAscii(
            listOf(
                "....##########....",
                "..##############..",
                ".################.",
                "##################",
                "###....####....###",
                "###....####....###",
                "##......##......##",
                "##......##......##",
                "##......##......##",
                "##......##......##",
                "###....####....###",
                "###....####....###",
                "##################",
                "##################",
                ".################.",
                "..##############..",
                "....##########....",
                "......######......"
            ),
            mapOf('.' to 0x00000000, '#' to 0xFFE53935.toInt())
        )
    }
}
