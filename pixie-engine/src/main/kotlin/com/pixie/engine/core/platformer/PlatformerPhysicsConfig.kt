package com.pixie.engine.core.platformer

/**
 * Authentic NES-era 2D platformer movement physics parameters.
 *
 * Configures horizontal acceleration/friction, sprint velocity thresholds, skidding friction,
 * vertical gravity, variable jump cut multipliers, coyote time window, and jump buffering.
 */
data class PlatformerPhysicsConfig(
    var walkAcceleration: Float = 420f,
    var runAcceleration: Float = 750f,
    var maxWalkSpeed: Float = 110f,
    var maxRunSpeed: Float = 185f,
    var groundFriction: Float = 550f,
    var skidDeceleration: Float = 950f,
    var gravity: Float = 920f,
    var jumpImpulse: Float = -270f,
    var jumpCutGravityMultiplier: Float = 2.4f,
    var coyoteTimeDuration: Float = 0.12f,
    var jumpBufferDuration: Float = 0.10f,
    var maxFallSpeed: Float = 360f,
    var airAccelerationMultiplier: Float = 0.85f,
    var minSkidSpeedThreshold: Float = 40f
) {
    /**
     * Fluent builder for setting walk and run maximum speeds.
     */
    fun withSpeeds(walk: Float, run: Float): PlatformerPhysicsConfig = apply {
        this.maxWalkSpeed = walk
        this.maxRunSpeed = run
    }

    /**
     * Fluent builder for setting walk and run acceleration rates.
     */
    fun withAccelerations(walkAcc: Float, runAcc: Float): PlatformerPhysicsConfig = apply {
        this.walkAcceleration = walkAcc
        this.runAcceleration = runAcc
    }

    /**
     * Fluent builder for configuring jump impulse and variable jump cut multiplier.
     */
    fun withJump(impulse: Float, cutMultiplier: Float = 2.4f): PlatformerPhysicsConfig = apply {
        this.jumpImpulse = impulse
        this.jumpCutGravityMultiplier = cutMultiplier
    }

    /**
     * Fluent builder for configuring gravity and terminal fall speed.
     */
    fun withGravity(grav: Float, maxFall: Float = 360f): PlatformerPhysicsConfig = apply {
        this.gravity = grav
        this.maxFallSpeed = maxFall
    }

    /**
     * Fluent builder for coyote time and jump buffer forgiveness windows.
     */
    fun withTolerances(coyote: Float, buffer: Float): PlatformerPhysicsConfig = apply {
        this.coyoteTimeDuration = coyote
        this.jumpBufferDuration = buffer
    }

    /**
     * Fluent builder for friction and skidding deceleration.
     */
    fun withFriction(friction: Float, skid: Float): PlatformerPhysicsConfig = apply {
        this.groundFriction = friction
        this.skidDeceleration = skid
    }

    companion object {
        /**
         * Authentic NES Super Mario Bros physics preset.
         */
        val NES_CLASSIC = PlatformerPhysicsConfig()

        /**
         * Heavy-crate carrying platformer physics preset (inspired by Chip 'n Dale).
         */
        val RESCUE_RANGERS = PlatformerPhysicsConfig(
            walkAcceleration = 460f,
            runAcceleration = 700f,
            maxWalkSpeed = 105f,
            maxRunSpeed = 160f,
            groundFriction = 620f,
            skidDeceleration = 1000f,
            gravity = 980f,
            jumpImpulse = -285f,
            jumpCutGravityMultiplier = 2.2f,
            coyoteTimeDuration = 0.14f,
            jumpBufferDuration = 0.12f
        )
    }
}

/**
 * Character animation and action states matching NES platformer archetypes.
 */
enum class PlayerAnimationState(
    val isAirborne: Boolean = false,
    val isGrounded: Boolean = true,
    val canMove: Boolean = true,
    val canJump: Boolean = true
) {
    IDLE(isAirborne = false, isGrounded = true),
    WALK(isAirborne = false, isGrounded = true),
    RUN(isAirborne = false, isGrounded = true),
    SKID(isAirborne = false, isGrounded = true),
    JUMP_ASCENT(isAirborne = true, isGrounded = false, canJump = false),
    FALL(isAirborne = true, isGrounded = false, canJump = false),
    DUCK(isAirborne = false, isGrounded = true, canMove = false),
    CARRY(isAirborne = false, isGrounded = true),
    THROW(isAirborne = false, isGrounded = true),
    HURT(isAirborne = true, isGrounded = false, canMove = false, canJump = false),
    VICTORY_FLAG(isAirborne = false, isGrounded = true, canMove = false, canJump = false);

    val isMovingHorizontally: Boolean
        get() = this == WALK || this == RUN || this == SKID
}

/**
 * Horizontal facing direction for directional physics, sprite flipping, and crate projectile tosses.
 */
enum class PlayerFacing(val sign: Float) {
    LEFT(-1f),
    RIGHT(1f);

    val opposite: PlayerFacing
        get() = if (this == RIGHT) LEFT else RIGHT
}
