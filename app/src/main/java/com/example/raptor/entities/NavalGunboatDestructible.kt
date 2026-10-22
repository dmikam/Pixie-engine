package com.example.raptor.entities

import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.weapon.ProjectilePool
import com.pixie.engine.core.weapon.StandardWeapons
import com.pixie.engine.core.weapon.WeaponSystem

/**
 * Naval Patrol Gunboat: Armed warship patrolling coastal waters.
 * Fires dual battery salvos.
 */
class NavalGunboatDestructible(
    id: String,
    initialPos: Vec2,
    projectilePool: ProjectilePool,
    audio: AudioPort? = null,
    onDeathDrop: ((dropPos: Vec2) -> Unit)? = null,
    onChainExplosion: ((centerPos: Vec2, blastRadius: Float, blastDamage: Float) -> Unit)? = null
) : GroundDestructible(
    id = id,
    type = DestructibleType.NAVAL_GUNBOAT,
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
        setAnimation("active", AnimationSequence.singleFrame(SPRITE_GUNBOAT))
        playAnimation("active")
    }

    companion object {
        val SPRITE_GUNBOAT = PixelSprite.fromAscii(
            listOf(
                ".......##.......",
                "......####......",
                "......####......",
                ".....######.....",
                ".....######.....",
                "....########....",
                "....########....",
                "...##########...",
                "...##..##..##...",
                "..###..##..###..",
                "..############..",
                "..############..",
                ".##############.",
                ".##############.",
                "################",
                "################",
                "################",
                ".##############.",
                ".##############.",
                "..############..",
                "..############..",
                "...##########...",
                "...##########...",
                "....########....",
                ".....######.....",
                "......####......"
            ),
            mapOf('.' to 0x00000000, '#' to 0xFF37474F.toInt())
        )
    }
}
