package com.example.platformer.entities.crate

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.RenderBatchPort
import com.pixie.engine.core.sprite.AnimationFrame
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.tilemap.CollisionContactSide
import com.pixie.engine.core.tilemap.TilemapChunkLayer
import com.pixie.engine.core.tilemap.TilemapCollisionQuery

/**
 * State lifecycle for Chip 'n Dale 2 style liftable and throwable wooden crates.
 */
enum class CrateState {
    /** Resting stationary on terrain; acts as solid platform obstacle. */
    PLACED_ON_GROUND,

    /** Held directly above player head while moving or jumping. */
    CARRIED,

    /** Hurled airborne as a high-impact destructive projectile. */
    THROWN_AIRBORNE,

    /** Impacted against enemy, breakable brick, or hard surface and shattered. */
    SHATTERED
}

/**
 * Authentic Chip 'n Dale 2 liftable wooden crate entity.
 *
 * Implements:
 * - 16x16 pixel wooden crate sprite with diagonal cross-beam bracing.
 * - 4-state lifecycle: [CrateState.PLACED_ON_GROUND], [CrateState.CARRIED],
 *   [CrateState.THROWN_AIRBORNE], [CrateState.SHATTERED].
 * - Solid ground obstacle and platform behavior while placed.
 * - Dynamic overhead attachment to player carrier.
 * - Gravitational arc when airborne.
 * - Fluent, chainable builders conforming to AGENTS.md.
 */
