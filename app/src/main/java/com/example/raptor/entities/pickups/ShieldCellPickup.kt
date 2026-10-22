package com.example.raptor.entities.pickups

import com.example.raptor.entities.PickupType
import com.example.raptor.entities.RaptorPickup
import com.example.raptor.entities.RaptorShip
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioPort

/**
 * Shield Cell Pod: Recharges energy barrier (+40 shield capacity).
 */
class ShieldCellPickup(
    id: String,
    initialPos: Vec2
) : RaptorPickup(id, PickupType.SHIELD_CELL, initialPos) {
    override fun applyEffect(playerShip: RaptorShip, context: GameContext, audio: AudioPort?) {
        playerShip.rechargeShield(40f)
    }
}
