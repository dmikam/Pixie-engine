package com.pixie.engine.core.platformer

import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.tilemap.TilemapChunkLayer
import kotlin.math.abs

/**
 * High-performance, pure Kotlin physics controller delivering authentic NES-style
 * platformer movement dynamics (Super Mario Bros momentum, sprint acceleration, skidding,
 * variable jump cut, coyote time, and jump buffering).
 */
class PlatformerPhysicsController(
    var config: PlatformerPhysicsConfig = PlatformerPhysicsConfig.NES_CLASSIC.copy()
) {
    var position: Vec2 = Vec2.ZERO
    var velocity: Vec2 = Vec2.ZERO

    var hitboxWidth: Float = 14f
    var hitboxHeight: Float = 16f
    var hitboxOffsetX: Float = 1f
    var hitboxOffsetY: Float = 0f

    var tier: PlayerTier = PlayerTier.SMALL
        private set

    var state: PlayerAnimationState = PlayerAnimationState.IDLE
        private set

    var facing: PlayerFacing = PlayerFacing.RIGHT
        private set

    var isGrounded: Boolean = true
        private set

    var isSkidding: Boolean = false
        private set

    var isDucking: Boolean = false
    var isCarrying: Boolean = false

    var moveInputX: Float = 0f
        private set

    var runHeld: Boolean = false
        private set

    var jumpHeld: Boolean = false
        private set

    var isJumpCut: Boolean = false
        private set

    var hasJumpedThisAirtime: Boolean = false
        private set

    // Timer tracking for coyote time and jump buffering
    var timeSinceGrounded: Float = 0f
        private set

    var jumpBufferTimer: Float = 0f
        private set

    // Event hooks for audio, particle, and haptic triggers
    var onSkidStart: (() -> Unit)? = null
    var onSkidEnd: (() -> Unit)? = null
    var onJump: (() -> Unit)? = null
    var onLand: (() -> Unit)? = null
    var onCeilingBonk: (() -> Unit)? = null
    var onHeadBonk: ((tileX: Int, tileY: Int) -> Unit)? = null

    val velocityX: Float get() = velocity.x
    val velocityY: Float get() = velocity.y

    fun getHitbox(pos: Vec2 = position): RectF {
        return RectF.fromTopLeft(pos.x + hitboxOffsetX, pos.y + hitboxOffsetY, hitboxWidth, hitboxHeight)
    }

    // --- Fluent Builder & Configuration Methods ---

    fun withConfig(config: PlatformerPhysicsConfig): PlatformerPhysicsController = apply {
        this.config = config
    }

    fun withPosition(x: Float, y: Float): PlatformerPhysicsController = apply {
        this.position = Vec2(x, y)
    }

    fun withPosition(pos: Vec2): PlatformerPhysicsController = apply {
        this.position = pos
    }

    fun withVelocity(vx: Float, vy: Float): PlatformerPhysicsController = apply {
        this.velocity = Vec2(vx, vy)
    }

    fun withVelocity(vel: Vec2): PlatformerPhysicsController = apply {
        this.velocity = vel
    }

    fun withGrounded(grounded: Boolean): PlatformerPhysicsController = apply {
        this.isGrounded = grounded
        if (grounded) {
            this.timeSinceGrounded = 0f
            this.hasJumpedThisAirtime = false
            this.isJumpCut = false
        }
    }

    fun setGrounded(grounded: Boolean): PlatformerPhysicsController = withGrounded(grounded)

    /**
     * Explicitly signals that the player left ground by slipping/walking off a ledge.
     * Starts the coyote time window if not already airborne.
     */
    fun leaveGround(initialAirborneDuration: Float = 0f): PlatformerPhysicsController = apply {
        this.isGrounded = false
        this.timeSinceGrounded = initialAirborneDuration
        this.hasJumpedThisAirtime = false
        this.isJumpCut = false
    }

    fun withFacing(facing: PlayerFacing): PlatformerPhysicsController = apply {
        this.facing = facing
    }

    fun withDuck(ducking: Boolean): PlatformerPhysicsController = apply {
        this.isDucking = ducking
    }

    fun withCarry(carrying: Boolean): PlatformerPhysicsController = apply {
        this.isCarrying = carrying
    }

    fun withHitbox(
        width: Float,
        height: Float,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ): PlatformerPhysicsController = apply {
        this.hitboxWidth = width
        this.hitboxHeight = height
        this.hitboxOffsetX = offsetX
        this.hitboxOffsetY = offsetY
    }

    fun withTier(tier: PlayerTier): PlatformerPhysicsController = apply {
        setTier(tier)
    }

    fun setTier(newTier: PlayerTier): PlatformerPhysicsController = apply {
        val oldHeight = this.hitboxHeight
        this.tier = newTier
        this.hitboxWidth = newTier.hitboxWidth
        this.hitboxHeight = newTier.hitboxHeight
        if (isGrounded && newTier.hitboxHeight > oldHeight) {
            position = Vec2(position.x, position.y - (newTier.hitboxHeight - oldHeight))
        }
    }

    fun upgradeTier(): PlayerTier {
        val next = tier.upgrade()
        setTier(next)
        return next
    }

    fun downgradeTier(): PlayerTier {
        val prev = tier.downgrade()
        setTier(prev)
        return prev
    }

    // --- Input Processors ---

    fun setInput(moveX: Float, run: Boolean = false, jump: Boolean = false): PlatformerPhysicsController = apply {
        this.moveInputX = moveX.coerceIn(-1f, 1f)
        this.runHeld = run

        val wasJumpHeld = this.jumpHeld
        this.jumpHeld = jump

        if (!wasJumpHeld && jump) {
            this.jumpBufferTimer = config.jumpBufferDuration
            checkAndExecuteJump()
        } else if (wasJumpHeld && !jump) {
            triggerJumpCut()
        }
    }

    fun setMoveX(moveX: Float): PlatformerPhysicsController = apply {
        this.moveInputX = moveX.coerceIn(-1f, 1f)
    }

    fun setRunHeld(held: Boolean): PlatformerPhysicsController = apply {
        this.runHeld = held
    }

    fun setJumpHeld(held: Boolean): PlatformerPhysicsController = apply {
        val wasHeld = this.jumpHeld
        this.jumpHeld = held
        if (!wasHeld && held) {
            pressJump()
        } else if (wasHeld && !held) {
            releaseJump()
        }
    }

    fun pressJump(): PlatformerPhysicsController = apply {
        this.jumpHeld = true
        this.jumpBufferTimer = config.jumpBufferDuration
        checkAndExecuteJump()
    }

    fun releaseJump(): PlatformerPhysicsController = apply {
        this.jumpHeld = false
        triggerJumpCut()
    }

    private fun triggerJumpCut() {
        if (velocity.y < 0f) {
            isJumpCut = true
        }
    }

    fun cancelJumpBuffer(): PlatformerPhysicsController = apply {
        this.jumpBufferTimer = 0f
    }

    // --- Physics Tick Execution ---

    fun update(dt: Float): PlatformerPhysicsController = apply {
        if (dt <= 0f) return this

        updateTimers(dt)
        val newVx = calculateHorizontalVelocity(dt)
        val newVy = calculateVerticalVelocity(dt)

        velocity = Vec2(newVx, newVy)
        position += velocity * dt

        state = resolveAnimationState()
    }

    /**
     * Executes physics update with 2-axis sub-stepped tilemap collision resolution.
     */
    fun update(
        dt: Float,
        layer: TilemapChunkLayer,
        resolver: PlatformerCollisionResolver = PlatformerCollisionResolver.DEFAULT
    ): PlatformerPhysicsController = apply {
        resolver.resolve(this, layer, dt)
    }

    /**
     * Alias for update(dt, layer, resolver) supporting fluent chainable calls.
     */
    fun updateWithCollision(
        layer: TilemapChunkLayer,
        dt: Float,
        resolver: PlatformerCollisionResolver = PlatformerCollisionResolver.DEFAULT
    ): PlatformerPhysicsController = update(dt, layer, resolver)

    internal fun updateTimers(dt: Float) {
        if (isGrounded) {
            timeSinceGrounded = 0f
        } else {
            timeSinceGrounded += dt
        }

        if (jumpBufferTimer > 0f) {
            jumpBufferTimer = (jumpBufferTimer - dt).coerceAtLeast(0f)
            checkAndExecuteJump()
        }
    }

    // --- Horizontal Movement & Skidding Physics ---

    internal fun calculateHorizontalVelocity(dt: Float): Float {
        val input = moveInputX
        return if (abs(input) > INPUT_DEADZONE) {
            val inputSign = if (input > 0f) 1f else -1f
            if (shouldTriggerSkid(inputSign)) {
                applySkidDeceleration(inputSign, dt)
            } else {
                applyNormalAcceleration(inputSign, dt)
            }
        } else {
            applyGroundFriction(dt)
        }
    }

    private fun shouldTriggerSkid(inputSign: Float): Boolean {
        if (!isGrounded) return false
        val movingOpposite = (velocity.x * inputSign) < 0f
        return movingOpposite && abs(velocity.x) >= config.minSkidSpeedThreshold
    }

    private fun applySkidDeceleration(inputSign: Float, dt: Float): Float {
        val wasSkidding = isSkidding
        isSkidding = true
        if (!wasSkidding) onSkidStart?.invoke()

        val skidDecel = config.skidDeceleration * dt
        val currentVx = velocity.x

        return if (currentVx > 0f) {
            val nextVx = currentVx - skidDecel
            if (nextVx <= 0f) endSkid(inputSign) else nextVx
        } else {
            val nextVx = currentVx + skidDecel
            if (nextVx >= 0f) endSkid(inputSign) else nextVx
        }
    }

    private fun endSkid(inputSign: Float): Float {
        val wasSkidding = isSkidding
        isSkidding = false
        if (wasSkidding) onSkidEnd?.invoke()
        facing = if (inputSign > 0f) PlayerFacing.RIGHT else PlayerFacing.LEFT
        return 0f
    }

    private fun applyNormalAcceleration(inputSign: Float, dt: Float): Float {
        if (isSkidding) {
            isSkidding = false
            onSkidEnd?.invoke()
        }

        facing = if (inputSign > 0f) PlayerFacing.RIGHT else PlayerFacing.LEFT
        val targetMaxSpeed = if (runHeld) config.maxRunSpeed else config.maxWalkSpeed
        val baseAccel = if (runHeld) config.runAcceleration else config.walkAcceleration
        val effectiveAccel = if (isGrounded) baseAccel else baseAccel * config.airAccelerationMultiplier

        val currentVx = velocity.x
        return if (inputSign > 0f) {
            if (currentVx < targetMaxSpeed) {
                (currentVx + effectiveAccel * dt).coerceAtMost(targetMaxSpeed)
            } else if (currentVx > targetMaxSpeed && isGrounded) {
                (currentVx - config.groundFriction * dt).coerceAtLeast(targetMaxSpeed)
            } else {
                currentVx
            }
        } else {
            if (currentVx > -targetMaxSpeed) {
                (currentVx - effectiveAccel * dt).coerceAtLeast(-targetMaxSpeed)
            } else if (currentVx < -targetMaxSpeed && isGrounded) {
                (currentVx + config.groundFriction * dt).coerceAtMost(-targetMaxSpeed)
            } else {
                currentVx
            }
        }
    }

    private fun applyGroundFriction(dt: Float): Float {
        if (isSkidding) {
            isSkidding = false
            onSkidEnd?.invoke()
        }

        val currentVx = velocity.x
        if (!isGrounded || currentVx == 0f) return currentVx

        val friction = config.groundFriction * dt
        return if (currentVx > 0f) {
            (currentVx - friction).coerceAtLeast(0f)
        } else {
            (currentVx + friction).coerceAtMost(0f)
        }
    }

    // --- Vertical Physics, Variable Jump Cut, Coyote Time & Buffering ---

    internal fun calculateVerticalVelocity(dt: Float): Float {
        if (isGrounded) return 0f

        val currentVy = velocity.y
        if (currentVy < 0f && !jumpHeld) {
            isJumpCut = true
        }
        if (currentVy >= 0f) {
            isJumpCut = false
        }

        val effectiveGravity = if (currentVy < 0f && (isJumpCut || !jumpHeld)) {
            config.gravity * config.jumpCutGravityMultiplier
        } else {
            config.gravity
        }

        return (currentVy + effectiveGravity * dt).coerceAtMost(config.maxFallSpeed)
    }

    private fun checkAndExecuteJump(): Boolean {
        val canCoyoteJump = isGrounded || (!hasJumpedThisAirtime && timeSinceGrounded <= config.coyoteTimeDuration)
        if (jumpBufferTimer > 0f && canCoyoteJump) {
            jumpBufferTimer = 0f
            timeSinceGrounded = config.coyoteTimeDuration + 1f
            isGrounded = false
            hasJumpedThisAirtime = true
            isJumpCut = false
            isSkidding = false
            velocity = Vec2(velocity.x, config.jumpImpulse)
            onJump?.invoke()
            return true
        }
        return false
    }

    fun landOnGround(groundY: Float): PlatformerPhysicsController = apply {
        val wasAirborne = !isGrounded
        isGrounded = true
        timeSinceGrounded = 0f
        hasJumpedThisAirtime = false
        isJumpCut = false
        position = Vec2(position.x, groundY)
        velocity = Vec2(velocity.x, 0f)
        if (wasAirborne) onLand?.invoke()
        checkAndExecuteJump()
    }

    fun bonkCeiling(ceilingY: Float): PlatformerPhysicsController = apply {
        position = Vec2(position.x, ceilingY)
        velocity = Vec2(velocity.x, 0f)
        isJumpCut = false
        onCeilingBonk?.invoke()
    }

    fun stopHorizontal(): PlatformerPhysicsController = apply {
        velocity = Vec2(0f, velocity.y)
        isSkidding = false
    }

    // --- State Resolution ---

    fun updateStateFromResolution(): PlayerAnimationState {
        state = resolveAnimationState()
        return state
    }

    internal fun resolveAnimationState(): PlayerAnimationState {
        if (isCarrying) {
            if (!isGrounded) return PlayerAnimationState.CARRY_JUMP
            if (isDucking) return PlayerAnimationState.DUCK
            return if (abs(velocity.x) <= 1f) PlayerAnimationState.CARRY_IDLE else PlayerAnimationState.CARRY_WALK
        }
        if (!isGrounded) {
            return if (velocity.y < 0f) PlayerAnimationState.JUMP_ASCENT else PlayerAnimationState.FALL
        }
        if (isSkidding) return PlayerAnimationState.SKID
        if (isDucking) return PlayerAnimationState.DUCK

        return when {
            abs(velocity.x) <= 1f -> PlayerAnimationState.IDLE
            abs(velocity.x) > config.maxWalkSpeed + 5f || (runHeld && abs(velocity.x) > config.maxWalkSpeed * 0.9f) ->
                PlayerAnimationState.RUN
            else -> PlayerAnimationState.WALK
        }
    }

    companion object {
        const val INPUT_DEADZONE = 0.05f
    }
}
