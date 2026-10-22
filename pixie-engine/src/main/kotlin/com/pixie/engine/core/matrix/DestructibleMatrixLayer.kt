package com.pixie.engine.core.matrix

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.layer.PixieLayer
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.RenderBatchPort

enum class BrickType {
    STANDARD,
    ARMORED,
    EXPLOSIVE,
    UNBREAKABLE
}

enum class PowerUpType {
    LASER,
    WIDE_PADDLE,
    MULTI_BALL,
    PLASMA_BALL,
    SLOW_BALL,
    SAFETY_BARRIER
}

data class BrickCell(
    val row: Int,
    val col: Int,
    var type: BrickType = BrickType.STANDARD,
    var hp: Int = 1,
    val maxHp: Int = 1,
    val color: Int = 0xFF00E5FF.toInt(),
    val score: Int = 100,
    val powerUp: PowerUpType? = null,
    var isDestroyed: Boolean = false
) {
    val isSolid: Boolean get() = !isDestroyed
}

data class BrickHitResult(
    val hit: Boolean,
    val cell: BrickCell? = null,
    val wasDestroyed: Boolean = false,
    val scoreAwarded: Int = 0,
    val droppedPowerUp: PowerUpType? = null,
    val chainedExplosions: List<BrickCell> = emptyList(),
    val hitPosition: Vec2 = Vec2.ZERO
)

/**
 * 2D Destructible Matrix Layer for fast O(1) grid collision lookups,
 * multi-tier brick armor, chained explosive reactions, and beveled pixel rendering.
 */
