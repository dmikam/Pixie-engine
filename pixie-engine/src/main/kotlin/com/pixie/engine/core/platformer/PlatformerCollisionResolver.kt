package com.pixie.engine.core.platformer

import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.tilemap.CollisionContactSide
import com.pixie.engine.core.tilemap.TileCollisionHit
import com.pixie.engine.core.tilemap.TilemapChunkLayer
import com.pixie.engine.core.tilemap.TilemapCollisionQuery
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Result data contract for 2-axis decoupled platformer collision resolution.
 */
data class PlatformerCollisionResult(
    val horizontalHits: List<TileCollisionHit> = emptyList(),
    val verticalHits: List<TileCollisionHit> = emptyList(),
    val isGrounded: Boolean = false,
    val hasBonkedCeiling: Boolean = false,
    val hasWallCollision: Boolean = false
) {
    val totalHits: Int get() = horizontalHits.size + verticalHits.size
}

/**
 * Internal helper for vertical sub-step resolution output.
 */
private data class VerticalStepResult(
    val hits: List<TileCollisionHit>,
    val isGrounded: Boolean,
    val hasBonkedCeiling: Boolean
)

/**
 * Decoupled 2-Axis Sub-Stepped Tilemap Collision Resolver.
 *
 * Implements authentic NES platformer collision resolution:
 * - Sub-steps physics updates to eliminate tunneling at high velocities.
 * - Decouples X and Y collision queries (resolves wall collisions first, then floor/ceiling).
 * - Clamps entity coordinates to tile boundaries and zeroes velocity upon contact.
 * - Manages ground landing, coyote time reset, and head-bonk recoil events on colliding blocks.
 */
