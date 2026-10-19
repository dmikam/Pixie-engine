package com.example.raptor.entities

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import kotlin.math.sin

enum class PickupType(val label: String, val badgeColor: Int) {
    WEAPON_UPGRADE("P", 0xFFFFD600.toInt()), // Bright Gold
    HOMING_MISSILES("M", 0xFFFF3D00.toInt()), // Neon Red
    SHIELD_CELL("S", 0xFF00E5FF.toInt()),     // Cyan
    REPAIR_KIT("R", 0xFF00E676.toInt()),      // Emerald Green
    MEGA_BOMB("B", 0xFFD500F9.toInt()),       // Electric Violet
    SCORE_GEM("G", 0xFFFFAB00.toInt()),       // Amber Gem
    PERK_CHIP("K", 0xFF76FF03.toInt())        // Lime Tech Perk
}

/**
 * Collectible power-ups and cargo pods dropped by enemies and ground structures.
 *
 * Supports magnet attraction towards the player ship when in proximity or with active perk.
 */
class RaptorPickup(
    id: String,
    val type: PickupType,
    initialPos: Vec2
) : PixieObject(
    id = id,
    position = initialPos,
    size = Vec2(10f, 10f),
    initialClasses = setOf("pickup")
) {
    var bobTime: Float = 0f
    private val driftSpeed: Float = 35f

    init {
        val sprite = createPickupSprite(type.badgeColor)
        setAnimation("default", AnimationSequence.singleFrame(sprite))
        playAnimation("default")
        isSolid = false // Pickups are collected via trigger check, not physics blocking
    }

    override fun update(dt: Float, context: GameContext) {
        if (!isAlive) return
        bobTime += dt

        // Gentle downward drift with subtle sine wave oscillation
        val vx = sin(bobTime * 3f) * 12f
        velocity = Vec2(vx, driftSpeed)

        super.update(dt, context)
    }

    /**
     * Magnet attraction towards player ship.
     */
    fun attractTowards(playerPos: Vec2, dt: Float, hasMagnetPerk: Boolean) {
        val delta = playerPos - position
        val distSq = delta.lengthSquared()
        val magnetRangeSq = if (hasMagnetPerk) 120f * 120f else 45f * 45f

        if (distSq < magnetRangeSq && distSq > 4f) {
            val pullSpeed = if (hasMagnetPerk) 220f else 120f
            val pull = delta.normalized() * pullSpeed
            position += pull * dt
        }
    }

    companion object {
        private fun createPickupSprite(accentColor: Int): PixelSprite {
            return PixelSprite.fromAscii(
                listOf(
                    "..######..",
                    ".########.",
                    "##########",
                    "###.##.###",
                    "###.##.###",
                    "###.##.###",
                    "###.##.###",
                    "##########",
                    ".########.",
                    "..######.."
                ),
                mapOf(
                    '.' to 0x00000000,
                    '#' to accentColor
                )
            )
        }
    }
}
