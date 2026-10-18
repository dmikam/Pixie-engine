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
 */
class AndroidHapticAdapter(context: Context) : HapticPort {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isEnabled: Boolean = true

    override fun trigger(type: HapticFeedbackType) {
        if (!isEnabled || vibrator == null || !vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    HapticFeedbackType.LIGHT -> VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticFeedbackType.MEDIUM -> VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticFeedbackType.HEAVY -> VibrationEffect.createOneShot(65, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticFeedbackType.DOUBLE_PULSE -> VibrationEffect.createWaveform(longArrayOf(0, 20, 40, 30), -1)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                when (type) {
                    HapticFeedbackType.LIGHT -> vibrator.vibrate(18)
                    HapticFeedbackType.MEDIUM -> vibrator.vibrate(35)
                    HapticFeedbackType.HEAVY -> vibrator.vibrate(65)
                    HapticFeedbackType.DOUBLE_PULSE -> vibrator.vibrate(longArrayOf(0, 20, 40, 30), -1)
                }
            }
        } catch (_: Exception) {
            // Gracefully ignore vibration errors
        }
    }
}
