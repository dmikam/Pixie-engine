package com.pixie.engine.core.weapon

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.ports.HapticPort

/**
 * Encapsulated weapon firing controller for ships, turrets, and bosses.
 *
 * Drives weapon cooldowns, multi-shot projectile spawning from [ProjectilePool],
 * trajectory assignment, and dynamic context calculation.
 */
class WeaponSystem(
    var weapon: WeaponDefinition,
    val pool: ProjectilePool,
    var isPlayer: Boolean = true,
    var audio: AudioPort? = null,
    var haptics: HapticPort? = null
) {
    var cooldownTimer: Float = 0f
    var shotCount: Int = 1 // 1 = single shot, 2 = twin parallel, 3 = 3-way spread, etc.
    var targetGetter: (() -> PixieObject?)? = null

    fun update(dt: Float) {
        if (cooldownTimer > 0f) {
            cooldownTimer = (cooldownTimer - dt).coerceAtLeast(0f)
        }
    }

    /**
     * Fires the weapon if cooldown has elapsed. Returns spawned projectiles (can be added to itemsLayer).
     */
    fun fire(
        origin: Vec2,
        headingRad: Float,
        context: GameContext,
        outList: MutableList<Projectile>
    ): Boolean {
        if (cooldownTimer > 0f) return false

        val dynamicFireRate = weapon.calculateDynamicFireRate(context, isPlayer)
        cooldownTimer = 1.0f / dynamicFireRate.coerceAtLeast(0.1f)

        val speed = weapon.calculateDynamicBulletSpeed(context, isPlayer)
        val damage = weapon.calculateDynamicHitPower(context, isPlayer)
        val target = targetGetter?.invoke()

        for (i in 0 until shotCount) {
            val proj = pool.obtain()
            val traj = weapon.trajectoryFactory(i, shotCount, headingRad, target)

            // Calculate spawn offset (e.g. parallel twin barrels or single center)
            val spawnPos = if (shotCount == 2) {
                val lateralOffset = if (i == 0) -5f else 5f
                origin + Vec2(lateralOffset, 0f)
            } else {
                origin
            }

            proj.spawn(
                pos = spawnPos,
                definition = weapon,
                isPlayer = isPlayer,
                traj = traj,
                speed = speed,
                damage = damage
            )
            outList.add(proj)
        }

        // Trigger sound & haptics
        audio?.playSound(weapon.audioEvent)
        weapon.hapticFeedback?.let { haptics?.trigger(it) }

        return true
    }

    fun reset() {
        cooldownTimer = 0f
    }
}
