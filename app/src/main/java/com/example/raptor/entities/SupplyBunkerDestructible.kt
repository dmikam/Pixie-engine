package com.example.raptor.entities

import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.weapon.ProjectilePool

/**
 * Reinforced Supply Bunker: Armored concrete structure housing valuable munitions.
 * High durability and guaranteed power-up drop when breached.
 */
class SupplyBunkerDestructible(
    id: String,
    initialPos: Vec2,
    projectilePool: ProjectilePool,
    audio: AudioPort? = null,
    onDeathDrop: ((dropPos: Vec2) -> Unit)? = null,
    onChainExplosion: ((centerPos: Vec2, blastRadius: Float, blastDamage: Float) -> Unit)? = null
) : GroundDestructible(
    id = id,
    type = DestructibleType.SUPPLY_BUNKER,
    initialPos = initialPos,
    projectilePool = projectilePool,
    audio = audio,
    onDeathDrop = onDeathDrop,
    onChainExplosion = onChainExplosion
) {
    init {
        setAnimation("active", AnimationSequence.singleFrame(SPRITE_BUNKER))
        playAnimation("active")
    }

    companion object {
        val SPRITE_BUNKER = PixelSprite.fromAscii(
            listOf(
                "....##############....",
                "..##################..",
                ".####################.",
                "######################",
                "######################",
                "####...########...####",
                "####...########...####",
                "######################",
                "######################",
                "########......########",
                "########......########",
                "########......########",
                "######################",
                "######################",
                ".####################.",
                "..##################..",
                "...################...",
                ".....############....."
            ),
            mapOf('.' to 0x00000000, '#' to 0xFF6D4C41.toInt())
        )
    }
}
