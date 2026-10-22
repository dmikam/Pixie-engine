package com.example.raptor.entities.pickups

import com.example.raptor.entities.PickupType
import com.example.raptor.entities.RaptorPickup
import com.example.raptor.entities.RaptorShip
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort

/**
 * Tech Perk Microchip: Integrates an instant avionics upgrade (OVERCLOCK, RAPID_FIRE, TITANIUM_HULL, or MAGNET_BEAM).
 */
class PerkChipPickup(
    id: String,
    initialPos: Vec2
) : RaptorPickup(id, PickupType.PERK_CHIP, initialPos) {
    override fun applyEffect(playerShip: RaptorShip, context: GameContext, audio: AudioPort?) {
        val perkOptions = listOf("OVERCLOCK", "RAPID_FIRE", "TITANIUM_HULL", "MAGNET_BEAM")
        val perk = perkOptions.random()
        context.addPerk(perk)
        audio?.playSound(AudioEvent.POWERUP_PICKUP)
    }
}
