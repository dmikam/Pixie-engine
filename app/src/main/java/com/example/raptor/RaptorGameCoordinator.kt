package com.example.raptor

import com.example.raptor.entities.DestructibleType
import com.example.raptor.entities.EnemyShip
import com.example.raptor.entities.EnemyType
import com.example.raptor.entities.GroundDestructible
import com.example.raptor.entities.PickupType
import com.example.raptor.entities.RaptorBoss
import com.example.raptor.entities.RaptorPickup
import com.example.raptor.entities.RaptorShip
import com.example.raptor.world.RaptorLevel
import com.example.raptor.world.RaptorLevelCatalog
import com.pixie.engine.core.PixieEngine
import com.pixie.engine.core.camera.GameViewMode
import com.pixie.engine.core.context.DifficultyPreset
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.ports.HapticPort
import com.pixie.engine.core.ports.VirtualResolution
import com.pixie.engine.core.weapon.Projectile
import kotlin.random.Random

/**
 * Game coordinator for Raptor: Vertical Arcade Combat Scroller.
 *
 * Drives the PixieEngine in vertical scrolling mode, managing:
 * - High-speed projectile simulation and pooling
 * - Procedural enemy squadrons & formation flight paths
 * - Destructible ground installations, turrets, and chain explosions
 * - Collectible power-ups and active perk badges
 * - Multi-stage boss encounters and campaign level progression
 * - Continuous difficulty scaling (0..100) and context property computations
 */
