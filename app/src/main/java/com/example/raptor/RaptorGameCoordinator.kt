package com.example.raptor

import com.example.raptor.entities.EnemyShip
import com.example.raptor.entities.GroundDestructible
import com.example.raptor.entities.PickupType
import com.example.raptor.entities.RaptorBoss
import com.example.raptor.entities.RaptorPickup
import com.example.raptor.entities.RaptorShip
import com.example.raptor.world.RaptorCloudLayer
import com.example.raptor.world.RaptorCollisionSystem
import com.example.raptor.world.RaptorLevel
import com.example.raptor.world.RaptorLevelCatalog
import com.example.raptor.world.RaptorProjectilesLayer
import com.example.raptor.world.RaptorTerrainLayer
import com.example.raptor.world.RaptorWaveSpawner
import com.pixie.engine.core.PixieEngine
import com.pixie.engine.core.camera.GameViewMode
import com.pixie.engine.core.context.DifficultyPreset
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.ports.HapticPort
import com.pixie.engine.core.ports.VirtualResolution
import com.pixie.engine.core.screen.StandardGameplayScreen
import com.pixie.engine.core.screen.StandardScreenIds
import com.pixie.engine.core.weapon.Projectile
import kotlin.random.Random

/**
 * Game coordinator for Raptor: Vertical Arcade Combat Scroller.
 *
 * Coordinates vertical scrolling gameplay, delegating procedural wave/ground spawning
 * to [RaptorWaveSpawner] and combat hit/ramming/AOE resolution to [RaptorCollisionSystem].
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
    val terrainLayer = RaptorTerrainLayer(currentLevelGetter = { currentLevel })
    val projectilesLayer = RaptorProjectilesLayer(
        playerProjectilesGetter = { playerProjectiles },
        enemyProjectilesGetter = { enemyProjectiles }
    )
    val cloudLayer = RaptorCloudLayer()

    // Player craft & Boss
    lateinit var playerShip: RaptorShip
    private var boss: RaptorBoss? = null
    var isBossSpawned: Boolean = false

    // Active object tracking lists
    val enemies = mutableListOf<EnemyShip>()
    val groundTargets = mutableListOf<GroundDestructible>()
    val pickups = mutableListOf<RaptorPickup>()
    val playerProjectiles = mutableListOf<Projectile>()
    val enemyProjectiles = mutableListOf<Projectile>()

    // Modular collision system
    val collisionSystem = RaptorCollisionSystem(
        context = engine.context,
        particleLayer = engine.particleLayer
    )

    // Modular wave & ground spawner
    val waveSpawner = RaptorWaveSpawner(
        virtualResolution = virtualResolution,
        projectilePool = engine.projectilePool,
        itemsLayer = engine.itemsLayer,
        enemies = enemies,
        groundTargets = groundTargets,
        audio = audio,
        onSpawnDrop = { dropPos -> spawnDrop(dropPos) },
        onChainExplosion = { center, radius, dmg ->
            collisionSystem.triggerAoeDamage(center, radius, dmg, enemies, groundTargets)
        }
    )

    // Timeline progress tracking
    private var levelDistanceTraveled: Float = 0f
    private var isLevelCompleted: Boolean = false

    var onGameOver: (() -> Unit)? = null
    var onVictory: (() -> Unit)? = null

    init {
        // Register Raptor vertical layers into engine
        engine.layerStack.addLayer(terrainLayer)
        engine.layerStack.addLayer(projectilesLayer)
        engine.layerStack.addLayer(cloudLayer)

        // Register standard screen support in engine core
        engine.registerScreen(StandardGameplayScreen(engine))
        engine.setScreen(StandardScreenIds.GAMEPLAY)

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

        waveSpawner.reset(waveDelay = 1.2f, groundDelay = 0.4f)

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

    /**
     * Frame update hook driven by PixieGameCanvas vsync loop.
     */
    fun update(dt: Float) {
        if (engine.context.isGameOver || isLevelCompleted) return

        updateFlightProgress(dt)
        updateSpawning(dt)
        updatePlayer(dt)
        updateEnemies()
        updateGroundTargets()
        updateBoss()
        updatePickups(dt)
        updateProjectiles(dt)

        collisionSystem.resolveCombatCollisions(
            playerShip = playerShip,
            boss = boss,
            enemies = enemies,
            groundTargets = groundTargets,
            playerProjectiles = playerProjectiles,
            enemyProjectiles = enemyProjectiles
        )

        checkGameOver()
    }

    private fun updateFlightProgress(dt: Float) {
        levelDistanceTraveled += dt * 36f
        engine.context.distance = levelDistanceTraveled
        engine.context.currentMovingSpeed = playerShip.velocity.length()
    }

    private fun updateSpawning(dt: Float) {
        val bossTriggerDistance = currentLevel.targetDistance
        if (!isBossSpawned && levelDistanceTraveled >= bossTriggerDistance) {
            spawnBoss()
        } else if (!isBossSpawned) {
            waveSpawner.update(dt, selectedDifficulty, currentLevelIndex)
        }
    }

    private fun updatePlayer(dt: Float) {
        if (isAutoFireEnabled && playerShip.isAlive) {
            playerShip.firePrimary(engine.context, playerProjectiles)
        }
        playerShip.moveTo(
            target = playerShip.targetPosition,
            dt = dt,
            boundsXMin = 4f,
            boundsXMax = virtualResolution.width.toFloat() - 4f,
            boundsYMin = 20f,
            boundsYMax = virtualResolution.height.toFloat() - 10f
        )
    }

    private fun updateEnemies() {
        val enemyIter = enemies.iterator()
        while (enemyIter.hasNext()) {
            val enemy = enemyIter.next()
            enemy.tryShoot(playerShip, engine.context, enemyProjectiles)
            if (enemy.position.y > virtualResolution.height + 30f || !enemy.isAlive) {
                engine.itemsLayer.removeObject(enemy.id)
                enemyIter.remove()
            }
        }
    }

    private fun updateGroundTargets() {
        val groundIter = groundTargets.iterator()
        while (groundIter.hasNext()) {
            val target = groundIter.next()
            target.tryShoot(playerShip, engine.context, enemyProjectiles)
            if (target.position.y > virtualResolution.height + 40f) {
                engine.itemsLayer.removeObject(target.id)
                groundIter.remove()
            }
        }
    }

    private fun updateBoss() {
        boss?.let { b ->
            b.attack(engine.context, enemyProjectiles)
            if (b.isDefeated) {
                engine.context.score += 10000L
                engine.context.earnCredits(3500L)
                isLevelCompleted = true
                engine.context.isVictory = true
                onVictory?.invoke()
            }
        }
    }

    private fun updatePickups(dt: Float) {
        val hasMagnet = engine.context.hasPerk("MAGNET_BEAM")
        val pickupIter = pickups.iterator()
        while (pickupIter.hasNext()) {
            val p = pickupIter.next()
            p.attractTowards(playerShip.position, dt, hasMagnet)
            if (playerShip.boundingBox.intersects(p.boundingBox)) {
                p.applyEffect(playerShip, engine.context, audio)
                p.isAlive = false
                engine.itemsLayer.removeObject(p.id)
                pickupIter.remove()
            } else if (p.position.y > virtualResolution.height + 20f || !p.isAlive) {
                engine.itemsLayer.removeObject(p.id)
                pickupIter.remove()
            }
        }
    }

    private fun updateProjectiles(dt: Float) {
        updateProjectileList(playerProjectiles, dt, -15f, virtualResolution.height + 15f, isPlayer = true)
        updateProjectileList(enemyProjectiles, dt, -15f, virtualResolution.height + 15f, isPlayer = false)
    }

    private fun updateProjectileList(
        projectiles: MutableList<Projectile>,
        dt: Float,
        yMin: Float,
        yMax: Float,
        isPlayer: Boolean
    ) {
        val iter = projectiles.iterator()
        while (iter.hasNext()) {
            val proj = iter.next()
            proj.update(dt, engine.context)
            val isOutOfBounds = if (isPlayer) {
                proj.position.y < yMin || proj.position.x < -15f || proj.position.x > virtualResolution.width + 15f
            } else {
                proj.position.y > yMax || proj.position.x < -15f || proj.position.x > virtualResolution.width + 15f
            }
            if (!proj.isAlive || isOutOfBounds) {
                proj.isAlive = false
                engine.projectilePool.recycle(proj)
                iter.remove()
            }
        }
    }

    private fun checkGameOver() {
        if (!playerShip.isAlive && !engine.context.isGameOver) {
            engine.context.isGameOver = true
            onGameOver?.invoke()
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
            roll < 20 -> PickupType.WEAPON_UPGRADE
            roll < 36 -> PickupType.HOMING_MISSILES
            roll < 54 -> PickupType.SHIELD_CELL
            roll < 70 -> PickupType.REPAIR_KIT
            roll < 80 -> PickupType.MEGA_BOMB
            roll < 92 -> PickupType.SCORE_GEM
            else -> PickupType.PERK_CHIP
        }

        val pickup = RaptorPickup.create(dropType, "pickup_${System.currentTimeMillis()}_${Random.nextInt(1000)}", pos).apply {
            velocity = Vec2(0f, 36f)
        }
        pickups.add(pickup)
        engine.itemsLayer.addObject(pickup)
    }

    fun applyPickup(type: PickupType) {
        RaptorPickup.create(type, "pickup_apply", Vec2.ZERO).applyEffect(playerShip, engine.context, audio)
    }

    fun triggerAoeDamage(center: Vec2, radius: Float, damage: Float) {
        collisionSystem.triggerAoeDamage(center, radius, damage, enemies, groundTargets)
    }

    fun fireSecondaryMissiles() {
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

    /**
     * Steers the player craft by a delta in virtual game units,
     * maintaining safe margins within playfield bounds.
     */
    fun movePlayer(deltaX: Float, deltaY: Float) {
        val currentTarget = playerShip.targetPosition
        playerShip.targetPosition = Vec2(
            (currentTarget.x + deltaX).coerceIn(4f, 218f),
            (currentTarget.y + deltaY).coerceIn(24f, 290f)
        )
    }

    /**
     * Handles initial drag contact in virtual game coordinates, acquiring control
     * if the touch begins near the player craft.
     */
    fun handleDragStart(virtualX: Float, virtualY: Float) {
        val touchPos = Vec2(virtualX, virtualY)
        val dist = (touchPos - playerShip.position).length()
        if (dist < 75f) {
            playerShip.targetPosition = Vec2(
                virtualX.coerceIn(4f, 218f),
                virtualY.coerceIn(24f, 290f)
            )
        }
    }
}
