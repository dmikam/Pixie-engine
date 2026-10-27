package com.pixie.engine.core.world

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.layer.CelestialTransitMode
import com.pixie.engine.core.math.Vec2

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

    // Sky & Celestial Orbit configuration
    val transitMode: CelestialTransitMode get() = CelestialTransitMode.CIRCULAR
    val celestialCenter: Vec2 get() = Vec2(160f, 140f)
    val celestialRadiusX: Float get() = 145f
    val celestialRadiusY: Float get() = 110f

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
