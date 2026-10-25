package com.pixie.engine.core.layer

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.ports.RenderBatchPort
import com.pixie.engine.core.sprite.PixelSprite

/**
 * Layer 4: Ground / Gamefield
 *
 * Horizontal ground with height variation, pits, or static bounded arena (Arkanoid mode).
 * Top collision surface exactly matches [horizon.horizonY].
 */
class GroundGamefieldLayer(
    val horizon: HorizonCoordinator,
    override val order: Int = 400,
    override var isEnabled: Boolean = true,
    override var isGridEnabled: Boolean = false, // Default: solid for ground
    var isStaticArena: Boolean = false
) : PixieLayer {
    override val name: String = "Layer4_Ground"

    var groundHeightOffset: Float = 0f
    val pits = mutableListOf<FloatRange>() // X ranges of holes/pits

    data class FloatRange(val start: Float, val end: Float) {
        fun contains(x: Float): Boolean = x in start..end
    }

    override fun update(dt: Float, context: GameContext) {}

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        val screenWidth = batch.resolution.width.toFloat()
        val screenHeight = batch.resolution.height.toFloat()
        val horizonY = horizon.horizonY - camera.position.y + groundHeightOffset

        if (isStaticArena) {
            // Arkanoid fixed screen borders
            val wallColor = 0xFF607D8B.toInt()
            batch.drawPixelRect(0f, 0f, screenWidth, 8f, wallColor, context.colorRecoveryProgress) // Top wall
            batch.drawPixelRect(0f, 0f, 8f, screenHeight, wallColor, context.colorRecoveryProgress) // Left wall
            batch.drawPixelRect(screenWidth - 8f, 0f, 8f, screenHeight, wallColor, context.colorRecoveryProgress) // Right wall
            return
        }

        // Horizontal runner terrain
        val groundColor = 0xFF5D4037.toInt() // Dirt brown
        val grassColor = 0xFF388E3C.toInt()  // Top surface green
        val bedrockColor = 0xFF3E2723.toInt()

        val groundHeight = (screenHeight - horizonY).coerceAtLeast(0f)

        // Main ground block
        batch.drawPixelRect(0f, horizonY, screenWidth, groundHeight, groundColor, context.colorRecoveryProgress)
        // Top surface contour stripe (3px)
        batch.drawPixelRect(0f, horizonY, screenWidth, 3f, grassColor, context.colorRecoveryProgress)
        // Subsurface bedrock layer
        batch.drawPixelRect(0f, horizonY + 14f, screenWidth, groundHeight, bedrockColor, context.colorRecoveryProgress)

        // Draw ground details / texture spots
        val tileOffset = ((-camera.position.x % 32f) + 32f) % 32f
        var x = tileOffset - 32f
        while (x < screenWidth + 32f) {
            batch.drawPixelRect(x, horizonY + 6f, 6f, 2f, 0xFF795548.toInt(), context.colorRecoveryProgress)
            batch.drawPixelRect(x + 14f, horizonY + 10f, 4f, 2f, 0xFF4E342E.toInt(), context.colorRecoveryProgress)
            x += 32f
        }
    }
}

/**
 * Layer 5: Game Items & Entities
 *
 * Manages and renders all active interactive entities: player, hazards, pickups, particles.
 */
class GameItemsLayer(
    override val order: Int = 500,
    override var isEnabled: Boolean = true,
    override var isGridEnabled: Boolean = true // Default: grid enabled for objects and player
) : PixieLayer {
    override val name: String = "Layer5_Items"

    val objects = mutableListOf<PixieObject>()

    fun addObject(obj: PixieObject) {
        objects.add(obj)
    }

    fun removeObject(obj: PixieObject) {
        objects.remove(obj)
    }

    fun removeObject(id: String) {
        objects.removeAll { it.id == id }
    }

    fun clear() {
        objects.clear()
    }

    fun getObjectsInClass(className: String): List<PixieObject> {
        return objects.filter { it.isAlive && it.isInClass(className) }
    }

    fun getObjectsInGroup(groupName: String): List<PixieObject> {
        return objects.filter { it.isAlive && it.isInGroup(groupName) }
    }

    override fun update(dt: Float, context: GameContext) {
        val iterator = objects.iterator()
        while (iterator.hasNext()) {
            val obj = iterator.next()
            if (!obj.isAlive) {
                iterator.remove()
                continue
            }
            obj.update(dt, context)
        }
    }

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        // Sort objects by zIndex for proper depth layering
        val sorted = objects.filter { it.isVisible && it.isAlive }.sortedBy { it.zIndex }

        for (obj in sorted) {
            val screenPos = camera.worldToScreen(obj.position)
            val sprite = obj.getCurrentSprite()
            val saturation = obj.resolveSaturation(context.colorRecoveryProgress)

            // Render motion blur trail if active or naturally fading out
            if (obj.trailHistory.isNotEmpty()) {
                val effectiveStrength = obj.motionBlur.strength * obj.trailFadeAlpha
                if (effectiveStrength > 0.01f) {
                    val screenTrail = obj.trailHistory.map { camera.worldToScreen(it) }
                    batch.drawMotionBlur(sprite, screenTrail, effectiveStrength, saturation)
                }
            }

            // Render current sprite
            if (sprite != PixelSprite.EMPTY) {
                batch.drawSprite(sprite, screenPos.x, screenPos.y, saturation)
            } else {
                // Fallback bounding box pixel rect if no sprite set
                batch.drawPixelRect(screenPos.x, screenPos.y, obj.size.x, obj.size.y, 0xFFFF00FF.toInt(), saturation)
            }
        }
    }
}
