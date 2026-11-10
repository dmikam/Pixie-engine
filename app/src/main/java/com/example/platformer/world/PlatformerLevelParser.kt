package com.example.platformer.world

import com.example.platformer.entities.GoombaEnemy
import com.example.platformer.entities.KoopaEnemy
import com.example.platformer.entities.crate.ThrowableCrate
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.platformer.BlockContent
import com.pixie.engine.core.platformer.InteractiveBlockController
import com.pixie.engine.core.tilemap.TileType
import com.pixie.engine.core.tilemap.TilemapChunkLayer

/**
 * Entity spawn type categorization for stage authoring.
 */
enum class SpawnType {
    GOOMBA,
    KOOPA,
    CRATE,
    PLAYER
}

/**
 * Immutable metadata record for an entity spawn position.
 */
data class EntitySpawnPoint(
    val col: Int,
    val row: Int,
    val type: SpawnType,
    val position: Vec2
)

/**
 * Flagpole geometric bounds, touch coordinates, and score bonus calculator.
 */
data class FlagpoleInfo(
    val col: Int,
    val topRow: Int,
    val bottomRow: Int,
    val topBallY: Float,
    val baseGroundY: Float,
    val poleBounds: RectF
) {
    /**
     * Calculates NES authentic score bonus awarded based on flagpole contact height.
     * Higher contacts yield larger scores (up to 5000 pts).
     */
    fun calculateScore(touchY: Float): Long {
        val totalHeight = baseGroundY - topBallY
        if (totalHeight <= 0f) return 100L
        val normalizedClimb = ((baseGroundY - touchY) / totalHeight).coerceIn(0f, 1f)
        return when {
            normalizedClimb >= 0.85f -> 5000L
            normalizedClimb >= 0.65f -> 2000L
            normalizedClimb >= 0.45f -> 1000L
            normalizedClimb >= 0.25f -> 400L
            else -> 100L
        }
    }
}

/**
 * End-of-stage castle fortress bounds and entrance doorway coordinates.
 */
data class CastleInfo(
    val minCol: Int,
    val maxCol: Int,
    val minRow: Int,
    val maxRow: Int,
    val bounds: RectF,
    val entrancePosition: Vec2
)

/**
 * Bundle of spawned interactive stage entities.
 */
data class StageEntityBundle(
    val goombas: List<GoombaEnemy>,
    val koopas: List<KoopaEnemy>,
    val crates: List<ThrowableCrate>
) {
    val allEntities: List<PixieObject> by lazy {
        goombas + koopas + crates
    }
}

/**
 * Fully parsed static stage model containing populated tile layers,
 * interactive block controllers, entities, and stage objective metadata.
 */
data class ParsedPlatformerStage(
    val cols: Int,
    val rows: Int,
    val tileSize: Float,
    val tilemap: TilemapChunkLayer,
    val blockController: InteractiveBlockController,
    val goombas: List<GoombaEnemy>,
    val koopas: List<KoopaEnemy>,
    val crates: List<ThrowableCrate>,
    val playerSpawnPosition: Vec2,
    val flagpole: FlagpoleInfo?,
    val castle: CastleInfo?,
    val spawnPoints: List<EntitySpawnPoint> = emptyList(),
    val blockContents: Map<Pair<Int, Int>, BlockContent> = emptyMap()
) {
    val stageWidthPx: Float get() = cols * tileSize
    val stageHeightPx: Float get() = rows * tileSize

    val allEntities: List<PixieObject> get() = goombas + koopas + crates

    /**
     * Re-instantiates a fresh set of entities from original spawn points.
     * Used for stage restarts and checkpoints without re-parsing ASCII data.
     */
    fun respawnEntities(
        customTilemap: TilemapChunkLayer? = null,
        customBlockController: InteractiveBlockController? = null
    ): StageEntityBundle {
        val targetTilemap = customTilemap ?: tilemap
        val targetBlockController = customBlockController ?: blockController

        val newGoombas = mutableListOf<GoombaEnemy>()
        val newKoopas = mutableListOf<KoopaEnemy>()
        val newCrates = mutableListOf<ThrowableCrate>()

        for (spawn in spawnPoints) {
            when (spawn.type) {
                SpawnType.GOOMBA -> {
                    newGoombas.add(
                        GoombaEnemy.spawn(
                            col = spawn.col,
                            row = spawn.row,
                            tileSize = tileSize,
                            direction = -1f,
                            tilemap = targetTilemap
                        )
                    )
                }
                SpawnType.KOOPA -> {
                    newKoopas.add(
                        KoopaEnemy.spawn(
                            col = spawn.col,
                            row = spawn.row,
                            tileSize = tileSize,
                            direction = -1f,
                            tilemap = targetTilemap,
                            blockController = targetBlockController
                        )
                    )
                }
                SpawnType.CRATE -> {
                    newCrates.add(
                        ThrowableCrate.spawn(
                            col = spawn.col,
                            row = spawn.row,
                            tileSize = tileSize,
                            tilemap = targetTilemap
                        )
                    )
                }
                SpawnType.PLAYER -> Unit
            }
        }

        return StageEntityBundle(newGoombas, newKoopas, newCrates)
    }
}

