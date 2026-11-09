package com.pixie.engine.core.platformer

/**
 * Powerup progression tier for platformer characters (NES Super Mario Bros archetypes).
 *
 * Defines physical hitbox dimensions, brick-shattering permissions, and powerup status.
 */
enum class PlayerTier(
    val hitboxWidth: Float,
    val hitboxHeight: Float,
    val canBreakBricks: Boolean = false
) {
    /** Regular 16x16 pixel Mario/Pixie. Cannot break bricks from underneath. */
    SMALL(
        hitboxWidth = 14f,
        hitboxHeight = 16f,
        canBreakBricks = false
    ),

    /** Powered-up 16x24 pixel Super Mario/Pixie. Breaks bricks into shards on head bonk. */
    SUPER(
        hitboxWidth = 14f,
        hitboxHeight = 24f,
        canBreakBricks = true
    );

    val isPoweredUp: Boolean get() = this != SMALL

    fun upgrade(): PlayerTier = when (this) {
        SMALL -> SUPER
        SUPER -> SUPER
    }

    fun downgrade(): PlayerTier = when (this) {
        SUPER -> SMALL
        SMALL -> SMALL
    }
}
