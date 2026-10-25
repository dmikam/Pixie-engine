package com.example.raptor.world

import com.example.raptor.entities.DestructibleType
import com.example.raptor.entities.EnemyShip
import com.example.raptor.entities.EnemyType
import com.example.raptor.entities.GroundDestructible
import com.pixie.engine.core.context.DifficultyPreset
import com.pixie.engine.core.layer.GameItemsLayer
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.ports.VirtualResolution
import com.pixie.engine.core.weapon.ProjectilePool
import kotlin.random.Random

/**
 * Procedural spawner for aerial enemy squadrons and ground destructibles in Raptor.
 *
 * Encapsulates wave timers, difficulty-scaled frequency, level-specific structure
 * distributions, and squadron formation geometry.
 */
class RaptorWaveSpawner(
    private val virtualResolution: VirtualResolution,
    private val projectilePool: ProjectilePool,
    private val itemsLayer: GameItemsLayer,
    private val enemies: MutableList<EnemyShip>,
    private val groundTargets: MutableList<GroundDestructible>,
    private val audio: AudioPort? = null,
    private val onSpawnDrop: (Vec2) -> Unit,
    private val onChainExplosion: (center: Vec2, radius: Float, damage: Float) -> Unit
) {
    var waveSpawnTimer: Float = 1.2f
        private set
    var groundSpawnTimer: Float = 0.4f
        private set

    fun reset(waveDelay: Float = 1.2f, groundDelay: Float = 0.4f): RaptorWaveSpawner = apply {
        waveSpawnTimer = waveDelay
        groundSpawnTimer = groundDelay
    }

    fun update(dt: Float, difficulty: DifficultyPreset, levelIndex: Int): RaptorWaveSpawner = apply {
        updateWaveSpawning(dt, difficulty)
        updateGroundSpawning(dt, levelIndex)
    }

    private fun updateWaveSpawning(dt: Float, difficulty: DifficultyPreset) {
        waveSpawnTimer -= dt
        if (waveSpawnTimer <= 0f) {
            waveSpawnTimer = getWaveSpawnDelay(difficulty)
            val spawnType = selectEnemyType()
            val squadronSize = getSquadronSize(spawnType)
            spawnSquadron(spawnType, squadronSize)
        }
    }

    private fun getWaveSpawnDelay(difficulty: DifficultyPreset): Float = when (difficulty) {
        DifficultyPreset.EASY -> 4.0f
        DifficultyPreset.MEDIUM -> 3.2f
        DifficultyPreset.DIFFICULT -> 2.5f
        DifficultyPreset.HEAVY_METAL -> 1.8f
        else -> 3.2f
    }

    private fun selectEnemyType(): EnemyType = when (Random.nextInt(10)) {
        in 0..4 -> EnemyType.SCOUT
        in 5..7 -> EnemyType.INTERCEPTOR
        in 8..8 -> EnemyType.GUNSHIP
        else -> EnemyType.STEALTH_BOMBER
    }

    private fun getSquadronSize(type: EnemyType): Int = when (type) {
        EnemyType.SCOUT -> 3
        EnemyType.INTERCEPTOR -> 2
        else -> 1
    }

    private fun spawnSquadron(type: EnemyType, size: Int) {
        val startX = Random.nextFloat() * (virtualResolution.width - 70f) + 35f
        for (i in 0 until size) {
            val posX = startX + (i - (size - 1) / 2f) * 24f
            val posY = -25f - (i * 22f)
            val enemy = EnemyShip.create(
                type = type,
                id = "enemy_${System.currentTimeMillis()}_$i",
                initialPos = Vec2(posX, posY),
                projectilePool = projectilePool,
                audio = audio,
                onDeathDrop = { dropPos -> onSpawnDrop(dropPos) }
            )
            enemies.add(enemy)
            itemsLayer.addObject(enemy)
        }
    }

    private fun updateGroundSpawning(dt: Float, levelIndex: Int) {
        groundSpawnTimer -= dt
        if (groundSpawnTimer <= 0f) {
            groundSpawnTimer = Random.nextFloat() * 1.0f + 1.5f
            val groundType = selectGroundStructure(levelIndex)
            spawnGroundTarget(groundType)
        }
    }

    private fun selectGroundStructure(levelIndex: Int): DestructibleType = when (levelIndex) {
        1 -> selectArchipelagoTarget()
        2 -> selectCanyonRefineryTarget()
        else -> selectOrbitalCitadelTarget()
    }

    private fun selectArchipelagoTarget(): DestructibleType = when (Random.nextInt(7)) {
        0 -> DestructibleType.NAVAL_GUNBOAT
        1 -> DestructibleType.GUN_TURRET
        2 -> DestructibleType.FUEL_SILO
        3 -> DestructibleType.RADAR_TOWER
        4 -> DestructibleType.SUPPLY_BUNKER
        5 -> DestructibleType.OIL_STORAGE_TANK
        else -> DestructibleType.MISSILE_SILO
    }

    private fun selectCanyonRefineryTarget(): DestructibleType = when (Random.nextInt(6)) {
        0 -> DestructibleType.FUEL_SILO
        1 -> DestructibleType.OIL_STORAGE_TANK
        2 -> DestructibleType.GUN_TURRET
        3 -> DestructibleType.SUPPLY_BUNKER
        4 -> DestructibleType.MISSILE_SILO
        else -> DestructibleType.RADAR_TOWER
    }

    private fun selectOrbitalCitadelTarget(): DestructibleType = when (Random.nextInt(4)) {
        0 -> DestructibleType.RADAR_TOWER
        1 -> DestructibleType.GUN_TURRET
        2 -> DestructibleType.MISSILE_SILO
        else -> DestructibleType.SUPPLY_BUNKER
    }

    private fun spawnGroundTarget(groundType: DestructibleType) {
        val posX = Random.nextFloat() * (virtualResolution.width - groundType.width - 24f) + 12f
        val groundObj = GroundDestructible.create(
            type = groundType,
            id = "ground_${System.currentTimeMillis()}_${Random.nextInt(1000)}",
            initialPos = Vec2(posX, -35f),
            projectilePool = projectilePool,
            audio = audio,
            onDeathDrop = { dropPos -> onSpawnDrop(dropPos) },
            onChainExplosion = { center, radius, dmg -> onChainExplosion(center, radius, dmg) }
        ).apply {
            velocity = Vec2(0f, 36f)
        }

        groundTargets.add(groundObj)
        itemsLayer.addObject(groundObj)
    }
}
