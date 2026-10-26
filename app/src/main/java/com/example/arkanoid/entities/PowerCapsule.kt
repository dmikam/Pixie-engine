package com.example.arkanoid.entities

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.matrix.PowerUpType
import com.pixie.engine.core.ports.RenderBatchPort

class PowerCapsule(
    var position: Vec2,
    val type: PowerUpType,
    val fallSpeed: Float = 38f
) {
    val width: Float = 12f
    val height: Float = 6f
    var isAlive: Boolean = true

    val boundingBox: RectF
        get() = RectF.fromCenter(position.x, position.y, width, height)

    fun update(dt: Float, paddle: ArkanoidPaddle, onCaught: (PowerUpType) -> Unit) {
        if (!isAlive) return

        position = Vec2(position.x, position.y + fallSpeed * dt)

        // Check if caught by paddle
        if (boundingBox.intersects(paddle.boundingBox)) {
            isAlive = false
            onCaught(type)
            return
        }

        // Falls below screen
        if (position.y > 185f) {
            isAlive = false
        }
    }

    fun render(batch: RenderBatchPort, camera: PixieCamera) {
        if (!isAlive) return

        val box = boundingBox
        val screenPos = camera.worldToScreen(Vec2(box.left, box.top))

        val def = com.example.arkanoid.world.ArkanoidPowerupCatalog.getDefinition(type)
        val capsuleColor = def.primaryColor
        val glyphColor = def.glyphColor

        // Pill shape: central body + beveled cap ends
        batch.drawPixelRect(screenPos.x, screenPos.y, box.width, box.height, capsuleColor)
        batch.drawPixelRect(screenPos.x, screenPos.y, box.width, 1f, 0x88FFFFFF.toInt())
        batch.drawPixelRect(screenPos.x, screenPos.y + box.height - 1f, box.width, 1f, 0x88000000.toInt())

        // Center identifier pip
        batch.drawText(def.glyph, screenPos.x + 3f, screenPos.y + 1f, glyphColor, sizePx = 6f)
    }
}
