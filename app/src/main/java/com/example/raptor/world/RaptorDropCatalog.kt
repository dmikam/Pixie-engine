package com.example.raptor.world

import com.example.raptor.entities.PickupType
import com.example.raptor.entities.RaptorPickup
import com.pixie.engine.core.math.Vec2
import kotlin.random.Random

/**
 * Drop & Cargo Pod Catalog for Raptor.
 *
 * Encapsulates randomized roll tables and entity instantiation for collectible rewards
 * dropped by defeated enemy aircraft and demolished ground fortifications.
 */
object RaptorDropCatalog {

    /**
     * Determines a random [PickupType] based on canonical drop table probabilities:
     * - WEAPON_UPGRADE: 20% (0..19)
     * - HOMING_MISSILES: 16% (20..35)
     * - SHIELD_CELL: 18% (36..53)
     * - REPAIR_KIT: 16% (54..69)
     * - MEGA_BOMB: 10% (70..79)
     * - SCORE_GEM: 12% (80..91)
     * - PERK_CHIP: 8% (92..99)
     */
    fun rollDropType(rng: Random = Random): PickupType {
        val roll = rng.nextInt(100)
        return when {
            roll < 20 -> PickupType.WEAPON_UPGRADE
            roll < 36 -> PickupType.HOMING_MISSILES
            roll < 54 -> PickupType.SHIELD_CELL
            roll < 70 -> PickupType.REPAIR_KIT
            roll < 80 -> PickupType.MEGA_BOMB
            roll < 92 -> PickupType.SCORE_GEM
            else -> PickupType.PERK_CHIP
        }
    }

    /**
     * Instantiates and configures a new [RaptorPickup] entity with standard downward drift velocity.
     */
    fun spawnPickup(pos: Vec2, rng: Random = Random): RaptorPickup {
        val dropType = rollDropType(rng)
        val id = "pickup_${System.currentTimeMillis()}_${rng.nextInt(1000)}"
        return RaptorPickup.create(dropType, id, pos).apply {
            velocity = Vec2(0f, 36f)
        }
    }
}
