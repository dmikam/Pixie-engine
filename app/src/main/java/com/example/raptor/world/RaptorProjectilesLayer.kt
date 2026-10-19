package com.example.raptor.world

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.layer.PixieLayer
import com.pixie.engine.core.ports.RenderBatchPort
import com.pixie.engine.core.weapon.Projectile

/**
 * Dedicated rendering layer for Raptor projectiles (order 520, between items and particles).
 *
 * Renders player kinetic bullets, plasma bolts, homing missiles with motion blur trails,
 * and enemy crimson laser salvos with high performance.
 */
class RaptorProjectilesLayer(
    var playerProjectilesGetter: () -> List<Projectile>,
    var enemyProjectilesGetter: () -> List<Projectile>,
    override val order: Int = 520,
    override var isEnabled: Boolean = true,
    override var isGridEnabled: Boolean = true
) : PixieLayer {
    override val name: String = "Raptor_Projectiles"

    override fun update(dt: Float, context: GameContext) {
        // Projectiles physics are driven in coordinator update loop
    }

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        if (!isEnabled) return

        val width = 240f
        val height = 320f

        // 1. Player Projectiles with motion blur trails
        val playerList = playerProjectilesGetter()
        for (i in 0 until playerList.size) {
            val proj = playerList[i]
            if (proj.isAlive && proj.position.y >= -20f && proj.position.y <= height + 20f &&
                proj.position.x >= -20f && proj.position.x <= width + 20f
            ) {
                // Draw motion blur trail first (behind bullet head)
                if (proj.motionBlur.enabled && proj.trailHistory.isNotEmpty()) {
                    batch.drawMotionBlur(
                        proj.getCurrentSprite(),
                        proj.trailHistory.toList(),
                        proj.motionBlur.strength
                    )
                }
                // Draw bullet sprite
                batch.drawSprite(proj.getCurrentSprite(), proj.position.x, proj.position.y)
            }
        }

        // 2. Enemy Projectiles
        val enemyList = enemyProjectilesGetter()
        for (i in 0 until enemyList.size) {
            val proj = enemyList[i]
            if (proj.isAlive && proj.position.y >= -20f && proj.position.y <= height + 20f &&
                proj.position.x >= -20f && proj.position.x <= width + 20f
            ) {
                batch.drawSprite(proj.getCurrentSprite(), proj.position.x, proj.position.y)
            }
        }
    }
}
