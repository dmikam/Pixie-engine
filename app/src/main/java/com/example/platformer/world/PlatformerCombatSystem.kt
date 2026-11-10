package com.example.platformer.world

import com.example.platformer.entities.KoopaEnemy
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.platformer.PlatformerEnemy
import com.pixie.engine.core.platformer.PlatformerEnemyState
import com.pixie.engine.core.platformer.PlayerTier
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort

/**
 * Combat interface contract for a platformer hero interacting with enemies and hazards.
 */
interface PlatformerCombatPlayer {
    val id: String
    var position: Vec2
    var velocity: Vec2
    var size: Vec2
    val bounds: RectF
        get() = RectF.fromTopLeft(position.x, position.y, size.x, size.y)
    var tier: PlayerTier
    var isInvulnerable: Boolean
    var invulnerabilityTimer: Float
    var isAlive: Boolean
    var isDefeated: Boolean
    var isJumpHeld: Boolean

    val velocityX: Float get() = velocity.x
    val velocityY: Float get() = velocity.y
    val isCrateShielded: Boolean get() = false

    fun applyStompRebound(reboundVelocityY: Float)
    fun downgradeTier(): PlayerTier
    fun grantInvulnerability(duration: Float)
    fun defeat(context: GameContext? = null)
}

/**
 * Result outcome of a combat collision between player and enemy.
 */
sealed class CombatCollisionResult {
    data object None : CombatCollisionResult()

    data class Stomp(
        val enemy: PlatformerEnemy,
        val reboundVelocityY: Float,
        val isBoosted: Boolean
    ) : CombatCollisionResult()

    data class ShellKicked(
        val enemy: PlatformerEnemy,
        val direction: Float
    ) : CombatCollisionResult()

    data class PlayerDamaged(
        val enemy: PlatformerEnemy,
        val oldTier: PlayerTier,
        val newTier: PlayerTier,
        val invulnerabilityDuration: Float
    ) : CombatCollisionResult()

    data class PlayerDefeated(
        val enemy: PlatformerEnemy,
        val deathHopVelocity: Vec2
    ) : CombatCollisionResult()
}

/**
 * High-performance collision resolver for authentic NES platformer combat dynamics.
 *
 * Resolves:
 * - Stomp (Top Hit): Landing on enemy while falling down triggers squashing/retracting,
 *   rebound impulse, audio event, and hit spark particles.
 * - Lateral Contact (Damage): Touching enemy sides or bottom damages or defeats player,
 *   or kicks stationary shells.
 * - Enemy-vs-Enemy Contact: Sliding shells defeat walking enemies; patrolling walkers bounce.
 */
