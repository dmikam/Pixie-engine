package com.example.platformer.entities.crate

import com.example.platformer.entities.PlatformerPlayer
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.particle.ProceduralExplosion
import com.pixie.engine.core.platformer.InteractiveBlockController
import com.pixie.engine.core.platformer.PlatformerEnemy
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.tilemap.TileType
import com.pixie.engine.core.tilemap.TilemapChunkLayer
import com.pixie.engine.core.tilemap.TilemapCollisionQuery

private val RectF.center: Vec2
    get() = Vec2(centerX, centerY)

/**
 * Impact category for shattered crates.
 */
enum class CrateImpactReason {
    ENEMY_HIT,
    BRICK_HIT,
    WALL_HIT
}

/**
 * Result outcomes of a thrown crate projectile impact.
 */
sealed class CrateImpactResult {
    data object None : CrateImpactResult()

    data class EnemySmash(
        val crate: ThrowableCrate,
        val enemy: PlatformerEnemy,
        val points: Long = 200L
    ) : CrateImpactResult()

    data class BrickSmash(
        val crate: ThrowableCrate,
        val col: Int,
        val row: Int,
        val points: Long = 50L
    ) : CrateImpactResult()

    data class WallSmash(
        val crate: ThrowableCrate,
        val col: Int,
        val row: Int
    ) : CrateImpactResult()
}

/**
 * Collision resolver and impact coordinator for Chip 'n Dale 2 wooden crate projectiles
 * and defensive crate ducking.
 *
 * Implements:
 * - Impact against enemies: defeats enemy with upside-down fall hop, shatters crate
 *   into 4 spinning wooden splinters + spark particles, and awards 200 points.
 * - Impact against breakable BRICK blocks: shatters the brick, shatters crate, awards points.
 * - Impact against solid terrain/walls: shatters crate with impact audio and splinters.
 * - Crate-ducking defense: ducking while carrying covers the hero, repelling lateral enemy contact.
 * - Fluent, chainable builders conforming to AGENTS.md.
 */