class DestructibleMatrixLayer(
    override val name: String = "Layer_DestructibleMatrix",
    override val order: Int = 450,
    val originX: Float = 20f,
    val originY: Float = 28f,
    val brickWidth: Float = 21f,
    val brickHeight: Float = 9f,
    val gap: Float = 2f
) : PixieLayer {

    override var isEnabled: Boolean = true
    override var isGridEnabled: Boolean = false

    var rows: Int = 0
        private set
    var cols: Int = 0
        private set

    private var cells: Array<BrickCell?> = emptyArray()

    fun loadLayout(numRows: Int, numCols: Int, cellList: List<BrickCell?>) {
        rows = numRows
        cols = numCols
        cells = Array(rows * cols) { idx ->
            if (idx < cellList.size) cellList[idx] else null
        }
    }

    fun getCell(row: Int, col: Int): BrickCell? {
        if (row !in 0 until rows || col !in 0 until cols) return null
        return cells[row * cols + col]
    }

    fun getCellBounds(row: Int, col: Int): RectF {
        val x = originX + col * (brickWidth + gap)
        val y = originY + row * (brickHeight + gap)
        return RectF.fromTopLeft(x, y, brickWidth, brickHeight)
    }

    fun worldToGrid(worldX: Float, worldY: Float): Pair<Int, Int>? {
        val relX = worldX - originX
        val relY = worldY - originY
        if (relX < 0f || relY < 0f) return null

        val col = (relX / (brickWidth + gap)).toInt()
        val row = (relY / (brickHeight + gap)).toInt()

        if (row !in 0 until rows || col !in 0 until cols) return null

        // Check if inside brick bounds rather than in between gaps
        val bounds = getCellBounds(row, col)
        if (bounds.contains(worldX, worldY)) {
            return Pair(row, col)
        }
        return null
    }

    fun hitCell(row: Int, col: Int, damage: Int = 1): BrickHitResult {
        val cell = getCell(row, col) ?: return BrickHitResult(hit = false)
        if (cell.isDestroyed) return BrickHitResult(hit = false)

        val bounds = getCellBounds(row, col)
        val hitCenter = Vec2(bounds.centerX, bounds.centerY)

        if (cell.type == BrickType.UNBREAKABLE) {
            return BrickHitResult(
                hit = true,
                cell = cell,
                wasDestroyed = false,
                scoreAwarded = 10,
                hitPosition = hitCenter
            )
        }

        cell.hp -= damage
        if (cell.hp <= 0) {
            cell.isDestroyed = true
            val chained = mutableListOf<BrickCell>()

            // Chained AOE if explosive
            if (cell.type == BrickType.EXPLOSIVE) {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val nr = row + dr
                        val nc = col + dc
                        val neighbor = getCell(nr, nc)
                        if (neighbor != null && !neighbor.isDestroyed && neighbor.type != BrickType.UNBREAKABLE) {
                            neighbor.hp = 0
                            neighbor.isDestroyed = true
                            chained.add(neighbor)
                        }
                    }
                }
            }

            var totalScore = cell.score
            for (c in chained) totalScore += c.score

            return BrickHitResult(
                hit = true,
                cell = cell,
                wasDestroyed = true,
                scoreAwarded = totalScore,
                droppedPowerUp = cell.powerUp,
                chainedExplosions = chained,
                hitPosition = hitCenter
            )
        }

        return BrickHitResult(
            hit = true,
            cell = cell,
            wasDestroyed = false,
            scoreAwarded = 25,
            hitPosition = hitCenter
        )
    }

    fun remainingDestructibles(): Int {
        var count = 0
        for (c in cells) {
            if (c != null && !c.isDestroyed && c.type != BrickType.UNBREAKABLE) {
                count++
            }
        }
        return count
    }

    override fun update(dt: Float, context: GameContext) {
        // Static matrix logic
    }

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        if (!isEnabled) return

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val cell = getCell(row, col) ?: continue
                if (cell.isDestroyed) continue

                val bounds = getCellBounds(row, col)
                val screenPos = camera.worldToScreen(Vec2(bounds.left, bounds.top))

                // Determine base color based on type and remaining hp
                val baseColor = when (cell.type) {
                    BrickType.STANDARD -> cell.color
                    BrickType.ARMORED -> if (cell.hp > 1) 0xFFB0BEC5.toInt() else 0xFFCFD8DC.toInt()
                    BrickType.EXPLOSIVE -> 0xFFFF3D00.toInt()
                    BrickType.UNBREAKABLE -> 0xFFFFD700.toInt()
                }

                // 1. Draw Brick Body
                batch.drawPixelRect(
                    screenPos.x,
                    screenPos.y,
                    bounds.width,
                    bounds.height,
                    baseColor
                )

                // 2. Beveled Top/Left Highlight
                batch.drawPixelRect(
                    screenPos.x,
                    screenPos.y,
                    bounds.width,
                    1f,
                    0x88FFFFFF.toInt()
                )
                batch.drawPixelRect(
                    screenPos.x,
                    screenPos.y,
                    1f,
                    bounds.height,
                    0x88FFFFFF.toInt()
                )

                // 3. Beveled Bottom/Right Shadow
                batch.drawPixelRect(
                    screenPos.x,
                    screenPos.y + bounds.height - 1f,
                    bounds.width,
                    1f,
                    0x88000000.toInt()
                )
                batch.drawPixelRect(
                    screenPos.x + bounds.width - 1f,
                    screenPos.y,
                    1f,
                    bounds.height,
                    0x88000000.toInt()
                )

                // 4. Special Archetype Details
                if (cell.type == BrickType.EXPLOSIVE) {
                    // Central warning pixel
                    batch.drawPixelRect(
                        screenPos.x + bounds.width * 0.5f - 2f,
                        screenPos.y + bounds.height * 0.5f - 1f,
                        4f,
                        2f,
                        0xFFFFFF00.toInt()
                    )
                } else if (cell.type == BrickType.ARMORED && cell.hp > 1) {
                    // Armor rivets
                    batch.drawPixelRect(screenPos.x + 2f, screenPos.y + 2f, 1f, 1f, 0xFF37474F.toInt())
                    batch.drawPixelRect(screenPos.x + bounds.width - 3f, screenPos.y + 2f, 1f, 1f, 0xFF37474F.toInt())
                } else if (cell.powerUp != null) {
                    // Small golden glyph dot in center
                    batch.drawPixelRect(
                        screenPos.x + bounds.width * 0.5f - 1f,
                        screenPos.y + bounds.height * 0.5f - 1f,
                        2f,
                        2f,
                        0xFFFFEE58.toInt()
                    )
                }
            }
        }
    }
}
