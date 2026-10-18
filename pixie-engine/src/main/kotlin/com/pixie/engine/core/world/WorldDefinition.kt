package com.pixie.engine.core.world

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject

/**
 * Encapsulates the visual, atmospheric, and procedural generation parameters for a game world or level.
 *
 * Allows projects to define multiple distinct worlds (e.g. Prehistoric Ash, Volcanic Wasteland,
 * Desert Canyon, Ice Glacier) or stages that can be plugged into the engine interchangeably.
 */
interface WorldDefinition {
    val id: String
    val displayName: String

    // Atmospheric & Horizon configuration
    val horizonY: Float
    val dayNightCycleSeconds: Float
    val windVelocity: Float

    // Terrain visual styling
    val groundColor: Int
    val surfaceColor: Int
    val bedrockColor: Int

    /**
     * Procedurally populates a terrain chunk with self-encapsulated game objects.
     */
    fun spawnChunkObjects(
        chunkIndex: Int,
        startX: Float,
        width: Float,
        groundY: Float,
        context: GameContext
    ): List<PixieObject>
}
