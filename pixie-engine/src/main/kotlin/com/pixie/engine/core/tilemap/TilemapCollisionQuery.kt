package com.pixie.engine.core.tilemap

import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Indicates which physical face of a tile was contacted during collision.
 */
enum class CollisionContactSide {
    CEILING,    // Tile was struck from below (e.g. player head bonking block)
    GROUND,     // Tile was struck from above (e.g. player landing on surface)
    LEFT_WALL,  // Tile's right face was struck by entity moving left
    RIGHT_WALL  // Tile's left face was struck by entity moving right
}

/**
 * Result data contract for an individual tile collision contact.
 */
data class TileCollisionHit(
    val col: Int,
    val row: Int,
    val tile: TileData,
    val side: CollisionContactSide,
    val bounds: RectF,
    val penetration: Float = 0f
)

/**
 * Tile grid coordinate bounds representing intersected columns and rows.
 */
data class TileGridRange(
    val minCol: Int,
    val maxCol: Int,
    val minRow: Int,
    val maxRow: Int
) {
    val isValid: Boolean get() = minCol <= maxCol && minRow <= maxRow

    inline fun forEachCell(action: (col: Int, row: Int) -> Unit) {
        if (!isValid) return
        for (r in minRow..maxRow) {
            for (c in minCol..maxCol) {
                action(c, r)
            }
        }
    }
}

/**
 * Result of resolving an entity's horizontal translation against tilemap geometry.
 */
data class HorizontalResolution(
    val resolvedX: Float,
    val resolvedVelocityX: Float,
    val hits: List<TileCollisionHit>
) {
    val hasWallCollision: Boolean get() = hits.isNotEmpty()
}

/**
 * Result of resolving an entity's vertical translation against tilemap geometry.
 */
data class VerticalResolution(
    val resolvedY: Float,
    val resolvedVelocityY: Float,
    val hits: List<TileCollisionHit>,
    val isGrounded: Boolean = false,
    val hasBonkedCeiling: Boolean = false
)

/**
 * High-performance AABB tilemap collision query utility for NES-style platformers.
 *
 * Implements decoupled 2-axis sweeps, grid range conversions, one-way ledge pass-through,
 * and contact side classifications.
 */
object TilemapCollisionQuery {

    const val BOUNDARY_EPSILON: Float = 0.001f
    const val ONE_WAY_LEDGE_TOLERANCE: Float = 4.0f

    /**
     * Converts an AABB bounding box to tile grid column and row ranges.
     */
    fun computeGridRange(
        box: RectF,
        tileSize: Float,
        maxCols: Int = Int.MAX_VALUE,
        maxRows: Int = Int.MAX_VALUE
    ): TileGridRange {
        if (tileSize <= 0f || maxCols <= 0 || maxRows <= 0) {
            return TileGridRange(0, -1, 0, -1)
        }
        val totalWidth = maxCols * tileSize
        val totalHeight = maxRows * tileSize

        if (box.right <= 0f || box.left >= totalWidth || box.bottom <= 0f || box.top >= totalHeight) {
            return TileGridRange(0, -1, 0, -1)
        }

        val rawMinCol = floor(box.left / tileSize).toInt()
        val rawMaxCol = floor((box.right - BOUNDARY_EPSILON) / tileSize).toInt()
        val rawMinRow = floor(box.top / tileSize).toInt()
        val rawMaxRow = floor((box.bottom - BOUNDARY_EPSILON) / tileSize).toInt()

        val minCol = rawMinCol.coerceAtLeast(0)
        val maxCol = rawMaxCol.coerceAtMost(maxCols - 1)
        val minRow = rawMinRow.coerceAtLeast(0)
        val maxRow = rawMaxRow.coerceAtMost(maxRows - 1)

        return if (minCol <= maxCol && minRow <= maxRow) {
            TileGridRange(minCol, maxCol, minRow, maxRow)
        } else {
            TileGridRange(0, -1, 0, -1)
        }
    }

    /**
     * Overload accepting explicit coordinates and dimensions.
     */
    fun computeGridRange(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        tileSize: Float,
        maxCols: Int = Int.MAX_VALUE,
        maxRows: Int = Int.MAX_VALUE
    ): TileGridRange {
        return computeGridRange(RectF.fromTopLeft(x, y, width, height), tileSize, maxCols, maxRows)
    }

