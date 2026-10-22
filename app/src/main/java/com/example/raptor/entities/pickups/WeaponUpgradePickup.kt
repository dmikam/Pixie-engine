package com.example.raptor.entities.pickups

import com.example.raptor.entities.PickupType
import com.example.raptor.entities.RaptorPickup
import com.example.raptor.entities.RaptorShip
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioPort

/**
 * Weapon Upgrade Pod: Advances player primary cannons (Vulcan -> Twin Plasma -> Odin Laser -> Deathray).
 */
class WeaponUpgradePickup(
    id: String,
    initialPos: Vec2
) : RaptorPickup(id, PickupType.WEAPON_UPGRADE, initialPos) {
    override fun applyEffect(playerShip: RaptorShip, context: GameContext, audio: AudioPort?) {
        playerShip.upgradeWeapon()
    }
}
