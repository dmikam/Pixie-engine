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
 * Prehistoric Ash Lands world definition.
 *
 * Encapsulates the visual horizon, day/night cycle, cloud drift, terrain colors,
 * and procedural chunk spawn tables with strict playability guarantees:
 * - Pterodactyls appear strictly after 500m distance.
 * - Pterodactyls never fly over or overlap with cacti (guaranteed minimum 140px safe clearance).
 * - Meteors appear strictly after 2000m distance.
 * - Meteors land with minimum 150px clearance from any cactus or pterodactyl.
 * - Meteors land and impact realistically on the ground surface without clipping underneath.
 */
class PrehistoricWorldDefinition(
    seed: Long = 42L
) : WorldDefinition {
    override val id: String = "prehistoric_ash"
    override val displayName: String = "Prehistoric Ash Lands"

    override val horizonY: Float = 135f
    override val dayNightCycleSeconds: Float = 60f
    override val windVelocity: Float = 12f

    override val groundColor: Int = 0xFF5D4037.toInt()   // Dirt brown
    override val surfaceColor: Int = 0xFF388E3C.toInt()  // Surface moss green
    override val bedrockColor: Int = 0xFF3E2723.toInt()  // Deep bedrock

    private val random = Random(seed)

    override fun spawnChunkObjects(
        chunkIndex: Int,
        startX: Float,
        width: Float,
        groundY: Float,
        context: GameContext
    ): List<PixieObject> {
        val items = mutableListOf<PixieObject>()
        if (chunkIndex <= 1) return items // Keep starting runway clear

        val r = random
        val difficulty = context.calculateDifficultyMultiplier(1.0f, 0.05f)

        // Track occupied hazard X coordinates to guarantee playability and clearance
        val occupiedX = mutableListOf<Float>()

        fun isPositionSafe(x: Float, minDistance: Float = 140f): Boolean {
            return occupiedX.none { kotlin.math.abs(it - x) < minDistance }
        }

        val allowPtero = context.distance >= 500f   // Pterodactyls strictly after 500m
        val allowMeteor = context.distance >= 2000f // Meteors strictly after 2000m

        // Slot 1: Primary hazard zone (startX + 60f to startX + 90f)
        val slot1X = startX + 60f + r.nextFloat() * 30f

        if (allowPtero && r.nextFloat() < 0.45f) {
            // Spawn Pterodactyl in Slot 1
            val isLow = r.nextBoolean()
            val pteroY = if (isLow) groundY - 22f else groundY - 44f
            val ptero = PterodactylHazard(
                id = "ptero_${chunkIndex}_1",
                position = Vec2(slot1X, pteroY),
                speed = 40f
            )
            items.add(ptero)
            occupiedX.add(slot1X)
        } else {
            // Spawn Ground Cactus in Slot 1
            val isDouble = difficulty > 1.3f && r.nextBoolean()
            val cactus = CactusObstacle(
                id = "cactus_${chunkIndex}_1",
                position = Vec2(slot1X, groundY - 18f),
                isDouble = isDouble
            )
            items.add(cactus)
            occupiedX.add(slot1X)
        }

        // Slot 2: Secondary hazard zone (startX + 210f to startX + 240f) - strictly separated
        val slot2X = startX + 210f + r.nextFloat() * 30f
        if (isPositionSafe(slot2X, 140f) && (difficulty > 1.2f || r.nextFloat() < 0.6f)) {
            // Never spawn over cactus: Slot 2 is at least 140px away from Slot 1
            if (allowPtero && r.nextFloat() < 0.5f) {
                val isLow = r.nextBoolean()
                val pteroY = if (isLow) groundY - 22f else groundY - 44f
                val ptero = PterodactylHazard(
                    id = "ptero_${chunkIndex}_2",
                    position = Vec2(slot2X, pteroY),
                    speed = 40f
                )
                items.add(ptero)
                occupiedX.add(slot2X)
            } else {
                val cactus = CactusObstacle(
                    id = "cactus_${chunkIndex}_2",
                    position = Vec2(slot2X, groundY - 18f),
                    isDouble = false
                )
                items.add(cactus)
                occupiedX.add(slot2X)
            }
        }

        // Slot 3: Primal Color Energy Sparks (Pickups that restore color)
        val sparkCount = if (r.nextFloat() < 0.85f) 2 else 1
        for (i in 0 until sparkCount) {
            val sparkX = startX + 30f + i * 110f
            val sparkY = groundY - 24f - r.nextFloat() * 26f
            val spark = ColorSparkPickup(
                id = "spark_${chunkIndex}_$i",
                position = Vec2(sparkX, sparkY)
            )
            items.add(spark)
        }

        // Slot 4: Volcanic Meteor - strictly after 2000m and with >= 150px clearance from hazards
        if (allowMeteor && chunkIndex > 4 && r.nextFloat() < 0.35f) {
            val meteorLandingX = startX + 135f
            if (isPositionSafe(meteorLandingX, 150f)) {
                val meteor = VolcanicMeteor(
                    id = "meteor_$chunkIndex",
                    targetGroundX = meteorLandingX,
                    groundY = groundY,
                    triggerDistance = 175f
                )
                items.add(meteor)
                occupiedX.add(meteorLandingX)
            }
        }

        return items
    }
}