class RaptorGameCoordinator(
    val audio: AudioPort? = null,
    val haptics: HapticPort? = null,
    var selectedDifficulty: DifficultyPreset = DifficultyPreset.MEDIUM,
    var currentLevelIndex: Int = 1
) {
    val virtualResolution = VirtualResolution(240, 320)
    val engine = PixieEngine(
        virtualResolution = virtualResolution,
        viewMode = GameViewMode.FixedScreen(Vec2.ZERO),
        audio = audio,
        haptics = haptics
    )

    var currentLevel: RaptorLevel = RaptorLevelCatalog.getLevel(currentLevelIndex)
    var isAutoFireEnabled: Boolean = true

    // Dedicated Vertical Scrolling Layers for Raptor
    val terrainLayer = com.example.raptor.world.RaptorTerrainLayer(currentLevelGetter = { currentLevel })
    val projectilesLayer = com.example.raptor.world.RaptorProjectilesLayer(
        playerProjectilesGetter = { playerProjectiles },
        enemyProjectilesGetter = { enemyProjectiles }
    )
    val cloudLayer = com.example.raptor.world.RaptorCloudLayer()

    // Player craft
    lateinit var playerShip: RaptorShip
    private var boss: RaptorBoss? = null
    var isBossSpawned: Boolean = false

    // Active object tracking lists
    val enemies = mutableListOf<EnemyShip>()
    val groundTargets = mutableListOf<GroundDestructible>()
    val pickups = mutableListOf<RaptorPickup>()
    val playerProjectiles = mutableListOf<Projectile>()
    val enemyProjectiles = mutableListOf<Projectile>()

    // Wave spawning timeline
    private var waveSpawnTimer: Float = 0f
    private var groundSpawnTimer: Float = 0f
    private var levelDistanceTraveled: Float = 0f
    private var isLevelCompleted: Boolean = false

    var onGameOver: (() -> Unit)? = null
    var onVictory: (() -> Unit)? = null

    init {
        // Register Raptor vertical layers into engine
        engine.layerStack.addLayer(terrainLayer)
        engine.layerStack.addLayer(projectilesLayer)
        engine.layerStack.addLayer(cloudLayer)

        setupGame()
    }

    fun setupGame() {
        engine.resetGame()
        currentLevel = RaptorLevelCatalog.getLevel(currentLevelIndex)

        // Set engine context difficulty and level
        engine.context.difficulty = selectedDifficulty.difficultyValue
        engine.context.currentLevel = currentLevelIndex
        engine.context.score = 0L
        engine.context.livesLeft = 3
        engine.context.activePerks.clear()

        enemies.clear()
        groundTargets.clear()
        pickups.clear()
        playerProjectiles.clear()
        enemyProjectiles.clear()
        boss = null
        isBossSpawned = false
        levelDistanceTraveled = 0f
        isLevelCompleted = false
        waveSpawnTimer = 1.0f
        groundSpawnTimer = 0.5f

        // Initialize player ship at lower third of screen (Y = 250)
        playerShip = RaptorShip(
            initialPosition = Vec2(120f - 9f, 250f),
            projectilePool = engine.projectilePool,
            audio = audio,
            haptics = haptics
        )
        playerShip.targetPosition = Vec2(120f - 9f, 250f)
        engine.player = playerShip
        engine.itemsLayer.addObject(playerShip)

        // Disable Dinorun horizontal parallax layers
        engine.skyLayer.isEnabled = false
        engine.cloudLayer.isEnabled = false
        engine.backgroundLayer.isEnabled = false
        engine.groundLayer.isEnabled = false

        // Enable Raptor vertical layers
        terrainLayer.isEnabled = true
        terrainLayer.reset()
        projectilesLayer.isEnabled = true
        cloudLayer.isEnabled = true
        cloudLayer.reset()

        // Keep camera locked at (0, 0)
        engine.camera.setPositionImmediate(Vec2.ZERO)
    }

    fun setDifficulty(preset: DifficultyPreset) {
        selectedDifficulty = preset
        engine.context.difficulty = preset.difficultyValue
    }

    fun setLevel(levelIndex: Int) {
        currentLevelIndex = levelIndex
        setupGame()
    }

    fun update(dt: Float) {
        if (engine.context.isGameOver || isLevelCompleted) return

        levelDistanceTraveled += dt * 80f
        engine.context.distance = levelDistanceTraveled
        engine.context.currentMovingSpeed = playerShip.velocity.length()

        // 1. Spawning timeline (waves & ground targets until boss)
        val bossTriggerDistance = currentLevel.targetDistance
        if (!isBossSpawned && levelDistanceTraveled >= bossTriggerDistance) {
            spawnBoss()
        } else if (!isBossSpawned) {
            updateWaveSpawning(dt)
            updateGroundSpawning(dt)
        }

        // 2. Player auto-fire
        if (isAutoFireEnabled && playerShip.isAlive) {
            playerShip.firePrimary(engine.context, playerProjectiles)
        }

        // 3. Update player ship movement clamp
        playerShip.moveTo(
            target = playerShip.targetPosition,
            dt = dt,
            boundsXMin = 4f,
            boundsXMax = virtualResolution.width.toFloat() - 4f,
            boundsYMin = 20f,
            boundsYMax = virtualResolution.height.toFloat() - 10f
        )

        // 4. Update enemies & enemy shooting
        val enemyIter = enemies.iterator()
        while (enemyIter.hasNext()) {
            val enemy = enemyIter.next()
            enemy.update(dt, engine.context)
            enemy.tryShoot(playerShip, engine.context, enemyProjectiles)

            // Despawn off bottom screen
            if (enemy.position.y > virtualResolution.height + 30f || !enemy.isAlive) {
                engine.itemsLayer.removeObject(enemy.id)
                enemyIter.remove()
            }
        }

        // 5. Update ground destructibles & ground turret fire
        val groundIter = groundTargets.iterator()
        while (groundIter.hasNext()) {
            val target = groundIter.next()
            // Ground targets drift downward as player flies forward
            target.position += Vec2(0f, 50f * dt)
            target.update(dt, engine.context)
            target.tryShoot(playerShip, engine.context, enemyProjectiles)

            if (target.position.y > virtualResolution.height + 40f) {
                engine.itemsLayer.removeObject(target.id)
                groundIter.remove()
            }
        }

        // 6. Update boss
        boss?.let { b ->
            b.update(dt, engine.context)
            b.attack(engine.context, enemyProjectiles)
            if (b.isDefeated) {
                engine.context.score += 10000L
                isLevelCompleted = true
                engine.context.isVictory = true
                onVictory?.invoke()
            }
        }

        // 7. Update pickups (magnet attraction to player ship)
        val hasMagnet = engine.context.hasPerk("MAGNET_BEAM")
        val pickupIter = pickups.iterator()
        while (pickupIter.hasNext()) {
            val p = pickupIter.next()
            p.update(dt, engine.context)
            p.attractTowards(playerShip.position, dt, hasMagnet)

            // Check collection by player
            if (playerShip.boundingBox.intersects(p.boundingBox)) {
                applyPickup(p.type)
                p.isAlive = false
                engine.itemsLayer.removeObject(p.id)
                pickupIter.remove()
            } else if (p.position.y > virtualResolution.height + 20f || !p.isAlive) {
                engine.itemsLayer.removeObject(p.id)
                pickupIter.remove()
            }
        }

        // 8. Update projectiles
        updateProjectiles(dt)

        // 9. Collision detection: Player projectiles vs Enemies, Boss, and Ground targets
        resolveCombatCollisions()

        // 10. Update engine main loop and tick particle subsystem
        engine.tick(dt)

        // Check game over
        if (!playerShip.isAlive && !engine.context.isGameOver) {
            engine.context.isGameOver = true
            onGameOver?.invoke()
        }
    }

    private fun updateProjectiles(dt: Float) {
        // Player projectiles
        val pIter = playerProjectiles.iterator()
        while (pIter.hasNext()) {
            val proj = pIter.next()
            proj.update(dt, engine.context)
            if (!proj.isAlive || proj.position.y < -15f || proj.position.x < -15f || proj.position.x > virtualResolution.width + 15f) {
                proj.isAlive = false
                engine.projectilePool.recycle(proj)
                pIter.remove()
            }
        }

        // Enemy projectiles
        val eIter = enemyProjectiles.iterator()
        while (eIter.hasNext()) {
            val proj = eIter.next()
            proj.update(dt, engine.context)
            if (!proj.isAlive || proj.position.y > virtualResolution.height + 15f || proj.position.x < -15f || proj.position.x > virtualResolution.width + 15f) {
                proj.isAlive = false
                engine.projectilePool.recycle(proj)
                eIter.remove()
            }
        }
    }

    private fun resolveCombatCollisions() {
        // Player bullets vs Enemies
        for (bullet in playerProjectiles) {
            if (!bullet.isAlive) continue

            // Against Boss
            val b = boss
            if (b != null && b.isAlive && bullet.boundingBox.intersects(b.boundingBox)) {
                b.takeDamage(bullet.hitPower, bullet.position, engine.particleLayer)
                bullet.onImpact(bullet.position, engine.particleLayer)
                continue
            }

            // Against Aerial Enemies
            for (enemy in enemies) {
                if (enemy.isAlive && bullet.boundingBox.intersects(enemy.boundingBox)) {
                    val killed = enemy.takeDamage(bullet.hitPower, engine.particleLayer)
                    if (killed) {
                        engine.context.score += enemy.scoreAward
                    }
                    bullet.onImpact(bullet.position, engine.particleLayer)
                    if (!bullet.isAlive) break
                }
            }

            // Against Ground Destructibles
            if (bullet.isAlive) {
                for (target in groundTargets) {
                    if (!target.isDestroyed && bullet.boundingBox.intersects(target.boundingBox)) {
                        val destroyed = target.takeDamage(bullet.hitPower, engine.particleLayer)
                        if (destroyed) {
                            engine.context.score += target.type.scoreValue
                        }
                        bullet.onImpact(bullet.position, engine.particleLayer)
                        if (!bullet.isAlive) break
                    }
                }
            }
        }

        // Enemy bullets vs Player ship
        if (playerShip.isAlive) {
            val playerBox = playerShip.boundingBox
            for (bullet in enemyProjectiles) {
                if (bullet.isAlive && playerBox.intersects(bullet.boundingBox)) {
                    playerShip.takeDamage(bullet.hitPower, engine.particleLayer)
                    bullet.onImpact(bullet.position, engine.particleLayer)
                }
            }
        }
    }

    private fun updateWaveSpawning(dt: Float) {
        waveSpawnTimer -= dt
        if (waveSpawnTimer <= 0f) {
            // Difficulty determines wave frequency
            val spawnDelay = when (selectedDifficulty) {
                DifficultyPreset.EASY -> 3.2f
                DifficultyPreset.MEDIUM -> 2.4f
                DifficultyPreset.DIFFICULT -> 1.7f
                DifficultyPreset.HEAVY_METAL -> 1.1f
                else -> 2.4f
            }
            waveSpawnTimer = spawnDelay

            // Pick formation
            val spawnType = when (Random.nextInt(10)) {
                in 0..4 -> EnemyType.SCOUT
                in 5..7 -> EnemyType.INTERCEPTOR
                in 8..8 -> EnemyType.GUNSHIP
                else -> EnemyType.STEALTH_BOMBER
            }

            val squadronSize = when (spawnType) {
                EnemyType.SCOUT -> 3
                EnemyType.INTERCEPTOR -> 2
                else -> 1
            }

            val startX = Random.nextFloat() * (virtualResolution.width - 60f) + 30f
            for (i in 0 until squadronSize) {
                val posX = startX + (i - (squadronSize - 1) / 2f) * 22f
                val posY = -20f - (i * 18f)
                val enemy = EnemyShip(
                    id = "enemy_${System.currentTimeMillis()}_$i",
                    type = spawnType,
                    initialPos = Vec2(posX, posY),
                    projectilePool = engine.projectilePool,
                    audio = audio,
                    onDeathDrop = { dropPos -> spawnDrop(dropPos) }
                )
                enemies.add(enemy)
                engine.itemsLayer.addObject(enemy)
            }
        }
    }

    private fun updateGroundSpawning(dt: Float) {
        groundSpawnTimer -= dt
        if (groundSpawnTimer <= 0f) {
            groundSpawnTimer = Random.nextFloat() * 4.0f + 2.5f

            val groundType = when (Random.nextInt(4)) {
                0 -> DestructibleType.GUN_TURRET
                1 -> DestructibleType.FUEL_SILO
                2 -> DestructibleType.RADAR_TOWER
                else -> DestructibleType.SUPPLY_BUNKER
            }

            val posX = Random.nextFloat() * (virtualResolution.width - 40f) + 20f
            val groundObj = GroundDestructible(
                id = "ground_${System.currentTimeMillis()}",
                type = groundType,
                initialPos = Vec2(posX, -30f),
                projectilePool = engine.projectilePool,
                audio = audio,
                onDeathDrop = { dropPos -> spawnDrop(dropPos) },
                onChainExplosion = { center, radius, dmg -> triggerAoeDamage(center, radius, dmg) }
            )
            groundTargets.add(groundObj)
            engine.itemsLayer.addObject(groundObj)
        }
    }

    private fun spawnBoss() {
        isBossSpawned = true
        audio?.playSound(AudioEvent.SCORE_MILESTONE)
        val newBoss = RaptorBoss(
            initialPos = Vec2(120f - 24f, -40f),
            projectilePool = engine.projectilePool,
            audio = audio,
            onBossDefeated = {
                isLevelCompleted = true
                engine.context.isVictory = true
                onVictory?.invoke()
            }
        )
        boss = newBoss
        engine.itemsLayer.addObject(newBoss)
    }

    private fun spawnDrop(pos: Vec2) {
        val roll = Random.nextInt(100)
        val dropType = when {
            roll < 18 -> PickupType.WEAPON_UPGRADE
            roll < 34 -> PickupType.HOMING_MISSILES
            roll < 52 -> PickupType.SHIELD_CELL
            roll < 68 -> PickupType.REPAIR_KIT
            roll < 78 -> PickupType.MEGA_BOMB
            roll < 90 -> PickupType.SCORE_GEM
            else -> PickupType.PERK_CHIP
        }

        val pickup = RaptorPickup("pickup_${System.currentTimeMillis()}", dropType, pos)
        pickups.add(pickup)
        engine.itemsLayer.addObject(pickup)
    }

    private fun applyPickup(type: PickupType) {
        when (type) {
            PickupType.WEAPON_UPGRADE -> playerShip.upgradeWeapon()
            PickupType.HOMING_MISSILES -> playerShip.addMissiles(4)
            PickupType.SHIELD_CELL -> playerShip.rechargeShield(40f)
            PickupType.REPAIR_KIT -> playerShip.repairHull(30f)
            PickupType.MEGA_BOMB -> playerShip.addBomb(1)
            PickupType.SCORE_GEM -> {
                engine.context.score += 1500L
                audio?.playSound(AudioEvent.COLLECT)
            }
            PickupType.PERK_CHIP -> {
                // Grant random unlocked perk
                val perkOptions = listOf("OVERCLOCK", "RAPID_FIRE", "TITANIUM_HULL", "MAGNET_BEAM")
                val perk = perkOptions.random()
                engine.context.addPerk(perk)
                audio?.playSound(AudioEvent.POWERUP_PICKUP)
            }
        }
    }

    private fun triggerAoeDamage(center: Vec2, radius: Float, damage: Float) {
        val radiusSq = radius * radius
        for (enemy in enemies) {
            if (enemy.isAlive && (enemy.position - center).lengthSquared() <= radiusSq) {
                val killed = enemy.takeDamage(damage, engine.particleLayer)
                if (killed) engine.context.score += enemy.scoreAward
            }
        }
    }

    fun fireSecondaryMissiles() {
        // Find nearest living aerial enemy
        var nearestEnemy: PixieObject? = null
        var nearestDistSq = Float.MAX_VALUE

        val bossObj = boss
        if (bossObj != null && bossObj.isAlive) {
            nearestEnemy = bossObj
        } else {
            for (enemy in enemies) {
                if (enemy.isAlive) {
                    val d2 = (enemy.position - playerShip.position).lengthSquared()
                    if (d2 < nearestDistSq) {
                        nearestDistSq = d2
                        nearestEnemy = enemy
                    }
                }
            }
        }
        playerShip.fireMissiles(engine.context, nearestEnemy, playerProjectiles)
    }

    fun triggerMegaBomb(): Boolean {
        return playerShip.triggerMegaBomb(
            visibleEnemies = enemies,
            activeEnemyProjectiles = enemyProjectiles,
            particleLayer = engine.particleLayer
        )
    }

    fun render(batch: com.pixie.engine.core.ports.RenderBatchPort) {
        // Draw deep biome background color
        batch.fillBackground(currentLevel.deepColor)

        // Draw active projectiles through the batch
        for (proj in playerProjectiles) {
            if (proj.isAlive) {
                batch.drawSprite(proj.getCurrentSprite(), proj.position.x, proj.position.y)
                if (proj.motionBlur.enabled && proj.trailHistory.isNotEmpty()) {
                    batch.drawMotionBlur(proj.getCurrentSprite(), proj.trailHistory, proj.motionBlur.strength)
                }
            }
        }
        for (proj in enemyProjectiles) {
            if (proj.isAlive) {
                batch.drawSprite(proj.getCurrentSprite(), proj.position.x, proj.position.y)
            }
        }

        // Render main engine layers (environment, terrain, items, particles, foreground)
        engine.render(batch)
    }
}
