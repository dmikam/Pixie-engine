package com.example.arkanoid.entities

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.matrix.BrickHitResult
import com.pixie.engine.core.matrix.DestructibleMatrixLayer
import com.pixie.engine.core.particle.ExplosionType
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.ports.RenderBatchPort

class ArkanoidLaser(
    var position: Vec2,
    val speed: Float = 240f
) {
    val width: Float = 2f
    val height: Float = 6f
    var isAlive: Boolean = true

    val boundingBox: RectF
        get() = RectF.fromCenter(position.x, position.y, width, height)

    fun update(
        dt: Float,
        matrix: DestructibleMatrixLayer,
        audioPort: AudioPort,
        particleLayer: ParticleLayer,
        onHit: (BrickHitResult) -> Unit
    ) {
        if (!isAlive) return

        position = Vec2(position.x, position.y - speed * dt)

        if (position.y < 10f) {
            isAlive = false
            return
        }

        // Check collision with brick matrix
        val gridCoords = matrix.worldToGrid(position.x, position.y)
        if (gridCoords != null) {
            val (row, col) = gridCoords
            val cell = matrix.getCell(row, col)
            if (cell != null && !cell.isDestroyed) {
                isAlive = false
                val hitRes = matrix.hitCell(row, col, damage = 1)
                audioPort.playSound(if (hitRes.wasDestroyed) AudioEvent.EXPLOSION_SMALL else AudioEvent.HIT)
                particleLayer.spawnExplosion(ExplosionType.SPARK_HIT, position, scale = 0.7f)
                onHit(hitRes)
            }
        }
    }

    fun render(batch: RenderBatchPort, camera: PixieCamera) {
        if (!isAlive) return

        val box = boundingBox
        val screenPos = camera.worldToScreen(Vec2(box.left, box.top))

        batch.drawPixelRect(screenPos.x, screenPos.y, box.width, box.height, 0xFFFF1744.toInt())
        batch.drawPixelRect(screenPos.x, screenPos.y + 1f, box.width, 2f, 0xFFFFEB3B.toInt())
    }
}
