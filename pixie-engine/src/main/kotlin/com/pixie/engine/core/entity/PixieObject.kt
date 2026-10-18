package com.pixie.engine.core.entity

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite

/**
 * Color restoration policy for an individual object, class, or group.
 */
sealed class ObjectColorMode {
    /** Follows the global colorRecoveryProgress from GameContext [0f..1f]. */
    data object InheritGlobal : ObjectColorMode()

    /** Always rendered in full vibrant color (e.g. Celestial Sun, Power Crystals). */
    data object AlwaysColor : ObjectColorMode()

    /** Always rendered in black-and-white / grayscale (e.g. cursed relics, ancient fossils). */
    data object AlwaysGrayscale : ObjectColorMode()

    /** Fixed partial color recovery level [0f..1f]. */
    data class CustomLevel(val level: Float) : ObjectColorMode()
}

/**
 * Motion blur trail configuration per object or object class.
 */
data class MotionBlurConfig(
    val enabled: Boolean = false,
    val strength: Float = 0.5f,
    val maxTrailSamples: Int = 3
)

/**
 * Base game object in Pixie Engine.
 *
 * Encapsulates its own sprites, animation states, collision bounding box,
 * dynamic context-calculated parameters, motion blur, and class/group tagging.
 */
open class PixieObject(
    val id: String,
    var position: Vec2 = Vec2.ZERO,
    var velocity: Vec2 = Vec2.ZERO,
    var size: Vec2 = Vec2(16f, 16f),
    var zIndex: Float = 0f,
    initialGroups: Set<String> = emptySet(),
    initialClasses: Set<String> = emptySet()
) {
    var previousPosition: Vec2 = position
    var isAlive: Boolean = true
    var isSolid: Boolean = true
    var isVisible: Boolean = true

    // Multi-tagging architecture: Same object can belong to multiple groups or classes
    val groups: MutableSet<String> = initialGroups.toMutableSet()
    val classes: MutableSet<String> = initialClasses.toMutableSet()

    // Sprite & Animation state
    val animations: MutableMap<String, AnimationSequence> = mutableMapOf()
    var currentAnimationName: String = "default"
    var animationTimeSeconds: Float = 0f

    // Color restoration configuration
    var colorMode: ObjectColorMode = ObjectColorMode.InheritGlobal

    // Motion blur configuration and trail history
    var motionBlur: MotionBlurConfig = MotionBlurConfig()
    val trailHistory = ArrayDeque<Vec2>()

    // Context-dependent parameters (speed, jumpForce, damage, health, etc.)
    val parameters: MutableMap<String, Float> = mutableMapOf()

    // Dynamic parameter computation hook
    var parameterCalculator: ((objectRef: PixieObject, context: GameContext) -> Unit)? = null

    val boundingBox: RectF
        get() = RectF.fromTopLeft(position.x, position.y, size.x, size.y)

    fun isInClass(className: String): Boolean = classes.contains(className)
    fun isInGroup(groupName: String): Boolean = groups.contains(groupName)

    fun addClass(className: String) { classes.add(className) }
    fun removeClass(className: String) { classes.remove(className) }

    fun addGroup(groupName: String) { groups.add(groupName) }
    fun removeGroup(groupName: String) { groups.remove(groupName) }

    fun setAnimation(name: String, sequence: AnimationSequence) {
        animations[name] = sequence
        if (currentAnimationName.isEmpty() || currentAnimationName == "default") {
            currentAnimationName = name
        }
    }

    fun playAnimation(name: String, resetTime: Boolean = false) {
        if (currentAnimationName != name) {
            currentAnimationName = name
            if (resetTime) animationTimeSeconds = 0f
        }
    }

    fun getCurrentSprite(): PixelSprite {
        val anim = animations[currentAnimationName] ?: animations["default"]
        return anim?.getFrameAt(animationTimeSeconds) ?: PixelSprite.EMPTY
    }

    /**
     * Compute effective saturation level [0f..1f] for this object based on its mode and context.
     */
    fun resolveSaturation(globalRecovery: Float): Float {
        return when (val mode = colorMode) {
            is ObjectColorMode.InheritGlobal -> globalRecovery.coerceIn(0f, 1f)
            is ObjectColorMode.AlwaysColor -> 1.0f
            is ObjectColorMode.AlwaysGrayscale -> 0.0f
            is ObjectColorMode.CustomLevel -> mode.level.coerceIn(0f, 1f)
        }
    }

    open fun onContextChanged(context: GameContext) {
        parameterCalculator?.invoke(this, context)
    }

    open fun update(dt: Float, context: GameContext) {
        previousPosition = position
        animationTimeSeconds += dt

        // Dynamic parameter updates
        onContextChanged(context)

        // Motion blur history
        if (motionBlur.enabled) {
            trailHistory.addFirst(position)
            while (trailHistory.size > motionBlur.maxTrailSamples) {
                trailHistory.removeLast()
            }
        } else if (trailHistory.isNotEmpty()) {
            trailHistory.clear()
        }

        // Apply basic velocity integration
        position += velocity * dt
    }

    open fun onCollision(other: PixieObject, context: GameContext) {
        // Subclasses implement collision reactions
    }
}
