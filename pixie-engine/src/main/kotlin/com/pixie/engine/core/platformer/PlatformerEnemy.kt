package com.pixie.engine.core.platformer

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieEnemy
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.entity.PixiePlayer
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.tilemap.CollisionContactSide
import com.pixie.engine.core.tilemap.TilemapChunkLayer
import com.pixie.engine.core.tilemap.TilemapCollisionQuery

/**
 * State lifecycle machine for classic NES platformer enemies (Goombas, Koopas, beetles, etc.).
 */
enum class PlatformerEnemyState {
    /** Autonomous horizontal patrol on the floor, bouncing off walls and ledges. */
    PATROL,

    /** Flattened/squashed from a vertical player stomp; counting down until despawn. */
    STOMPED,

    /** Retracted into an idle, stationary shell waiting to be kicked or picked up. */
    SHELL_STATIONARY,

    /** Fast sliding shell moving across terrain, rebounding off walls and defeating obstacles. */
    SHELL_SLIDING,

    /** Defeated via projectile, star power, or shell hit; upside-down hop falling off-screen. */
    DEFEATED_FALL
}

/**
 * Core platformer enemy entity for the Pixie Engine.
 *
 * Encapsulates authentic NES platformer AI and physics:
 * - Autonomous horizontal patrol velocity (`direction = -1f or 1f`)
 * - Decoupled 2-axis swept AABB tilemap collision with floor snapping and wall bounce
 * - Stage boundary detection and pit fall despawning
 * - State machine supporting walking, stomping, stationary shell, sliding shell, and defeat hops
 * - Fluent, chainable builders conforming to AGENTS.md
 */
