package com.pixie.engine.core.camera

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import kotlin.math.max

/**
 * NES-grade 2D platformer camera controller.
 *
 * Provides:
 * - Horizontal deadzone: Camera remains completely static while the target moves within a configurable central box.
 * - Directional lookahead lead: Smoothly biases framing forward in the target's facing direction.
 * - Stage boundary clamping: Prevents camera from scrolling out of stage bounds (both horizontally and vertically).
 * - Fixed baseline horizon lock: Prevents nauseating vertical camera jitter during jumps.
 * - Backward scroll restriction: Optional SMB1-style lock preventing camera from retreating left.
 * - Fluent chaining API adhering to AGENTS.md conventions.
 */
class PlatformerFollowViewMode(
    var targetPosition: (() -> Vec2)? = null,
    var deadzoneWidth: Float = 48f,
    var leadOffset: Float = 32f,
    var stageWidth: Float = 3200f,
    var stageHeight: Float = 180f,
    var fixedBaselineY: Float = 0f,
    var isVerticalLocked: Boolean = true,
    var smoothing: Float = 10f,
    var verticalSmoothing: Float = 4f,
    var leadSmoothing: Float = 6f,
    var minX: Float = 0f,
    var minY: Float = 0f,
    var facingDirection: Float = 1f,
    var allowScrollBackwards: Boolean = true,
    var verticalDeadzoneHeight: Float = 48f
) : GameViewMode {

    var currentLeadOffset: Float = 0f
        private set

    var maxSeenCameraX: Float = 0f
        private set

    var previousTargetPos: Vec2? = null
        private set

    var facingProvider: (() -> Float)? = null

    // --- Fluent Builders ---

    fun target(provider: () -> Vec2): PlatformerFollowViewMode = apply {
        this.targetPosition = provider
    }

    fun target(obj: PixieObject): PlatformerFollowViewMode = apply {
        this.targetPosition = { obj.position }
    }

    fun deadzone(width: Float, verticalHeight: Float = verticalDeadzoneHeight): PlatformerFollowViewMode = apply {
        this.deadzoneWidth = width
        this.verticalDeadzoneHeight = verticalHeight
    }

    fun lookahead(lead: Float, smoothing: Float = leadSmoothing): PlatformerFollowViewMode = apply {
        this.leadOffset = lead
        this.leadSmoothing = smoothing
    }

    fun stageBounds(
        width: Float,
        height: Float = stageHeight,
        minX: Float = 0f,
        minY: Float = 0f
    ): PlatformerFollowViewMode = apply {
        this.stageWidth = width
        this.stageHeight = height
        this.minX = minX
        this.minY = minY
    }

    fun horizonLock(baselineY: Float = 0f, locked: Boolean = true): PlatformerFollowViewMode = apply {
        this.fixedBaselineY = baselineY
        this.isVerticalLocked = locked
    }

    fun smoothing(
        horizontal: Float = smoothing,
        vertical: Float = verticalSmoothing
    ): PlatformerFollowViewMode = apply {
        this.smoothing = horizontal
        this.verticalSmoothing = vertical
    }

    fun facing(direction: Float): PlatformerFollowViewMode = apply {
        this.facingDirection = if (direction >= 0f) 1f else -1f
    }

    fun facing(provider: () -> Float): PlatformerFollowViewMode = apply {
        this.facingProvider = provider
    }

    fun allowBackwardsScroll(allow: Boolean): PlatformerFollowViewMode = apply {
        this.allowScrollBackwards = allow
    }

    // --- Core Camera Update ---

    fun update(camera: PixieCamera, target: PixieObject?, dt: Float, context: GameContext) {
        if (dt <= 0f) return

        val targetPos = resolveTargetPosition(target)
        updateFacingDirection(targetPos)
        updateLookaheadLead(dt)

        val nextX = calculateHorizontalCameraX(camera, targetPos, dt)
        val nextY = calculateVerticalCameraY(camera, targetPos, dt)

        val clampedPos = clampToBounds(camera, nextX, nextY)
        camera.setPositionImmediate(clampedPos)
    }

    fun snapToTarget(camera: PixieCamera, target: PixieObject? = null): PlatformerFollowViewMode = apply {
        val targetPos = resolveTargetPosition(target)
        val facing = facingProvider?.invoke() ?: facingDirection
        facingDirection = if (facing >= 0f) 1f else -1f
        currentLeadOffset = facingDirection * leadOffset

        val viewportWidth = camera.viewport.width.toFloat()
        val viewportHeight = camera.viewport.height.toFloat()

        val deadzoneCenterScreen = (viewportWidth * 0.5f) - currentLeadOffset
        val idealX = targetPos.x - deadzoneCenterScreen

        val idealY = if (isVerticalLocked) {
            fixedBaselineY
        } else {
            targetPos.y - (viewportHeight * 0.5f)
        }

        maxSeenCameraX = minX
        val clamped = clampToBounds(camera, idealX, idealY)
        camera.setPositionImmediate(clamped)
        maxSeenCameraX = clamped.x
        previousTargetPos = targetPos
    }

    fun resetScrollLock(newMinX: Float = minX) {
        maxSeenCameraX = newMinX
    }

    // --- Private Calculation Helpers ---

    private fun resolveTargetPosition(target: PixieObject?): Vec2 {
        return targetPosition?.invoke() ?: target?.position ?: Vec2.ZERO
    }

    private fun updateFacingDirection(targetPos: Vec2) {
        val externalFacing = facingProvider?.invoke()
        if (externalFacing != null) {
            facingDirection = if (externalFacing >= 0f) 1f else -1f
        } else {
            val prev = previousTargetPos
            if (prev != null) {
                val deltaX = targetPos.x - prev.x
                if (deltaX > 0.05f) {
                    facingDirection = 1f
                } else if (deltaX < -0.05f) {
                    facingDirection = -1f
                }
            }
        }
        previousTargetPos = targetPos
    }

    private fun updateLookaheadLead(dt: Float) {
        val targetLead = facingDirection * leadOffset
        val t = (leadSmoothing * dt).coerceIn(0f, 1f)
        currentLeadOffset += (targetLead - currentLeadOffset) * t
    }

    private fun calculateHorizontalCameraX(camera: PixieCamera, targetPos: Vec2, dt: Float): Float {
        val viewportWidth = camera.viewport.width.toFloat()
        val halfViewport = viewportWidth * 0.5f

        // Center of deadzone on screen, biased by lookahead
        val deadzoneCenterScreen = halfViewport - currentLeadOffset
        val halfDeadzone = deadzoneWidth * 0.5f

        val deadzoneLeftWorld = camera.position.x + deadzoneCenterScreen - halfDeadzone
        val deadzoneRightWorld = camera.position.x + deadzoneCenterScreen + halfDeadzone

        val desiredCameraX = when {
            targetPos.x > deadzoneRightWorld -> camera.position.x + (targetPos.x - deadzoneRightWorld)
            targetPos.x < deadzoneLeftWorld -> camera.position.x + (targetPos.x - deadzoneLeftWorld)
            else -> camera.position.x
        }

        return if (smoothing > 0f && desiredCameraX != camera.position.x) {
            val t = (smoothing * dt).coerceIn(0f, 1f)
            camera.position.x + (desiredCameraX - camera.position.x) * t
        } else {
            desiredCameraX
        }
    }

    private fun calculateVerticalCameraY(camera: PixieCamera, targetPos: Vec2, dt: Float): Float {
        if (isVerticalLocked) {
            if (verticalSmoothing > 0f && camera.position.y != fixedBaselineY) {
                val t = (verticalSmoothing * dt).coerceIn(0f, 1f)
                return camera.position.y + (fixedBaselineY - camera.position.y) * t
            }
            return fixedBaselineY
        }

        val viewportHeight = camera.viewport.height.toFloat()
        val halfViewport = viewportHeight * 0.5f
        val halfVDeadzone = verticalDeadzoneHeight * 0.5f

        val deadzoneTopWorld = camera.position.y + halfViewport - halfVDeadzone
        val deadzoneBottomWorld = camera.position.y + halfViewport + halfVDeadzone

        val desiredCameraY = when {
            targetPos.y > deadzoneBottomWorld -> camera.position.y + (targetPos.y - deadzoneBottomWorld)
            targetPos.y < deadzoneTopWorld -> camera.position.y + (targetPos.y - deadzoneTopWorld)
            else -> camera.position.y
        }

        return if (verticalSmoothing > 0f && desiredCameraY != camera.position.y) {
            val t = (verticalSmoothing * dt).coerceIn(0f, 1f)
            camera.position.y + (desiredCameraY - camera.position.y) * t
        } else {
            desiredCameraY
        }
    }

    private fun clampToBounds(camera: PixieCamera, x: Float, y: Float): Vec2 {
        val viewportWidth = camera.viewport.width.toFloat()
        val viewportHeight = camera.viewport.height.toFloat()

        val maxCameraX = max(minX, stageWidth - viewportWidth)
        val effectiveMinX = if (!allowScrollBackwards) {
            maxSeenCameraX = maxOf(maxSeenCameraX, minX)
            maxSeenCameraX
        } else {
            minX
        }

        val clampedX = x.coerceIn(effectiveMinX, maxCameraX)
        if (!allowScrollBackwards && clampedX > maxSeenCameraX) {
            maxSeenCameraX = clampedX
        }

        val maxCameraY = max(minY, stageHeight - viewportHeight)
        val clampedY = y.coerceIn(minY, maxCameraY)

        return Vec2(clampedX, clampedY)
    }
}
