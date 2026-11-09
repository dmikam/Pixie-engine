package com.pixie.engine.core.tilemap

/**
 * Fundamental tile classifications for 2D platformer grid systems.
 *
 * Encapsulates physical solid/hazard flags, bumpable/destructible properties,
 * and character mappings for ASCII-based level authoring.
 */
enum class TileType(
    val isSolid: Boolean,
    val isBumpable: Boolean = false,
    val isDestructible: Boolean = false,
    val isOneWay: Boolean = false,
    val isHazard: Boolean = false,
    val isExit: Boolean = false,
    val charSymbol: Char = ' '
) {
    EMPTY(
        isSolid = false,
        charSymbol = '.'
    ),
    GROUND(
        isSolid = true,
        charSymbol = '#'
    ),
    QUESTION_BLOCK(
        isSolid = true,
        isBumpable = true,
        charSymbol = '?'
    ),
    BRICK(
        isSolid = true,
        isBumpable = true,
        isDestructible = true,
        charSymbol = 'B'
    ),
    USED_BLOCK(
        isSolid = true,
        charSymbol = 'U'
    ),
    PIPE_TOP_LEFT(
        isSolid = true,
        charSymbol = '['
    ),
    PIPE_TOP_RIGHT(
        isSolid = true,
        charSymbol = ']'
    ),
    PIPE_BODY_LEFT(
        isSolid = true,
        charSymbol = '|'
    ),
    PIPE_BODY_RIGHT(
        isSolid = true,
        charSymbol = '/'
    ),
    ONE_WAY_LEDGE(
        isSolid = false,
        isOneWay = true,
        charSymbol = '-'
    ),
    SPIKE_HAZARD(
        isSolid = false,
        isHazard = true,
        charSymbol = '^'
    ),
    FLAG_POLE(
        isSolid = false,
        isExit = true,
        charSymbol = 'F'
    ),
    FLAG_TOP(
        isSolid = false,
        isExit = true,
        charSymbol = '*'
    ),
    CASTLE_BRICK(
        isSolid = true,
        isExit = true,
        charSymbol = 'C'
    );

    val isPipe: Boolean
        get() = this == PIPE_TOP_LEFT || this == PIPE_TOP_RIGHT ||
                this == PIPE_BODY_LEFT || this == PIPE_BODY_RIGHT

    companion object {
        fun fromChar(char: Char): TileType = when (char) {
            '#' -> GROUND
            '?' -> QUESTION_BLOCK
            'B', 'b' -> BRICK
            'U', 'u' -> USED_BLOCK
            '[' -> PIPE_TOP_LEFT
            ']' -> PIPE_TOP_RIGHT
            '|', 'P', 'p' -> PIPE_BODY_LEFT
            '/', '}' -> PIPE_BODY_RIGHT
            '-', '_' -> ONE_WAY_LEDGE
            '^' -> SPIKE_HAZARD
            'F', 'f' -> FLAG_POLE
            '*' -> FLAG_TOP
            'C', 'c' -> CASTLE_BRICK
            else -> EMPTY
        }
    }
}

/**
 * Lightweight per-cell tile instance data holding dynamic state
 * such as type mutations and vertical spring/recoil animations.
 */
data class TileData(
    var type: TileType,
    var bumpOffsetY: Float = 0f,
    var bumpVelocityY: Float = 0f,
    var bumpTimer: Float = 0f
) {
    val isSolid: Boolean get() = type.isSolid
    val isBumpable: Boolean get() = type.isBumpable
    val isDestructible: Boolean get() = type.isDestructible
    val isOneWay: Boolean get() = type.isOneWay
    val isHazard: Boolean get() = type.isHazard
    val isExit: Boolean get() = type.isExit

    fun resetBump(): TileData = apply {
        bumpOffsetY = 0f
        bumpVelocityY = 0f
        bumpTimer = 0f
    }

    fun triggerBump(velocity: Float = -90f): TileData = apply {
        bumpVelocityY = velocity
    }

    /**
     * Updates spring recoil motion towards resting offset 0f.
     */
    fun updateBump(dt: Float, gravity: Float = 800f): TileData = apply {
        if (bumpOffsetY != 0f || bumpVelocityY != 0f) {
            bumpOffsetY += bumpVelocityY * dt
            bumpVelocityY += gravity * dt
            bumpTimer += dt
            if (bumpOffsetY >= 0f) {
                bumpOffsetY = 0f
                bumpVelocityY = 0f
                bumpTimer = 0f
            }
        }
    }

    fun mutateTo(newType: TileType): TileData = apply {
        type = newType
    }
}
