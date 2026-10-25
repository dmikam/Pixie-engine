package com.example.raptor.world

import com.example.raptor.entities.EnemyShip
import com.example.raptor.entities.EnemyType
import com.example.raptor.entities.GroundDestructible
import com.example.raptor.entities.RaptorBoss
import com.example.raptor.entities.RaptorShip
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.weapon.Projectile

/**
 * Handles combat collision detection and damage resolution in Raptor.
 *
 * Evaluates projectile impacts (player vs boss/enemies/ground, enemies vs player),
 * ramming impacts with spark dissipation and invulnerability frames, and chain-reaction AOE explosions.
 */
class RaptorCollisionSystem(
    private val context: GameContext,
    private val particleLayer: ParticleLayer
) {
    fun resolveCombatCollisions(
        playerShip: RaptorShip,
        boss: RaptorBoss?,
        enemies: List<EnemyShip>,
        groundTargets: List<GroundDestructible>,
        playerProjectiles: List<Projectile>,
        enemyProjectiles: List<Projectile>
    ): RaptorCollisionSystem = apply {
        resolvePlayerProjectiles(playerProjectiles, boss, enemies, groundTargets)
        resolveEnemyProjectiles(enemyProjectiles, playerShip)
        resolveRammingCollisions(playerShip, boss, enemies)
    }

    private fun resolvePlayerProjectiles(
        playerProjectiles: List<Projectile>,
        boss: RaptorBoss?,
        enemies: List<EnemyShip>,
        groundTargets: List<GroundDestructible>
    ) {
        for (bullet in playerProjectiles) {
            if (!bullet.isAlive) continue

            if (checkBossHit(bullet, boss)) continue
            if (checkEnemyHits(bullet, enemies)) continue
            checkGroundHits(bullet, groundTargets)
        }
    }

    private fun checkBossHit(bullet: Projectile, boss: RaptorBoss?): Boolean {
        if (boss != null && boss.isAlive && bullet.boundingBox.intersects(boss.boundingBox)) {
            boss.takeDamage(bullet.hitPower, bullet.position, particleLayer)
            bullet.onImpact(bullet.position, particleLayer)
            return true
        }
        return false
    }

    private fun checkEnemyHits(bullet: Projectile, enemies: List<EnemyShip>): Boolean {
        for (enemy in enemies) {
            if (enemy.isAlive && bullet.boundingBox.intersects(enemy.boundingBox)) {
                val killed = enemy.takeDamage(bullet.hitPower, particleLayer)
                if (killed) {
                    context.score += enemy.scoreAward
                    context.earnCredits(enemy.scoreAward / 2)
                }
                bullet.onImpact(bullet.position, particleLayer)
                if (!bullet.isAlive) return true
            }
        }
        return !bullet.isAlive
    }

    private fun checkGroundHits(bullet: Projectile, groundTargets: List<GroundDestructible>) {
        if (!bullet.isAlive) return
        for (target in groundTargets) {
            if (!target.isDestroyed && bullet.boundingBox.intersects(target.boundingBox)) {
                val destroyed = target.takeDamage(bullet.hitPower, particleLayer)
                if (destroyed) {
                    context.score += target.type.scoreValue
                    context.earnCredits(target.type.scoreValue / 2)
                }
                bullet.onImpact(bullet.position, particleLayer)
                if (!bullet.isAlive) break
            }
        }
    }

    private fun resolveEnemyProjectiles(
        enemyProjectiles: List<Projectile>,
        playerShip: RaptorShip
    ) {
        if (!playerShip.isAlive) return
        val playerBox = playerShip.boundingBox
        for (bullet in enemyProjectiles) {
            if (bullet.isAlive && playerBox.intersects(bullet.boundingBox)) {
                playerShip.takeDamage(bullet.hitPower, particleLayer)
                bullet.onImpact(bullet.position, particleLayer)
            }
        }
    }

    private fun resolveRammingCollisions(
        playerShip: RaptorShip,
        boss: RaptorBoss?,
        enemies: List<EnemyShip>
    ) {
        if (!playerShip.isAlive || playerShip.isInvulnerable) return
        val playerBox = playerShip.boundingBox

        for (enemy in enemies) {
            if (enemy.isAlive && !enemy.isWreck && playerBox.intersects(enemy.boundingBox)) {
                handlePlayerVsEnemyRam(playerShip, enemy)
                break
            }
        }

        if (boss != null && boss.isAlive && !boss.isDefeated && playerBox.intersects(boss.boundingBox)) {
            handlePlayerVsBossRam(playerShip, boss)
        }
    }

    private fun handlePlayerVsEnemyRam(playerShip: RaptorShip, enemy: EnemyShip) {
        val playerHitAmount = when (enemy.type) {
            EnemyType.SCOUT -> 25f
            EnemyType.INTERCEPTOR -> 35f
            EnemyType.GUNSHIP -> 45f
            EnemyType.STEALTH_BOMBER -> 55f
        }
        playerShip.takeDamage(playerHitAmount, particleLayer)
        playerShip.grantInvulnerability(1.0f)

        val killed = enemy.takeDamage(50f, particleLayer)
        if (killed) {
            context.score += enemy.scoreAward
            context.earnCredits(enemy.scoreAward / 2)
        }
        particleLayer.spawnSparks(
            playerShip.position + (playerShip.size * 0.5f),
            count = 12,
            color = 0xFFFF5722.toInt()
        )
    }

    private fun handlePlayerVsBossRam(playerShip: RaptorShip, boss: RaptorBoss) {
        playerShip.takeDamage(40f, particleLayer)
        playerShip.grantInvulnerability(1.2f)
        boss.takeDamage(35f, playerShip.position, particleLayer)
        particleLayer.spawnSparks(playerShip.position, count = 14, color = 0xFFFF5722.toInt())
    }

    fun triggerAoeDamage(
        center: Vec2,
        radius: Float,
        damage: Float,
        enemies: List<EnemyShip>,
        groundTargets: List<GroundDestructible>
    ): RaptorCollisionSystem = apply {
        val radiusSq = radius * radius
        for (enemy in enemies) {
            if (enemy.isAlive && (enemy.position - center).lengthSquared() <= radiusSq) {
                val killed = enemy.takeDamage(damage, particleLayer)
                if (killed) {
                    context.score += enemy.scoreAward
                    context.earnCredits(enemy.scoreAward / 2)
                }
            }
        }
        for (target in groundTargets) {
            if (!target.isDestroyed && (target.position - center).lengthSquared() <= radiusSq) {
                val destroyed = target.takeDamage(damage, particleLayer)
                if (destroyed) {
                    context.score += target.type.scoreValue
                    context.earnCredits(target.type.scoreValue / 2)
                }
            }
        }
    }
}
