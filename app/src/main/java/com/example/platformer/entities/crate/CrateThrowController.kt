package com.example.platformer.entities.crate

import com.example.platformer.entities.PlatformerPlayer
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.ports.InputPort

/**
 * Controller managing aimed projectile throw physics and trajectory tracking
 * for Chip 'n Dale 2 style wooden crates in Super Pixie NES Platformer.
 *
 * Implements:
 * - Forward horizontal toss with gentle parabolic arc (`vx = facing * 210f, vy = -70f`).
 * - Upward aimed toss with steep high arc (`vx = facing * 40f, vy = -280f`) when aiming UP.
 * - Crate detachment, state transition to [CrateState.THROWN_AIRBORNE], and gravity integration.
 * - Sound playback via [AudioPort] and sound event callbacks.
 * - Active airborne projectile tracking and cleanup.
 * - Fluent, chainable builders adhering to AGENTS.md.
 */
class CrateThrowController(
    var forwardThrowVx: Float = FORWARD_THROW_VX,
    var forwardThrowVy: Float = FORWARD_THROW_VY,
    var upwardThrowVx: Float = UPWARD_THROW_VX,
    var upwardThrowVy: Float = UPWARD_THROW_VY,
    var throwGravity: Float = DEFAULT_THROW_GRAVITY,
    var audioPort: AudioPort? = null
) {
    private val _airborneCrates = mutableListOf<ThrowableCrate>()
    val activeAirborneCrates: List<ThrowableCrate>
        get() = _airborneCrates

    var onPlaySound: ((soundKey: String) -> Unit)? = null
    var onCrateThrown: ((crate: ThrowableCrate, velocity: Vec2, isAimedUp: Boolean) -> Unit)? = null

    /**
     * Calculates the initial launch velocity vector for a thrown crate
     * based on the actor's facing direction sign (-1f or +1f) and vertical aim.
     */
    fun calculateThrowVelocity(facingSign: Float, isAimingUp: Boolean): Vec2 {
        val sign = if (facingSign < 0f) -1f else 1f
        return if (isAimingUp) {
            Vec2(sign * upwardThrowVx, upwardThrowVy)
        } else {
            Vec2(sign * forwardThrowVx, forwardThrowVy)
        }
    }

    /**
     * Calculates launch velocity directly from a player's facing direction.
     */
    fun calculateThrowVelocity(player: PlatformerPlayer, isAimingUp: Boolean): Vec2 {
        return calculateThrowVelocity(player.physics.facing.sign, isAimingUp)
    }

    /**
     * Hurls the crate currently carried by the player as an airborne projectile.
     *
     * Detaches the crate from the player, updates its velocity and gravity,
     * transitions it to [CrateState.THROWN_AIRBORNE], triggers audio,
     * and tracks it in [activeAirborneCrates].
     *
     * @param player The carrier player actor.
     * @param isAimingUp True if directional input UP is pressed for high arc.
     * @return The thrown [ThrowableCrate], or null if the player was not carrying a crate.
     */
    fun throwCarriedCrate(player: PlatformerPlayer, isAimingUp: Boolean = false): ThrowableCrate? {
        val crate = player.carriedObject ?: return null
        val launchVelocity = calculateThrowVelocity(player, isAimingUp)

        // Detach from player and assign launch velocity
        player.throwCrate(launchVelocity)
        crate.gravity = throwGravity

        _airborneCrates.add(crate)
        playThrowSound()
        onCrateThrown?.invoke(crate, launchVelocity, isAimingUp)

        return crate
    }

    /**
     * Handles Action / Throw button press (<kbd>B</kbd>):
     * - If player is carrying: throws the carried crate with aimed trajectory.
     * - If player is empty-handed: attempts to pick up an adjacent intact crate.
     */
    fun handleActionInput(
        player: PlatformerPlayer,
        isAimingUp: Boolean,
        crates: Collection<ThrowableCrate> = emptyList()
    ): Boolean {
        return if (player.isCarrying) {
            throwCarriedCrate(player, isAimingUp) != null
        } else {
            player.tryPickUpAdjacentCrate(crates) != null
        }
    }

    /**
     * Overload accepting [InputPort] to automatically extract vertical aim and action state.
     */
    fun handleActionInput(
        player: PlatformerPlayer,
        input: InputPort,
        crates: Collection<ThrowableCrate> = emptyList()
    ): Boolean {
        if (!input.isActionPressed) return false
        val isAimingUp = input.moveY < -AIM_UP_THRESHOLD
        return handleActionInput(player, isAimingUp, crates)
    }

    /**
     * Advances simulation for all active thrown crates and prunes inactive/shattered ones.
     */
    fun update(dt: Float, context: GameContext) {
        val iterator = _airborneCrates.iterator()
        while (iterator.hasNext()) {
            val crate = iterator.next()
            if (crate.state != CrateState.THROWN_AIRBORNE || !crate.isAlive) {
                iterator.remove()
            } else {
                crate.update(dt, context)
            }
        }
    }

    /**
     * Registers an externally thrown crate for trajectory tracking.
     */
    fun registerThrownCrate(crate: ThrowableCrate) {
        if (crate.state == CrateState.THROWN_AIRBORNE && !_airborneCrates.contains(crate)) {
            _airborneCrates.add(crate)
        }
    }

    /**
     * Clears all tracked airborne projectiles.
     */
    fun clear() {
        _airborneCrates.clear()
    }

    private fun playThrowSound() {
        onPlaySound?.invoke(SOUND_CRATE_THROW)
        val port = audioPort ?: return
        try {
            val throwEvent = AudioEvent.valueOf("CRATE_THROW")
            port.playSound(throwEvent)
        } catch (_: IllegalArgumentException) {
            port.playSound(AudioEvent.FIRE_MISSILE)
        }
    }

    // --- Fluent Builders ---

    fun withForwardVelocity(vx: Float, vy: Float): CrateThrowController = apply {
        this.forwardThrowVx = vx
        this.forwardThrowVy = vy
    }

    fun withUpwardVelocity(vx: Float, vy: Float): CrateThrowController = apply {
        this.upwardThrowVx = vx
        this.upwardThrowVy = vy
    }

    fun withGravity(gravity: Float): CrateThrowController = apply {
        this.throwGravity = gravity
    }

    fun withAudioPort(port: AudioPort?): CrateThrowController = apply {
        this.audioPort = port
    }

    fun onPlaySound(listener: (soundKey: String) -> Unit): CrateThrowController = apply {
        this.onPlaySound = listener
    }

    fun onCrateThrown(listener: (crate: ThrowableCrate, velocity: Vec2, isAimedUp: Boolean) -> Unit): CrateThrowController = apply {
        this.onCrateThrown = listener
    }

    companion object {
        const val FORWARD_THROW_VX = 210f
        const val FORWARD_THROW_VY = -70f
        const val UPWARD_THROW_VX = 40f
        const val UPWARD_THROW_VY = -280f
        const val DEFAULT_THROW_GRAVITY = 650f
        const val AIM_UP_THRESHOLD = 0.3f
        const val SOUND_CRATE_THROW = "crate_throw"

        val DEFAULT = CrateThrowController()
    }
}
