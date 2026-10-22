package com.example.raptor.entities

import com.example.raptor.entities.pickups.HomingMissilesPickup
import com.example.raptor.entities.pickups.MegaBombPickup
import com.example.raptor.entities.pickups.PerkChipPickup
import com.example.raptor.entities.pickups.RepairKitPickup
import com.example.raptor.entities.pickups.ScoreGemPickup
import com.example.raptor.entities.pickups.ShieldCellPickup
import com.example.raptor.entities.pickups.WeaponUpgradePickup
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieCollectible
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioPort
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
 * Common abstract parent class for collectible power-ups and cargo pods dropped by enemies and ground installations.
 * Inherits from engine core [PixieCollectible] base class to eliminate entity boilerplate.
 */
abstract class RaptorPickup(
    id: String,
    val type: PickupType,
    initialPos: Vec2
) : PixieCollectible(
    id = id,
    position = initialPos,
    size = Vec2(10f, 10f),
    defaultMagnetRange = 45f,
    defaultPullSpeed = 120f,
    initialClasses = setOf("pickup")
) {
    var bobTime: Float = 0f
    private val driftSpeed: Float = 35f

    init {
        val sprite = createPickupSprite(type.badgeColor)
        setAnimation("default", AnimationSequence.singleFrame(sprite))
        playAnimation("default")
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
     * Magnet attraction towards player ship utilizing core PixieCollectible magnet physics.
     */
    fun attractTowards(playerPos: Vec2, dt: Float, hasMagnetPerk: Boolean) {
        val range = if (hasMagnetPerk) 120f else 45f
        val speed = if (hasMagnetPerk) 220f else 120f
        attractTowards(playerPos, dt, range = range, pullSpeed = speed)
    }

    override fun onCollected(collector: PixieObject, context: GameContext) {
        if (collector is RaptorShip) {
            applyEffect(collector, context, collector.audio)
        }
    }

    /**
     * Polymorphic effect applied when the player collects this pickup.
     */
    abstract fun applyEffect(playerShip: RaptorShip, context: GameContext, audio: AudioPort?)

    companion object {
        fun createPickupSprite(accentColor: Int): PixelSprite {
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

        /**
         * Polymorphic factory for RaptorPickup subclasses.
         */
        fun create(type: PickupType, id: String, initialPos: Vec2): RaptorPickup = when (type) {
            PickupType.WEAPON_UPGRADE -> WeaponUpgradePickup(id, initialPos)
            PickupType.HOMING_MISSILES -> HomingMissilesPickup(id, initialPos)
            PickupType.SHIELD_CELL -> ShieldCellPickup(id, initialPos)
            PickupType.REPAIR_KIT -> RepairKitPickup(id, initialPos)
            PickupType.MEGA_BOMB -> MegaBombPickup(id, initialPos)
            PickupType.SCORE_GEM -> ScoreGemPickup(id, initialPos)
            PickupType.PERK_CHIP -> PerkChipPickup(id, initialPos)
        }

        /**
         * Operator invoke for constructor syntax RaptorPickup(id, type, pos).
         */
        operator fun invoke(id: String, type: PickupType, initialPos: Vec2): RaptorPickup =
            create(type, id, initialPos)
    }
}
