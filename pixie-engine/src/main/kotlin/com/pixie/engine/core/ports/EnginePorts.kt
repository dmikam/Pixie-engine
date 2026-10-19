package com.pixie.engine.core.ports

import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.sprite.PixelSprite

data class VirtualResolution(val width: Int, val height: Int)

/**
 * Pure Kotlin rendering port implemented by platform-specific UI adapters (e.g. Android Canvas).
 */
interface RenderBatchPort {
    val resolution: VirtualResolution
    var pixelsGap: Int
    var isCurrentLayerGridEnabled: Boolean

    fun fillBackground(color: Int)
    fun drawPixelRect(x: Float, y: Float, width: Float, height: Float, color: Int, saturation: Float = 1f, alpha: Float = 1f)
    fun drawSprite(sprite: PixelSprite, x: Float, y: Float, saturation: Float = 1f, alpha: Float = 1f)
    fun drawMotionBlur(sprite: PixelSprite, trail: List<Vec2>, strength: Float, saturation: Float = 1f)
    fun drawText(text: String, x: Float, y: Float, color: Int, sizePx: Float = 8f)
    fun endFrame() {}
}

/**
 * Pure Kotlin sound events port implemented by SoundPool or platform audio engine.
 */
enum class AudioEvent {
    JUMP,
    DUCK,
    HIT,
    COLLECT,
    SCORE_MILESTONE,
    GAME_OVER,
    BUTTON_CLICK,
    DAY_NIGHT_SHIFT,
    FIRE_PRIMARY,
    FIRE_MISSILE,
    EXPLOSION_SMALL,
    EXPLOSION_LARGE,
    SHIELD_HIT,
    POWERUP_PICKUP
}

interface AudioPort {
    fun playSound(event: AudioEvent)
}

/**
 * Pure Kotlin haptic feedback port implemented by Android Vibrator adapter.
 */
enum class HapticFeedbackType {
    LIGHT,
    MEDIUM,
    HEAVY,
    DOUBLE_PULSE
}

interface HapticPort {
    fun trigger(type: HapticFeedbackType)
}

/**
 * Pure Kotlin input port providing normalized player controls.
 */
interface InputPort {
    val moveX: Float
    val moveY: Float
    val isJumpPressed: Boolean
    val isDuckPressed: Boolean
    val isActionPressed: Boolean
}
