package com.example.platformer.entities

import com.example.platformer.entities.crate.CrateState
import com.example.platformer.entities.crate.CrateThrowController
import com.example.platformer.entities.crate.ThrowableCrate
import com.example.platformer.world.PlatformerCombatPlayer
import com.example.platformer.world.PlatformerCombatSystem
import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixiePlayer
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.platformer.PlatformerPhysicsController
import com.pixie.engine.core.platformer.PlayerAnimationState
import com.pixie.engine.core.platformer.PlayerFacing
import com.pixie.engine.core.platformer.PlayerTier
import com.pixie.engine.core.ports.RenderBatchPort

/**
 * Super Pixie hero entity for the NES platformer.
 *
 * Implements [PlatformerCombatPlayer] and encapsulates:
 * - Progression tier ([PlayerTier.SMALL] vs [PlayerTier.SUPER])
 * - Physics synchronization via [PlatformerPhysicsController]
 * - Chip 'n Dale 2 overhead crate carrying and throwing states
 * - Stomp bounce and damage reactions
 * - Invulnerability frames and death hop arcs
 */
class PlatformerPlayer(
    id: String = "platformer_player_${System.nanoTime()}",
    position: Vec2 = Vec2.ZERO,
    initialTier: PlayerTier = PlayerTier.SMALL,
    initialLives: Int = 3,
    val physics: PlatformerPhysicsController = PlatformerPhysicsController()
) : PixiePlayer(
    id = id,
    position = position,
    size = Vec2(initialTier.hitboxWidth, initialTier.hitboxHeight),
    initialHealth = 100f,
    initialLives = initialLives,
    initialClasses = setOf("player", "platformer_hero")
), PlatformerCombatPlayer {

    override var tier: PlayerTier = initialTier
        set(value) {
            field = value
            size = Vec2(value.hitboxWidth, value.hitboxHeight)
            physics.setTier(value)
        }

    override var isDefeated: Boolean = false
    override var isJumpHeld: Boolean = false
        get() = physics.jumpHeld
        set(value) {
            field = value
            physics.setJumpHeld(value)
        }

    override val bounds: RectF
        get() = RectF.fromTopLeft(position.x, position.y, size.x, size.y)

    var deathHopGravity: Float = 700f

    // --- Crate Carry & Interaction Subsystem ---

    var carriedObject: ThrowableCrate? = null
        private set

    val isCarrying: Boolean
        get() = carriedObject != null || physics.isCarrying

    var isDucking: Boolean
        get() = physics.isDucking
        set(value) {
            physics.withDuck(value)
        }

    override val isCrateShielded: Boolean
        get() = isCarrying && isDucking && carriedObject != null && carriedObject?.isAlive == true

    var carryOffset: Vec2 = DEFAULT_CARRY_OFFSET

    val animationState: PlayerAnimationState
        get() = physics.state

    init {
        physics.withPosition(position)
        physics.withTier(initialTier)
    }

    override fun applyStompRebound(reboundVelocityY: Float) {
        velocity = Vec2(velocity.x, reboundVelocityY)
        physics.velocity = Vec2(physics.velocity.x, reboundVelocityY)
        physics.setGrounded(false)
    }

    override fun downgradeTier(): PlayerTier {
        val newTier = tier.downgrade()
        tier = newTier
        return newTier
    }

    fun upgradeTier(): PlayerTier {
        val newTier = tier.upgrade()
        tier = newTier
        return newTier
    }

    override fun grantInvulnerability(duration: Float) {
        super.grantInvulnerability(duration)
    }

    override fun defeat(context: GameContext?) {
        dropCrate(placeOnGround = false)
        isAlive = false
        isDefeated = true
        velocity = Vec2(0f, PlatformerCombatSystem.DEATH_HOP_VELOCITY_Y)
        physics.velocity = Vec2(0f, PlatformerCombatSystem.DEATH_HOP_VELOCITY_Y)
        physics.setGrounded(false)
    }

    // --- Crate Handling Operations ---

    /**
     * Picks up a crate and anchors it directly overhead above the player's head.
     */
    fun pickUpCrate(crate: ThrowableCrate): Boolean {
        if (carriedObject != null || !crate.isAlive || crate.state != CrateState.PLACED_ON_GROUND) {
            return false
        }
        carriedObject = crate
        physics.withCarry(true)
        crate.carryOffset = carryOffset
        crate.pickUp(this)
        crate.position = Vec2(position.x + carryOffset.x, position.y + carryOffset.y)
        physics.updateStateFromResolution()
        return true
    }

    /**
     * Places down or drops currently carried crate.
     */
    fun dropCrate(placeOnGround: Boolean = true): ThrowableCrate? {
        val crate = carriedObject ?: return null
        carriedObject = null
        physics.withCarry(false)
        crate.carrier = null
        if (placeOnGround) {
            val dropX = position.x + (physics.facing.sign * (size.x + 2f))
            crate.placeOnGround(Vec2(dropX, position.y))
        }
        physics.updateStateFromResolution()
        return crate
    }

    /**
     * Hurls the currently carried crate as a projectile with the given velocity.
     */
    fun throwCrate(throwVelocity: Vec2): ThrowableCrate? {
        val crate = carriedObject ?: return null
        carriedObject = null
        physics.withCarry(false)
        crate.throwCrate(throwVelocity)
        physics.updateStateFromResolution()
        return crate
    }

    /**
     * Determines whether an intact ground crate is within reach distance.
     */
    fun canReachCrate(crate: ThrowableCrate, reachDistance: Float = DEFAULT_REACH_DISTANCE): Boolean {
        if (!crate.isAlive || crate.state != CrateState.PLACED_ON_GROUND) return false
        val playerBox = bounds
        val reachLeft = if (physics.facing == PlayerFacing.RIGHT) playerBox.left else playerBox.left - reachDistance
        val reachBox = RectF.fromTopLeft(
            x = reachLeft,
            y = playerBox.top - 4f,
            width = playerBox.width + reachDistance,
            height = playerBox.height + 8f
        )
        return reachBox.intersects(crate.boundingBox)
    }

    /**
     * Attempts to find and pick up an adjacent intact crate from the scene.
     */
    fun tryPickUpAdjacentCrate(
        crates: Collection<ThrowableCrate>,
        reachDistance: Float = DEFAULT_REACH_DISTANCE
    ): ThrowableCrate? {
        if (carriedObject != null) return null
        val candidate = crates.firstOrNull { crate -> canReachCrate(crate, reachDistance) }
        return if (candidate != null && pickUpCrate(candidate)) candidate else null
    }

    /**
     * Hurls the carried crate with aimed arc trajectory.
     */
    fun throwCrateWithAim(
        isAimingUp: Boolean = false,
        controller: CrateThrowController = CrateThrowController.DEFAULT
    ): ThrowableCrate? {
        return controller.throwCarriedCrate(this, isAimingUp)
    }

    /**
     * Handles Action / Throw (B) button press:
     * - If carrying: throws the carried crate (forward arc or upward arc if [isAimingUp] is true).
     * - If not carrying: checks for and picks up adjacent crate.
     */
    fun onActionPress(
        crates: Collection<ThrowableCrate> = emptyList(),
        isAimingUp: Boolean = false,
        customThrowVelocity: Vec2? = null
    ): Boolean {
        return if (isCarrying) {
            val throwVel = customThrowVelocity ?: if (isAimingUp) {
                Vec2(physics.facing.sign * CrateThrowController.UPWARD_THROW_VX, CrateThrowController.UPWARD_THROW_VY)
            } else {
                Vec2(physics.facing.sign * CrateThrowController.FORWARD_THROW_VX, CrateThrowController.FORWARD_THROW_VY)
            }
            throwCrate(throwVel) != null
        } else {
            tryPickUpAdjacentCrate(crates) != null
        }
    }

    override fun update(dt: Float, context: GameContext) {
        if (isDefeated) {
            updateDeathHop(dt)
            return
        }

        super.update(dt, context)
        // Keep physics controller in sync with entity position/velocity
        position = physics.position
        velocity = physics.velocity

        // Keep carried crate overhead in sync with player position (pulled over player when ducking)
        carriedObject?.let { crate ->
            if (crate.state == CrateState.CARRIED) {
                val currentOffset = if (isDucking) DUCK_CARRY_OFFSET else carryOffset
                crate.position = Vec2(position.x + currentOffset.x, position.y + currentOffset.y)
                crate.velocity = velocity
            } else {
                carriedObject = null
                physics.withCarry(false)
            }
        }
    }

    private fun updateDeathHop(dt: Float) {
        val nextVy = velocity.y + deathHopGravity * dt
        velocity = Vec2(0f, nextVy)
        position += velocity * dt
        physics.position = position
        physics.velocity = velocity
    }

    fun render(batch: RenderBatchPort, camera: PixieCamera) {
        if (!isVisible) return
        val screenPos = camera.worldToScreen(position)
        val color = if (tier == PlayerTier.SUPER) 0xFFFF5722.toInt() else 0xFFE91E63.toInt()
        batch.drawPixelRect(
            x = screenPos.x,
            y = screenPos.y,
            width = size.x,
            height = size.y,
            color = color
        )
    }

    // --- Fluent Builders ---

    fun withPosition(pos: Vec2): PlatformerPlayer = apply {
        this.position = pos
        physics.withPosition(pos)
    }

    fun withVelocity(vel: Vec2): PlatformerPlayer = apply {
        this.velocity = vel
        physics.withVelocity(vel)
    }

    fun withPlayerTier(tier: PlayerTier): PlatformerPlayer = apply {
        this.tier = tier
    }

    fun withJumpHeld(held: Boolean): PlatformerPlayer = apply {
        this.isJumpHeld = held
    }

    fun withFacing(facing: PlayerFacing): PlatformerPlayer = apply {
        this.physics.withFacing(facing)
    }

    fun withCarriedCrate(crate: ThrowableCrate?): PlatformerPlayer = apply {
        if (crate != null) {
            pickUpCrate(crate)
        } else {
            dropCrate(placeOnGround = false)
        }
    }

    fun withCarryOffset(offset: Vec2): PlatformerPlayer = apply {
        this.carryOffset = offset
        carriedObject?.carryOffset = offset
    }

    fun withDucking(ducking: Boolean): PlatformerPlayer = apply {
        this.isDucking = ducking
    }

    companion object {
        val DEFAULT_CARRY_OFFSET = Vec2(0f, -14f)
        val DUCK_CARRY_OFFSET = Vec2(0f, 0f)
        const val DEFAULT_REACH_DISTANCE = 16f
    }
}
