package com.pixie.engine.core.platformer

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.layer.PixieLayer
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.ports.RenderBatchPort
import com.pixie.engine.core.tilemap.TileData
import com.pixie.engine.core.tilemap.TileType
import com.pixie.engine.core.tilemap.TilemapChunkLayer

/**
 * Item or reward concealed inside an interactive platformer block.
 */
sealed class BlockContent {
    /** Awards 200 points, 1 coin, and plays the popping coin arc animation. */
    data object Coin : BlockContent()

    /** Spawns a Super Mushroom rising smoothly out of the top of the block. */
    data object SuperMushroom : BlockContent()

    /** Multi-coin block (e.g. 10-coin brick in SMB). */
    data class MultiCoin(val initialCoins: Int = 10, var remainingCoins: Int = 10) : BlockContent()

    /** Inert block that yields no items upon being struck. */
    data object Empty : BlockContent()
}

/**
 * Result data contract returned when an interactive block is struck from underneath.
 */
sealed class BlockHitResult {
    data object None : BlockHitResult()
    data class QuestionCoin(val col: Int, val row: Int, val points: Long = 200L, val coins: Int = 1) : BlockHitResult()
    data class QuestionMushroom(val col: Int, val row: Int) : BlockHitResult()
    data class BrickBumped(val col: Int, val row: Int) : BlockHitResult()
    data class BrickShattered(val col: Int, val row: Int) : BlockHitResult()
    data class GenericBump(val col: Int, val row: Int) : BlockHitResult()
}

/**
 * Visual popping coin emerging from a bonked block.
 *
 * Launches upward along a parabolic arc with initial velocity -180f and gravity 675f,
 * achieving an authentic 24-pixel vertical crest before descending and despawning in ~0.53s.
 */
data class PoppingCoin(
    val id: String,
    var position: Vec2,
    val startY: Float,
    var velocityY: Float = -180f,
    val gravity: Float = 675f,
    var timeElapsed: Float = 0f,
    var isFinished: Boolean = false,
    val pointsAwarded: Long = 200L
) {
    var animFrame: Int = 0

    fun update(dt: Float): Boolean {
        if (isFinished) return false
        timeElapsed += dt
        val nextVy = velocityY + gravity * dt
        position = Vec2(position.x, position.y + (velocityY + nextVy) * 0.5f * dt)
        velocityY = nextVy
        animFrame = ((timeElapsed / 0.066f).toInt()) % 4

        if (velocityY > 0f && position.y >= startY) {
            isFinished = true
            return false
        }
        return true
    }
}

/**
 * Emerging Super Mushroom rising smoothly from within a struck question block.
 */
data class RisingMushroom(
    val id: String,
    val col: Int,
    val row: Int,
    val startY: Float,
    var currentY: Float,
    val targetY: Float,
    val riseSpeed: Float = 24f,
    var isEmerged: Boolean = false
) {
    val x: Float = col * 16f

    fun update(dt: Float): Boolean {
        if (isEmerged) return false
        currentY -= riseSpeed * dt
        if (currentY <= targetY) {
            currentY = targetY
            isEmerged = true
            return false
        }
        return true
    }
}

/**
 * Controller and renderer for interactive NES-style block interactions and item emergence.
 *
 * Manages Question Mark blocks, popping coin physics/rendering, rising powerups,
 * and brick shattering mechanics.
 */
