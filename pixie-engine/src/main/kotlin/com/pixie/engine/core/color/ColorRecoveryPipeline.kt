package com.pixie.engine.core.color

import com.pixie.engine.core.entity.ObjectColorMode
import com.pixie.engine.core.entity.PixieObject

/**
 * Manages the global and fine-grained grayscale-to-color recovery state.
 *
 * Supports partial and gradual color restoration, configurable globally and
 * fine-tuned per object, group, or object class.
 */
class ColorRecoveryPipeline(
    initialProgress: Float = 0f,
    val autoRecoverRatePerSecond: Float = 0f
) {
    var progress: Float = initialProgress.coerceIn(0f, 1f)
        private set

    // Overrides for entire classes of objects (e.g. "sun", "coin", "sparks")
    val classOverrides = mutableMapOf<String, ObjectColorMode>()

    // Overrides for groups of objects (e.g. "chunk_oasis")
    val groupOverrides = mutableMapOf<String, ObjectColorMode>()

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
    }

    fun addProgress(amount: Float) {
        progress = (progress + amount).coerceIn(0f, 1f)
    }

    fun setClassOverride(className: String, mode: ObjectColorMode) {
        classOverrides[className] = mode
    }

    fun setGroupOverride(groupName: String, mode: ObjectColorMode) {
        groupOverrides[groupName] = mode
    }

    fun update(dt: Float) {
        if (autoRecoverRatePerSecond > 0f && progress < 1f) {
            addProgress(autoRecoverRatePerSecond * dt)
        }
    }

    /**
     * Resolves the effective saturation [0.0f..1.0f] for a specific object,
     * honoring object overrides first, then class overrides, then group overrides,
     * and finally the global recovery progress.
     */
    fun resolveObjectSaturation(obj: PixieObject): Float {
        // 1. Direct object override
        if (obj.colorMode !is ObjectColorMode.InheritGlobal) {
            return obj.resolveSaturation(progress)
        }

        // 2. Class override
        for (className in obj.classes) {
            val override = classOverrides[className]
            if (override != null) {
                return when (override) {
                    is ObjectColorMode.InheritGlobal -> progress
                    is ObjectColorMode.AlwaysColor -> 1.0f
                    is ObjectColorMode.AlwaysGrayscale -> 0.0f
                    is ObjectColorMode.CustomLevel -> override.level.coerceIn(0f, 1f)
                }
            }
        }

        // 3. Group override
        for (groupName in obj.groups) {
            val override = groupOverrides[groupName]
            if (override != null) {
                return when (override) {
                    is ObjectColorMode.InheritGlobal -> progress
                    is ObjectColorMode.AlwaysColor -> 1.0f
                    is ObjectColorMode.AlwaysGrayscale -> 0.0f
                    is ObjectColorMode.CustomLevel -> override.level.coerceIn(0f, 1f)
                }
            }
        }

        // 4. Fallback to global recovery progress
        return progress
    }

    fun reset(progress: Float = 0f) {
        this.progress = progress.coerceIn(0f, 1f)
    }
}
