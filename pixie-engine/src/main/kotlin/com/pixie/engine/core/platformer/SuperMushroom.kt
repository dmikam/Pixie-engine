package com.pixie.engine.core.platformer

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieCollectible
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.RenderBatchPort
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.tilemap.CollisionContactSide
import com.pixie.engine.core.tilemap.TilemapChunkLayer
import com.pixie.engine.core.tilemap.TilemapCollisionQuery

/**
 * State lifecycle for an emerging or crawling Super Mushroom power-up.
 */
enum class MushroomState {
    /** Emerging vertically upward from within a struck block. */
    EMERGING,

    /** Active on the stage: crawling horizontally, bouncing off walls, and falling off ledges. */
    MOVING,

    /** Collected by player; awaiting despawn or already consumed. */
    COLLECTED
}

/**
 * Authentic NES Super Mario Bros-style Super Mushroom powerup item.
 *
 * Implements:
 * - Upward emergence out of Question Blocks (`riseSpeed = 24f`)
 * - Constant horizontal patrol movement (`speed = 45f`)
 * - Wall bounce reflection on solid tile contact
 * - Ledge falloffs with gravitational acceleration (`gravity = 500f`)
 * - Player collection: grants 1000 points, upgrades tier from `SMALL` (16x16) to `SUPER` (16x24),
 *   and triggers powerup audio fanfare.
 */
