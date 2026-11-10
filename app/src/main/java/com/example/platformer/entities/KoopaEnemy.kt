package com.example.platformer.entities

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.entity.PixiePlayer
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.platformer.InteractiveBlockController
import com.pixie.engine.core.platformer.PlatformerEnemy
import com.pixie.engine.core.platformer.PlatformerEnemyState
import com.pixie.engine.core.ports.RenderBatchPort
import com.pixie.engine.core.sprite.AnimationFrame
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.tilemap.TileType
import com.pixie.engine.core.tilemap.TilemapChunkLayer

/**
 * Authentic NES Koopa Troopa turtle entity.
 *
 * Implements classic platformer mechanics:
 * - 2-frame walking turtle animation (16x24 pixel sprites).
 * - First stomp: retracts into stationary shell (16x16, halts movement, harmless).
 * - Lateral kick: kicked into high-speed sliding shell (speed = 180f, awards 400 pts).
 * - Sliding shell rebounds off solid walls, shatters BRICK blocks, and defeats enemies.
 * - Second stomp: halts sliding shell back into stationary idle.
 * - Fluent, chainable builders conforming to AGENTS.md.
 */
open class KoopaEnemy(
    id: String = "koopa_${System.nanoTime()}",
    position: Vec2 = Vec2.ZERO,
    direction: Float = -1f,
    patrolSpeed: Float = DEFAULT_PATROL_SPEED,
    shellSlideSpeed: Float = DEFAULT_SHELL_SLIDE_SPEED,
    gravity: Float = DEFAULT_GRAVITY,
    maxFallSpeed: Float = DEFAULT_MAX_FALL_SPEED,
    stompedDuration: Float = DEFAULT_STOMPED_DURATION,
    tilemap: TilemapChunkLayer? = null,
    var blockController: InteractiveBlockController? = null,
    initialClasses: Set<String> = emptySet()
) : PlatformerEnemy(
    id = id,
    position = position,
    size = Vec2(WALK_WIDTH, WALK_HEIGHT),
    initialHealth = 1f,
    scoreAward = DEFAULT_SCORE_AWARD,
    contactDamage = 10f,
    state = PlatformerEnemyState.PATROL,
    direction = direction,
    patrolSpeed = patrolSpeed,
    shellSlideSpeed = shellSlideSpeed,
    gravity = gravity,
    maxFallSpeed = maxFallSpeed,
    stompedDuration = stompedDuration,
    tilemap = tilemap,
    initialClasses = initialClasses + setOf("koopa", "turtle_enemy", "shell_enemy")
) {

    var lastKicker: PixieObject? = null
    var kickSafetyTimer: Float = 0f
    var enemiesDefeatedByShell: Int = 0

    private var lastContext: GameContext? = null

    var onEnemyDefeatedByShell: ((enemy: PlatformerEnemy, points: Long) -> Unit)? = null
    var onBrickShatteredByShell: ((col: Int, row: Int) -> Unit)? = null

    init {
        setAnimation(ANIM_WALK, WALK_ANIMATION)
        setAnimation(ANIM_SHELL_IDLE, SHELL_IDLE_ANIMATION)
        setAnimation(ANIM_SHELL_SLIDE, SHELL_SLIDE_ANIMATION)
        setAnimation(ANIM_DEFEATED_FALL, DEFEATED_FALL_ANIMATION)
        playAnimation(ANIM_WALK)

        val previousBounce = onWallBounce
        onWallBounce = { col, row ->
            handleWallBounce(col, row)
            previousBounce?.invoke(col, row)
        }
    }

    override fun onStateTransition(oldState: PlatformerEnemyState, newState: PlatformerEnemyState) {
        when (newState) {
            PlatformerEnemyState.PATROL -> {
                if (oldState == PlatformerEnemyState.SHELL_STATIONARY || oldState == PlatformerEnemyState.SHELL_SLIDING) {
                    position = Vec2(position.x, position.y - (WALK_HEIGHT - SHELL_HEIGHT))
                    size = Vec2(WALK_WIDTH, WALK_HEIGHT)
                    hitboxHeight = WALK_HEIGHT
                }
                playAnimation(ANIM_WALK)
            }
            PlatformerEnemyState.SHELL_STATIONARY -> {
                if (oldState == PlatformerEnemyState.PATROL) {
                    position = Vec2(position.x, position.y + (WALK_HEIGHT - SHELL_HEIGHT))
                    size = Vec2(SHELL_WIDTH, SHELL_HEIGHT)
                    hitboxHeight = SHELL_HEIGHT
                }
                enemiesDefeatedByShell = 0
                playAnimation(ANIM_SHELL_IDLE, resetTime = true)
            }
            PlatformerEnemyState.SHELL_SLIDING -> {
                if (oldState == PlatformerEnemyState.PATROL) {
                    position = Vec2(position.x, position.y + (WALK_HEIGHT - SHELL_HEIGHT))
                    size = Vec2(SHELL_WIDTH, SHELL_HEIGHT)
                    hitboxHeight = SHELL_HEIGHT
                }
                playAnimation(ANIM_SHELL_SLIDE, resetTime = true)
            }
            PlatformerEnemyState.DEFEATED_FALL -> {
                if (oldState == PlatformerEnemyState.PATROL) {
                    size = Vec2(SHELL_WIDTH, SHELL_HEIGHT)
                    hitboxHeight = SHELL_HEIGHT
                }
                playAnimation(ANIM_DEFEATED_FALL, resetTime = true)
            }
            else -> {}
        }
    }

    override fun onStomped(stompSource: PixieObject?, context: GameContext?) {
        if (!isAlive || isDefeated) return

        when (state) {
            PlatformerEnemyState.PATROL -> {
                transitionTo(PlatformerEnemyState.SHELL_STATIONARY)
                if (scoreAward > 0L && context != null) {
                    context.score += scoreAward
                }
            }
            PlatformerEnemyState.SHELL_SLIDING -> {
                transitionTo(PlatformerEnemyState.SHELL_STATIONARY)
                context?.let { it.score += 100L }
            }
            PlatformerEnemyState.SHELL_STATIONARY -> {
                val kickDir = stompSource?.let { if (it.position.x <= position.x) 1f else -1f } ?: direction
                kickShell(kickDir, stompSource, context)
            }
            else -> {}
        }

        onStompedListener?.invoke(this)
        onPlaySound?.invoke("stomp")
    }

    override fun kickShell(kickDirection: Float, kicker: PixieObject?, context: GameContext?) {
        if (!isAlive || state != PlatformerEnemyState.SHELL_STATIONARY) return
        this.direction = if (kickDirection >= 0f) 1f else -1f
        this.lastKicker = kicker
        this.kickSafetyTimer = KICK_SAFETY_WINDOW
        this.enemiesDefeatedByShell = 0
        transitionTo(PlatformerEnemyState.SHELL_SLIDING)
        context?.let { it.score += 400L }
        onShellKicked?.invoke(this, this.direction)
        onPlaySound?.invoke("kick")
    }

    override fun stopShell() {
        if (state == PlatformerEnemyState.SHELL_SLIDING) {
            transitionTo(PlatformerEnemyState.SHELL_STATIONARY)
        }
    }

    override fun update(dt: Float, context: GameContext) {
        lastContext = context
        if (kickSafetyTimer > 0f) {
            kickSafetyTimer -= dt
        }
        super.update(dt, context)
    }

    override fun onCollision(other: PixieObject, context: GameContext) {
        if (!isAlive || isDefeated) return

        when (other) {
            is PixiePlayer -> handlePlayerContact(other, context)
            is PlatformerEnemy -> handleEnemyContact(other, context)
        }
    }

    private fun handlePlayerContact(player: PixiePlayer, context: GameContext) {
        when (state) {
            PlatformerEnemyState.SHELL_STATIONARY -> {
                val kickDir = if (player.position.x <= position.x) 1f else -1f
                kickShell(kickDir, kicker = player, context = context)
            }
            PlatformerEnemyState.SHELL_SLIDING -> {
                if (kickSafetyTimer > 0f && player === lastKicker) {
                    return
                }
                if (dealsContactDamage && player.isAlive && !player.isInvulnerable) {
                    player.takeDamage(contactDamage, context)
                }
            }
            PlatformerEnemyState.PATROL -> {
                if (dealsContactDamage && player.isAlive && !player.isInvulnerable) {
                    player.takeDamage(contactDamage, context)
                }
            }
            else -> {}
        }
    }

    fun handleEnemyContact(other: PlatformerEnemy, context: GameContext? = null) {
        if (other === this || !other.isAlive || other.isDefeated) return

        if (state == PlatformerEnemyState.SHELL_SLIDING) {
            other.defeatWithFall(hitDirection = this.direction)
            enemiesDefeatedByShell++
            val points = calculateChainScore(enemiesDefeatedByShell)
            val ctx = context ?: lastContext
            ctx?.let { it.score += points }
            onEnemyDefeatedByShell?.invoke(other, points)
            onPlaySound?.invoke("kick")
        }
    }

    private fun handleWallBounce(col: Int, row: Int) {
        if (state != PlatformerEnemyState.SHELL_SLIDING) return

        val controller = blockController
        val shattered = if (controller != null) {
            controller.shatterBrick(col, row, lastContext)
        } else {
            val tile = tilemap?.getTile(col, row)
            if (tile?.type == TileType.BRICK) {
                tilemap?.setTile(col, row, TileType.EMPTY)
                lastContext?.let { it.score += 50L }
                true
            } else {
                false
            }
        }

        if (shattered) {
            onBrickShatteredByShell?.invoke(col, row)
        }
        onPlaySound?.invoke("bump")
    }

    override fun getCurrentSprite(): PixelSprite {
        val base = when (state) {
            PlatformerEnemyState.SHELL_STATIONARY -> SHELL_IDLE_SPRITE
            PlatformerEnemyState.SHELL_SLIDING -> super.getCurrentSprite()
            PlatformerEnemyState.DEFEATED_FALL -> UPSIDE_DOWN_SHELL_SPRITE
            else -> super.getCurrentSprite()
        }
        return if (state == PlatformerEnemyState.PATROL && direction > 0f) {
            base.flipHorizontal()
        } else {
            base
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

    fun withPosition(pos: Vec2): KoopaEnemy = apply {
        this.position = pos
    }

    fun withEnemyState(newState: PlatformerEnemyState): KoopaEnemy = apply {
        transitionTo(newState)
    }

    fun withSpawnDirection(dir: Float): KoopaEnemy = apply {
        super.withDirection(dir)
    }

    fun withPatrolSpeedRate(speed: Float): KoopaEnemy = apply {
        super.withPatrolSpeed(speed)
    }

    fun withShellSpeedRate(speed: Float): KoopaEnemy = apply {
        super.withShellSlideSpeed(speed)
    }

    fun withStageMap(layer: TilemapChunkLayer): KoopaEnemy = apply {
        super.withTilemap(layer)
    }

    fun withBlockController(controller: InteractiveBlockController): KoopaEnemy = apply {
        this.blockController = controller
    }

    fun onEnemyDefeated(listener: (PlatformerEnemy, Long) -> Unit): KoopaEnemy = apply {
        this.onEnemyDefeatedByShell = listener
    }

    fun onBrickShattered(listener: (col: Int, row: Int) -> Unit): KoopaEnemy = apply {
        this.onBrickShatteredByShell = listener
    }

    companion object {
        const val WALK_WIDTH = 16f
        const val WALK_HEIGHT = 24f
        const val SHELL_WIDTH = 16f
        const val SHELL_HEIGHT = 16f

        const val KICK_SAFETY_WINDOW = 0.25f

        const val ANIM_WALK = "walk"
        const val ANIM_SHELL_IDLE = "shell_idle"
        const val ANIM_SHELL_SLIDE = "shell_slide"
        const val ANIM_DEFEATED_FALL = "defeated_fall"

        fun calculateChainScore(chainCount: Int): Long = when (chainCount) {
            1 -> 500L
            2 -> 800L
            3 -> 1000L
            4 -> 2000L
            5 -> 4000L
            6 -> 8000L
            else -> 10000L
        }

        private val PALETTE = mapOf(
            '.' to 0x00000000,
            'G' to 0xFF00A800.toInt(), // NES Emerald Green Shell
            'D' to 0xFF005800.toInt(), // Dark Green Shell Grooves
            'Y' to 0xFFFCBC3C.toInt(), // Warm Yellow / Orange Skin & Feet
            'W' to 0xFFFCFCFC.toInt(), // White Eye Sclera & Shell Rim
            'K' to 0xFF000000.toInt()  // Black Pupils & Border
        )

        // 16x24 Walking Frame 1 (Facing Left)
        val WALK_FRAME_1 = PixelSprite.fromAscii(
            listOf(
                "......YYYY......",
                "....YYYYYYYY....",
                "...YYWKKYYYY....",
                "...YYWWKYYYY....",
                "..YYYYYYYYYY....",
                "..YYYYYYYY......",
                "...YYYYYY.......",
                "....YYYY.GG.....",
                "...GGGGGGGGG....",
                "..GGGGGGGGGGGG..",
                ".GGDDGGDDGGGGW..",
                ".GGDDGGDDGGGGW..",
                "GGGGGGGGGGGGGW..",
                "GGGGGGGGGGGGGW..",
                "GGDDGGDDGGGGGW..",
                "GGDDGGDDGGGGGW..",
                ".WWWWWWWWWWWWW..",
                "..YYYYYYYYYY....",
                "...YYYYYYYY.....",
                "....YY....YY....",
                "....YY....YY....",
                "...YYY...YYYY...",
                "..YYYY..YYYYYY..",
                "..YYYY..YYYYYY.."
            ),
            PALETTE
        )

        // 16x24 Walking Frame 2 (Stride Stance)
        val WALK_FRAME_2 = PixelSprite.fromAscii(
            listOf(
                "......YYYY......",
                "....YYYYYYYY....",
                "...YYWKKYYYY....",
                "...YYWWKYYYY....",
                "..YYYYYYYYYY....",
                "..YYYYYYYY......",
                "...YYYYYY.......",
                "....YYYY.GG.....",
                "...GGGGGGGGG....",
                "..GGGGGGGGGGGG..",
                ".GGDDGGDDGGGGW..",
                ".GGDDGGDDGGGGW..",
                "GGGGGGGGGGGGGW..",
                "GGGGGGGGGGGGGW..",
                "GGDDGGDDGGGGGW..",
                "GGDDGGDDGGGGGW..",
                ".WWWWWWWWWWWWW..",
                "..YYYYYYYYYY....",
                "...YYYYYYYY.....",
                "....YY....YY....",
                "....YY....YY....",
                "...YYYY...YYY...",
                "..YYYYYY..YYYY..",
                "..YYYYYY..YYYY.."
            ),
            PALETTE
        )

        // 16x16 Stationary Shell
        val SHELL_IDLE_SPRITE = PixelSprite.fromAscii(
            listOf(
                "......GGGG......",
                "....GGGGGGGG....",
                "...GGGGGGGGGG...",
                "..GGDDGGDDGGGG..",
                "..GGDDGGDDGGGG..",
                ".GGGGGGGGGGGGGW.",
                ".GGGGGGGGGGGGGW.",
                "GGDDGGDDGGGGGGW.",
                "GGDDGGDDGGGGGGW.",
                "GGGGGGGGGGGGGGW.",
                ".GGDDGGDDGGGGW..",
                ".GGDDGGDDGGGGW..",
                "..WWWWWWWWWWWW..",
                "...YYYYYYYYYY...",
                "....YYYYYYYY....",
                "......YYYY......"
            ),
            PALETTE
        )

        // 16x16 Sliding Shell Frame 2 (Angled Spin)
        val SHELL_SLIDE_FRAME_2 = PixelSprite.fromAscii(
            listOf(
                ".....GGGGGG.....",
                "...GGGGGGGGGG...",
                "..GGGGGGGGGGGG..",
                ".GGDDGGGGDDGGGG.",
                ".GGDDGGGGDDGGGG.",
                "GGGGGGGGGGGGGGGW",
                "GGDDGGGGDDGGGGGW",
                "GGDDGGGGDDGGGGGW",
                "GGGGGGGGGGGGGGGW",
                ".GGDDGGGGDDGGGW.",
                ".GGDDGGGGDDGGGW.",
                "..GGGGGGGGGGGG..",
                "...WWWWWWWWWW...",
                "....YYYYYYYY....",
                ".....YYYYYY.....",
                "......YYYY......"
            ),
            PALETTE
        )

        // 16x16 Sliding Shell Frame 3 (Flat Spin)
        val SHELL_SLIDE_FRAME_3 = PixelSprite.fromAscii(
            listOf(
                "....GGGGGGGG....",
                "..GGGGGGGGGGGG..",
                ".GGGGGGGGGGGGGG.",
                "GGDDGGDDDDGGDDGG",
                "GGDDGGDDDDGGDDGG",
                "GGGGGGGGGGGGGGGG",
                "GGDDGGDDDDGGDDGG",
                "GGDDGGDDDDGGDDGG",
                "GGGGGGGGGGGGGGGG",
                "GGDDGGDDDDGGDDGG",
                "GGDDGGDDDDGGDDGG",
                ".WWWWWWWWWWWWWW.",
                "..YYYYYYYYYYYY..",
                "...YYYYYYYYYY...",
                "....YYYYYYYY....",
                ".....YYYYYY....."
            ),
            PALETTE
        )

        val SHELL_SLIDE_FRAME_4 = SHELL_SLIDE_FRAME_2.flipHorizontal()

        // 16x16 Upside Down Shell for Defeated Fall
        val UPSIDE_DOWN_SHELL_SPRITE = SHELL_IDLE_SPRITE.flipVertical()

        val WALK_ANIMATION = AnimationSequence(
            name = ANIM_WALK,
            frames = listOf(
                AnimationFrame(WALK_FRAME_1, 0.2f),
                AnimationFrame(WALK_FRAME_2, 0.2f)
            )
        )

        val SHELL_IDLE_ANIMATION = AnimationSequence(
            name = ANIM_SHELL_IDLE,
            frames = listOf(
                AnimationFrame(SHELL_IDLE_SPRITE, 1.0f)
            ),
            isLooping = true
        )

        val SHELL_SLIDE_ANIMATION = AnimationSequence(
            name = ANIM_SHELL_SLIDE,
            frames = listOf(
                AnimationFrame(SHELL_IDLE_SPRITE, 0.06f),
                AnimationFrame(SHELL_SLIDE_FRAME_2, 0.06f),
                AnimationFrame(SHELL_SLIDE_FRAME_3, 0.06f),
                AnimationFrame(SHELL_SLIDE_FRAME_4, 0.06f)
            ),
            isLooping = true
        )

        val DEFEATED_FALL_ANIMATION = AnimationSequence(
            name = ANIM_DEFEATED_FALL,
            frames = listOf(
                AnimationFrame(UPSIDE_DOWN_SHELL_SPRITE, 1.0f)
            ),
            isLooping = false
        )

        fun spawn(
            col: Int,
            row: Int,
            tileSize: Float = 16f,
            direction: Float = -1f,
            tilemap: TilemapChunkLayer? = null,
            blockController: InteractiveBlockController? = null
        ): KoopaEnemy {
            // Foot of 24px walker aligns with bottom of 16px tile row: row * tileSize - 8f
            val spawnY = (row * tileSize) - (WALK_HEIGHT - tileSize)
            return KoopaEnemy(
                position = Vec2(col * tileSize, spawnY),
                direction = direction,
                tilemap = tilemap,
                blockController = blockController
            )
        }
    }
}
