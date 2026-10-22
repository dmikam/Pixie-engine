package com.example.raptor.entities

import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.weapon.ProjectilePool

/**
 * Volatile Fuel Silo: Industrial fuel repository.
 * Highly volatile: when destroyed, detonates a secondary shockwave causing chained AoE damage to nearby targets.
 */
class FuelSiloDestructible(
    id: String,
    initialPos: Vec2,
    projectilePool: ProjectilePool,
    audio: AudioPort? = null,
    onDeathDrop: ((dropPos: Vec2) -> Unit)? = null,
    onChainExplosion: ((centerPos: Vec2, blastRadius: Float, blastDamage: Float) -> Unit)? = null
) : GroundDestructible(
    id = id,
    type = DestructibleType.FUEL_SILO,
    initialPos = initialPos,
    projectilePool = projectilePool,
    audio = audio,
    onDeathDrop = onDeathDrop,
    onChainExplosion = onChainExplosion
) {
    init {
        setAnimation("active", AnimationSequence.singleFrame(SPRITE_SILO))
        playAnimation("active")
    }

    override fun onDeathEffect(particleLayer: ParticleLayer?) {
        particleLayer?.spawnExplosion(ExplosionType.SHOCKWAVE, position + (size * 0.5f), scale = 2.0f)
        onChainExplosion?.invoke(position + (size * 0.5f), 85f, 75f)
    }

    companion object {
        val SPRITE_SILO = PixelSprite.fromAscii(
            listOf(
                ".....########.....",
                "...############...",
                "..##############..",
                ".################.",
                "##################",
                "##################",
                "###..########..###",
                "###..########..###",
                "##################",
                "##..##..##..##..##",
                "##################",
                "##################",
                "##..##..##..##..##",
                "##################",
                "##################",
                "##..##..##..##..##",
                "##################",
                "##################",
                "##################",
                ".################.",
                "..##############..",
                "...############..."
            ),
            mapOf('.' to 0x00000000, '#' to 0xFFFF7043.toInt())
        )
    }
}