open class PlatformerEnemy(
    id: String = "platformer_enemy_${System.nanoTime()}",
    position: Vec2 = Vec2.ZERO,
    size: Vec2 = Vec2(16f, 16f),
    initialHealth: Float = 1f,
    scoreAward: Long = DEFAULT_SCORE_AWARD,
    contactDamage: Float = 10f,
    var state: PlatformerEnemyState = PlatformerEnemyState.PATROL,
    var direction: Float = -1f,
    var patrolSpeed: Float = DEFAULT_PATROL_SPEED,
    var shellSlideSpeed: Float = DEFAULT_SHELL_SLIDE_SPEED,
    var gravity: Float = DEFAULT_GRAVITY,
    var maxFallSpeed: Float = DEFAULT_MAX_FALL_SPEED,
    var stompedDuration: Float = DEFAULT_STOMPED_DURATION,
    var tilemap: TilemapChunkLayer? = null,
    initialClasses: Set<String> = emptySet(),
    initialGroups: Set<String> = emptySet()
) : PixieEnemy(
    id = id,
    position = position,
    size = size,
    initialHealth = initialHealth,
    scoreAward = scoreAward,
    contactDamage = contactDamage,
    initialClasses = initialClasses + setOf("platformer_enemy"),
    initialGroups = initialGroups
) {
    var isGrounded: Boolean = false
        internal set

    var stompedTimer: Float = 0f
        internal set

    var hitboxOffsetX: Float = 0f
    var hitboxOffsetY: Float = 0f
    var hitboxWidth: Float = size.x
    var hitboxHeight: Float = size.y

    // Event listeners
    var onStateChanged: ((oldState: PlatformerEnemyState, newState: PlatformerEnemyState) -> Unit)? = null
    var onDirectionChanged: ((Float) -> Unit)? = null
    var onWallBounce: ((col: Int, row: Int) -> Unit)? = null
    var onStompedListener: ((PlatformerEnemy) -> Unit)? = null
    var onShellKicked: ((PlatformerEnemy, Float) -> Unit)? = null
    var onPlaySound: ((String) -> Unit)? = null

    val effectiveHitbox: RectF
        get() = RectF.fromTopLeft(
            position.x + hitboxOffsetX,
            position.y + hitboxOffsetY,
            hitboxWidth,
            hitboxHeight
        )

    val isPatrolling: Boolean get() = state == PlatformerEnemyState.PATROL
    val isStomped: Boolean get() = state == PlatformerEnemyState.STOMPED
    val isShellStationary: Boolean get() = state == PlatformerEnemyState.SHELL_STATIONARY
    val isShellSliding: Boolean get() = state == PlatformerEnemyState.SHELL_SLIDING
    val isDefeatedFall: Boolean get() = state == PlatformerEnemyState.DEFEATED_FALL

    val isHazardToEnemies: Boolean get() = state == PlatformerEnemyState.SHELL_SLIDING
    val canBeKicked: Boolean get() = state == PlatformerEnemyState.SHELL_STATIONARY
    val dealsContactDamage: Boolean
        get() = isAlive && !isDefeated && (state == PlatformerEnemyState.PATROL || state == PlatformerEnemyState.SHELL_SLIDING)

    init {
        updateHorizontalVelocity()
    }

    // --- Fluent Builders ---

    fun withState(newState: PlatformerEnemyState): PlatformerEnemy = apply {
        transitionTo(newState)
    }

    fun withDirection(dir: Float): PlatformerEnemy = apply {
        this.direction = if (dir >= 0f) 1f else -1f
        updateHorizontalVelocity()
    }

    fun withPatrolSpeed(speed: Float): PlatformerEnemy = apply {
        this.patrolSpeed = speed
        updateHorizontalVelocity()
    }

    fun withShellSlideSpeed(speed: Float): PlatformerEnemy = apply {
        this.shellSlideSpeed = speed
        updateHorizontalVelocity()
    }

    fun withGravity(gravity: Float): PlatformerEnemy = apply {
        this.gravity = gravity
    }

    fun withTilemap(layer: TilemapChunkLayer): PlatformerEnemy = apply {
        this.tilemap = layer
    }

    fun withHitbox(
        width: Float,
        height: Float,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ): PlatformerEnemy = apply {
        this.hitboxWidth = width
        this.hitboxHeight = height
        this.hitboxOffsetX = offsetX
        this.hitboxOffsetY = offsetY
    }

    fun onStateChanged(listener: (old: PlatformerEnemyState, new: PlatformerEnemyState) -> Unit): PlatformerEnemy = apply {
        this.onStateChanged = listener
    }

    fun onDirectionChanged(listener: (Float) -> Unit): PlatformerEnemy = apply {
        this.onDirectionChanged = listener
    }

    fun onWallBounce(listener: (col: Int, row: Int) -> Unit): PlatformerEnemy = apply {
        this.onWallBounce = listener
    }

    fun onStomped(listener: (PlatformerEnemy) -> Unit): PlatformerEnemy = apply {
        this.onStompedListener = listener
    }

    fun onShellKicked(listener: (PlatformerEnemy, Float) -> Unit): PlatformerEnemy = apply {
        this.onShellKicked = listener
    }

    fun onPlaySound(listener: (String) -> Unit): PlatformerEnemy = apply {
        this.onPlaySound = listener
    }

    // --- State Machine Transitions ---

    fun transitionTo(newState: PlatformerEnemyState) {
        if (state == newState) return
        val oldState = state
        state = newState

        when (newState) {
            PlatformerEnemyState.PATROL -> updateHorizontalVelocity()
            PlatformerEnemyState.STOMPED -> {
                stompedTimer = stompedDuration
                velocity = Vec2(0f, velocity.y)
            }
            PlatformerEnemyState.SHELL_STATIONARY -> {
                velocity = Vec2(0f, velocity.y)
            }
            PlatformerEnemyState.SHELL_SLIDING -> {
                velocity = Vec2(direction * shellSlideSpeed, velocity.y)
            }
            PlatformerEnemyState.DEFEATED_FALL -> {
                velocity = Vec2(direction * 40f, -160f)
                isGrounded = false
                isDefeated = true
            }
        }

        onStateTransition(oldState, newState)
        onStateChanged?.invoke(oldState, newState)
    }

    open fun onStateTransition(oldState: PlatformerEnemyState, newState: PlatformerEnemyState) {
        // Subclasses may hook into state transitions
    }

    fun reverseDirection() {
        direction = -direction
        updateHorizontalVelocity()
        onDirectionChanged?.invoke(direction)
    }

    private fun updateHorizontalVelocity() {
        val vx = when (state) {
            PlatformerEnemyState.PATROL -> direction * patrolSpeed
            PlatformerEnemyState.SHELL_SLIDING -> direction * shellSlideSpeed
            else -> 0f
        }
        velocity = Vec2(vx, velocity.y)
    }

    // --- Combat & Interaction Hooks ---

    /**
     * Invoked when a player lands directly on top of this enemy.
     */
    open fun onStomped(stompSource: PixieObject? = null, context: GameContext? = null) {
        if (!isAlive || isDefeated) return

        when (state) {
            PlatformerEnemyState.PATROL -> {
                transitionTo(PlatformerEnemyState.STOMPED)
                if (scoreAward > 0L && context != null) {
                    context.score += scoreAward
                }
            }
            PlatformerEnemyState.SHELL_SLIDING -> {
                transitionTo(PlatformerEnemyState.SHELL_STATIONARY)
            }
            PlatformerEnemyState.SHELL_STATIONARY -> {
                // Stomping stationary shell kicks it away in player's forward direction
                val kickDir = stompSource?.let { if (it.position.x <= position.x) 1f else -1f } ?: direction
                kickShell(kickDir, stompSource, context)
            }
            else -> {}
        }

        onStompedListener?.invoke(this)
        onPlaySound?.invoke("stomp")
    }

    /**
     * Kicks a stationary or caught shell into sliding motion.
     */
    open fun kickShell(kickDirection: Float, kicker: PixieObject? = null, context: GameContext? = null) {
        if (!isAlive || state != PlatformerEnemyState.SHELL_STATIONARY) return
        this.direction = if (kickDirection >= 0f) 1f else -1f
        transitionTo(PlatformerEnemyState.SHELL_SLIDING)
        onShellKicked?.invoke(this, this.direction)
        onPlaySound?.invoke("kick")
    }

    /**
     * Stops a sliding shell back into stationary idle state.
     */
    open fun stopShell() {
        if (state == PlatformerEnemyState.SHELL_SLIDING) {
            transitionTo(PlatformerEnemyState.SHELL_STATIONARY)
        }
    }

    /**
     * Knocks the enemy into an upside-down defeat fall off the screen (e.g., hit by shell/crate/star).
     */
    open fun defeatWithFall(hitDirection: Float = 0f, verticalImpulse: Float = -160f) {
        if (!isAlive) return
        if (hitDirection != 0f) {
            this.direction = if (hitDirection >= 0f) 1f else -1f
        }
        transitionTo(PlatformerEnemyState.DEFEATED_FALL)
        velocity = Vec2(direction * 40f, verticalImpulse)
        onPlaySound?.invoke("kick")
    }

    // --- Lifecycle & Simulation Updates ---

    override fun update(dt: Float, context: GameContext) {
        if (!isAlive) return
        animationTimeSeconds += dt
        previousPosition = position
        onContextChanged(context)

        when (state) {
            PlatformerEnemyState.STOMPED -> {
                stompedTimer -= dt
                if (stompedTimer <= 0f) {
                    isAlive = false
                    isDefeated = true
                    onDefeated(context)
                }
            }
            PlatformerEnemyState.DEFEATED_FALL -> updateDefeatFall(dt, context)
            PlatformerEnemyState.PATROL,
            PlatformerEnemyState.SHELL_SLIDING,
            PlatformerEnemyState.SHELL_STATIONARY -> {
                val layer = tilemap
                if (layer != null) {
                    updatePhysics(dt, layer)
                } else {
                    position += velocity * dt
                }
            }
        }
    }

    fun updateWithTilemap(dt: Float, layer: TilemapChunkLayer): PlatformerEnemy = apply {
        this.tilemap = layer
        update(dt, GameContext())
    }

    private fun updateDefeatFall(dt: Float, context: GameContext) {
        val newVy = (velocity.y + gravity * dt).coerceAtMost(maxFallSpeed)
        velocity = Vec2(velocity.x, newVy)
        position += velocity * dt

        val pitY = tilemap?.let { it.rows * it.tileSize + 64f } ?: 300f
        if (position.y > pitY) {
            isAlive = false
            onDefeated(context)
        }
    }

    // --- Decoupled 2-Axis Physics Resolution ---

    fun updatePhysics(dt: Float, layer: TilemapChunkLayer) {
        if (dt <= 0f || !isAlive || state == PlatformerEnemyState.STOMPED || state == PlatformerEnemyState.DEFEATED_FALL) {
            return
        }

        resolveHorizontalMovement(dt, layer)
        checkLedgeSupport(layer)
        resolveVerticalMovement(dt, layer)
        checkStagePit(layer)
    }

    private fun resolveHorizontalMovement(dt: Float, layer: TilemapChunkLayer) {
        val currentVx = when (state) {
            PlatformerEnemyState.PATROL -> direction * patrolSpeed
            PlatformerEnemyState.SHELL_SLIDING -> direction * shellSlideSpeed
            else -> 0f
        }
        if (currentVx == 0f) {
            velocity = Vec2(0f, velocity.y)
            return
        }

        val targetX = position.x + currentVx * dt
        val hBox = RectF.fromTopLeft(targetX + hitboxOffsetX, position.y + hitboxOffsetY, hitboxWidth, hitboxHeight)
        val hHits = TilemapCollisionQuery.queryHorizontal(layer, hBox, currentVx)

        if (hHits.isNotEmpty()) {
            if (currentVx > 0f) {
                val minWallLeft = hHits.minOf { it.bounds.left }
                position = Vec2(minWallLeft - hitboxWidth - hitboxOffsetX, position.y)
            } else {
                val maxWallRight = hHits.maxOf { it.bounds.right }
                position = Vec2(maxWallRight - hitboxOffsetX, position.y)
            }
            reverseDirection()
            val firstHit = hHits.first()
            onWallBounce?.invoke(firstHit.col, firstHit.row)
            return
        }

        // Check stage boundaries if no solid tile was hit
        val minStageX = 0f
        val maxStageX = layer.cols * layer.tileSize

        if (targetX + hitboxOffsetX <= minStageX) {
            position = Vec2(minStageX - hitboxOffsetX, position.y)
            reverseDirection()
        } else if (targetX + hitboxOffsetX + hitboxWidth >= maxStageX) {
            position = Vec2(maxStageX - hitboxOffsetX - hitboxWidth, position.y)
            reverseDirection()
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
        velocity = Vec2(velocity.x, newVy)

        val targetY = position.y + velocity.y * dt
        val currentBox = effectiveHitbox
        val vBox = RectF.fromTopLeft(currentBox.left, targetY + hitboxOffsetY, hitboxWidth, hitboxHeight)
        val prevBottom = currentBox.bottom

        val vHits = TilemapCollisionQuery.queryVertical(layer, vBox, velocity.y, prevBottom)
        val groundHits = vHits.filter { it.side == CollisionContactSide.GROUND }

        if (groundHits.isNotEmpty() && velocity.y >= 0f) {
            val highestGroundTop = groundHits.minOf { it.bounds.top }
            position = Vec2(position.x, highestGroundTop - hitboxHeight - hitboxOffsetY)
            velocity = Vec2(velocity.x, 0f)
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
        val box = effectiveHitbox
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

    private fun checkStagePit(layer: TilemapChunkLayer) {
        val pitY = layer.rows * layer.tileSize + 32f
        if (position.y > pitY) {
            isAlive = false
            isDefeated = true
        }
    }

    // --- Collision Damage Handling ---

    override fun onCollision(other: PixieObject, context: GameContext) {
        if (!dealsContactDamage) return

        if (other is PixiePlayer && other.isAlive && !other.isInvulnerable) {
            other.takeDamage(contactDamage, context)
        }
    }

    companion object {
        const val DEFAULT_PATROL_SPEED = 35f
        const val DEFAULT_SHELL_SLIDE_SPEED = 180f
        const val DEFAULT_GRAVITY = 500f
        const val DEFAULT_MAX_FALL_SPEED = 240f
        const val DEFAULT_STOMPED_DURATION = 0.4f
        const val DEFAULT_SCORE_AWARD = 100L
    }
}