class PlatformerCollisionResolver(
    var hitboxWidth: Float = 14f,
    var hitboxHeight: Float = 16f,
    var hitboxOffsetX: Float = 1f,
    var hitboxOffsetY: Float = 0f,
    var maxSubStepDt: Float = 1f / 60f,
    var maxSubStepSize: Float = 8f,
    var groundProbeDistance: Float = 1.5f
) {
    var onHeadBonk: ((tileX: Int, tileY: Int) -> Unit)? = null
    var onLand: ((col: Int, row: Int) -> Unit)? = null
    var onWallHit: ((col: Int, row: Int, isRightWall: Boolean) -> Unit)? = null

    // --- Fluent Configuration Builders ---

    fun withHitbox(
        width: Float,
        height: Float,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ): PlatformerCollisionResolver = apply {
        this.hitboxWidth = width
        this.hitboxHeight = height
        this.hitboxOffsetX = offsetX
        this.hitboxOffsetY = offsetY
    }

    fun withSubStepping(maxDt: Float, maxSize: Float): PlatformerCollisionResolver = apply {
        this.maxSubStepDt = maxDt
        this.maxSubStepSize = maxSize
    }

    fun onHeadBonk(listener: (tileX: Int, tileY: Int) -> Unit): PlatformerCollisionResolver = apply {
        this.onHeadBonk = listener
    }

    fun onLand(listener: (col: Int, row: Int) -> Unit): PlatformerCollisionResolver = apply {
        this.onLand = listener
    }

    fun onWallHit(listener: (col: Int, row: Int, isRightWall: Boolean) -> Unit): PlatformerCollisionResolver = apply {
        this.onWallHit = listener
    }

    fun getHitbox(pos: Vec2): RectF {
        return RectF.fromTopLeft(pos.x + hitboxOffsetX, pos.y + hitboxOffsetY, hitboxWidth, hitboxHeight)
    }

    // --- Primary Resolution Pipeline ---

    fun resolve(
        controller: PlatformerPhysicsController,
        layer: TilemapChunkLayer,
        dt: Float
    ): PlatformerCollisionResult {
        if (dt <= 0f) {
            return PlatformerCollisionResult(isGrounded = controller.isGrounded)
        }

        val steps = calculateSubStepCount(controller, dt)
        val subDt = dt / steps

        val allHorizontalHits = mutableListOf<TileCollisionHit>()
        val allVerticalHits = mutableListOf<TileCollisionHit>()
        var wallHitOccurred = false
        var ceilingBonked = false

        for (step in 0 until steps) {
            val stepResult = resolveSubStep(controller, layer, subDt)
            allHorizontalHits.addAll(stepResult.horizontalHits)
            allVerticalHits.addAll(stepResult.verticalHits)
            if (stepResult.hasWallCollision) wallHitOccurred = true
            if (stepResult.hasBonkedCeiling) ceilingBonked = true
        }

        return PlatformerCollisionResult(
            horizontalHits = allHorizontalHits,
            verticalHits = allVerticalHits,
            isGrounded = controller.isGrounded,
            hasBonkedCeiling = ceilingBonked,
            hasWallCollision = wallHitOccurred
        )
    }

    private fun calculateSubStepCount(controller: PlatformerPhysicsController, dt: Float): Int {
        val maxVel = max(abs(controller.velocityX), abs(controller.velocityY))
        val maxDisplacement = maxVel * dt
        val timeSteps = ceil(dt / maxSubStepDt).toInt()
        val distSteps = if (maxSubStepSize > 0f) ceil(maxDisplacement / maxSubStepSize).toInt() else 1
        return max(timeSteps, distSteps).coerceIn(1, 8)
    }

    private fun resolveSubStep(
        controller: PlatformerPhysicsController,
        layer: TilemapChunkLayer,
        subDt: Float
    ): PlatformerCollisionResult {
        controller.updateTimers(subDt)

        // Step 1: Update X position and resolve wall collisions
        val hHits = resolveHorizontalStep(controller, layer, subDt)

        // Verify ground support after horizontal translation
        if (controller.isGrounded && controller.velocityY >= 0f) {
            val hasSupport = checkGroundSupport(controller, layer)
            if (!hasSupport) {
                controller.leaveGround()
            }
        }

        // Step 2: Update Y position and resolve floor/ceiling collisions
        val vResult = resolveVerticalStep(controller, layer, subDt)

        // Update animation state
        controller.updateStateFromResolution()

        return PlatformerCollisionResult(
            horizontalHits = hHits,
            verticalHits = vResult.hits,
            isGrounded = controller.isGrounded,
            hasBonkedCeiling = vResult.hasBonkedCeiling,
            hasWallCollision = hHits.isNotEmpty()
        )
    }

    // --- Step 1: Horizontal Movement & Wall Collision Resolution ---

    private fun resolveHorizontalStep(
        controller: PlatformerPhysicsController,
        layer: TilemapChunkLayer,
        subDt: Float
    ): List<TileCollisionHit> {
        val newVx = controller.calculateHorizontalVelocity(subDt)
        controller.velocity = Vec2(newVx, controller.velocityY)

        if (newVx == 0f) return emptyList()

        val targetX = controller.position.x + newVx * subDt
        val currentBox = getHitbox(controller.position)
        val targetBox = RectF.fromTopLeft(targetX + hitboxOffsetX, currentBox.top, hitboxWidth, hitboxHeight)

        val hits = TilemapCollisionQuery.queryHorizontal(layer, targetBox, newVx)
        return if (hits.isNotEmpty()) {
            clampHorizontalToWall(controller, hits, newVx)
            hits
        } else {
            controller.position = Vec2(targetX, controller.position.y)
            emptyList()
        }
    }

    private fun clampHorizontalToWall(
        controller: PlatformerPhysicsController,
        hits: List<TileCollisionHit>,
        newVx: Float
    ) {
        if (newVx > 0f) {
            val minWallLeft = hits.minOf { it.bounds.left }
            val clampedX = minWallLeft - hitboxWidth - hitboxOffsetX
            controller.position = Vec2(clampedX, controller.position.y)
            hits.forEach { onWallHit?.invoke(it.col, it.row, true) }
        } else {
            val maxWallRight = hits.maxOf { it.bounds.right }
            val clampedX = maxWallRight - hitboxOffsetX
            controller.position = Vec2(clampedX, controller.position.y)
            hits.forEach { onWallHit?.invoke(it.col, it.row, false) }
        }
        controller.stopHorizontal()
    }

    // --- Step 2: Vertical Movement & Floor / Ceiling Collision Resolution ---

    private fun resolveVerticalStep(
        controller: PlatformerPhysicsController,
        layer: TilemapChunkLayer,
        subDt: Float
    ): VerticalStepResult {
        val newVy = controller.calculateVerticalVelocity(subDt)
        controller.velocity = Vec2(controller.velocityX, newVy)

        val prevBottom = getHitbox(controller.position).bottom
        val targetY = controller.position.y + newVy * subDt
        val currentBox = getHitbox(controller.position)
        val targetBox = RectF.fromTopLeft(currentBox.left, targetY + hitboxOffsetY, hitboxWidth, hitboxHeight)

        val hits = TilemapCollisionQuery.queryVertical(layer, targetBox, newVy, prevBottom)

        return when {
            newVy > 0f -> handleFloorLanding(controller, hits, targetY)
            newVy < 0f -> handleCeilingBonk(controller, layer, hits, targetY, targetBox)
            else -> {
                controller.position = Vec2(controller.position.x, targetY)
                VerticalStepResult(emptyList(), isGrounded = controller.isGrounded, hasBonkedCeiling = false)
            }
        }
    }

    private fun handleFloorLanding(
        controller: PlatformerPhysicsController,
        hits: List<TileCollisionHit>,
        targetY: Float
    ): VerticalStepResult {
        val groundHits = hits.filter { it.side == CollisionContactSide.GROUND }
        return if (groundHits.isNotEmpty()) {
            val highestGroundTop = groundHits.minOf { it.bounds.top }
            val clampedY = highestGroundTop - hitboxHeight - hitboxOffsetY
            controller.landOnGround(clampedY)
            groundHits.forEach { onLand?.invoke(it.col, it.row) }
            VerticalStepResult(hits, isGrounded = true, hasBonkedCeiling = false)
        } else {
            controller.position = Vec2(controller.position.x, targetY)
            controller.setGrounded(false)
            VerticalStepResult(emptyList(), isGrounded = false, hasBonkedCeiling = false)
        }
    }

    private fun handleCeilingBonk(
        controller: PlatformerPhysicsController,
        layer: TilemapChunkLayer,
        hits: List<TileCollisionHit>,
        targetY: Float,
        targetBox: RectF
    ): VerticalStepResult {
        val ceilingHits = hits.filter { it.side == CollisionContactSide.CEILING }
        return if (ceilingHits.isNotEmpty()) {
            val lowestCeilingBottom = ceilingHits.maxOf { it.bounds.bottom }
            val clampedY = lowestCeilingBottom - hitboxOffsetY
            controller.bonkCeiling(clampedY)

            val primaryHit = pickPrimaryCeilingTile(ceilingHits, targetBox)
            if (primaryHit != null) {
                layer.triggerBlockBonk(primaryHit.col, primaryHit.row)
                onHeadBonk?.invoke(primaryHit.col, primaryHit.row)
                controller.onHeadBonk?.invoke(primaryHit.col, primaryHit.row)
            }

            VerticalStepResult(hits, isGrounded = false, hasBonkedCeiling = true)
        } else {
            controller.position = Vec2(controller.position.x, targetY)
            VerticalStepResult(emptyList(), isGrounded = false, hasBonkedCeiling = false)
        }
    }

    private fun pickPrimaryCeilingTile(hits: List<TileCollisionHit>, box: RectF): TileCollisionHit? {
        if (hits.isEmpty()) return null
        return hits.minByOrNull { abs(it.bounds.centerX - box.centerX) }
    }

    // --- Ground Support Probe ---

    fun checkGroundSupport(
        controller: PlatformerPhysicsController,
        layer: TilemapChunkLayer
    ): Boolean {
        if (layer.cols <= 0 || layer.rows <= 0) return false
        val box = getHitbox(controller.position)
        val inset = 1f.coerceAtMost(box.width * 0.25f)
        val probeBox = RectF.fromTopLeft(
            box.left + inset,
            box.bottom - 0.5f,
            box.width - (inset * 2f),
            groundProbeDistance + 0.5f
        )
        val range = TilemapCollisionQuery.computeGridRange(probeBox, layer.tileSize, layer.cols, layer.rows)
        if (!range.isValid) return false

        var supported = false
        range.forEachCell { col, row ->
            val tile = layer.getTile(col, row)
            if (tile != null) {
                val tileBounds = layer.getTileBounds(col, row)
                if (tile.isSolid && probeBox.intersects(tileBounds)) {
                    supported = true
                } else if (tile.isOneWay && probeBox.intersects(tileBounds)) {
                    if (box.bottom <= tileBounds.top + TilemapCollisionQuery.ONE_WAY_LEDGE_TOLERANCE) {
                        supported = true
                    }
                }
            }
        }
        return supported
    }

    companion object {
        val DEFAULT = PlatformerCollisionResolver()
    }
}
