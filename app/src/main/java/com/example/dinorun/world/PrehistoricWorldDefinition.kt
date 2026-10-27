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

    // Majestic high-arch circular orbit with 60s day/night cycle and full rotating star field
    override val transitMode: CelestialTransitMode = CelestialTransitMode.CIRCULAR
    override val celestialCenter: Vec2 = Vec2(160f, 140f)
    override val celestialRadiusX: Float = 145f
    override val celestialRadiusY: Float = 110f

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
        val occupiedX = mutableListOf<Float>()

        fun isPositionSafe(x: Float, minDistance: Float = 140f): Boolean {
            return occupiedX.none { kotlin.math.abs(it - x) < minDistance }
        }

        val allowPtero = context.distance >= 500f   // Pterodactyls strictly after 500m
        val allowMeteor = context.distance >= 2000f // Meteors strictly after 2000m

        // Slot 1: Primary hazard zone (startX + 60f to startX + 75f)
        val slot1X = startX + 60f + r.nextFloat() * 15f
        spawnSlot1Hazard(items, occupiedX, slot1X, groundY, chunkIndex, allowPtero, difficulty, r)

        // Slot 2 / Meteor hazard zone (startX + 225f to startX + 235f, >= 150px safe clearance from Slot 1)
        val slot2X = startX + 225f + r.nextFloat() * 10f
        val meteorSpawned = spawnMeteorHazard(items, occupiedX, slot2X, groundY, chunkIndex, allowMeteor, r, ::isPositionSafe)
        if (!meteorSpawned && isPositionSafe(slot2X, 140f) && (difficulty > 1.2f || r.nextFloat() < 0.6f)) {
            spawnSlot2Hazard(items, occupiedX, slot2X, groundY, chunkIndex, allowPtero, r)
        }

        // Slot 3: Primal Color Energy Sparks (Pickups that restore color)
        spawnSparks(items, startX, groundY, chunkIndex, r)

        return items
    }

    private fun spawnSlot1Hazard(
        items: MutableList<PixieObject>,
        occupiedX: MutableList<Float>,
        x: Float,
        groundY: Float,
        chunkIndex: Int,
        allowPtero: Boolean,
        difficulty: Float,
        r: Random
    ) {
        if (allowPtero && r.nextFloat() < 0.45f) {
            val isLow = r.nextBoolean()
            val pteroY = if (isLow) groundY - 22f else groundY - 44f
            items.add(PterodactylHazard("ptero_${chunkIndex}_1", Vec2(x, pteroY), speed = 40f))
        } else {
            val isDouble = difficulty > 1.3f && r.nextBoolean()
            items.add(CactusObstacle("cactus_${chunkIndex}_1", Vec2(x, groundY - 18f), isDouble = isDouble))
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
        if (allowMeteor && chunkIndex > 4 && r.nextFloat() < 0.35f && isPositionSafe(x, 150f)) {
            val meteor = VolcanicMeteor(
                id = "meteor_$chunkIndex",
                targetGroundX = x,
                groundY = groundY,
                triggerDistance = 175f
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
            items.add(PterodactylHazard("ptero_${chunkIndex}_2", Vec2(x, pteroY), speed = 40f))
        } else {
            items.add(CactusObstacle("cactus_${chunkIndex}_2", Vec2(x, groundY - 18f), isDouble = false))
        }
        occupiedX.add(x)
    }

    private fun spawnSparks(
        items: MutableList<PixieObject>,
        startX: Float,
        groundY: Float,
        chunkIndex: Int,
        r: Random
    ) {
        val sparkCount = if (r.nextFloat() < 0.85f) 2 else 1
        for (i in 0 until sparkCount) {
            val sparkX = startX + 30f + i * 110f
            val sparkY = groundY - 24f - r.nextFloat() * 26f
            items.add(ColorSparkPickup("spark_${chunkIndex}_$i", Vec2(sparkX, sparkY)))
        }
    }
}
