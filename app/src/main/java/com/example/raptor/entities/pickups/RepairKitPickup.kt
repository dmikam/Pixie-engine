package com.example.raptor.entities.pickups

import com.example.raptor.entities.PickupType
import com.example.raptor.entities.RaptorPickup
import com.example.raptor.entities.RaptorShip
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioPort

/**
 * Field Repair Pod: Repairs hull plating (+30 hull integrity).
 */
class RepairKitPickup(
    id: String,
    initialPos: Vec2
) : RaptorPickup(id, PickupType.REPAIR_KIT, initialPos) {
    override fun applyEffect(playerShip: RaptorShip, context: GameContext, audio: AudioPort?) {
        playerShip.repairHull(30f)
    }
}