/**
 * High-performance parser that translates multi-line ASCII strings into a fully
 * initialized [ParsedPlatformerStage] conforming to AGENTS.md guidelines.
 *
 * Supported character tokens:
 * - `'#'`: Solid Ground / Stone ([TileType.GROUND])
 * - `'?'`: Question Block holding Coin ([TileType.QUESTION_BLOCK], [BlockContent.Coin])
 * - `'M'`: Question Block holding Super Mushroom ([TileType.QUESTION_BLOCK], [BlockContent.SuperMushroom])
 * - `'B'`: Breakable Terracotta Brick ([TileType.BRICK])
 * - `'['`, `']'`: Pipe Entrance Top Left / Right
 * - `'|'`, `'/'`: Pipe Vertical Body Left / Right
 * - `'C'`: Spawn [ThrowableCrate] (ground obstacle)
 * - `'G'`: Spawn [GoombaEnemy] walker
 * - `'K'`: Spawn [KoopaEnemy] turtle
 * - `'F'`: Flagpole vertical segment ([TileType.FLAG_POLE])
 * - `'^'`: Flagpole top finial ball ([TileType.FLAG_TOP])
 * - `'*'`: Castle fortress brick ([TileType.CASTLE_BRICK])
 * - `'P'`, `'S'`: Player Start position
 * - `'-'`, `'_'`: Semi-solid one-way platform ([TileType.ONE_WAY_LEDGE])
 * - `'U'`: Inert spent block ([TileType.USED_BLOCK])
 * - `'.'`: Empty air
 */
