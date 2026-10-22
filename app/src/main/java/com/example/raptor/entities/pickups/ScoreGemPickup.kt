package com.example.raptor.entities.pickups

import com.example.raptor.entities.PickupType
import com.example.raptor.entities.RaptorPickup
import com.example.raptor.entities.RaptorShip
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort

/**
 * Score Gem Cargo: High-value minerals awarding +1500 score points and credits.
 */
class ScoreGemPickup(
    id: String,
    initialPos: Vec2
) : RaptorPickup(id, PickupType.SCORE_GEM, initialPos) {
    override fun applyEffect(playerShip: RaptorShip, context: GameContext, audio: AudioPort?) {
        context.score += 1500L
        audio?.playSound(AudioEvent.COLLECT)
    }
}
