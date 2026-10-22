package com.example.raptor.entities.pickups

import com.example.raptor.entities.PickupType
import com.example.raptor.entities.RaptorPickup
import com.example.raptor.entities.RaptorShip
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioPort

/**
 * Homing Missiles Pod: Replenishes secondary target-seeking missiles (+4 payload).
 */
class HomingMissilesPickup(
    id: String,
    initialPos: Vec2
) : RaptorPickup(id, PickupType.HOMING_MISSILES, initialPos) {
    override fun applyEffect(playerShip: RaptorShip, context: GameContext, audio: AudioPort?) {
        playerShip.addMissiles(4)
    }
}
