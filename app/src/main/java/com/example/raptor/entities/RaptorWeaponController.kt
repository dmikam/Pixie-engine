package com.example.raptor.entities

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.ports.HapticFeedbackType
import com.pixie.engine.core.ports.HapticPort
import com.pixie.engine.core.weapon.Projectile
import com.pixie.engine.core.weapon.ProjectilePool
import com.pixie.engine.core.weapon.StandardWeapons
import com.pixie.engine.core.weapon.WeaponSystem

/**
 * Controller orchestrating weapon tiers, primary firing salvos, homing missile deployment,
 * and mega-bomb screen clearance for the player ship.
 *
 * Encapsulates projectile salvo math, barrel offsets, and weapon switching logic.
 */
class RaptorWeaponController(
    val projectilePool: ProjectilePool,
    var audio: AudioPort? = null,
    var haptics: HapticPort? = null
) {
    var weaponTier: Int = 1
    var activeWeaponId: String = "vulcan"

    val primaryWeaponSystem = WeaponSystem(
        weapon = StandardWeapons.VULCAN_CANNON,
        pool = projectilePool,
        isPlayer = true,
        audio = audio,
        haptics = haptics
    )

    val missileWeaponSystem = WeaponSystem(
        weapon = StandardWeapons.HOMING_MISSILES,
        pool = projectilePool,
        isPlayer = true,
        audio = audio,
        haptics = haptics
    )

    fun equipWeapon(weaponId: String) {
        activeWeaponId = weaponId
        updateWeaponTier(GameContext())
    }

    fun updateWeaponTier(context: GameContext) {
        when (activeWeaponId) {
            "odin_laser" -> configureOdinLaser()
            "deathray" -> configureDeathray()
            "twin_plasma" -> configureTwinPlasma()
            else -> configureStandardVulcanOrPlasma()
        }
    }

    private fun configureOdinLaser() {
        primaryWeaponSystem.weapon = StandardWeapons.ODIN_LASER
        primaryWeaponSystem.shotCount = if (weaponTier >= 3) 2 else 1
    }

    private fun configureDeathray() {
        primaryWeaponSystem.weapon = StandardWeapons.DEATHRAY
        primaryWeaponSystem.shotCount = 1
    }

    private fun configureTwinPlasma() {
        primaryWeaponSystem.weapon = StandardWeapons.TWIN_PLASMA
        primaryWeaponSystem.shotCount = if (weaponTier >= 3) 4 else 2
    }

    private fun configureStandardVulcanOrPlasma() {
        when (weaponTier) {
            1 -> {
                primaryWeaponSystem.weapon = StandardWeapons.VULCAN_CANNON
                primaryWeaponSystem.shotCount = 1
            }
            2 -> {
                primaryWeaponSystem.weapon = StandardWeapons.VULCAN_CANNON
                primaryWeaponSystem.shotCount = 2
            }
            3 -> {
                primaryWeaponSystem.weapon = StandardWeapons.TWIN_PLASMA
                primaryWeaponSystem.shotCount = 2
            }
            else -> {
                primaryWeaponSystem.weapon = StandardWeapons.TWIN_PLASMA
                primaryWeaponSystem.shotCount = 4
            }
        }
    }

    fun upgradeWeapon(): Boolean {
        if (weaponTier < 4) {
            weaponTier++
            audio?.playSound(AudioEvent.POWERUP_PICKUP)
            haptics?.trigger(HapticFeedbackType.DOUBLE_PULSE)
            return true
        }
        return false
    }

    fun firePrimary(shipPos: Vec2, shipSize: Vec2, context: GameContext, outList: MutableList<Projectile>): Boolean {
        val nosePos = shipPos + Vec2(shipSize.x * 0.5f, 0f)
        return primaryWeaponSystem.fire(
            origin = nosePos,
            headingRad = -1.5707963f, // straight up
            context = context,
            outList = outList
        )
    }

    fun fireMissiles(
        shipPos: Vec2,
        shipSize: Vec2,
        context: GameContext,
        target: PixieObject?,
        outList: MutableList<Projectile>
    ): Boolean {
        missileWeaponSystem.targetGetter = { target }
        val wingLeft = shipPos + Vec2(2f, 4f)
        val wingRight = shipPos + Vec2(shipSize.x - 2f, 4f)

        val fired = missileWeaponSystem.fire(
            origin = wingLeft,
            headingRad = -1.5707963f,
            context = context,
            outList = outList
        )
        if (fired) {
            missileWeaponSystem.fire(
                origin = wingRight,
                headingRad = -1.5707963f,
                context = context,
                outList = outList
            )
        }
        return fired
    }

    fun triggerMegaBomb(
        shipPos: Vec2,
        shipSize: Vec2,
        sourcePlayer: PixieObject,
        visibleEnemies: List<PixieObject>,
        activeEnemyProjectiles: List<Projectile>,
        particleLayer: ParticleLayer?
    ) {
        audio?.playSound(AudioEvent.EXPLOSION_LARGE)
        haptics?.trigger(HapticFeedbackType.HEAVY)

        for (bullet in activeEnemyProjectiles) {
            bullet.isAlive = false
            particleLayer?.spawnSparks(bullet.position, count = 4, color = 0xFF00E5FF.toInt())
        }

        for (enemy in visibleEnemies) {
            if (enemy.isAlive) {
                particleLayer?.spawnExplosion(ExplosionType.AERIAL_EXPLOSION, enemy.position, scale = 1.2f)
                enemy.onCollision(sourcePlayer, GameContext())
            }
        }

        particleLayer?.spawnExplosion(ExplosionType.SHOCKWAVE, shipPos + (shipSize * 0.5f), scale = 2.0f)
    }

    fun update(dt: Float) {
        primaryWeaponSystem.update(dt)
        missileWeaponSystem.update(dt)
    }
}