class PlatformerCombatSystem(
    var particleLayer: ParticleLayer? = null,
    var audioPort: AudioPort? = null
) {
    var onStomp: ((enemy: PlatformerEnemy, reboundVy: Float, isBoosted: Boolean) -> Unit)? = null
    var onShellKicked: ((enemy: PlatformerEnemy, direction: Float) -> Unit)? = null
    var onPlayerHurt: ((enemy: PlatformerEnemy, oldTier: PlayerTier, newTier: PlayerTier) -> Unit)? = null
    var onPlayerDefeated: ((enemy: PlatformerEnemy) -> Unit)? = null
    var onCrateDefended: ((enemy: PlatformerEnemy) -> Unit)? = null
    var onPlaySound: ((soundKey: String) -> Unit)? = null

    // --- Fluent Configuration Builders ---

    fun withParticleLayer(layer: ParticleLayer): PlatformerCombatSystem = apply {
        this.particleLayer = layer
    }

    fun withAudioPort(port: AudioPort): PlatformerCombatSystem = apply {
        this.audioPort = port
    }

    fun onStomp(listener: (enemy: PlatformerEnemy, reboundVy: Float, isBoosted: Boolean) -> Unit): PlatformerCombatSystem = apply {
        this.onStomp = listener
    }

    fun onShellKicked(listener: (enemy: PlatformerEnemy, direction: Float) -> Unit): PlatformerCombatSystem = apply {
        this.onShellKicked = listener
    }

    fun onPlayerHurt(listener: (enemy: PlatformerEnemy, oldTier: PlayerTier, newTier: PlayerTier) -> Unit): PlatformerCombatSystem = apply {
        this.onPlayerHurt = listener
    }

    fun onPlayerDefeated(listener: (enemy: PlatformerEnemy) -> Unit): PlatformerCombatSystem = apply {
        this.onPlayerDefeated = listener
    }

    fun onCrateDefended(listener: (enemy: PlatformerEnemy) -> Unit): PlatformerCombatSystem = apply {
        this.onCrateDefended = listener
    }

    fun onPlaySound(listener: (soundKey: String) -> Unit): PlatformerCombatSystem = apply {
        this.onPlaySound = listener
    }

    // --- Core Collision Resolution ---

    fun resolvePlayerEnemyCollision(
        player: PlatformerCombatPlayer,
        enemy: PlatformerEnemy,
        context: GameContext? = null
    ): CombatCollisionResult {
        if (!player.isAlive || player.isDefeated || !enemy.isAlive || enemy.isDefeated) {
            return CombatCollisionResult.None
        }

        val playerBox = player.bounds
        val enemyBox = enemy.effectiveHitbox
        if (!playerBox.intersects(enemyBox)) {
            return CombatCollisionResult.None
        }

        val isFalling = player.velocityY > 0f
        val isAboveEnemyTop = playerBox.bottom <= enemyBox.top + STOMP_OVERLAP_TOLERANCE_Y

        return if (isFalling && isAboveEnemyTop) {
            handleStomp(player, enemy, playerBox, enemyBox, context)
        } else {
            handleLateralHit(player, enemy, playerBox, enemyBox, context)
        }
    }

    private fun handleStomp(
        player: PlatformerCombatPlayer,
        enemy: PlatformerEnemy,
        playerBox: RectF,
        enemyBox: RectF,
        context: GameContext?
    ): CombatCollisionResult {
        enemy.onStomped(player as? PixieObject, context)

        val isBoosted = player.isJumpHeld
        val reboundVy = if (isBoosted) STOMP_REBOUND_BOOSTED_VY else STOMP_REBOUND_NORMAL_VY
        player.applyStompRebound(reboundVy)

        spawnStompEffects(playerBox.centerX, enemyBox.top)
        onStomp?.invoke(enemy, reboundVy, isBoosted)

        return CombatCollisionResult.Stomp(enemy, reboundVy, isBoosted)
    }

    private fun spawnStompEffects(centerX: Float, topY: Float) {
        val sparkPos = Vec2(centerX, topY)
        particleLayer?.spawnSparks(sparkPos, count = SPARK_PARTICLE_COUNT)
        audioPort?.playSound(AudioEvent.HIT)
        onPlaySound?.invoke("stomp")
    }

    private fun handleLateralHit(
        player: PlatformerCombatPlayer,
        enemy: PlatformerEnemy,
        playerBox: RectF,
        enemyBox: RectF,
        context: GameContext?
    ): CombatCollisionResult {
        if (enemy.canBeKicked || enemy.state == PlatformerEnemyState.SHELL_STATIONARY) {
            return kickStationaryShell(player, enemy, playerBox, enemyBox, context)
        }

        if (enemy is KoopaEnemy && enemy.kickSafetyTimer > 0f && enemy.lastKicker === player) {
            return CombatCollisionResult.None
        }

        if (player.isCrateShielded) {
            if (enemy.isPatrolling || enemy.isShellSliding) {
                enemy.reverseDirection()
            }
            audioPort?.playSound(AudioEvent.HIT)
            onPlaySound?.invoke("bump")
            onCrateDefended?.invoke(enemy)
            return CombatCollisionResult.None
        }

        if (!enemy.dealsContactDamage || player.isInvulnerable || player.invulnerabilityTimer > 0f) {
            return CombatCollisionResult.None
        }

        return applyDamageToPlayer(player, enemy, context)
    }

    private fun kickStationaryShell(
        player: PlatformerCombatPlayer,
        enemy: PlatformerEnemy,
        playerBox: RectF,
        enemyBox: RectF,
        context: GameContext?
    ): CombatCollisionResult {
        val kickDir = if (playerBox.centerX <= enemyBox.centerX) 1f else -1f
        enemy.kickShell(kickDir, player as? PixieObject, context)
        audioPort?.playSound(AudioEvent.HIT)
        onPlaySound?.invoke("kick")
        onShellKicked?.invoke(enemy, kickDir)
        return CombatCollisionResult.ShellKicked(enemy, kickDir)
    }

    private fun applyDamageToPlayer(
        player: PlatformerCombatPlayer,
        enemy: PlatformerEnemy,
        context: GameContext?
    ): CombatCollisionResult {
        return if (player.tier == PlayerTier.SUPER) {
            val oldTier = player.tier
            val newTier = player.downgradeTier()
            player.grantInvulnerability(HURT_INVULNERABILITY_DURATION)
            audioPort?.playSound(AudioEvent.HIT)
            onPlaySound?.invoke("pipe")
            onPlayerHurt?.invoke(enemy, oldTier, newTier)
            CombatCollisionResult.PlayerDamaged(enemy, oldTier, newTier, HURT_INVULNERABILITY_DURATION)
        } else {
            player.defeat(context)
            audioPort?.playSound(AudioEvent.GAME_OVER)
            onPlaySound?.invoke("death")
            onPlayerDefeated?.invoke(enemy)
            CombatCollisionResult.PlayerDefeated(enemy, Vec2(0f, DEATH_HOP_VELOCITY_Y))
        }
    }

    // --- Group & Batch Resolution ---

    fun resolvePlayerVsEnemies(
        player: PlatformerCombatPlayer,
        enemies: List<PlatformerEnemy>,
        context: GameContext? = null
    ): List<CombatCollisionResult> {
        val results = mutableListOf<CombatCollisionResult>()
        for (i in enemies.indices) {
            val result = resolvePlayerEnemyCollision(player, enemies[i], context)
            if (result !is CombatCollisionResult.None) {
                results.add(result)
            }
        }
        return results
    }

    fun resolveEnemyCollisions(
        enemies: List<PlatformerEnemy>,
        context: GameContext? = null
    ) {
        val count = enemies.size
        for (i in 0 until count) {
            val e1 = enemies[i]
            if (!e1.isAlive || e1.isDefeated) continue

            for (j in (i + 1) until count) {
                val e2 = enemies[j]
                if (!e2.isAlive || e2.isDefeated) continue

                if (e1.effectiveHitbox.intersects(e2.effectiveHitbox)) {
                    resolveEnemyPairContact(e1, e2, context)
                }
            }
        }
    }

    private fun resolveEnemyPairContact(
        e1: PlatformerEnemy,
        e2: PlatformerEnemy,
        context: GameContext?
    ) {
        when {
            e1.isShellSliding && e1 is KoopaEnemy -> e1.handleEnemyContact(e2, context)
            e2.isShellSliding && e2 is KoopaEnemy -> e2.handleEnemyContact(e1, context)
            e1.isPatrolling && e2.isPatrolling -> {
                e1.reverseDirection()
                e2.reverseDirection()
            }
        }
    }

    fun update(
        dt: Float,
        player: PlatformerCombatPlayer,
        enemies: List<PlatformerEnemy>,
        context: GameContext? = null
    ): List<CombatCollisionResult> {
        val results = resolvePlayerVsEnemies(player, enemies, context)
        resolveEnemyCollisions(enemies, context)
        return results
    }

    companion object {
        const val STOMP_OVERLAP_TOLERANCE_Y = 8f
        const val STOMP_REBOUND_NORMAL_VY = -210f
        const val STOMP_REBOUND_BOOSTED_VY = -280f
        const val HURT_INVULNERABILITY_DURATION = 1.5f
        const val DEATH_HOP_VELOCITY_Y = -240f
        const val SPARK_PARTICLE_COUNT = 6
    }
}