class PlatformerLevelParser(
    var tileSize: Float = DEFAULT_TILE_SIZE,
    var defaultPlayerSpawn: Vec2 = DEFAULT_PLAYER_SPAWN,
    var goombaPatrolSpeed: Float = DEFAULT_GOOMBA_SPEED,
    var koopaPatrolSpeed: Float = DEFAULT_KOOPA_SPEED,
    var koopaShellSpeed: Float = DEFAULT_KOOPA_SHELL_SPEED
) {
    fun withTileSize(size: Float): PlatformerLevelParser = apply {
        this.tileSize = size
    }

    fun withDefaultPlayerSpawn(pos: Vec2): PlatformerLevelParser = apply {
        this.defaultPlayerSpawn = pos
    }

    fun withGoombaPatrolSpeed(speed: Float): PlatformerLevelParser = apply {
        this.goombaPatrolSpeed = speed
    }

    fun withKoopaPatrolSpeed(speed: Float): PlatformerLevelParser = apply {
        this.koopaPatrolSpeed = speed
    }

    fun withKoopaShellSpeed(speed: Float): PlatformerLevelParser = apply {
        this.koopaShellSpeed = speed
    }

    fun parse(ascii: String): ParsedPlatformerStage {
        val lines = ascii.trimIndent().lines().filter { it.isNotEmpty() }
        return parse(lines)
    }

    fun parse(lines: List<String>): ParsedPlatformerStage {
        val rows = lines.size
        val cols = lines.maxOfOrNull { it.length } ?: 0

        val tilemap = TilemapChunkLayer(tileSize = tileSize)
        val blockController = InteractiveBlockController(tilemap)
        val goombas = mutableListOf<GoombaEnemy>()
        val koopas = mutableListOf<KoopaEnemy>()
        val crates = mutableListOf<ThrowableCrate>()
        val spawnPoints = mutableListOf<EntitySpawnPoint>()
        val blockContents = mutableMapOf<Pair<Int, Int>, BlockContent>()

        var parsedPlayerSpawn: Vec2? = null
        val flagpoleRows = mutableListOf<Int>()
        var flagpoleCol: Int? = null
        val castleCols = mutableListOf<Int>()
        val castleRows = mutableListOf<Int>()

        // Initialize empty tilemap grid dimensions
        tilemap.loadAscii(List(rows) { ".".repeat(cols) })

        for (row in 0 until rows) {
            val line = lines.getOrNull(row) ?: ""
            for (col in 0 until cols) {
                val char = line.getOrNull(col) ?: '.'
                processTileCell(
                    char = char,
                    col = col,
                    row = row,
                    tilemap = tilemap,
                    blockController = blockController,
                    blockContents = blockContents,
                    goombas = goombas,
                    koopas = koopas,
                    crates = crates,
                    spawnPoints = spawnPoints,
                    onPlayerSpawn = { pos -> parsedPlayerSpawn = pos },
                    onFlagpoleTile = { fCol, fRow ->
                        flagpoleCol = fCol
                        flagpoleRows.add(fRow)
                    },
                    onCastleTile = { cCol, cRow ->
                        castleCols.add(cCol)
                        castleRows.add(cRow)
                    }
                )
            }
        }

        val flagpole = buildFlagpoleInfo(flagpoleCol, flagpoleRows)
        val castle = buildCastleInfo(castleCols, castleRows)
        val playerSpawn = parsedPlayerSpawn ?: defaultPlayerSpawn

        return ParsedPlatformerStage(
            cols = cols,
            rows = rows,
            tileSize = tileSize,
            tilemap = tilemap,
            blockController = blockController,
            goombas = goombas,
            koopas = koopas,
            crates = crates,
            playerSpawnPosition = playerSpawn,
            flagpole = flagpole,
            castle = castle,
            spawnPoints = spawnPoints,
            blockContents = blockContents
        )
    }

    private fun processTileCell(
        char: Char,
        col: Int,
        row: Int,
        tilemap: TilemapChunkLayer,
        blockController: InteractiveBlockController,
        blockContents: MutableMap<Pair<Int, Int>, BlockContent>,
        goombas: MutableList<GoombaEnemy>,
        koopas: MutableList<KoopaEnemy>,
        crates: MutableList<ThrowableCrate>,
        spawnPoints: MutableList<EntitySpawnPoint>,
        onPlayerSpawn: (Vec2) -> Unit,
        onFlagpoleTile: (Int, Int) -> Unit,
        onCastleTile: (Int, Int) -> Unit
    ) {
        when (char) {
            '#' -> tilemap.setTile(col, row, TileType.GROUND)
            '?' -> {
                tilemap.setTile(col, row, TileType.QUESTION_BLOCK)
                blockController.withCoinBlock(col, row)
                blockContents[col to row] = BlockContent.Coin
            }
            'M' -> {
                tilemap.setTile(col, row, TileType.QUESTION_BLOCK)
                blockController.withMushroomBlock(col, row)
                blockContents[col to row] = BlockContent.SuperMushroom
            }
            'B', 'b' -> tilemap.setTile(col, row, TileType.BRICK)
            '[' -> tilemap.setTile(col, row, TileType.PIPE_TOP_LEFT)
            ']' -> tilemap.setTile(col, row, TileType.PIPE_TOP_RIGHT)
            '|' -> tilemap.setTile(col, row, TileType.PIPE_BODY_LEFT)
            '/' -> tilemap.setTile(col, row, TileType.PIPE_BODY_RIGHT)
            '-', '_' -> tilemap.setTile(col, row, TileType.ONE_WAY_LEDGE)
            'U', 'u' -> tilemap.setTile(col, row, TileType.USED_BLOCK)
            'F', 'f' -> {
                tilemap.setTile(col, row, TileType.FLAG_POLE)
                onFlagpoleTile(col, row)
            }
            '^' -> {
                tilemap.setTile(col, row, TileType.FLAG_TOP)
                onFlagpoleTile(col, row)
            }
            '*' -> {
                tilemap.setTile(col, row, TileType.CASTLE_BRICK)
                onCastleTile(col, row)
            }
            'G', 'g' -> spawnGoomba(col, row, tilemap, goombas, spawnPoints)
            'K', 'k' -> spawnKoopa(col, row, tilemap, blockController, koopas, spawnPoints)
            'C', 'c' -> spawnCrate(col, row, tilemap, crates, spawnPoints)
            'P', 'p', 'S', 's' -> {
                val pos = Vec2(col * tileSize, row * tileSize)
                spawnPoints.add(EntitySpawnPoint(col, row, SpawnType.PLAYER, pos))
                onPlayerSpawn(pos)
            }
            else -> Unit
        }
    }

    private fun spawnGoomba(
        col: Int,
        row: Int,
        tilemap: TilemapChunkLayer,
        goombas: MutableList<GoombaEnemy>,
        spawnPoints: MutableList<EntitySpawnPoint>
    ) {
        val pos = Vec2(col * tileSize, row * tileSize)
        val enemy = GoombaEnemy.spawn(
            col = col,
            row = row,
            tileSize = tileSize,
            direction = -1f,
            tilemap = tilemap
        ).withSpeed(goombaPatrolSpeed)
        goombas.add(enemy)
        spawnPoints.add(EntitySpawnPoint(col, row, SpawnType.GOOMBA, pos))
    }

    private fun spawnKoopa(
        col: Int,
        row: Int,
        tilemap: TilemapChunkLayer,
        blockController: InteractiveBlockController,
        koopas: MutableList<KoopaEnemy>,
        spawnPoints: MutableList<EntitySpawnPoint>
    ) {
        val enemy = KoopaEnemy.spawn(
            col = col,
            row = row,
            tileSize = tileSize,
            direction = -1f,
            tilemap = tilemap,
            blockController = blockController
        ).withPatrolSpeedRate(koopaPatrolSpeed).withShellSpeedRate(koopaShellSpeed)
        koopas.add(enemy)
        spawnPoints.add(EntitySpawnPoint(col, row, SpawnType.KOOPA, enemy.position))
    }

    private fun spawnCrate(
        col: Int,
        row: Int,
        tilemap: TilemapChunkLayer,
        crates: MutableList<ThrowableCrate>,
        spawnPoints: MutableList<EntitySpawnPoint>
    ) {
        val pos = Vec2(col * tileSize, row * tileSize)
        val crate = ThrowableCrate.spawn(
            col = col,
            row = row,
            tileSize = tileSize,
            tilemap = tilemap
        )
        crates.add(crate)
        spawnPoints.add(EntitySpawnPoint(col, row, SpawnType.CRATE, pos))
    }

    private fun buildFlagpoleInfo(col: Int?, rows: List<Int>): FlagpoleInfo? {
        if (col == null || rows.isEmpty()) return null
        val topRow = rows.minOrNull() ?: return null
        val bottomRow = rows.maxOrNull() ?: return null

        val topBallY = topRow * tileSize
        val baseGroundY = (bottomRow + 1) * tileSize
        val poleX = col * tileSize
        val poleBounds = RectF.fromTopLeft(
            x = poleX + 4f,
            y = topBallY,
            width = tileSize - 8f,
            height = baseGroundY - topBallY
        )

        return FlagpoleInfo(
            col = col,
            topRow = topRow,
            bottomRow = bottomRow,
            topBallY = topBallY,
            baseGroundY = baseGroundY,
            poleBounds = poleBounds
        )
    }

    private fun buildCastleInfo(cols: List<Int>, rows: List<Int>): CastleInfo? {
        if (cols.isEmpty() || rows.isEmpty()) return null
        val minCol = cols.minOrNull() ?: return null
        val maxCol = cols.maxOrNull() ?: return null
        val minRow = rows.minOrNull() ?: return null
        val maxRow = rows.maxOrNull() ?: return null

        val left = minCol * tileSize
        val top = minRow * tileSize
        val width = (maxCol - minCol + 1) * tileSize
        val height = (maxRow - minRow + 1) * tileSize
        val bounds = RectF.fromTopLeft(left, top, width, height)

        val entrancePos = Vec2(bounds.centerX - 8f, bounds.bottom - tileSize)

        return CastleInfo(
            minCol = minCol,
            maxCol = maxCol,
            minRow = minRow,
            maxRow = maxRow,
            bounds = bounds,
            entrancePosition = entrancePos
        )
    }

    companion object {
        const val DEFAULT_TILE_SIZE = 16f
        val DEFAULT_PLAYER_SPAWN = Vec2(32f, 192f)
        const val DEFAULT_GOOMBA_SPEED = 35f
        const val DEFAULT_KOOPA_SPEED = 30f
        const val DEFAULT_KOOPA_SHELL_SPEED = 180f

        fun parse(ascii: String, tileSize: Float = DEFAULT_TILE_SIZE): ParsedPlatformerStage {
            return PlatformerLevelParser(tileSize).parse(ascii)
        }

        fun parse(lines: List<String>, tileSize: Float = DEFAULT_TILE_SIZE): ParsedPlatformerStage {
            return PlatformerLevelParser(tileSize).parse(lines)
        }
    }
}
