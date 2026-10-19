package com.example.raptor.world

import com.pixie.engine.core.math.Vec2
import kotlin.random.Random

data class RaptorLevel(
    val levelIndex: Int,
    val name: String,
    val subtitle: String,
    val deepColor: Int,
    val surfaceColor: Int,
    val targetDistance: Float
)

object RaptorLevelCatalog {
    val LEVEL_1 = RaptorLevel(
        levelIndex = 1,
        name = "Level 1: Archipelago Infiltration",
        subtitle = "Deep oceanic surface, island airbases & naval defense",
        deepColor = 0xFF0D47A1.toInt(),    // Deep Navy Ocean
        surfaceColor = 0xFF1976D2.toInt(), // Tropical Blue Waters
        targetDistance = 1600f
    )

    val LEVEL_2 = RaptorLevel(
        levelIndex = 2,
        name = "Level 2: Sulfur Canyon & Refinery",
        subtitle = "Arid desert canyons, industrial silos & automated flak",
        deepColor = 0xFF4E342E.toInt(),    // Dark canyon basalt
        surfaceColor = 0xFF8D6E63.toInt(), // Red sandstone
        targetDistance = 2200f
    )

    val LEVEL_3 = RaptorLevel(
        levelIndex = 3,
        name = "Level 3: Orbital Command Citadel",
        subtitle = "Low orbit space station, energy grids & apex mothership",
        deepColor = 0xFF0A0E17.toInt(),    // Void black
        surfaceColor = 0xFF1A237E.toInt(), // Deep space cobalt
        targetDistance = 2800f
    )

    val ALL_LEVELS = listOf(LEVEL_1, LEVEL_2, LEVEL_3)

    fun getLevel(index: Int): RaptorLevel {
        return ALL_LEVELS.getOrNull((index - 1).coerceAtLeast(0)) ?: LEVEL_1
    }
}
