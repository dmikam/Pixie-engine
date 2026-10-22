package com.example.raptor.entities

import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.weapon.ProjectilePool

/**
 * Crude Oil Reserve: Heavy industrial storage reservoir.
 * Highly combustible: triggers secondary chain explosion upon destruction.
 */
class OilStorageTankDestructible(
    id: String,
    initialPos: Vec2,
    projectilePool: ProjectilePool,
    audio: AudioPort? = null,
    onDeathDrop: ((dropPos: Vec2) -> Unit)? = null,
    onChainExplosion: ((centerPos: Vec2, blastRadius: Float, blastDamage: Float) -> Unit)? = null
) : GroundDestructible(
    id = id,
    type = DestructibleType.OIL_STORAGE_TANK,
    initialPos = initialPos,
    projectilePool = projectilePool,
    audio = audio,
    onDeathDrop = onDeathDrop,
    onChainExplosion = onChainExplosion
) {
    init {
        setAnimation("active", AnimationSequence.singleFrame(SPRITE_OIL_TANK))
        playAnimation("active")
    }

    override fun onDeathEffect(particleLayer: ParticleLayer?) {
        particleLayer?.spawnExplosion(ExplosionType.SHOCKWAVE, position + (size * 0.5f), scale = 2.0f)
        onChainExplosion?.invoke(position + (size * 0.5f), 85f, 75f)
    }

    companion object {
        val SPRITE_OIL_TANK = PixelSprite.fromAscii(
            listOf(
                ".....##########.....",
                "...##############...",
                "..################..",
                ".##################.",
                "####################",
                "####################",
                "###..##########..###",
                "###..##########..###",
                "####################",
                "####################",
                "####################",
                "####################",
                "####################",
                "####################",
                "####################",
                ".##################.",
                "..################..",
                "...##############...",
                ".....##########.....",
                ".......######......."
            ),
            mapOf('.' to 0x00000000, '#' to 0xFFFFB300.toInt())
        )
    }
}
