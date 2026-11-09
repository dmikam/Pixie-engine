package com.example.raptor

import com.example.raptor.entities.EnemyShip
import com.example.raptor.entities.GroundDestructible
import com.example.raptor.entities.PickupType
import com.example.raptor.entities.RaptorBoss
import com.example.raptor.entities.RaptorPickup
import com.example.raptor.entities.RaptorShip
import com.example.raptor.world.RaptorCloudLayer
import com.example.raptor.world.RaptorCollisionSystem
import com.example.raptor.world.RaptorDropCatalog
import com.example.raptor.world.RaptorLevel
import com.example.raptor.world.RaptorLevelCatalog
import com.example.raptor.world.RaptorProjectilesLayer
import com.example.raptor.world.RaptorTerrainLayer
import com.example.raptor.world.RaptorWaveSpawner
import com.pixie.android.input.DualModeInputAdapter
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

/**
 * Game coordinator for Raptor: Vertical Arcade Combat Scroller.
 * Coordinates vertical scrolling gameplay, wave spawning, and combat resolution.
 */
class RaptorGameCoordinator(
    val audio: AudioPort? = null,
    val haptics: HapticPort? = null,
    val inputAdapter: DualModeInputAdapter = DualModeInputAdapter(),
    var selectedDifficulty: DifficultyPreset = DifficultyPreset.MEDIUM,
    var currentLevelIndex: Int = 1
) {
    val virtualResolution = VirtualResolution(240, 320)
    val engine = PixieEngine(
        virtualResolution = virtualResolution,
        viewMode = GameViewMode.FixedScreen(Vec2.ZERO),
        audio = audio,
        haptics = haptics,
        input = inputAdapter
    )

    var currentLevel: RaptorLevel = RaptorLevelCatalog.getLevel(currentLevelIndex)
    var isAutoFireEnabled: Boolean = true

    val terrainLayer = RaptorTerrainLayer({ currentLevel })
    val projectilesLayer = RaptorProjectilesLayer({ playerProjectiles }, { enemyProjectiles })
    val cloudLayer = RaptorCloudLayer()

    lateinit var playerShip: RaptorShip
    var boss: RaptorBoss? = null
    var isBossSpawned: Boolean = false

    val enemies = mutableListOf<EnemyShip>()
    val groundTargets = mutableListOf<GroundDestructible>()
    val pickups = mutableListOf<RaptorPickup>()
    val playerProjectiles = mutableListOf<Projectile>()
    val enemyProjectiles = mutableListOf<Projectile>()
    val collisionSystem = RaptorCollisionSystem(context = engine.context, particleLayer = engine.particleLayer)

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

    var levelDistanceTraveled: Float = 0f
    var isLevelCompleted: Boolean = false
    var lastScoreMilestone: Long = 0L
    var onGameOver: (() -> Unit)? = null
    var onVictory: (() -> Unit)? = null

    // Persistent Hangar Armory & Loadout State
    val unlockedWeapons: MutableList<String> = mutableListOf("vulcan")
    var activeWeaponId: String = "vulcan"
    var savedWeaponTier: Int = 1
    var savedMissiles: Int = 6
    var savedBombs: Int = 2
    val persistentPerks = mutableSetOf<String>()

    init {
        engine.layerStack.apply { addLayer(terrainLayer); addLayer(projectilesLayer); addLayer(cloudLayer) }
        engine.registerScreen(StandardGameplayScreen(engine))
        engine.setScreen(StandardScreenIds.GAMEPLAY)
        setupGame()
    }

    fun setupGame() {
        engine.resetGame()
        currentLevel = RaptorLevelCatalog.getLevel(currentLevelIndex)
        engine.context.apply {
            difficulty = selectedDifficulty.difficultyValue
            currentLevel = currentLevelIndex
            score = 0L
            livesLeft = 3
            activePerks.clear()
            activePerks.addAll(persistentPerks)
        }

        listOf(enemies, groundTargets, pickups, playerProjectiles, enemyProjectiles).forEach { it.clear() }
        boss = null
        isBossSpawned = false
        levelDistanceTraveled = 0f
        isLevelCompleted = false
        lastScoreMilestone = 0L

        inputAdapter.reset()
        waveSpawner.reset(waveDelay = 1.2f, groundDelay = 0.4f)

        playerShip = RaptorShip(
            initialPosition = Vec2(120f - 9f, 250f),
            projectilePool = engine.projectilePool,
            audio = audio,
            haptics = haptics
        ).apply {
            targetPosition = Vec2(120f - 9f, 250f)
            // Restore persistent armory state from coordinator
            this@RaptorGameCoordinator.unlockedWeapons.forEach { unlockWeapon(it) }
            this.weaponTier = savedWeaponTier
            this.equipWeapon(this@RaptorGameCoordinator.activeWeaponId)
            this.missileSalvoes = savedMissiles
            this.megaBombs = savedBombs
        }
        engine.player = playerShip
        engine.itemsLayer.addObject(playerShip)

        // Disable Dinorun horizontal parallax layers
        engine.apply {
            skyLayer.isEnabled = false; cloudLayer.isEnabled = false
            backgroundLayer.isEnabled = false; groundLayer.isEnabled = false
            foregroundLayer.isEnabled = false; camera.setPositionImmediate(Vec2.ZERO)
        }

        // Enable Raptor vertical layers
        terrainLayer.apply { isEnabled = true; reset() }
        projectilesLayer.isEnabled = true
        cloudLayer.apply { isEnabled = true; reset() }
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
     * Unlocks a weapon in the armory and immediately equips it on the player craft.
     */
    fun unlockWeapon(weaponId: String) {
        if (!unlockedWeapons.contains(weaponId)) {
            unlockedWeapons.add(weaponId)
        }
        activeWeaponId = weaponId
        playerShip.unlockWeapon(weaponId)
    }

    /**
     * Switches the active weapon during combat or in hangar if owned.
     */
    fun switchWeapon(weaponId: String): Boolean {
        if (unlockedWeapons.contains(weaponId)) {
            activeWeaponId = weaponId
            playerShip.equipWeapon(weaponId)
            audio?.playSound(com.pixie.engine.core.ports.AudioEvent.POWERUP_PICKUP)
            haptics?.trigger(com.pixie.engine.core.ports.HapticFeedbackType.LIGHT)
            return true
        }
        return false
    }

    /**
     * Cycles to the next available unlocked weapon in the player's arsenal.
     */
    fun cycleWeapon(): String {
        val next = playerShip.cycleWeapon()
        activeWeaponId = next
        if (!unlockedWeapons.contains(next)) {
            unlockedWeapons.add(next)
        }
        audio?.playSound(com.pixie.engine.core.ports.AudioEvent.POWERUP_PICKUP)
        haptics?.trigger(com.pixie.engine.core.ports.HapticFeedbackType.LIGHT)
        return next
    }

    /**
     * Frame update hook driven by PixieGame vsync loop.
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
            playerShip, boss, enemies, groundTargets, playerProjectiles, enemyProjectiles
        )

        checkScoreMilestones()
        checkGameOver()
    }

    private fun updateFlightProgress(dt: Float) {
        levelDistanceTraveled += dt * 36f
        engine.context.distance = levelDistanceTraveled
        engine.context.currentMovingSpeed = playerShip.velocity.length()
    }

    private fun updateSpawning(dt: Float) {
        if (!isBossSpawned && levelDistanceTraveled >= currentLevel.targetDistance) {
            spawnBoss()
        } else if (!isBossSpawned) {
            waveSpawner.update(dt, selectedDifficulty, currentLevelIndex)
        }
    }

    private fun updatePlayer(dt: Float) {
        if ((isAutoFireEnabled || inputAdapter.isActionPressed) && playerShip.isAlive) {
            playerShip.firePrimary(engine.context, playerProjectiles)
        }
        if (inputAdapter.moveX != 0f || inputAdapter.moveY != 0f) {
            val speed = playerShip.parameters["speed"] ?: 180f
            movePlayer(inputAdapter.moveX * speed * dt, inputAdapter.moveY * speed * dt)
        }
        playerShip.moveTo(playerShip.targetPosition, dt, 4f, virtualResolution.width - 4f, 20f, virtualResolution.height - 10f)
    }

    private fun updateEnemies() {
        val iter = enemies.iterator()
        while (iter.hasNext()) {
            val enemy = iter.next()
            enemy.tryShoot(playerShip, engine.context, enemyProjectiles)
            if (enemy.position.y > virtualResolution.height + 30f || !enemy.isAlive) {
                engine.itemsLayer.removeObject(enemy.id)
                iter.remove()
            }
        }
    }

    private fun updateGroundTargets() {
        val iter = groundTargets.iterator()
        while (iter.hasNext()) {
            val target = iter.next()
            target.tryShoot(playerShip, engine.context, enemyProjectiles)
            if (target.position.y > virtualResolution.height + 40f) {
                engine.itemsLayer.removeObject(target.id)
                iter.remove()
            }
        }
    }

    private fun updateBoss() {
        boss?.let { b ->
            b.attack(engine.context, enemyProjectiles)
            if (b.isDefeated) {
                handleBossVictory()
            }
        }
    }

    private fun handleBossVictory() {
        engine.context.score += 10000L
        engine.context.earnCredits(3500L)
        isLevelCompleted = true
        engine.context.isVictory = true
        audio?.playSound(AudioEvent.SCORE_MILESTONE)
        onVictory?.invoke()
    }

    private fun updatePickups(dt: Float) {
        val hasMagnet = engine.context.hasPerk("MAGNET_BEAM")
        val iter = pickups.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.attractTowards(playerShip.position, dt, hasMagnet)
            val collected = playerShip.boundingBox.intersects(p.boundingBox)
            if (collected) {
                p.applyEffect(playerShip, engine.context, audio)
                p.isAlive = false
            }
            if (collected || p.position.y > virtualResolution.height + 20f || !p.isAlive) {
                engine.itemsLayer.removeObject(p.id)
                iter.remove()
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
            val outOfBounds = if (isPlayer) proj.position.y < yMin else proj.position.y > yMax
                || proj.position.x < -15f || proj.position.x > virtualResolution.width + 15f
            if (!proj.isAlive || outOfBounds) {
                proj.isAlive = false
                engine.projectilePool.recycle(proj)
                iter.remove()
            }
        }
    }

    private fun checkScoreMilestones() {
        val interval = 5000L
        val currentScore = engine.context.score
        if (currentScore >= interval && (currentScore / interval > lastScoreMilestone / interval)) {
            lastScoreMilestone = (currentScore / interval) * interval
            audio?.playSound(AudioEvent.SCORE_MILESTONE)
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
        boss = RaptorBoss(
            initialPos = Vec2(120f - 24f, -40f),
            projectilePool = engine.projectilePool,
            audio = audio,
            onBossDefeated = { handleBossVictory() }
        ).also { engine.itemsLayer.addObject(it) }
    }

    private fun spawnDrop(pos: Vec2) {
        val pickup = RaptorDropCatalog.spawnPickup(pos)
        pickups.add(pickup)
        engine.itemsLayer.addObject(pickup)
    }

    fun applyPickup(type: PickupType) {
        RaptorPickup.create(type, "pickup_apply", Vec2.ZERO).applyEffect(playerShip, engine.context, audio)
        savedWeaponTier = playerShip.weaponTier
        savedMissiles = playerShip.missileSalvoes
        savedBombs = playerShip.megaBombs
    }

    fun triggerAoeDamage(center: Vec2, radius: Float, damage: Float) {
        collisionSystem.triggerAoeDamage(center, radius, damage, enemies, groundTargets)
    }

    fun fireSecondaryMissiles() {
        val target: PixieObject? = boss?.takeIf { it.isAlive }
            ?: enemies.filter { it.isAlive }.minByOrNull { (it.position - playerShip.position).lengthSquared() }
        val fired = playerShip.fireMissiles(engine.context, target, playerProjectiles)
        if (fired) {
            savedMissiles = playerShip.missileSalvoes
        }
    }

    fun triggerMegaBomb(): Boolean {
        val triggered = playerShip.triggerMegaBomb(
            visibleEnemies = enemies,
            activeEnemyProjectiles = enemyProjectiles,
            particleLayer = engine.particleLayer
        )
        if (triggered) {
            savedBombs = playerShip.megaBombs
        }
        return triggered
    }

    fun movePlayer(deltaX: Float, deltaY: Float) {
        val cur = playerShip.targetPosition
        playerShip.targetPosition = Vec2((cur.x + deltaX).coerceIn(4f, 218f), (cur.y + deltaY).coerceIn(24f, 290f))
    }

    fun handleDragStart(virtualX: Float, virtualY: Float) {
        if ((Vec2(virtualX, virtualY) - playerShip.position).length() < 75f) {
            playerShip.targetPosition = Vec2(virtualX.coerceIn(4f, 218f), virtualY.coerceIn(24f, 290f))
        }
    }
}
