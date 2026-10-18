package com.example.dinorun.world

import com.example.dinorun.entities.CactusObstacle
import com.example.dinorun.entities.ColorSparkPickup
import com.example.dinorun.entities.PterodactylHazard
import com.example.dinorun.entities.VolcanicMeteor
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
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

        // Slot 1: Hazard at startX + 70f
        val slot1X = startX + 70f
        if (allowPtero && r.nextFloat() < 0.4f) {
            val isLow = r.nextBoolean()
            val pteroY = if (isLow) groundY - 22f else groundY - 44f
            val ptero = PterodactylHazard(
                id = "volcano_ptero_$chunkIndex",
                position = Vec2(slot1X, pteroY),
                speed = 48f
            )
            items.add(ptero)
            occupiedX.add(slot1X)
        } else {
            val cactus = CactusObstacle(
                id = "crater_spike_$chunkIndex",
                position = Vec2(slot1X, groundY - 18f),
                isDouble = true
            )
            items.add(cactus)
            occupiedX.add(slot1X)
        }

        // Slot 2: Hazard at startX + 230f (guaranteed 160px clear of Slot 1)
        val slot2X = startX + 230f
        if (isPositionSafe(slot2X, 140f)) {
            if (allowPtero && r.nextFloat() < 0.5f) {
                val isLow = r.nextBoolean()
                val pteroY = if (isLow) groundY - 22f else groundY - 44f
                val ptero = PterodactylHazard(
                    id = "volcano_ptero_2_$chunkIndex",
                    position = Vec2(slot2X, pteroY),
                    speed = 50f
                )
                items.add(ptero)
                occupiedX.add(slot2X)
            } else if (r.nextFloat() < 0.6f) {
                val cactus = CactusObstacle(
                    id = "crater_spike_2_$chunkIndex",
                    position = Vec2(slot2X, groundY - 18f),
                    isDouble = false
                )
                items.add(cactus)
                occupiedX.add(slot2X)
            }
        }

        // Slot 3: Falling Meteor strictly after 2000m with clearance
        if (allowMeteor && chunkIndex > 4 && r.nextFloat() < 0.45f) {
            val meteorLandingX = startX + 150f
            if (isPositionSafe(meteorLandingX, 150f)) {
                val meteor = VolcanicMeteor(
                    id = "volcano_meteor_$chunkIndex",
                    targetGroundX = meteorLandingX,
                    groundY = groundY,
                    triggerDistance = 180f
                )
                items.add(meteor)
                occupiedX.add(meteorLandingX)
            }
        }

        // Color Energy Sparks (Pickups)
        val spark = ColorSparkPickup(
            id = "magma_spark_$chunkIndex",
            position = Vec2(startX + 115f, groundY - 30f)
        )
        items.add(spark)

        return items
    }
}
