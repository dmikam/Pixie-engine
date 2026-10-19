package com.pixie.engine.core.context

/**
 * Standard preset for game difficulty selection in Pixie Engine.
 *
 * Provides discrete named options (e.g. Easy=10, Medium=25, Difficult=50, Heavy Metal=100)
 * on top of the engine's continuous numeric difficulty scale (0..MAX_DIFFICULTY).
 *
 * Every game can establish its own specific presets while leveraging the engine's continuous scale.
 */
data class DifficultyPreset(
    val id: String,
    val name: String,
    val difficultyValue: Int,
    val description: String = ""
) {
    init {
        require(difficultyValue in GameContext.MIN_DIFFICULTY..GameContext.MAX_DIFFICULTY) {
            "Difficulty value ($difficultyValue) must be within [${GameContext.MIN_DIFFICULTY}..${GameContext.MAX_DIFFICULTY}]"
        }
    }

    companion object {
        val EASY = DifficultyPreset("easy", "Easy", 10, "Forgiving hazards and relaxed pace.")
        val MEDIUM = DifficultyPreset("medium", "Medium", 25, "Balanced arcade challenge.")
        val DIFFICULT = DifficultyPreset("difficult", "Difficult", 50, "Intense combat, fast projectiles.")
        val HEAVY_METAL = DifficultyPreset("heavy_metal", "Heavy Metal", 100, "Relentless bullet fury.")

        val DEFAULT_PRESETS = listOf(EASY, MEDIUM, DIFFICULT, HEAVY_METAL)
    }
}