class CrateImpactResolver(
    var particleLayer: ParticleLayer? = null,
    var audioPort: AudioPort? = null,
    var interactiveBlockController: InteractiveBlockController? = null,
    var enemyScoreAward: Long = DEFAULT_ENEMY_SCORE,
    var brickScoreAward: Long = DEFAULT_BRICK_SCORE,
    var splinterColor: Int = DEFAULT_SPLINTER_COLOR,
    var splinterGravity: Float = DEFAULT_SPLINTER_GRAVITY
) {
    // Event listeners
    var onEnemySmashed: ((crate: ThrowableCrate, enemy: PlatformerEnemy, points: Long) -> Unit)? = null
    var onBrickSmashed: ((crate: ThrowableCrate, col: Int, row: Int, points: Long) -> Unit)? = null
    var onWallSmashed: ((crate: ThrowableCrate, col: Int, row: Int) -> Unit)? = null
    var onCrateDuckedDefense: ((player: PlatformerPlayer, enemy: PlatformerEnemy) -> Unit)? = null
    var onPlaySound: ((soundKey: String) -> Unit)? = null

    // --- Fluent Builders ---

    fun withParticleLayer(layer: ParticleLayer?): CrateImpactResolver = apply {
        this.particleLayer = layer
    }

    fun withAudioPort(port: AudioPort?): CrateImpactResolver = apply {
        this.audioPort = port
    }

    fun withInteractiveBlockController(controller: InteractiveBlockController?): CrateImpactResolver = apply {
        this.interactiveBlockController = controller
    }

    fun withEnemyScoreAward(score: Long): CrateImpactResolver = apply {
        this.enemyScoreAward = score
    }

    fun withBrickScoreAward(score: Long): CrateImpactResolver = apply {
        this.brickScoreAward = score
    }

    fun onEnemySmashed(listener: (crate: ThrowableCrate, enemy: PlatformerEnemy, points: Long) -> Unit): CrateImpactResolver = apply {
        this.onEnemySmashed = listener
    }

    fun onBrickSmashed(listener: (crate: ThrowableCrate, col: Int, row: Int, points: Long) -> Unit): CrateImpactResolver = apply {
        this.onBrickSmashed = listener
    }

    fun onWallSmashed(listener: (crate: ThrowableCrate, col: Int, row: Int) -> Unit): CrateImpactResolver = apply {
        this.onWallSmashed = listener
    }

    fun onCrateDuckedDefense(listener: (player: PlatformerPlayer, enemy: PlatformerEnemy) -> Unit): CrateImpactResolver = apply {
        this.onCrateDuckedDefense = listener
    }

    fun onPlaySound(listener: (soundKey: String) -> Unit): CrateImpactResolver = apply {
        this.onPlaySound = listener
    }

    // --- Crate vs Enemy Resolution ---

    fun resolveCrateVsEnemy(
        crate: ThrowableCrate,
        enemy: PlatformerEnemy,
        context: GameContext? = null
    ): CrateImpactResult {
        if (crate.state != CrateState.THROWN_AIRBORNE || !crate.isAlive) {
            return CrateImpactResult.None
        }
        if (!enemy.isAlive || enemy.isDefeated) {
            return CrateImpactResult.None
        }
        if (!crate.boundingBox.intersects(enemy.effectiveHitbox)) {
            return CrateImpactResult.None
        }

        // Defeat enemy with fall hop
        val hitDir = if (crate.velocity.x != 0f) crate.velocity.x else 1f
        enemy.defeatWithFall(hitDirection = hitDir, verticalImpulse = ENEMY_DEFEAT_IMPULSE_Y)

        // Shatter crate and spawn wooden splinters + spark explosion
        crate.shatter()
        spawnCrateShatterEffects(crate.boundingBox.center)

        // Award score
        context?.let { it.score += enemyScoreAward }

        // Audio & Event hooks
        audioPort?.playSound(AudioEvent.HIT)
        onPlaySound?.invoke(SOUND_CRATE_SMASH)
        onEnemySmashed?.invoke(crate, enemy, enemyScoreAward)

        return CrateImpactResult.EnemySmash(crate, enemy, enemyScoreAward)
    }

    fun resolveCrateVsEnemies(
        crate: ThrowableCrate,
        enemies: List<PlatformerEnemy>,
        context: GameContext? = null
    ): CrateImpactResult {
        for (i in enemies.indices) {
            val result = resolveCrateVsEnemy(crate, enemies[i], context)
            if (result !is CrateImpactResult.None) {
                return result
            }
        }
        return CrateImpactResult.None
    }

    // --- Crate vs Tilemap Resolution ---

    fun resolveCrateVsTilemap(
        crate: ThrowableCrate,
        tilemap: TilemapChunkLayer,
        context: GameContext? = null
    ): CrateImpactResult {
        if (crate.state != CrateState.THROWN_AIRBORNE || !crate.isAlive) {
            return CrateImpactResult.None
        }

        val range = TilemapCollisionQuery.computeGridRange(
            crate.boundingBox,
            tilemap.tileSize,
            tilemap.cols,
            tilemap.rows
        )
        if (!range.isValid) return CrateImpactResult.None

        var hitResult: CrateImpactResult = CrateImpactResult.None
        range.forEachCell { col, row ->
            if (hitResult !is CrateImpactResult.None) return@forEachCell
            val tile = tilemap.getTile(col, row) ?: return@forEachCell
            val tileBounds = tilemap.getTileBounds(col, row)
            if (!crate.boundingBox.intersects(tileBounds)) return@forEachCell

            if (tile.type == TileType.BRICK) {
                hitResult = handleBrickImpact(crate, col, row, tilemap, context)
            } else if (tile.isSolid && !tile.isOneWay) {
                hitResult = handleWallImpact(crate, col, row, context)
            }
        }
        return hitResult
    }

    private fun handleBrickImpact(
        crate: ThrowableCrate,
        col: Int,
        row: Int,
        tilemap: TilemapChunkLayer,
        context: GameContext?
    ): CrateImpactResult {
        // Destroy brick
        val controller = interactiveBlockController
        if (controller != null) {
            controller.shatterBrick(col, row, context)
        } else {
            tilemap.setTile(col, row, TileType.EMPTY)
            context?.let { it.score += brickScoreAward }
            val center = Vec2(col * tilemap.tileSize + 8f, row * tilemap.tileSize + 8f)
            particleLayer?.spawnBrickShards(center)
        }

        // Shatter crate
        crate.shatter()
        spawnCrateShatterEffects(crate.boundingBox.center)

        audioPort?.playSound(AudioEvent.HIT)
        onPlaySound?.invoke(SOUND_BREAK_BLOCK)
        onBrickSmashed?.invoke(crate, col, row, brickScoreAward)

        return CrateImpactResult.BrickSmash(crate, col, row, brickScoreAward)
    }

    private fun handleWallImpact(
        crate: ThrowableCrate,
        col: Int,
        row: Int,
        context: GameContext?
    ): CrateImpactResult {
        crate.shatter()
        spawnCrateShatterEffects(crate.boundingBox.center)

        audioPort?.playSound(AudioEvent.HIT)
        onPlaySound?.invoke(SOUND_CRATE_SMASH)
        onWallSmashed?.invoke(crate, col, row)

        return CrateImpactResult.WallSmash(crate, col, row)
    }

    // --- Crate Ducking Defense ---

    /**
     * Checks if the player is currently ducking inside/under a carried crate.
     */
    fun isCrateShieldActive(player: PlatformerPlayer): Boolean {
        return player.isCarrying && player.isDucking && player.carriedObject?.isAlive == true
    }

    /**
     * Defends against lateral enemy contact when ducking while carrying a crate.
     * Repels the enemy, triggers audio and haptics, and nullifies player damage.
     */
    fun resolveDuckingDefense(
        player: PlatformerPlayer,
        enemy: PlatformerEnemy,
        context: GameContext? = null
    ): Boolean {
        if (!isCrateShieldActive(player)) return false
        if (!enemy.isAlive || enemy.isDefeated) return false
        if (!player.bounds.intersects(enemy.effectiveHitbox)) return false

        if (enemy.isPatrolling || enemy.isShellSliding) {
            enemy.reverseDirection()
        }

        audioPort?.playSound(AudioEvent.HIT)
        onPlaySound?.invoke(SOUND_BUMP)
        onCrateDuckedDefense?.invoke(player, enemy)
        return true
    }

    // --- Particle Effects Spawning ---

    /**
     * Spawns spark hit explosion and 4 spinning wooden splinters launching outward.
     */
    fun spawnCrateShatterEffects(position: Vec2) {
        val layer = particleLayer ?: return
        layer.spawnExplosion(ExplosionType.SPARK_HIT, position)
        layer.spawnBrickShards(position, gravity = splinterGravity, color = splinterColor)
    }

    // --- Batch Simulation Update ---

    /**
     * Updates and resolves all active thrown airborne crates against enemies and tilemap.
     */
    fun update(
        crates: List<ThrowableCrate>,
        enemies: List<PlatformerEnemy>,
        tilemap: TilemapChunkLayer?,
        context: GameContext? = null
    ): List<CrateImpactResult> {
        val results = mutableListOf<CrateImpactResult>()
        for (i in crates.indices) {
            val crate = crates[i]
            if (crate.state != CrateState.THROWN_AIRBORNE || !crate.isAlive) continue

            // 1. Check enemy impacts
            val enemyHit = resolveCrateVsEnemies(crate, enemies, context)
            if (enemyHit !is CrateImpactResult.None) {
                results.add(enemyHit)
                continue
            }

            // 2. Check tilemap impacts
            if (tilemap != null) {
                val tileHit = resolveCrateVsTilemap(crate, tilemap, context)
                if (tileHit !is CrateImpactResult.None) {
                    results.add(tileHit)
                }
            }
        }
        return results
    }

    companion object {
        const val DEFAULT_ENEMY_SCORE = 200L
        const val DEFAULT_BRICK_SCORE = 50L
        const val ENEMY_DEFEAT_IMPULSE_Y = -160f
        const val DEFAULT_SPLINTER_GRAVITY = 550f
        const val DEFAULT_SPLINTER_COLOR = 0xFFA0551E.toInt() // Warm wood plank

        const val SOUND_CRATE_SMASH = "crate_smash"
        const val SOUND_BREAK_BLOCK = "break_block"
        const val SOUND_BUMP = "bump"

        val DEFAULT = CrateImpactResolver()
    }
}