class InteractiveBlockController(
    val tilemap: TilemapChunkLayer,
    override val order: Int = 210,
    override val name: String = "Layer_InteractiveBlocks"
) : PixieLayer {

    override var isEnabled: Boolean = true
    override var isGridEnabled: Boolean = false

    private val contents = mutableMapOf<Pair<Int, Int>, BlockContent>()
    val activeCoins = mutableListOf<PoppingCoin>()
    val activeMushrooms = mutableListOf<RisingMushroom>()

    var defaultQuestionBlockContent: BlockContent = BlockContent.Coin
    var particleLayer: ParticleLayer? = null

    var coinsAwardedTotal: Int = 0
        private set
    var scoreAwardedTotal: Long = 0L
        private set
    var brickShatteredTotal: Int = 0
        private set

    // Event hooks
    var onCoinPopped: ((col: Int, row: Int, points: Long, coins: Int) -> Unit)? = null
    var onMushroomSpawned: ((col: Int, row: Int, position: Vec2) -> Unit)? = null
    var onMushroomEmerged: ((col: Int, row: Int, position: Vec2) -> Unit)? = null
    var onBrickBumped: ((col: Int, row: Int) -> Unit)? = null
    var onBrickShattered: ((col: Int, row: Int) -> Unit)? = null
    var onPlaySound: ((soundKey: String) -> Unit)? = null

    // --- Fluent Configuration Builders ---

    fun withDefaultContent(content: BlockContent): InteractiveBlockController = apply {
        this.defaultQuestionBlockContent = content
    }

    fun withContent(col: Int, row: Int, content: BlockContent): InteractiveBlockController = apply {
        contents[col to row] = content
    }

    fun withCoinBlock(col: Int, row: Int): InteractiveBlockController = withContent(col, row, BlockContent.Coin)

    fun withMushroomBlock(col: Int, row: Int): InteractiveBlockController = withContent(col, row, BlockContent.SuperMushroom)

    fun withParticleLayer(layer: ParticleLayer): InteractiveBlockController = apply {
        this.particleLayer = layer
    }

    fun onCoinPopped(listener: (col: Int, row: Int, points: Long, coins: Int) -> Unit): InteractiveBlockController = apply {
        this.onCoinPopped = listener
    }

    fun onMushroomSpawned(listener: (col: Int, row: Int, position: Vec2) -> Unit): InteractiveBlockController = apply {
        this.onMushroomSpawned = listener
    }

    fun onMushroomEmerged(listener: (col: Int, row: Int, position: Vec2) -> Unit): InteractiveBlockController = apply {
        this.onMushroomEmerged = listener
    }

    fun onBrickBumped(listener: (col: Int, row: Int) -> Unit): InteractiveBlockController = apply {
        this.onBrickBumped = listener
    }

    fun onBrickShattered(listener: (col: Int, row: Int) -> Unit): InteractiveBlockController = apply {
        this.onBrickShattered = listener
    }

    fun onPlaySound(listener: (soundKey: String) -> Unit): InteractiveBlockController = apply {
        this.onPlaySound = listener
    }

    fun attachToCollisionResolver(
        resolver: PlatformerCollisionResolver,
        playerTierProvider: () -> PlayerTier = { PlayerTier.SMALL },
        contextProvider: () -> GameContext? = { null }
    ): InteractiveBlockController = apply {
        resolver.onHeadBonk { tileX, tileY ->
            handleBlockHit(tileX, tileY, playerTierProvider(), contextProvider())
        }
    }

    fun getBlockContent(col: Int, row: Int): BlockContent {
        return contents[col to row] ?: defaultQuestionBlockContent
    }

    // --- Block Hit Processing Pipeline ---

    fun handleBlockHit(
        col: Int,
        row: Int,
        playerTier: PlayerTier = PlayerTier.SMALL,
        context: GameContext? = null,
        forceBreak: Boolean = false
    ): BlockHitResult {
        val tile = tilemap.getTile(col, row) ?: return BlockHitResult.None
        if (!tile.isBumpable && tile.type != TileType.BRICK) return BlockHitResult.None

        return when (tile.type) {
            TileType.QUESTION_BLOCK -> processQuestionBlockHit(col, row, tile, context)
            TileType.BRICK -> processBrickHit(col, row, tile, playerTier, context, forceBreak)
            else -> processGenericBump(col, row, tile)
        }
    }

    private fun processQuestionBlockHit(
        col: Int,
        row: Int,
        tile: TileData,
        context: GameContext?
    ): BlockHitResult {
        tilemap.triggerBlockBonk(col, row)
        tile.mutateTo(TileType.USED_BLOCK)

        val content = contents.remove(col to row) ?: defaultQuestionBlockContent
        return when (content) {
            is BlockContent.Coin -> {
                spawnPoppingCoin(col, row, context)
                BlockHitResult.QuestionCoin(col, row)
            }
            is BlockContent.SuperMushroom -> {
                spawnRisingMushroom(col, row)
                BlockHitResult.QuestionMushroom(col, row)
            }
            is BlockContent.MultiCoin -> {
                spawnPoppingCoin(col, row, context)
                content.remainingCoins--
                if (content.remainingCoins > 0) {
                    contents[col to row] = content
                    tile.mutateTo(TileType.QUESTION_BLOCK)
                }
                BlockHitResult.QuestionCoin(col, row)
            }
            is BlockContent.Empty -> {
                onPlaySound?.invoke("bump")
                BlockHitResult.GenericBump(col, row)
            }
        }
    }

    private fun processBrickHit(
        col: Int,
        row: Int,
        tile: TileData,
        playerTier: PlayerTier,
        context: GameContext?,
        forceBreak: Boolean = false
    ): BlockHitResult {
        return if (forceBreak || playerTier.canBreakBricks) {
            shatterBrick(col, row, context)
            BlockHitResult.BrickShattered(col, row)
        } else {
            tilemap.triggerBlockBonk(col, row)
            onBrickBumped?.invoke(col, row)
            onPlaySound?.invoke("bump")
            BlockHitResult.BrickBumped(col, row)
        }
    }

    /**
     * Shatters a breakable brick tile into 4 spinning debris shards,
     * clears the tile to EMPTY, spawns collision sparks, awards 50 points,
     * and triggers event hooks and audio.
     *
     * @return true if the tile at [col], [row] was a breakable brick and shattered, false otherwise.
     */
    fun shatterBrick(
        col: Int,
        row: Int,
        context: GameContext? = null
    ): Boolean {
        val tile = tilemap.getTile(col, row) ?: return false
        if (tile.type != TileType.BRICK) return false

        tilemap.setTile(col, row, TileType.EMPTY)
        spawnBrickShards(col, row)
        particleLayer?.spawnExplosion(
            ExplosionType.SPARK_HIT,
            Vec2(col * tilemap.tileSize + 8f, row * tilemap.tileSize + 8f)
        )

        brickShatteredTotal += 1
        scoreAwardedTotal += 50L
        context?.let { ctx ->
            ctx.score += 50L
        }

        onBrickShattered?.invoke(col, row)
        onPlaySound?.invoke("break_block")
        return true
    }

    /**
     * Spawns 4 spinning brick shards into [particleLayer] launching diagonally outward with gravity.
     */
    fun spawnBrickShards(
        col: Int,
        row: Int,
        gravity: Float = 600f,
        color: Int = 0xFFB84418.toInt()
    ) {
        val layer = particleLayer ?: return
        val size = tilemap.tileSize
        val center = Vec2(col * size + size * 0.5f, row * size + size * 0.5f)
        layer.spawnBrickShards(center, gravity, color)
    }

    private fun processGenericBump(col: Int, row: Int, tile: TileData): BlockHitResult {
        tilemap.triggerBlockBonk(col, row)
        onPlaySound?.invoke("bump")
        return BlockHitResult.GenericBump(col, row)
    }

    // --- Entity Spawning Helpers ---

    private fun spawnPoppingCoin(col: Int, row: Int, context: GameContext?) {
        val size = tilemap.tileSize
        val startX = col * size + (size - 8f) * 0.5f
        val startY = row * size - 2f

        val coin = PoppingCoin(
            id = "coin_${col}_${row}_${System.nanoTime()}",
            position = Vec2(startX, startY),
            startY = startY
        )
        activeCoins.add(coin)

        coinsAwardedTotal += 1
        scoreAwardedTotal += 200L

        context?.let { ctx ->
            ctx.score += 200L
            val currentCoins = ctx.getAttribute("coins", 0)
            ctx.setAttribute("coins", currentCoins + 1)
        }

        onCoinPopped?.invoke(col, row, 200L, 1)
        onPlaySound?.invoke("coin")
    }

    private fun spawnRisingMushroom(col: Int, row: Int) {
        val size = tilemap.tileSize
        val startY = row * size
        val targetY = (row - 1) * size

        val mushroom = RisingMushroom(
            id = "mushroom_${col}_${row}_${System.nanoTime()}",
            col = col,
            row = row,
            startY = startY,
            currentY = startY,
            targetY = targetY
        )
        activeMushrooms.add(mushroom)

        val spawnPos = Vec2(col * size, targetY)
        onMushroomSpawned?.invoke(col, row, spawnPos)
        onPlaySound?.invoke("powerup_appears")
    }

    /**
     * Spawns a physical crawling [SuperMushroom] entity emerging from the given tile position.
     */
    fun spawnMushroomEntity(col: Int, row: Int, direction: Float = 1f): SuperMushroom {
        return SuperMushroom.spawnEmerging(col, row, tilemap.tileSize, direction, tilemap = tilemap)
    }

    // --- PixieLayer Lifecycle & Rendering ---

    override fun update(dt: Float, context: GameContext) {
        if (!isEnabled) return
        updateCoins(dt)
        updateMushrooms(dt)
    }

    private fun updateCoins(dt: Float) {
        val coinIter = activeCoins.iterator()
        while (coinIter.hasNext()) {
            val coin = coinIter.next()
            if (!coin.update(dt)) {
                coinIter.remove()
            }
        }
    }

    private fun updateMushrooms(dt: Float) {
        val mushIter = activeMushrooms.iterator()
        while (mushIter.hasNext()) {
            val mush = mushIter.next()
            if (!mush.update(dt)) {
                onMushroomEmerged?.invoke(mush.col, mush.row, Vec2(mush.x, mush.targetY))
                mushIter.remove()
            }
        }
    }

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        if (!isEnabled) return
        renderMushrooms(batch, camera)
        renderCoins(batch, camera)
    }

    private fun renderCoins(batch: RenderBatchPort, camera: PixieCamera) {
        if (activeCoins.isEmpty()) return
        for (i in 0 until activeCoins.size) {
            val coin = activeCoins[i]
            val screenX = coin.position.x - camera.position.x
            val screenY = coin.position.y - camera.position.y
            renderCoinSprite(batch, screenX, screenY, coin.animFrame)
        }
    }

    private fun renderCoinSprite(batch: RenderBatchPort, x: Float, y: Float, frame: Int) {
        val gold = 0xFFFFD700.toInt()
        val shine = 0xFFFFF080.toInt()
        val border = 0xFFB8860B.toInt()

        when (frame) {
            0 -> { // Full width face (8x14)
                batch.drawPixelRect(x, y + 1f, 8f, 12f, gold)
                batch.drawPixelRect(x + 1f, y, 6f, 14f, gold)
                batch.drawPixelRect(x + 2f, y + 2f, 2f, 10f, shine)
                batch.drawPixelRect(x + 5f, y + 2f, 1f, 10f, border)
            }
            1, 3 -> { // Angled face (5x14)
                val ox = x + 1.5f
                batch.drawPixelRect(ox, y + 1f, 5f, 12f, gold)
                batch.drawPixelRect(ox + 1f, y, 3f, 14f, gold)
                batch.drawPixelRect(ox + 1f, y + 2f, 1f, 10f, shine)
                batch.drawPixelRect(ox + 3f, y + 2f, 1f, 10f, border)
            }
            2 -> { // Thin edge-on (2x14)
                val ox = x + 3f
                batch.drawPixelRect(ox, y, 2f, 14f, gold)
                batch.drawPixelRect(ox, y + 2f, 1f, 10f, shine)
            }
        }
    }

    private fun renderMushrooms(batch: RenderBatchPort, camera: PixieCamera) {
        if (activeMushrooms.isEmpty()) return
        for (i in 0 until activeMushrooms.size) {
            val mush = activeMushrooms[i]
            val screenX = mush.x - camera.position.x
            val screenY = mush.currentY - camera.position.y
            renderMushroomSprite(batch, screenX, screenY)
        }
    }

    private fun renderMushroomSprite(batch: RenderBatchPort, x: Float, y: Float) {
        val red = 0xFFE52521.toInt()
        val white = 0xFFFFFFFF.toInt()
        val beige = 0xFFFFECC3.toInt()
        val black = 0xFF000000.toInt()

        // Red Mushroom Cap (16x10)
        batch.drawPixelRect(x + 2f, y, 12f, 10f, red)
        batch.drawPixelRect(x, y + 4f, 16f, 6f, red)
        // White Spots
        batch.drawPixelRect(x + 6f, y + 1f, 4f, 4f, white)
        batch.drawPixelRect(x + 1f, y + 5f, 3f, 4f, white)
        batch.drawPixelRect(x + 12f, y + 5f, 3f, 4f, white)
        // Beige Stalk (10x6)
        batch.drawPixelRect(x + 3f, y + 10f, 10f, 6f, beige)
        // Black eyes
        batch.drawPixelRect(x + 4f, y + 11f, 1f, 3f, black)
        batch.drawPixelRect(x + 11f, y + 11f, 1f, 3f, black)
    }

    fun clear(): InteractiveBlockController = apply {
        contents.clear()
        activeCoins.clear()
        activeMushrooms.clear()
        coinsAwardedTotal = 0
        scoreAwardedTotal = 0L
        brickShatteredTotal = 0
    }
}
