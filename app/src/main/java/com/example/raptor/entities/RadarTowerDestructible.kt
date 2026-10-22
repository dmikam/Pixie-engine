package com.example.raptor.entities

import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.weapon.ProjectilePool

/**
 * Radar Installation: Early-warning scanning facility with rotating dish receiver.
 */
class RadarTowerDestructible(
    id: String,
    initialPos: Vec2,
    projectilePool: ProjectilePool,
    audio: AudioPort? = null,
    onDeathDrop: ((dropPos: Vec2) -> Unit)? = null,
    onChainExplosion: ((centerPos: Vec2, blastRadius: Float, blastDamage: Float) -> Unit)? = null
) : GroundDestructible(
    id = id,
    type = DestructibleType.RADAR_TOWER,
    initialPos = initialPos,
    projectilePool = projectilePool,
    audio = audio,
    onDeathDrop = onDeathDrop,
    onChainExplosion = onChainExplosion
) {
    init {
        setAnimation("active", AnimationSequence.singleFrame(SPRITE_RADAR))
        playAnimation("active")
    }

    companion object {
        val SPRITE_RADAR = PixelSprite.fromAscii(
            listOf(
                "....########....",
                "..############..",
                ".##############.",
                "###...####...###",
                "###...####...###",
                "################",
                ".##############.",
                "..############..",
                "....########....",
                "......####......",
                "......####......",
                "......####......",
                ".....######.....",
                "....##.##.##....",
                "...###.##.###...",
                "..####.##.####..",
                ".#####.##.#####.",
                "################",
                "################",
                "################"
            ),
            mapOf('.' to 0x00000000, '#' to 0xFF00E5FF.toInt())
        )
    }
}