class SuperMushroom(
    id: String = "super_mushroom_${System.nanoTime()}",
    position: Vec2 = Vec2.ZERO,
    var state: MushroomState = MushroomState.MOVING,
    var direction: Float = 1f,
    var speed: Float = DEFAULT_SPEED,
    var gravity: Float = DEFAULT_GRAVITY,
    var maxFallSpeed: Float = DEFAULT_MAX_FALL_SPEED,
    var riseSpeed: Float = DEFAULT_RISE_SPEED,
    var pointsAwarded: Long = DEFAULT_POINTS,
    var tilemap: TilemapChunkLayer? = null
) : PixieCollectible(
    id = id,
    position = position,
    size = Vec2(16f, 16f),
    initialClasses = setOf("pickup", "powerup", "mushroom", "super_mushroom")
) {
    var startY: Float = position.y
    var targetY: Float = position.y - 16f
    var isGrounded: Boolean = false
        internal set

    var onEmerged: ((SuperMushroom) -> Unit)? = null
    var onDirectionChanged: ((Float) -> Unit)? = null
    var onPowerupCollected: ((collector: PixieObject) -> Unit)? = null
    var onPlaySound: ((String) -> Unit)? = null

    init {
        if (state == MushroomState.MOVING) {
            velocity = Vec2(direction * speed, 0f)
        }
    }

    // --- Fluent Builders ---

    fun withDirection(dir: Float): SuperMushroom = apply {
        this.direction = if (dir >= 0f) 1f else -1f
        if (state == MushroomState.MOVING) {
            velocity = Vec2(this.direction * speed, velocity.y)
        }
    }

    fun withSpeed(speed: Float): SuperMushroom = apply {
        this.speed = speed
        if (state == MushroomState.MOVING) {
            velocity = Vec2(direction * this.speed, velocity.y)
        }
    }

    fun withGravity(gravity: Float): SuperMushroom = apply {
        this.gravity = gravity
    }

    fun withTilemap(layer: TilemapChunkLayer): SuperMushroom = apply {
        this.tilemap = layer
    }

    fun withState(state: MushroomState): SuperMushroom = apply {
        this.state = state
        if (state == MushroomState.MOVING) {
            velocity = Vec2(direction * speed, velocity.y)
        }
    }

    fun onEmerged(listener: (SuperMushroom) -> Unit): SuperMushroom = apply {
        this.onEmerged = listener
    }

    fun onDirectionChanged(listener: (Float) -> Unit): SuperMushroom = apply {
        this.onDirectionChanged = listener
    }

    fun onPowerupCollected(listener: (PixieObject) -> Unit): SuperMushroom = apply {
        this.onPowerupCollected = listener
    }

    fun onPlaySound(listener: (String) -> Unit): SuperMushroom = apply {
        this.onPlaySound = listener
    }

    // --- Lifecycle Updates ---

    override fun update(dt: Float, context: GameContext) {
        if (!isAlive || isCollected) return

        when (state) {
            MushroomState.EMERGING -> updateEmergence(dt)
            MushroomState.MOVING -> {
                val layer = tilemap
                if (layer != null) {
                    updatePhysics(dt, layer)
                } else {
                    position += velocity * dt
                }
            }
            MushroomState.COLLECTED -> {}
        }
    }

    fun updateWithTilemap(dt: Float, layer: TilemapChunkLayer): SuperMushroom = apply {
        if (!isAlive || isCollected) return this
        when (state) {
            MushroomState.EMERGING -> updateEmergence(dt)
            MushroomState.MOVING -> updatePhysics(dt, layer)
            MushroomState.COLLECTED -> {}
        }
    }

    private fun updateEmergence(dt: Float) {
        val nextY = position.y - riseSpeed * dt
        if (nextY <= targetY) {
            position = Vec2(position.x, targetY)
            state = MushroomState.MOVING
            velocity = Vec2(direction * speed, 0f)
            onEmerged?.invoke(this)
        } else {
            position = Vec2(position.x, nextY)
        }
    }

    fun updatePhysics(dt: Float, layer: TilemapChunkLayer) {
        if (dt <= 0f) return

        resolveHorizontalMovement(dt, layer)
        checkLedgeSupport(layer)
        resolveVerticalMovement(dt, layer)
    }

    private fun resolveHorizontalMovement(dt: Float, layer: TilemapChunkLayer) {
        val currentVx = direction * speed
        val targetX = position.x + currentVx * dt
        val hBox = RectF.fromTopLeft(targetX, position.y, size.x, size.y)
        val hHits = TilemapCollisionQuery.queryHorizontal(layer, hBox, currentVx)

        if (hHits.isNotEmpty()) {
            if (currentVx > 0f) {
                val minWallLeft = hHits.minOf { it.bounds.left }
                position = Vec2(minWallLeft - size.x, position.y)
                direction = -1f
            } else {
                val maxWallRight = hHits.maxOf { it.bounds.right }
                position = Vec2(maxWallRight, position.y)
                direction = 1f
            }
            velocity = Vec2(direction * speed, velocity.y)
            onDirectionChanged?.invoke(direction)
        } else {
            position = Vec2(targetX, position.y)
        }
    }

    private fun checkLedgeSupport(layer: TilemapChunkLayer) {
        if (isGrounded) {
            isGrounded = queryGroundSupport(layer)
        }
    }

    private fun resolveVerticalMovement(dt: Float, layer: TilemapChunkLayer) {
        val newVy = if (!isGrounded) {
            (velocity.y + gravity * dt).coerceAtMost(maxFallSpeed)
        } else {
            0f
        }
        velocity = Vec2(direction * speed, newVy)

        val targetY = position.y + velocity.y * dt
        val vBox = RectF.fromTopLeft(position.x, targetY, size.x, size.y)
        val prevBottom = boundingBox.bottom

        val vHits = TilemapCollisionQuery.queryVertical(layer, vBox, velocity.y, prevBottom)
        val groundHits = vHits.filter { it.side == CollisionContactSide.GROUND }

        if (groundHits.isNotEmpty() && velocity.y >= 0f) {
            val highestGroundTop = groundHits.minOf { it.bounds.top }
            position = Vec2(position.x, highestGroundTop - size.y)
            velocity = Vec2(direction * speed, 0f)
            isGrounded = true
        } else {
            position = Vec2(position.x, targetY)
            if (velocity.y != 0f) {
                isGrounded = false
            }
        }
    }

    private fun queryGroundSupport(layer: TilemapChunkLayer): Boolean {
        if (layer.cols <= 0 || layer.rows <= 0) return false
        val box = boundingBox
        val inset = 2f
        val probeBox = RectF.fromTopLeft(
            box.left + inset,
            box.bottom - 0.5f,
            box.width - (inset * 2f),
            2f
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

    // --- Player Collection & Power-Up Upgrades ---

    override fun onCollected(collector: PixieObject, context: GameContext) {
        state = MushroomState.COLLECTED
        context.score += pointsAwarded
        context.setAttribute("player_tier", PlayerTier.SUPER.name)

        val oldHeight = collector.size.y
        val targetHeight = PlayerTier.SUPER.hitboxHeight
        val targetWidth = PlayerTier.SUPER.hitboxWidth
        if (oldHeight < targetHeight) {
            val deltaHeight = targetHeight - oldHeight
            collector.size = Vec2(targetWidth, targetHeight)
            collector.position = Vec2(collector.position.x, collector.position.y - deltaHeight)
        }
        collector.parameters["tier"] = 1f

        onPowerupCollected?.invoke(collector)
        onPlaySound?.invoke("powerup")
    }

    fun collect(
        collector: PixieObject,
        context: GameContext,
        controller: PlatformerPhysicsController?
    ): Boolean {
        if (!isAlive || isCollected) return false
        controller?.setTier(PlayerTier.SUPER)
        return super.collect(collector, context)
    }

    fun collect(
        controller: PlatformerPhysicsController,
        context: GameContext
    ): Boolean {
        if (!isAlive || isCollected) return false
        isCollected = true
        isAlive = false
        state = MushroomState.COLLECTED
        context.score += pointsAwarded
        context.setAttribute("player_tier", PlayerTier.SUPER.name)
        controller.setTier(PlayerTier.SUPER)
        onPlaySound?.invoke("powerup")
        return true
    }

    fun checkPlayerOverlap(playerBox: RectF): Boolean {
        if (!isAlive || isCollected || state == MushroomState.EMERGING) return false
        return boundingBox.intersects(playerBox)
    }

    fun checkAndCollect(
        player: PixieObject,
        context: GameContext,
        controller: PlatformerPhysicsController? = null
    ): Boolean {
        if (checkPlayerOverlap(player.boundingBox)) {
            return collect(player, context, controller)
        }
        return false
    }

    // --- Rendering ---

    fun render(batch: RenderBatchPort, camera: PixieCamera) {
        if (!isVisible || !isAlive || isCollected) return
        val screenX = position.x - camera.position.x
        val screenY = position.y - camera.position.y
        batch.drawSprite(SPRITE, screenX, screenY)
    }

    companion object {
        const val DEFAULT_SPEED = 45f
        const val DEFAULT_GRAVITY = 500f
        const val DEFAULT_MAX_FALL_SPEED = 240f
        const val DEFAULT_RISE_SPEED = 24f
        const val DEFAULT_POINTS = 1000L

        fun spawnEmerging(
            col: Int,
            row: Int,
            tileSize: Float = 16f,
            direction: Float = 1f,
            speed: Float = DEFAULT_SPEED,
            tilemap: TilemapChunkLayer? = null
        ): SuperMushroom {
            val startY = row * tileSize
            return SuperMushroom(
                id = "mushroom_${col}_${row}_${System.nanoTime()}",
                position = Vec2(col * tileSize, startY),
                state = MushroomState.EMERGING,
                direction = direction,
                speed = speed,
                tilemap = tilemap
            ).apply {
                this.startY = startY
                this.targetY = (row - 1) * tileSize
            }
        }

        val SPRITE = PixelSprite.fromAscii(
            listOf(
                "....RRRRRRRR....",
                "..RRRRRRRRRRRR..",
                ".RRRWWWRRRWWWRR.",
                ".RRWWWWWRWWWWWR.",
                "RRWWWWWWWWWWWWWR",
                "RRWWWWWWWWWWWWWR",
                "RRRWWWRRRWWWRRRR",
                "RRRRRRRRRRRRRRRR",
                ".RRRRRRRRRRRRRR.",
                "..BBBBBBBBBBBB..",
                "..BBBEBBBBEBBB..",
                "..BBBEBBBBEBBB..",
                "..BBBBBBBBBBBB..",
                "..BBBBBBBBBBBB..",
                "...BBBBBBBBBB...",
                "................"
            ),
            mapOf(
                '.' to 0x00000000,
                'R' to 0xFFE52521.toInt(), // NES Red
                'W' to 0xFFFFFFFF.toInt(), // White spot
                'B' to 0xFFFFECC3.toInt(), // Beige stalk
                'E' to 0xFF000000.toInt()  // Black eye
            )
        )
    }
}
