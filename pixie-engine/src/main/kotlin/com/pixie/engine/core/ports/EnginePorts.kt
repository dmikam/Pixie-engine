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
    POWERUP_PICKUP,

    // NES Platformer Audio Events
    PLATFORMER_JUMP,
    PLATFORMER_JUMP_SUPER,
    COIN_DING,
    BLOCK_BUMP,
    BRICK_BREAK,
    POWERUP_SPAWN,
    POWERUP_COLLECT,
    ENEMY_STOMP,
    CRATE_THROW,
    STAGE_CLEAR
}

interface AudioPort {
    var isMuted: Boolean
        get() = false
        set(value) {}
    fun playSound(event: AudioEvent)
}

/**
 * Pure Kotlin haptic feedback port implemented by Android Vibrator adapter.
 */
enum class HapticFeedbackType {
    LIGHT,
    MEDIUM,
    HEAVY,
    DOUBLE_PULSE,

    // Platformer tactile micro-haptic patterns
    TICK,
    DOUBLE_TAP,
    PULSE,
    JOLT
}

interface HapticPort {
    var isEnabled: Boolean
        get() = true
        set(value) {}
    fun trigger(type: HapticFeedbackType)

    fun triggerJump(): HapticPort = apply { trigger(HapticFeedbackType.TICK) }
    fun triggerCoinCollect(): HapticPort = apply { trigger(HapticFeedbackType.DOUBLE_TAP) }
    fun triggerStomp(): HapticPort = apply { trigger(HapticFeedbackType.PULSE) }
    fun triggerCrateThrow(): HapticPort = apply { trigger(HapticFeedbackType.PULSE) }
    fun triggerBrickShatter(): HapticPort = apply { trigger(HapticFeedbackType.JOLT) }
    fun triggerPlayerDamage(): HapticPort = apply { trigger(HapticFeedbackType.JOLT) }
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
    val isActionSecondaryPressed: Boolean
        get() = false
}
