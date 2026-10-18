package com.pixie.engine.core.camera

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.ports.VirtualResolution

/**
 * Strategy interface governing how the camera frames and follows action.
 */
sealed interface GameViewMode {
    /**
     * Follows player along horizontal axis (e.g. Dinorun, Chiki).
     */
    data class HorizontalScroller(
        val lookAheadX: Float = 60f,
        val lockY: Boolean = true,
        val fixedY: Float = 0f,
        val smoothing: Float = 10f
    ) : GameViewMode

    /**
     * Follows player or scrolls upward/downward along vertical axis.
     */
    data class VerticalScroller(
        val lookAheadY: Float = 60f,
        val lockX: Boolean = true,
        val fixedX: Float = 0f,
        val smoothing: Float = 10f
    ) : GameViewMode

    /**
     * Static arena viewport with fixed bounds (e.g. Arkanoid).
     */
    data class FixedScreen(
        val screenOrigin: Vec2 = Vec2.ZERO
    ) : GameViewMode
}

/**
 * 2D Camera that transforms world coordinates into virtual screen space according to the view mode.
 */
class PixieCamera(
    var viewport: VirtualResolution = VirtualResolution(320, 180),
    var viewMode: GameViewMode = GameViewMode.HorizontalScroller()
) {
    var position: Vec2 = Vec2.ZERO
        private set

    fun setPositionImmediate(pos: Vec2) {
        position = pos
    }

    fun update(target: PixieObject?, dt: Float, context: GameContext) {
        when (val mode = viewMode) {
            is GameViewMode.HorizontalScroller -> {
                val targetX = (target?.position?.x ?: 0f) - mode.lookAheadX
                val targetY = if (mode.lockY) mode.fixedY else (target?.position?.y ?: 0f)
                val targetPos = Vec2(targetX, targetY)
                position = position.lerp(targetPos, (mode.smoothing * dt).coerceIn(0f, 1f))
            }
            is GameViewMode.VerticalScroller -> {
                val targetX = if (mode.lockX) mode.fixedX else (target?.position?.x ?: 0f)
                val targetY = (target?.position?.y ?: 0f) - mode.lookAheadY
                val targetPos = Vec2(targetX, targetY)
                position = position.lerp(targetPos, (mode.smoothing * dt).coerceIn(0f, 1f))
            }
            is GameViewMode.FixedScreen -> {
                position = mode.screenOrigin
            }
        }
    }

    fun worldToScreen(worldPos: Vec2): Vec2 {
        return worldPos - position
    }

    fun screenToWorld(screenPos: Vec2): Vec2 {
        return screenPos + position
    }
}
