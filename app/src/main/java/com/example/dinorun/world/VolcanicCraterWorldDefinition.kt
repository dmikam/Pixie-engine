package com.example.dinorun.world

import com.example.dinorun.entities.CactusObstacle
import com.example.dinorun.entities.ColorSparkPickup
import com.example.dinorun.entities.PterodactylHazard
import com.example.dinorun.entities.VolcanicMeteor
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.layer.CelestialTransitMode
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.world.WorldDefinition
import java.util.Random

/**
 * Volcanic Magma Ridge world definition.
 *
 * Demonstrates world swappability in Pixie Engine with high-intensity volcanic skies,
 * accelerated day/night cycle, and molten magma terrain styling.
 *
 * Enforces playability constraints:
 * - Pterodactyls strictly after 500m distance.
 * - Pterodactyls never overlap or fly over ground spikes.
 * - Meteors strictly after 2000m distance with >= 150px safe clearance from any obstacle.
 * - Meteors land and impact realistically on the surface.
 */
class VolcanicCraterWorldDefinition(
    seed: Long = 999L
) : WorldDefinition {
    override val id: String = "volcanic_ridge"
    override val displayName: String = "Volcanic Magma Ridge"

    override val horizonY: Float = 135f
    override val dayNightCycleSeconds: Float = 40f
    override val windVelocity: Float = 28f

    // Aggressive, low-horizon orbit with 40s cycle and reddish twilight
    override val transitMode: CelestialTransitMode = CelestialTransitMode.CIRCULAR
    override val celestialCenter: Vec2 = Vec2(160f, 155f)
    override val celestialRadiusX: Float = 150f
    override val celestialRadiusY: Float = 65f

    override val groundColor: Int = 0xFF212121.toInt()   // Basalt black
    override val surfaceColor: Int = 0xFFFF5722.toInt()  // Molten magma orange
    override val bedrockColor: Int = 0xFFBF360C.toInt()  // Deep crimson

    private val random = Random(seed)

    override fun spawnChunkObjects(
        chunkIndex: Int,
        startX: Float,
        width: Float,
        groundY: Float,
        context: GameContext
    ): List<PixieObject> {
        val items = mutableListOf<PixieObject>()
        if (chunkIndex <= 1) return items

        val r = random
        val occupiedX = mutableListOf<Float>()

        fun isPositionSafe(x: Float, minDistance: Float = 140f): Boolean {
            return occupiedX.none { kotlin.math.abs(it - x) < minDistance }
        }

        val allowPtero = context.distance >= 500f
        val allowMeteor = context.distance >= 2000f

        // Slot 1: Hazard at startX + 65f
        val slot1X = startX + 65f
        spawnSlot1Hazard(items, occupiedX, slot1X, groundY, chunkIndex, allowPtero, r)

        // Slot 2 / Meteor hazard zone (startX + 230f, >= 165px safe clearance from Slot 1)
        val slot2X = startX + 230f
        val meteorSpawned = spawnMeteorHazard(items, occupiedX, slot2X, groundY, chunkIndex, allowMeteor, r, ::isPositionSafe)
        if (!meteorSpawned && isPositionSafe(slot2X, 140f)) {
            spawnSlot2Hazard(items, occupiedX, slot2X, groundY, chunkIndex, allowPtero, r)
        }

        // Color Energy Sparks (Pickups)
        items.add(ColorSparkPickup("magma_spark_$chunkIndex", Vec2(startX + 115f, groundY - 30f)))

        return items
    }

    private fun spawnSlot1Hazard(
        items: MutableList<PixieObject>,
        occupiedX: MutableList<Float>,
        x: Float,
        groundY: Float,
        chunkIndex: Int,
        allowPtero: Boolean,
        r: Random
    ) {
        if (allowPtero && r.nextFloat() < 0.4f) {
            val isLow = r.nextBoolean()
            val pteroY = if (isLow) groundY - 22f else groundY - 44f
            items.add(PterodactylHazard("volcano_ptero_$chunkIndex", Vec2(x, pteroY), speed = 48f))
        } else {
            items.add(CactusObstacle("crater_spike_$chunkIndex", Vec2(x, groundY - 18f), isDouble = true))
        }
        occupiedX.add(x)
    }

    private fun spawnMeteorHazard(
        items: MutableList<PixieObject>,
        occupiedX: MutableList<Float>,
        x: Float,
        groundY: Float,
        chunkIndex: Int,
        allowMeteor: Boolean,
        r: Random,
        isPositionSafe: (Float, Float) -> Boolean
    ): Boolean {
        if (allowMeteor && chunkIndex > 4 && r.nextFloat() < 0.45f && isPositionSafe(x, 150f)) {
            val meteor = VolcanicMeteor(
                id = "volcano_meteor_$chunkIndex",
                targetGroundX = x,
                groundY = groundY,
                triggerDistance = 180f
            )
            items.add(meteor)
            occupiedX.add(x)
            return true
        }
        return false
    }

    private fun spawnSlot2Hazard(
        items: MutableList<PixieObject>,
        occupiedX: MutableList<Float>,
        x: Float,
        groundY: Float,
        chunkIndex: Int,
        allowPtero: Boolean,
        r: Random
    ) {
        if (allowPtero && r.nextFloat() < 0.5f) {
            val isLow = r.nextBoolean()
            val pteroY = if (isLow) groundY - 22f else groundY - 44f
            items.add(PterodactylHazard("volcano_ptero_2_$chunkIndex", Vec2(x, pteroY), speed = 50f))
            occupiedX.add(x)
        } else if (r.nextFloat() < 0.6f) {
            items.add(CactusObstacle("crater_spike_2_$chunkIndex", Vec2(x, groundY - 18f), isDouble = false))
            occupiedX.add(x)
        }
    }
}
