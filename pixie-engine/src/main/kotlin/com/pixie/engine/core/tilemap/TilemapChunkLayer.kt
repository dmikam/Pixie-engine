package com.pixie.engine.core.tilemap

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.layer.PixieLayer
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.RenderBatchPort

/**
 * 2D Tilemap Chunk Layer designed for NES-style platformers.
 *
 * Provides O(1) tile cell indexing, zero-allocation viewport culling,
 * ASCII/IntArray level streaming, dynamic block mutations, and retro-shaded pixel rendering.
 */
class TilemapChunkLayer(
    val tileSize: Float = 16f,
    val zIndex: Int = 200,
    override val name: String = "Layer_TilemapChunk"
) : PixieLayer {

    override val order: Int = zIndex
    override var isEnabled: Boolean = true
    override var isGridEnabled: Boolean = false

    var cols: Int = 0
        private set
    var rows: Int = 0
        private set

    val widthPx: Float get() = cols * tileSize
    val heightPx: Float get() = rows * tileSize

    private var cells: Array<TileData?> = emptyArray()
    private val activeBumpingTiles = mutableListOf<TileData>()

    var bumpGravity: Float = 800f
    var entityProvider: (() -> Collection<PixieObject>)? = null
    var onEntityKnocked: ((entity: PixieObject, col: Int, row: Int) -> Unit)? = null
    var onBlockBonk: ((col: Int, row: Int, tile: TileData) -> Unit)? = null

    fun withBumpGravity(gravity: Float): TilemapChunkLayer = apply {
        this.bumpGravity = gravity
    }

    fun withEntityProvider(provider: () -> Collection<PixieObject>): TilemapChunkLayer = apply {
        this.entityProvider = provider
    }

    fun onBlockBonk(listener: (col: Int, row: Int, tile: TileData) -> Unit): TilemapChunkLayer = apply {
        this.onBlockBonk = listener
    }

    fun onEntityKnocked(listener: (entity: PixieObject, col: Int, row: Int) -> Unit): TilemapChunkLayer = apply {
        this.onEntityKnocked = listener
    }

    fun loadAscii(lines: List<String>): TilemapChunkLayer = apply {
        rows = lines.size
        cols = lines.maxOfOrNull { it.length } ?: 0
        cells = Array(rows * cols) { idx ->
            val r = idx / cols
            val c = idx % cols
            val char = lines.getOrNull(r)?.getOrNull(c) ?: '.'
            val type = TileType.fromChar(char)
            if (type != TileType.EMPTY) TileData(type) else null
        }
        activeBumpingTiles.clear()
    }

    fun loadAscii(ascii: String): TilemapChunkLayer = apply {
        val lines = ascii.trimIndent().lines().filter { it.isNotEmpty() }
        loadAscii(lines)
    }

    fun loadIntArray(
        grid: Array<IntArray>,
        typeMapper: (Int) -> TileType = { id ->
            TileType.entries.getOrElse(id) { TileType.EMPTY }
        }
    ): TilemapChunkLayer = apply {
        rows = grid.size
        cols = if (rows > 0) grid[0].size else 0
        cells = Array(rows * cols) { idx ->
            val r = idx / cols
            val c = idx % cols
            val tileId = if (r in grid.indices && c in grid[r].indices) grid[r][c] else 0
            val type = typeMapper(tileId)
            if (type != TileType.EMPTY) TileData(type) else null
        }
        activeBumpingTiles.clear()
    }

    fun getTile(col: Int, row: Int): TileData? {
        if (col !in 0 until cols || row !in 0 until rows) return null
        return cells[row * cols + col]
    }

    fun setTile(col: Int, row: Int, type: TileType): TileData? {
        if (col !in 0 until cols || row !in 0 until rows) return null
        val idx = row * cols + col
        val existing = cells[idx]
        return if (type == TileType.EMPTY) {
            cells[idx] = null
            null
        } else if (existing != null) {
            existing.mutateTo(type)
        } else {
            val created = TileData(type)
            cells[idx] = created
            created
        }
    }

    fun worldToTileCol(worldX: Float): Int = (worldX / tileSize).toInt()

    fun worldToTileRow(worldY: Float): Int = (worldY / tileSize).toInt()

    fun getTileAtWorld(worldX: Float, worldY: Float): TileData? {
        return getTile(worldToTileCol(worldX), worldToTileRow(worldY))
    }

    fun getTileBounds(col: Int, row: Int): RectF {
        val x = col * tileSize
        val y = row * tileSize
        return RectF.fromTopLeft(x, y, tileSize, tileSize)
    }

    /**
     * Triggers a head-bonk spring recoil animation on the tile at [col], [row].
     *
     * - Launches the tile upward with initial [velocity] (default -90f).
     * - In layer update tick, applies gravity return until bumpOffsetY == 0f.
     * - Applies an upward knock impulse of [knockVelocityY] (default -120f) to any entity
     *   standing directly on top of this tile.
     *
     * @return true if the tile was valid and bumpable, false otherwise.
     */
    fun triggerBlockBonk(
        col: Int,
        row: Int,
        velocity: Float = -90f,
        entities: Collection<PixieObject>? = null,
        knockVelocityY: Float = -120f
    ): Boolean {
        val tile = getTile(col, row) ?: return false
        if (!tile.isBumpable) return false
        tile.triggerBump(velocity)
        if (!activeBumpingTiles.contains(tile)) {
            activeBumpingTiles.add(tile)
        }

        knockEntitiesOnTop(col, row, knockVelocityY, entities)
        onBlockBonk?.invoke(col, row, tile)
        return true
    }

    /**
     * Scans for any active entity standing directly on top of the tile at [col], [row]
     * and imparts an upward knock impulse [knockVelocityY].
     */
    fun knockEntitiesOnTop(
        col: Int,
        row: Int,
        knockVelocityY: Float = -120f,
        entities: Collection<PixieObject>? = null
    ): List<PixieObject> {
        val tileBounds = getTileBounds(col, row)
        val candidateEntities = entities ?: entityProvider?.invoke() ?: return emptyList()
        val affected = mutableListOf<PixieObject>()

        for (entity in candidateEntities) {
            if (!entity.isAlive) continue
            val box = entity.boundingBox
            val overlapsX = box.right > tileBounds.left && box.left < tileBounds.right
            val standingOnTop = box.bottom >= tileBounds.top - 3f && box.bottom <= tileBounds.top + 3f
            if (overlapsX && standingOnTop) {
                entity.velocity = Vec2(entity.velocity.x, knockVelocityY)
                affected.add(entity)
                onEntityKnocked?.invoke(entity, col, row)
            }
        }
        return affected
    }

    fun triggerBump(
        col: Int,
        row: Int,
        velocity: Float = -90f,
        entities: Collection<PixieObject>? = null
    ): Boolean = triggerBlockBonk(col, row, velocity, entities)

    fun clear(): TilemapChunkLayer = apply {
        cells = emptyArray()
        cols = 0
        rows = 0
        activeBumpingTiles.clear()
    }

    override fun update(dt: Float, context: GameContext) {
        if (!isEnabled || activeBumpingTiles.isEmpty()) return
        val iterator = activeBumpingTiles.iterator()
        while (iterator.hasNext()) {
            val tile = iterator.next()
            tile.updateBump(dt, bumpGravity)
            if (tile.bumpOffsetY == 0f && tile.bumpVelocityY == 0f) {
                iterator.remove()
            }
        }
    }

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        if (!isEnabled || cols == 0 || rows == 0) return

        val view = camera.viewportRect
        if (view.right < 0f || view.left >= widthPx || view.bottom < 0f || view.top >= heightPx) {
            return
        }

        val minCol = (view.left / tileSize).toInt().coerceAtLeast(0)
        val maxCol = (view.right / tileSize).toInt().coerceAtMost(cols - 1)
        val minRow = (view.top / tileSize).toInt().coerceAtLeast(0)
        val maxRow = (view.bottom / tileSize).toInt().coerceAtMost(rows - 1)

        if (minCol > maxCol || minRow > maxRow) return

        for (row in minRow..maxRow) {
            val rowOffset = row * cols
            val worldY = row * tileSize
            for (col in minCol..maxCol) {
                val tile = cells[rowOffset + col] ?: continue
                if (tile.type == TileType.EMPTY) continue

                val worldX = col * tileSize
                val screenX = worldX - camera.position.x
                val screenY = worldY - camera.position.y + tile.bumpOffsetY

                renderTile(batch, tile.type, screenX, screenY, tileSize)
            }
        }
    }

    private fun renderTile(batch: RenderBatchPort, type: TileType, x: Float, y: Float, size: Float) {
        when (type) {
            TileType.GROUND -> renderGround(batch, x, y, size)
            TileType.QUESTION_BLOCK -> renderQuestionBlock(batch, x, y, size)
            TileType.BRICK -> renderBrick(batch, x, y, size)
            TileType.USED_BLOCK -> renderUsedBlock(batch, x, y, size)
            TileType.PIPE_TOP_LEFT, TileType.PIPE_TOP_RIGHT,
            TileType.PIPE_BODY_LEFT, TileType.PIPE_BODY_RIGHT -> renderPipe(batch, type, x, y, size)
            TileType.ONE_WAY_LEDGE -> renderOneWayLedge(batch, x, y, size)
            TileType.SPIKE_HAZARD -> renderSpikeHazard(batch, x, y, size)
            TileType.FLAG_POLE, TileType.FLAG_TOP -> renderFlagPole(batch, type, x, y, size)
            TileType.CASTLE_BRICK -> renderCastleBrick(batch, x, y, size)
            TileType.EMPTY -> Unit
        }
    }

    private fun renderGround(batch: RenderBatchPort, x: Float, y: Float, size: Float) {
        // Base terra-cotta earth
        batch.drawPixelRect(x, y, size, size, 0xFFC84C0C.toInt())
        // Top surface grass/soil highlight
        batch.drawPixelRect(x, y, size, 2f, 0xFFFFA044.toInt())
        // Beveled shadow on right and bottom
        batch.drawPixelRect(x, y + size - 1f, size, 1f, 0xFF682400.toInt())
        batch.drawPixelRect(x + size - 1f, y, 1f, size, 0xFF682400.toInt())
        // Flecks of stone texture
        batch.drawPixelRect(x + 4f, y + 6f, 2f, 2f, 0xFF882000.toInt())
        batch.drawPixelRect(x + 10f, y + 11f, 2f, 2f, 0xFF882000.toInt())
    }

    private fun renderQuestionBlock(batch: RenderBatchPort, x: Float, y: Float, size: Float) {
        // Golden body
        batch.drawPixelRect(x, y, size, size, 0xFFF8A800.toInt())
        // Beveled highlight and shadow
        batch.drawPixelRect(x, y, size, 1f, 0xFFFFF070.toInt())
        batch.drawPixelRect(x, y, 1f, size, 0xFFFFF070.toInt())
        batch.drawPixelRect(x, y + size - 1f, size, 1f, 0xFF884800.toInt())
        batch.drawPixelRect(x + size - 1f, y, 1f, size, 0xFF884800.toInt())
        // Corner rivets
        batch.drawPixelRect(x + 1f, y + 1f, 1f, 1f, 0xFF884800.toInt())
        batch.drawPixelRect(x + size - 2f, y + 1f, 1f, 1f, 0xFF884800.toInt())
        batch.drawPixelRect(x + 1f, y + size - 2f, 1f, 1f, 0xFF884800.toInt())
        batch.drawPixelRect(x + size - 2f, y + size - 2f, 1f, 1f, 0xFF884800.toInt())
        // Centered Question Mark '?'
        val cx = x + (size * 0.5f)
        val cy = y + (size * 0.5f)
        batch.drawPixelRect(cx - 2f, cy - 4f, 4f, 1f, 0xFFFFFFFF.toInt())
        batch.drawPixelRect(cx + 1f, cy - 3f, 1f, 2f, 0xFFFFFFFF.toInt())
        batch.drawPixelRect(cx - 1f, cy - 1f, 2f, 1f, 0xFFFFFFFF.toInt())
        batch.drawPixelRect(cx - 0.5f, cy, 1f, 1f, 0xFFFFFFFF.toInt())
        batch.drawPixelRect(cx - 0.5f, cy + 2f, 1f, 1f, 0xFFFFFFFF.toInt())
    }

    private fun renderBrick(batch: RenderBatchPort, x: Float, y: Float, size: Float) {
        // Terracotta brick
        batch.drawPixelRect(x, y, size, size, 0xFFB84418.toInt())
        // Beveled edges
        batch.drawPixelRect(x, y, size, 1f, 0xFFFF8C60.toInt())
        batch.drawPixelRect(x, y, 1f, size, 0xFFFF8C60.toInt())
        batch.drawPixelRect(x, y + size - 1f, size, 1f, 0xFF581C08.toInt())
        batch.drawPixelRect(x + size - 1f, y, 1f, size, 0xFF581C08.toInt())
        // Mortar lines: horizontal midline
        val midY = y + (size * 0.5f).toInt()
        batch.drawPixelRect(x, midY, size, 1f, 0xFF581C08.toInt())
        // Vertical mortar joints
        batch.drawPixelRect(x + (size * 0.5f).toInt(), y, 1f, size * 0.5f, 0xFF581C08.toInt())
        batch.drawPixelRect(x + (size * 0.25f).toInt(), midY, 1f, size * 0.5f, 0xFF581C08.toInt())
        batch.drawPixelRect(x + (size * 0.75f).toInt(), midY, 1f, size * 0.5f, 0xFF581C08.toInt())
    }

    private fun renderUsedBlock(batch: RenderBatchPort, x: Float, y: Float, size: Float) {
        // Inert spent block brown/slate
        batch.drawPixelRect(x, y, size, size, 0xFF8C5838.toInt())
        batch.drawPixelRect(x, y, size, 1f, 0xFFA87850.toInt())
        batch.drawPixelRect(x, y, 1f, size, 0xFFA87850.toInt())
        batch.drawPixelRect(x, y + size - 1f, size, 1f, 0xFF482810.toInt())
        batch.drawPixelRect(x + size - 1f, y, 1f, size, 0xFF482810.toInt())
        // 4 dark bolt rivets
        batch.drawPixelRect(x + 2f, y + 2f, 1f, 1f, 0xFF281408.toInt())
        batch.drawPixelRect(x + size - 3f, y + 2f, 1f, 1f, 0xFF281408.toInt())
        batch.drawPixelRect(x + 2f, y + size - 3f, 1f, 1f, 0xFF281408.toInt())
        batch.drawPixelRect(x + size - 3f, y + size - 3f, 1f, 1f, 0xFF281408.toInt())
    }

    private fun renderPipe(batch: RenderBatchPort, type: TileType, x: Float, y: Float, size: Float) {
        // Classic NES tube green
        batch.drawPixelRect(x, y, size, size, 0xFF00A800.toInt())
        // Bright vertical highlight stripe
        batch.drawPixelRect(x + 3f, y, 2f, size, 0xFF58D858.toInt())
        // Dark green side shadow
        batch.drawPixelRect(x + size - 2f, y, 2f, size, 0xFF005800.toInt())

        // Top collar rim for entrance tops
        if (type == TileType.PIPE_TOP_LEFT || type == TileType.PIPE_TOP_RIGHT) {
            batch.drawPixelRect(x, y, size, 3f, 0xFF005800.toInt())
            batch.drawPixelRect(x, y + 1f, size, 1f, 0xFF58D858.toInt())
        }
    }

    private fun renderOneWayLedge(batch: RenderBatchPort, x: Float, y: Float, size: Float) {
        // Semi-solid wooden platform / cloud ledge
        val ledgeHeight = 4f
        batch.drawPixelRect(x, y, size, ledgeHeight, 0xFFD29858.toInt())
        batch.drawPixelRect(x, y, size, 1f, 0xFFFFE4B8.toInt())
        batch.drawPixelRect(x, y + ledgeHeight - 1f, size, 1f, 0xFF704018.toInt())
    }

    private fun renderSpikeHazard(batch: RenderBatchPort, x: Float, y: Float, size: Float) {
        // Dual metallic spikes
        val half = size * 0.5f
        batch.drawPixelRect(x + 1f, y + 6f, half - 2f, size - 6f, 0xFFC0C0C0.toInt())
        batch.drawPixelRect(x + half + 1f, y + 6f, half - 2f, size - 6f, 0xFFC0C0C0.toInt())
        // Sharp crimson tips
        batch.drawPixelRect(x + half * 0.5f - 1f, y + 2f, 2f, 4f, 0xFFE53935.toInt())
        batch.drawPixelRect(x + half + half * 0.5f - 1f, y + 2f, 2f, 4f, 0xFFE53935.toInt())
    }

    private fun renderFlagPole(batch: RenderBatchPort, type: TileType, x: Float, y: Float, size: Float) {
        val midX = x + (size * 0.5f) - 1f
        // Metallic pole
        batch.drawPixelRect(midX, y, 2f, size, 0xFF58D858.toInt())
        batch.drawPixelRect(midX, y, 1f, size, 0xFF88FF88.toInt())
        if (type == TileType.FLAG_TOP) {
            // Golden finial ball at top
            batch.drawPixelRect(midX - 2f, y, 6f, 6f, 0xFFFFD700.toInt())
            batch.drawPixelRect(midX - 1f, y + 1f, 2f, 2f, 0xFFFFFF80.toInt())
        }
    }

    private fun renderCastleBrick(batch: RenderBatchPort, x: Float, y: Float, size: Float) {
        // Dark fortress castle slate
        batch.drawPixelRect(x, y, size, size, 0xFF505060.toInt())
        batch.drawPixelRect(x, y, size, 1f, 0xFF787888.toInt())
        batch.drawPixelRect(x, y + size - 1f, size, 1f, 0xFF282830.toInt())
        batch.drawPixelRect(x + size - 1f, y, 1f, size, 0xFF282830.toInt())
        val midY = y + (size * 0.5f).toInt()
        batch.drawPixelRect(x, midY, size, 1f, 0xFF282830.toInt())
    }
}
