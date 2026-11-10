package com.pixie.android.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.pixie.engine.core.ports.HapticFeedbackType
import com.pixie.engine.core.ports.HapticPort

/**
 * Standardized Android Haptics adapter for Pixie Engine.
 * Supports classic arcade pulses, modern tactile micro-haptics, and 8-bit platformer
 * feedback signatures with graceful backward compatibility across Android API levels.
 */
class AndroidHapticAdapter(context: Context) : HapticPort {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    override var isEnabled: Boolean = true

    /**
     * Fluent chainable setter for toggling haptic feedback.
     */
    fun setEnabled(enabled: Boolean): AndroidHapticAdapter = apply {
        this.isEnabled = enabled
    }

    override fun trigger(type: HapticFeedbackType) {
        if (!isEnabled || vibrator == null || !vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = createVibrationEffect(type)
                if (effect != null) {
                    vibrator.vibrate(effect)
                }
            } else {
                triggerLegacyVibration(type)
            }
        } catch (_: Exception) {
            // Gracefully ignore vibration errors on test shadows or unsupported hardware
        }
    }

    // Platformer distinct feedback signatures (fluent / chainable)
    override fun triggerJump(): AndroidHapticAdapter = apply { trigger(HapticFeedbackType.TICK) }
    override fun triggerCoinCollect(): AndroidHapticAdapter = apply { trigger(HapticFeedbackType.DOUBLE_TAP) }
    override fun triggerStomp(): AndroidHapticAdapter = apply { trigger(HapticFeedbackType.PULSE) }
    override fun triggerCrateThrow(): AndroidHapticAdapter = apply { trigger(HapticFeedbackType.PULSE) }
    override fun triggerBrickShatter(): AndroidHapticAdapter = apply { trigger(HapticFeedbackType.JOLT) }
    override fun triggerPlayerDamage(): AndroidHapticAdapter = apply { trigger(HapticFeedbackType.JOLT) }

    private fun createVibrationEffect(type: HapticFeedbackType): VibrationEffect? {
        val v = vibrator ?: return null
        val hasAmp = v.hasAmplitudeControl()

        return when (type) {
            // Jump: Crisp tick (10ms, light amplitude)
            HapticFeedbackType.TICK -> {
                val amp = if (hasAmp) 80 else VibrationEffect.DEFAULT_AMPLITUDE
                VibrationEffect.createOneShot(10, amp)
            }

            // Coin collect: Light double-tap click
            HapticFeedbackType.DOUBLE_TAP -> {
                val timings = longArrayOf(0, 10, 35, 12)
                if (hasAmp) {
                    val amplitudes = intArrayOf(0, 90, 0, 110)
                    VibrationEffect.createWaveform(timings, amplitudes, -1)
                } else {
                    VibrationEffect.createWaveform(timings, -1)
                }
            }

            // Stomp / Crate throw: Medium punchy pulse (30ms)
            HapticFeedbackType.PULSE -> {
                val amp = if (hasAmp) 180 else VibrationEffect.DEFAULT_AMPLITUDE
                VibrationEffect.createOneShot(30, amp)
            }

            // Brick shatter / Player damage: Heavy tactile jolt (50ms)
            HapticFeedbackType.JOLT -> {
                val amp = if (hasAmp) 255 else VibrationEffect.DEFAULT_AMPLITUDE
                VibrationEffect.createOneShot(50, amp)
            }

            // Standard general presets
            HapticFeedbackType.LIGHT -> {
                val amp = if (hasAmp) 100 else VibrationEffect.DEFAULT_AMPLITUDE
                VibrationEffect.createOneShot(18, amp)
            }
            HapticFeedbackType.MEDIUM -> {
                val amp = if (hasAmp) 180 else VibrationEffect.DEFAULT_AMPLITUDE
                VibrationEffect.createOneShot(35, amp)
            }
            HapticFeedbackType.HEAVY -> {
                val amp = if (hasAmp) 255 else VibrationEffect.DEFAULT_AMPLITUDE
                VibrationEffect.createOneShot(65, amp)
            }
            HapticFeedbackType.DOUBLE_PULSE -> {
                VibrationEffect.createWaveform(longArrayOf(0, 20, 40, 30), -1)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun triggerLegacyVibration(type: HapticFeedbackType) {
        val v = vibrator ?: return
        when (type) {
            HapticFeedbackType.TICK -> v.vibrate(10)
            HapticFeedbackType.DOUBLE_TAP -> v.vibrate(longArrayOf(0, 10, 35, 12), -1)
            HapticFeedbackType.PULSE -> v.vibrate(30)
            HapticFeedbackType.JOLT -> v.vibrate(50)
            HapticFeedbackType.LIGHT -> v.vibrate(18)
            HapticFeedbackType.MEDIUM -> v.vibrate(35)
            HapticFeedbackType.HEAVY -> v.vibrate(65)
            HapticFeedbackType.DOUBLE_PULSE -> v.vibrate(longArrayOf(0, 20, 40, 30), -1)
        }
    }
}