    /**
     * Queries horizontal collisions against solid tiles.
     */
    fun queryHorizontal(
        layer: TilemapChunkLayer,
        box: RectF,
        velocityX: Float
    ): List<TileCollisionHit> {
        if (velocityX == 0f || layer.cols <= 0 || layer.rows <= 0) return emptyList()

        val range = computeGridRange(box, layer.tileSize, layer.cols, layer.rows)
        if (!range.isValid) return emptyList()

        val hits = mutableListOf<TileCollisionHit>()
        val side = if (velocityX > 0f) CollisionContactSide.RIGHT_WALL else CollisionContactSide.LEFT_WALL

        range.forEachCell { col, row ->
            val tile = layer.getTile(col, row)
            if (tile != null && tile.isSolid) {
                val tileBounds = layer.getTileBounds(col, row)
                if (box.intersects(tileBounds)) {
                    val penetration = if (velocityX > 0f) {
                        box.right - tileBounds.left
                    } else {
                        tileBounds.right - box.left
                    }
                    hits.add(TileCollisionHit(col, row, tile, side, tileBounds, penetration))
                }
            }
        }
        return hits
    }

    /**
     * Resolves horizontal position and velocity against solid walls.
     */
    fun resolveHorizontal(
        layer: TilemapChunkLayer,
        box: RectF,
        velocityX: Float
    ): HorizontalResolution {
        val hits = queryHorizontal(layer, box, velocityX)
        if (hits.isEmpty()) {
            return HorizontalResolution(box.left, velocityX, emptyList())
        }

        var resolvedLeft = box.left
        var resolvedVelX = velocityX

        if (velocityX > 0f) {
            val minWallLeft = hits.minOf { it.bounds.left }
            resolvedLeft = minWallLeft - box.width
            resolvedVelX = 0f
        } else if (velocityX < 0f) {
            val maxWallRight = hits.maxOf { it.bounds.right }
            resolvedLeft = maxWallRight
            resolvedVelX = 0f
        }

        return HorizontalResolution(resolvedLeft, resolvedVelX, hits)
    }

    /**
     * Queries vertical collisions against solid ceilings, floors, and one-way ledges.
     */
    fun queryVertical(
        layer: TilemapChunkLayer,
        box: RectF,
        velocityY: Float,
        previousBottom: Float? = null,
        oneWayTolerance: Float = ONE_WAY_LEDGE_TOLERANCE
    ): List<TileCollisionHit> {
        if (layer.cols <= 0 || layer.rows <= 0) return emptyList()

        val range = computeGridRange(box, layer.tileSize, layer.cols, layer.rows)
        if (!range.isValid) return emptyList()

        val hits = mutableListOf<TileCollisionHit>()

        range.forEachCell { col, row ->
            val tile = layer.getTile(col, row) ?: return@forEachCell
            val tileBounds = layer.getTileBounds(col, row)

            if (velocityY > 0f) {
                // Downward falling: test ground and one-way ledges
                if (tile.isSolid && box.intersects(tileBounds)) {
                    val penetration = box.bottom - tileBounds.top
                    hits.add(TileCollisionHit(col, row, tile, CollisionContactSide.GROUND, tileBounds, penetration))
                } else if (tile.isOneWay && box.intersects(tileBounds)) {
                    val canLand = previousBottom == null || previousBottom <= tileBounds.top + oneWayTolerance
                    if (canLand) {
                        val penetration = box.bottom - tileBounds.top
                        hits.add(TileCollisionHit(col, row, tile, CollisionContactSide.GROUND, tileBounds, penetration))
                    }
                }
            } else if (velocityY < 0f) {
                // Upward jumping: test solid ceilings only (one-way ledges are passed through)
                if (tile.isSolid && box.intersects(tileBounds)) {
                    val penetration = tileBounds.bottom - box.top
                    hits.add(TileCollisionHit(col, row, tile, CollisionContactSide.CEILING, tileBounds, penetration))
                }
            }
        }
        return hits
    }

