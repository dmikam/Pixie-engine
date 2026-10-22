package com.pixie.engine.core.context

/**
 * Global game state context passed to all game objects, layers, weapons, and physics updates.
 *
 * Encapsulates dynamic progression factors so objects can dynamically compute their runtime properties
 * (run speed, shooting speed, hit-power, health, spawn rates, and obstacle densities) based on:
 * - Continuous integer difficulty [difficulty] (0 to [MAX_DIFFICULTY])
 * - Current progress level [currentLevel] (1, 2, 3...)
 * - Lives remaining [livesLeft]
 * - Current moving speed [currentMovingSpeed]
 * - Active perks [activePerks]
 * - Elapsed time and custom attributes.
 */
data class GameContext(
    var currentLevel: Int = 1,
    var livesLeft: Int = 3,
    var score: Long = 0L,
    var credits: Long = 1000L,
    var distance: Float = 0f,
    var gameSpeed: Float = 1.0f,
    var currentMovingSpeed: Float = 0f,
    var colorRecoveryProgress: Float = 0f,
    var isGameOver: Boolean = false,
    var isVictory: Boolean = false,
    var timeElapsedSeconds: Float = 0f,
    val activePerks: MutableSet<String> = mutableSetOf(),
    val customAttributes: MutableMap<String, Any> = mutableMapOf()
) {
    /**
     * Attempts to spend the specified amount of credits.
     * @return true if successful, false if insufficient credits.
     */
    fun spendCredits(amount: Long): Boolean {
        if (amount <= 0L) return true
        if (credits >= amount) {
            credits -= amount
            return true
        }
        return false
    }

    fun earnCredits(amount: Long) {
        if (amount > 0L) {
            credits += amount
        }
    }

    fun recoverColor(amount: Float) {
        if (amount > 0f) {
            colorRecoveryProgress = (colorRecoveryProgress + amount).coerceAtMost(1.0f)
        }
    }
    companion object {
        const val MIN_DIFFICULTY: Int = 0
        const val MAX_DIFFICULTY: Int = 100
    }

    /**
     * Continuous integer difficulty level in range [0..100].
     * Initialized to standard Medium preset (25).
     */
    var difficulty: Int = 25
        set(value) {
            field = value.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        }

    /**
     * Normalized difficulty ratio in range [0.0f..1.0f].
     */
    val normalizedDifficulty: Float
        get() = difficulty.toFloat() / MAX_DIFFICULTY.toFloat()

    /**
     * Float representation for backward compatibility with early PoC modules.
     * Maps 0..100 to 0.0f..4.0f (with 25 = 1.0f).
     */
    var difficultyLevel: Float
        get() = (difficulty.toFloat() / 25.0f).coerceAtLeast(0.0f)
        set(value) {
            difficulty = (value * 25.0f).toInt().coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        }

    /**
     * Secondary constructor allowing initialization with legacy float difficultyLevel if desired.
     */
    constructor(
        currentLevel: Int = 1,
        difficultyLevel: Float,
        livesLeft: Int = 3,
        score: Long = 0L,
        distance: Float = 0f,
        gameSpeed: Float = 1.0f,
        currentMovingSpeed: Float = 0f,
        colorRecoveryProgress: Float = 0f,
        isGameOver: Boolean = false,
        isVictory: Boolean = false,
        timeElapsedSeconds: Float = 0f
    ) : this(
        currentLevel = currentLevel,
        livesLeft = livesLeft,
        score = score,
        distance = distance,
        gameSpeed = gameSpeed,
        currentMovingSpeed = currentMovingSpeed,
        colorRecoveryProgress = colorRecoveryProgress,
        isGameOver = isGameOver,
        isVictory = isVictory,
        timeElapsedSeconds = timeElapsedSeconds
    ) {
        this.difficultyLevel = difficultyLevel
    }

    /**
     * Primary dynamic property calculation engine.
     *
     * Computes a final property value (e.g. speed, fire rate, damage, health) by scaling [baseValue]
     * according to the continuous difficulty (0..100), current level progression, lives remaining,
     * current moving speed, and active perk multiplier.
     */
    fun calculateDynamicProperty(
        baseValue: Float,
        difficultyWeight: Float = 1.0f,
        levelWeight: Float = 0.15f,
        perkBonus: Float = 1.0f,
        speedInfluence: Float = 0.0f,
        livesInfluence: Float = 0.0f
    ): Float {
        val diffRatio = normalizedDifficulty // 0.0f to 1.0f
        val diffFactor = 1.0f + (diffRatio - 0.25f) * difficultyWeight
        val levelFactor = 1.0f + ((currentLevel - 1).coerceAtLeast(0) * levelWeight)
        val speedFactor = 1.0f + (currentMovingSpeed * speedInfluence)
        // Lives factor can provide rubber-banding mercy buffs or pressure scaling
        val livesFactor = 1.0f + ((3 - livesLeft.coerceIn(0, 3)) * livesInfluence)

        val combinedScale = (diffFactor * levelFactor * speedFactor * livesFactor * perkBonus).coerceAtLeast(0.1f)
        return baseValue * combinedScale
    }

    /**
     * Legacy multiplier calculation preserved for horizontal runner compatibility (Dinorun).
     */
    fun calculateDifficultyMultiplier(base: Float = 1.0f, scaleRate: Float = 0.05f): Float {
        return base + (difficultyLevel - 1.0f) * scaleRate + (distance / 500f) * 0.1f
    }

    fun hasPerk(perk: String): Boolean = activePerks.contains(perk)

    fun addPerk(perk: String) {
        activePerks.add(perk)
    }

    fun removePerk(perk: String) {
        activePerks.remove(perk)
    }

    fun setAttribute(key: String, value: Any) {
        customAttributes[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getAttribute(key: String, default: T): T {
        return (customAttributes[key] as? T) ?: default
    }
}
