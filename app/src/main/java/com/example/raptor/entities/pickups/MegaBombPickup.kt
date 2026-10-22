package com.example.raptor.entities.pickups

import com.example.raptor.entities.PickupType
import com.example.raptor.entities.RaptorPickup
import com.example.raptor.entities.RaptorShip
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioPort

/**
 * Mega Bomb Munitions: Replenishes tactical tactical screen-clearing bomb (+1 bomb payload).
 */
class MegaBombPickup(
    id: String,
    initialPos: Vec2
) : RaptorPickup(id, PickupType.MEGA_BOMB, initialPos) {
    override fun applyEffect(playerShip: RaptorShip, context: GameContext, audio: AudioPort?) {
        playerShip.addBomb(1)
    }
}
