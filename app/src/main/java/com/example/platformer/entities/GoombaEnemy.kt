package com.example.platformer.entities

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.platformer.PlatformerEnemy
import com.pixie.engine.core.platformer.PlatformerEnemyState
import com.pixie.engine.core.ports.RenderBatchPort
import com.pixie.engine.core.sprite.AnimationFrame
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.tilemap.TilemapChunkLayer

/**
 * Authentic NES Goomba mushroom walker entity.
 *
 * Implements:
 * - 2-frame walking wobble animation (16x16 pixel sprites).
 * - Constant horizontal patrol speed (default 35 px/sec).
 * - Stomp squash reaction: switches to 16x8 flattened sprite, halts movement,
 *   remains visible for 0.4s before despawning, and awards 100 points.
 * - Upside-down sprite on defeat fall.
 * - Fluent, chainable builders conforming to AGENTS.md.
 */
open class GoombaEnemy(
    id: String = "goomba_${System.nanoTime()}",
    position: Vec2 = Vec2.ZERO,
    direction: Float = -1f,
    patrolSpeed: Float = DEFAULT_PATROL_SPEED,
    gravity: Float = DEFAULT_GRAVITY,
    maxFallSpeed: Float = DEFAULT_MAX_FALL_SPEED,
    stompedDuration: Float = DEFAULT_STOMPED_DURATION,
    tilemap: TilemapChunkLayer? = null
) : PlatformerEnemy(
    id = id,
    position = position,
    size = Vec2(NORMAL_WIDTH, NORMAL_HEIGHT),
    initialHealth = 1f,
    scoreAward = DEFAULT_SCORE_AWARD,
    contactDamage = 10f,
    state = PlatformerEnemyState.PATROL,
    direction = direction,
    patrolSpeed = patrolSpeed,
    shellSlideSpeed = 0f,
    gravity = gravity,
    maxFallSpeed = maxFallSpeed,
    stompedDuration = stompedDuration,
    tilemap = tilemap,
    initialClasses = setOf("goomba", "mushroom_walker", "walker_enemy")
) {

    init {
        setAnimation(ANIM_WALK, WALK_ANIMATION)
        setAnimation(ANIM_STOMPED, STOMPED_ANIMATION)
        setAnimation(ANIM_DEFEATED_FALL, DEFEATED_FALL_ANIMATION)
        playAnimation(ANIM_WALK)
    }

    override fun onStateTransition(oldState: PlatformerEnemyState, newState: PlatformerEnemyState) {
        when (newState) {
            PlatformerEnemyState.PATROL -> {
                playAnimation(ANIM_WALK)
            }
            PlatformerEnemyState.STOMPED -> {
                playAnimation(ANIM_STOMPED, resetTime = true)
                applySquash()
            }
            PlatformerEnemyState.DEFEATED_FALL -> {
                playAnimation(ANIM_DEFEATED_FALL, resetTime = true)
            }
            else -> {}
        }
    }

    private fun applySquash() {
        if (size.y > SQUASHED_HEIGHT) {
            val delta = size.y - SQUASHED_HEIGHT
            position = Vec2(position.x, position.y + delta)
            size = Vec2(NORMAL_WIDTH, SQUASHED_HEIGHT)
            hitboxHeight = SQUASHED_HEIGHT
        }
    }

    override fun getCurrentSprite(): PixelSprite {
        return when (state) {
            PlatformerEnemyState.STOMPED -> SQUASHED_SPRITE
            PlatformerEnemyState.DEFEATED_FALL -> UPSIDE_DOWN_SPRITE
            else -> super.getCurrentSprite()
        }
    }

    fun render(batch: RenderBatchPort, camera: PixieCamera) {
        if (!isVisible || !isAlive) return
        val sprite = getCurrentSprite()
        if (sprite != PixelSprite.EMPTY) {
            val screenX = position.x - camera.position.x
            val screenY = position.y - camera.position.y
            batch.drawSprite(sprite, screenX, screenY)
        }
    }

    // --- Fluent Builders ---

    fun withPosition(pos: Vec2): GoombaEnemy = apply {
        this.position = pos
    }

    fun withSpawnDirection(dir: Float): GoombaEnemy = apply {
        super.withDirection(dir)
    }

    fun withSpeed(speed: Float): GoombaEnemy = apply {
        super.withPatrolSpeed(speed)
    }

    fun withStageMap(layer: TilemapChunkLayer): GoombaEnemy = apply {
        super.withTilemap(layer)
    }

    fun withSquashDuration(duration: Float): GoombaEnemy = apply {
        this.stompedDuration = duration
    }

    fun withPoints(points: Long): GoombaEnemy = apply {
        this.scoreAward = points
    }

    fun withDamage(damage: Float): GoombaEnemy = apply {
        this.contactDamage = damage
    }

    companion object {
        const val NORMAL_WIDTH = 16f
        const val NORMAL_HEIGHT = 16f
        const val SQUASHED_HEIGHT = 8f

        const val ANIM_WALK = "walk"
        const val ANIM_STOMPED = "stomped"
        const val ANIM_DEFEATED_FALL = "defeated_fall"

        private val PALETTE = mapOf(
            '.' to 0x00000000,
            'B' to 0xFF9E4A06.toInt(), // NES Chestnut Brown
            'T' to 0xFFFFCCA0.toInt(), // Warm Beige / Tan Stem
            'W' to 0xFFFFFFFF.toInt(), // White Eye Sclera
            'K' to 0xFF000000.toInt()  // Black Pupils / Brows / Feet
        )

        val WALK_FRAME_1 = PixelSprite.fromAscii(
            listOf(
                "......BBBB......",
                "....BBBBBBBB....",
                "...BBBBBBBBBB...",
                "..BBBBBBBBBBBB..",
                "..BBBBBBBBBBBB..",
                ".BBBBBBBBBBBBBB.",
                ".BBBBBBBBBBBBBB.",
                "BBBBBBBBBBBBBBBB",
                "BBBBBBBBBBBBBBBB",
                "...KKTTTTTTKK...",
                "..WKKTTTTTTKKW..",
                "..WWWKKTTKKWWW..",
                "..WWWKKTTKKWWW..",
                "...WWTTTTTTWW...",
                "..KKKK....KK....",
                ".KKKKKK...KKK..."
            ),
            PALETTE
        )

        val WALK_FRAME_2 = PixelSprite.fromAscii(
            listOf(
                "......BBBB......",
                "....BBBBBBBB....",
                "...BBBBBBBBBB...",
                "..BBBBBBBBBBBB..",
                "..BBBBBBBBBBBB..",
                ".BBBBBBBBBBBBBB.",
                ".BBBBBBBBBBBBBB.",
                "BBBBBBBBBBBBBBBB",
                "BBBBBBBBBBBBBBBB",
                "...KKTTTTTTKK...",
                "..WKKTTTTTTKKW..",
                "..WWWKKTTKKWWW..",
                "..WWWKKTTKKWWW..",
                "...WWTTTTTTWW...",
                "....KK....KKKK..",
                "...KKK...KKKKKK."
            ),
            PALETTE
        )

        val SQUASHED_SPRITE = PixelSprite.fromAscii(
            listOf(
                "..BBBBBBBBBBBB..",
                ".BBBBBBBBBBBBBB.",
                "BBBBBBBBBBBBBBBB",
                "BBBBBBBBBBBBBBBB",
                ".KKTTTTTTTTTTKK.",
                "WKKWKKTTTTKKWKKW",
                "KKKKKKTTTTKKKKKK",
                ".KKKKKKKKKKKKKK."
            ),
            PALETTE
        )

        val UPSIDE_DOWN_SPRITE = PixelSprite.fromAscii(
            listOf(
                ".KKKKKK...KKK...",
                "..KKKK....KK....",
                "...WWTTTTTTWW...",
                "..WWWKKTTKKWWW..",
                "..WWWKKTTKKWWW..",
                "..WKKTTTTTTKKW..",
                "...KKTTTTTTKK...",
                "BBBBBBBBBBBBBBBB",
                "BBBBBBBBBBBBBBBB",
                ".BBBBBBBBBBBBBB.",
                ".BBBBBBBBBBBBBB.",
                "..BBBBBBBBBBBB..",
                "..BBBBBBBBBBBB..",
                "...BBBBBBBBBB...",
                "....BBBBBBBB....",
                "......BBBB......"
            ),
            PALETTE
        )

        val WALK_ANIMATION = AnimationSequence(
            name = ANIM_WALK,
            frames = listOf(
                AnimationFrame(WALK_FRAME_1, 0.2f),
                AnimationFrame(WALK_FRAME_2, 0.2f)
            )
        )

        val STOMPED_ANIMATION = AnimationSequence(
            name = ANIM_STOMPED,
            frames = listOf(
                AnimationFrame(SQUASHED_SPRITE, 1.0f)
            ),
            isLooping = false
        )

        val DEFEATED_FALL_ANIMATION = AnimationSequence(
            name = ANIM_DEFEATED_FALL,
            frames = listOf(
                AnimationFrame(UPSIDE_DOWN_SPRITE, 1.0f)
            ),
            isLooping = false
        )

        fun spawn(
            col: Int,
            row: Int,
            tileSize: Float = 16f,
            direction: Float = -1f,
            tilemap: TilemapChunkLayer? = null
        ): GoombaEnemy {
            return GoombaEnemy(
                position = Vec2(col * tileSize, row * tileSize),
                direction = direction,
                tilemap = tilemap
            )
        }
    }
}