    /**
     * Resolves vertical position and velocity against ground and ceiling obstacles.
     */
    fun resolveVertical(
        layer: TilemapChunkLayer,
        box: RectF,
        velocityY: Float,
        previousBottom: Float? = null,
        oneWayTolerance: Float = ONE_WAY_LEDGE_TOLERANCE
    ): VerticalResolution {
        val hits = queryVertical(layer, box, velocityY, previousBottom, oneWayTolerance)
        if (hits.isEmpty()) {
            return VerticalResolution(box.top, velocityY, emptyList(), isGrounded = false, hasBonkedCeiling = false)
        }

        var resolvedTop = box.top
        var resolvedVelY = velocityY
        var isGrounded = false
        var hasBonkedCeiling = false

        if (velocityY > 0f) {
            val groundHits = hits.filter { it.side == CollisionContactSide.GROUND }
            if (groundHits.isNotEmpty()) {
                val highestGroundTop = groundHits.minOf { it.bounds.top }
                resolvedTop = highestGroundTop - box.height
                resolvedVelY = 0f
                isGrounded = true
            }
        } else if (velocityY < 0f) {
            val ceilingHits = hits.filter { it.side == CollisionContactSide.CEILING }
            if (ceilingHits.isNotEmpty()) {
                val lowestCeilingBottom = ceilingHits.maxOf { it.bounds.bottom }
                resolvedTop = lowestCeilingBottom
                resolvedVelY = 0f
                hasBonkedCeiling = true
            }
        }

        return VerticalResolution(resolvedTop, resolvedVelY, hits, isGrounded, hasBonkedCeiling)
    }

    /**
     * Combined collision query identifying all intersecting tiles and classifying contact sides.
     */
    fun queryCollisions(
        layer: TilemapChunkLayer,
        currentBox: RectF,
        previousBox: RectF? = null,
        velocity: Vec2 = Vec2.ZERO
    ): List<TileCollisionHit> {
        val range = computeGridRange(currentBox, layer.tileSize, layer.cols, layer.rows)
        if (!range.isValid) return emptyList()

        val hits = mutableListOf<TileCollisionHit>()

        range.forEachCell { col, row ->
            val tile = layer.getTile(col, row) ?: return@forEachCell
            val tileBounds = layer.getTileBounds(col, row)
            if (!currentBox.intersects(tileBounds)) return@forEachCell

            if (tile.isOneWay) {
                val prevBottom = previousBox?.bottom
                val canLand = velocity.y >= 0f && (prevBottom == null || prevBottom <= tileBounds.top + ONE_WAY_LEDGE_TOLERANCE)
                if (canLand) {
                    hits.add(TileCollisionHit(col, row, tile, CollisionContactSide.GROUND, tileBounds, currentBox.bottom - tileBounds.top))
                }
            } else if (tile.isSolid) {
                val side = classifyContactSide(currentBox, previousBox, velocity, tileBounds)
                val penetration = computePenetration(currentBox, tileBounds, side)
                hits.add(TileCollisionHit(col, row, tile, side, tileBounds, penetration))
            }
        }

        return hits
    }

    private fun classifyContactSide(
        current: RectF,
        previous: RectF?,
        velocity: Vec2,
        tileBounds: RectF
    ): CollisionContactSide {
        if (previous != null) {
            if (previous.bottom <= tileBounds.top + ONE_WAY_LEDGE_TOLERANCE && velocity.y >= 0f) {
                return CollisionContactSide.GROUND
            }
            if (previous.top >= tileBounds.bottom - ONE_WAY_LEDGE_TOLERANCE && velocity.y <= 0f) {
                return CollisionContactSide.CEILING
            }
            if (previous.right <= tileBounds.left + ONE_WAY_LEDGE_TOLERANCE && velocity.x >= 0f) {
                return CollisionContactSide.RIGHT_WALL
            }
            if (previous.left >= tileBounds.right - ONE_WAY_LEDGE_TOLERANCE && velocity.x <= 0f) {
                return CollisionContactSide.LEFT_WALL
            }
        }

        val overlapX = min(current.right, tileBounds.right) - max(current.left, tileBounds.left)
        val overlapY = min(current.bottom, tileBounds.bottom) - max(current.top, tileBounds.top)

        return if (overlapX < overlapY) {
            if (current.centerX < tileBounds.centerX) CollisionContactSide.RIGHT_WALL else CollisionContactSide.LEFT_WALL
        } else {
            if (current.centerY < tileBounds.centerY) CollisionContactSide.GROUND else CollisionContactSide.CEILING
        }
    }

    private fun computePenetration(box: RectF, tileBounds: RectF, side: CollisionContactSide): Float {
        return when (side) {
            CollisionContactSide.GROUND -> box.bottom - tileBounds.top
            CollisionContactSide.CEILING -> tileBounds.bottom - box.top
            CollisionContactSide.RIGHT_WALL -> box.right - tileBounds.left
            CollisionContactSide.LEFT_WALL -> tileBounds.right - box.left
        }
    }
}
