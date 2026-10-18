package com.pixie.engine.core.context

/**
 * Global game state context passed to all game objects, layers, and physics updates.
 *
 * Encapsulates dynamic progression factors so objects can dynamically compute their parameters
 * (speed, jump force, spawn rates, obstacle densities, hazards) as difficulty or score scales.
 */
data class GameContext(
    var currentLevel: Int = 1,
    var difficultyLevel: Float = 1.0f,
    var livesLeft: Int = 3,
    var score: Long = 0L,
    var distance: Float = 0f,
    var gameSpeed: Float = 1.0f,
    var colorRecoveryProgress: Float = 0f,
    var isGameOver: Boolean = false,
    var isVictory: Boolean = false,
    var timeElapsedSeconds: Float = 0f,
    val customAttributes: MutableMap<String, Any> = mutableMapOf()
) {
    /**
     * Compute a dynamic difficulty scaling factor (e.g. 1.0 to 3.0+)
     */
    fun calculateDifficultyMultiplier(base: Float = 1.0f, scaleRate: Float = 0.05f): Float {
        return base + (difficultyLevel - 1.0f) * scaleRate + (distance / 500f) * 0.1f
    }

    fun setAttribute(key: String, value: Any) {
        customAttributes[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getAttribute(key: String, default: T): T {
        return (customAttributes[key] as? T) ?: default
    }
}