open class ThrowableCrate(
    id: String = "crate_${System.nanoTime()}",
    position: Vec2 = Vec2.ZERO,
    var state: CrateState = CrateState.PLACED_ON_GROUND,
    var gravity: Float = DEFAULT_GRAVITY,
    var maxFallSpeed: Float = DEFAULT_MAX_FALL_SPEED,
    var tilemap: TilemapChunkLayer? = null
) : PixieObject(
    id = id,
    position = position,
    size = Vec2(CRATE_SIZE, CRATE_SIZE),
    initialClasses = setOf("crate", "throwable_crate", "solid_obstacle", "interactive_platform")
) {
    var carrier: PixieObject? = null
        internal set

    var carryOffset: Vec2 = DEFAULT_CARRY_OFFSET
    var isGrounded: Boolean = (state == CrateState.PLACED_ON_GROUND)
        internal set

    var shatterTimer: Float = 0f
        internal set
    var shatterDuration: Float = DEFAULT_SHATTER_DURATION

    // Event callbacks
    var onStateChanged: ((oldState: CrateState, newState: CrateState) -> Unit)? = null
    var onPickedUp: ((carrier: PixieObject) -> Unit)? = null
    var onThrown: ((velocity: Vec2) -> Unit)? = null
    var onShattered: (() -> Unit)? = null

    init {
        configureState(state)
        setAnimation(ANIM_INTACT, INTACT_ANIMATION)
        setAnimation(ANIM_SHATTERED, SHATTERED_ANIMATION)
        playAnimation(if (state == CrateState.SHATTERED) ANIM_SHATTERED else ANIM_INTACT)
    }

    /**
     * Checks if this crate can currently be stood on by players or actors as a platform.
     */
    fun canStandOn(): Boolean {
        return state == CrateState.PLACED_ON_GROUND && isAlive && isSolid
    }

    /**
     * Transitions this crate to a new lifecycle state, updating solid and active flags.
     */
    fun transitionTo(newState: CrateState) {
        if (state == newState) return
        val oldState = state
        state = newState
        configureState(newState)
        onStateChanged?.invoke(oldState, newState)
    }

    private fun configureState(targetState: CrateState) {
        when (targetState) {
            CrateState.PLACED_ON_GROUND -> {
                isSolid = true
                isAlive = true
                isVisible = true
                carrier = null
                playAnimation(ANIM_INTACT)
            }
            CrateState.CARRIED -> {
                isSolid = false
                isAlive = true
                isVisible = true
                velocity = Vec2.ZERO
                playAnimation(ANIM_INTACT)
            }
            CrateState.THROWN_AIRBORNE -> {
                isSolid = false
                isAlive = true
                isVisible = true
                carrier = null
                isGrounded = false
                playAnimation(ANIM_INTACT)
            }
            CrateState.SHATTERED -> {
                isSolid = false
                isAlive = false
                carrier = null
                velocity = Vec2.ZERO
                shatterTimer = 0f
                playAnimation(ANIM_SHATTERED, resetTime = true)
                onShattered?.invoke()
            }
        }
    }

    /**
     * Attaches this crate to a carrier actor (e.g. PlatformerPlayer).
     */
    fun pickUp(by: PixieObject) {
        carrier = by
        position = Vec2(by.position.x + carryOffset.x, by.position.y + carryOffset.y)
        transitionTo(CrateState.CARRIED)
        onPickedUp?.invoke(by)
    }

    /**
     * Hurls this crate as an airborne projectile with the given velocity vector.
     */
    fun throwCrate(throwVelocity: Vec2) {
        velocity = throwVelocity
        transitionTo(CrateState.THROWN_AIRBORNE)
        onThrown?.invoke(throwVelocity)
    }

    /**
     * Drops or places crate back down onto ground.
     */
    fun placeOnGround(pos: Vec2 = position) {
        position = pos
        velocity = Vec2.ZERO
        isGrounded = true
        transitionTo(CrateState.PLACED_ON_GROUND)
    }

    /**
     * Shatters this crate into splinter fragments upon impact.
     */
    fun shatter() {
        transitionTo(CrateState.SHATTERED)
    }

    override fun update(dt: Float, context: GameContext) {
        super.update(dt, context)
        when (state) {
            CrateState.PLACED_ON_GROUND -> updatePlaced(dt)
            CrateState.CARRIED -> updateCarried()
            CrateState.THROWN_AIRBORNE -> updateAirborne(dt)
            CrateState.SHATTERED -> updateShattered(dt)
        }
    }

    private fun updateCarried() {
        val currentCarrier = carrier ?: return
        position = Vec2(
            currentCarrier.position.x + carryOffset.x,
            currentCarrier.position.y + carryOffset.y
        )
        velocity = currentCarrier.velocity
    }

    private fun updateAirborne(dt: Float) {
        val nextVy = (velocity.y + gravity * dt).coerceAtMost(maxFallSpeed)
        velocity = Vec2(velocity.x, nextVy)
        position += velocity * dt
    }

    private fun updatePlaced(dt: Float) {
        val layer = tilemap
        if (layer != null && !isGrounded) {
            val nextVy = (velocity.y + gravity * dt).coerceAtMost(maxFallSpeed)
            velocity = Vec2(0f, nextVy)
            resolveVerticalFall(dt, layer)
        }
    }

    private fun resolveVerticalFall(dt: Float, layer: TilemapChunkLayer) {
        val targetY = position.y + velocity.y * dt
        val vBox = RectF.fromTopLeft(position.x, targetY, size.x, size.y)
        val prevBottom = boundingBox.bottom

        val vHits = TilemapCollisionQuery.queryVertical(layer, vBox, velocity.y, prevBottom)
        val groundHits = vHits.filter { it.side == CollisionContactSide.GROUND }

        if (groundHits.isNotEmpty() && velocity.y >= 0f) {
            val highestGroundTop = groundHits.minOf { it.bounds.top }
            position = Vec2(position.x, highestGroundTop - size.y)
            velocity = Vec2.ZERO
            isGrounded = true
        } else {
            position = Vec2(position.x, targetY)
        }
    }

    private fun updateShattered(dt: Float) {
        shatterTimer += dt
        if (shatterTimer >= shatterDuration) {
            isVisible = false
        }
    }

    open fun render(batch: RenderBatchPort, camera: PixieCamera) {
        if (!isVisible) return
        val screenPos = camera.worldToScreen(position)
        val sprite = getCurrentSprite()
        if (sprite != PixelSprite.EMPTY) {
            batch.drawSprite(sprite, screenPos.x, screenPos.y)
        }
    }

    // --- Fluent Builders ---

    fun withPosition(pos: Vec2): ThrowableCrate = apply {
        this.position = pos
    }

    fun withState(targetState: CrateState): ThrowableCrate = apply {
        transitionTo(targetState)
    }

    fun withGravity(gravity: Float): ThrowableCrate = apply {
        this.gravity = gravity
    }

    fun withMaxFallSpeed(maxSpeed: Float): ThrowableCrate = apply {
        this.maxFallSpeed = maxSpeed
    }

    fun withTilemap(layer: TilemapChunkLayer?): ThrowableCrate = apply {
        this.tilemap = layer
    }

    fun withCarryOffset(offset: Vec2): ThrowableCrate = apply {
        this.carryOffset = offset
    }

    fun withCarrier(by: PixieObject?): ThrowableCrate = apply {
        if (by != null) pickUp(by)
    }

    companion object {
        const val CRATE_SIZE = 16f
        const val DEFAULT_GRAVITY = 650f
        const val DEFAULT_MAX_FALL_SPEED = 400f
        const val DEFAULT_SHATTER_DURATION = 0.25f

        val DEFAULT_CARRY_OFFSET = Vec2(0f, -14f)

        const val ANIM_INTACT = "intact"
        const val ANIM_SHATTERED = "shattered"

        private val CRATE_PALETTE = mapOf(
            '.' to 0x00000000,
            'B' to 0xFF2B1607.toInt(), // Outer border & groove lines (Very dark wood)
            'D' to 0xFF6A3610.toInt(), // Dark plank shadow / interior recessed
            'M' to 0xFFA0551E.toInt(), // Warm wood plank base
            'L' to 0xFFD88942.toInt(), // Cross-beam face & upper edge bevel
            'H' to 0xFFF5B97B.toInt(), // Highlight & corner brackets
            'N' to 0xFFFFE8C4.toInt()  // Metal rivets / nails
        )

        /**
         * Authentic 16x16 wooden crate with diagonal cross-beam ("X") bracing,
         * beveled outer frame, and corner rivet fastenings.
         */
        val CRATE_SPRITE = PixelSprite.fromAscii(
            listOf(
                "BBBBBBBBBBBBBBBB",
                "BHHHHHHHHHHHHHHB",
                "BHNLLMMDDMMLLNHB",
                "BHL...MDDM...LHB",
                "BHMLL...DD...MLB",
                "BHMDDLL....DDMLB",
                "BHM..DDLLLLDDMLB",
                "BHMDD..LLLL..MLB",
                "BHMDD..LLLL..MLB",
                "BHM..DDLLLLDDMLB",
                "BHMDDLL....DDMLB",
                "BHMLL...DD...MLB",
                "BHL...MDDM...LHB",
                "BHNLLMMDDMMLLNHB",
                "BDDDDDDDDDDDDDDB",
                "BBBBBBBBBBBBBBBB"
            ),
            CRATE_PALETTE
        )

        /**
         * Shattered crate frame displaying flying wooden splinters.
         */
        val SHATTERED_SPRITE = PixelSprite.fromAscii(
            listOf(
                "..LL......DD....",
                ".LLH......DDB...",
                "....LL..DD......",
                "....LL..DD......",
                "..DD......LL....",
                ".DDB......LLH...",
                "....DD..LL......",
                "....DD..LL......",
                "..LL......DD....",
                ".LLH......DDB...",
                "....LL..DD......",
                "....LL..DD......",
                "..DD......LL....",
                ".DDB......LLH...",
                "....DD..LL......",
                "....DD..LL......"
            ),
            CRATE_PALETTE
        )

        val INTACT_ANIMATION = AnimationSequence(
            name = ANIM_INTACT,
            frames = listOf(AnimationFrame(CRATE_SPRITE, 1.0f)),
            isLooping = true
        )

        val SHATTERED_ANIMATION = AnimationSequence(
            name = ANIM_SHATTERED,
            frames = listOf(AnimationFrame(SHATTERED_SPRITE, DEFAULT_SHATTER_DURATION)),
            isLooping = false
        )

        /**
         * Spawns an intact wooden crate resting at stage tile coordinates.
         */
        fun spawn(
            col: Int,
            row: Int,
            tileSize: Float = 16f,
            tilemap: TilemapChunkLayer? = null
        ): ThrowableCrate {
            return ThrowableCrate(
                position = Vec2(col * tileSize, row * tileSize),
                tilemap = tilemap
            )
        }

        /**
         * Spawns a crate emerging from a struck block popped into place.
         */
        fun spawnFromBlock(
            col: Int,
            row: Int,
            tileSize: Float = 16f,
            tilemap: TilemapChunkLayer? = null
        ): ThrowableCrate {
            return ThrowableCrate(
                position = Vec2(col * tileSize, (row - 1) * tileSize),
                tilemap = tilemap
            )
        }
    }
}
