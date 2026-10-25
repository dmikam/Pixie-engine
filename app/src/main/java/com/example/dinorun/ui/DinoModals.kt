package com.example.dinorun.ui

import androidx.compose.runtime.Composable
import com.example.dinorun.DinoGameCoordinator
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.HapticFeedbackType

/**
 * Encapsulated dialog overlays for Dinorun (Pause and Game Over states).
 */
@Composable
fun DinoModals(
    isPaused: Boolean,
    isGameOver: Boolean,
    coordinator: DinoGameCoordinator,
    audioAdapter: AndroidSoundPoolAudioAdapter,
    hapticAdapter: AndroidHapticAdapter,
    isSoundEnabled: Boolean,
    isHapticEnabled: Boolean,
    score: Long,
    distance: Float,
    colorProgress: Float,
    onSoundToggled: (Boolean) -> Unit,
    onHapticToggled: (Boolean) -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit
) {
    if (isPaused) {
        DinoPauseDialog(
            layerStack = coordinator.engine.layerStack,
            currentWorld = coordinator.currentWorld,
            isSoundEnabled = isSoundEnabled,
            isHapticEnabled = isHapticEnabled,
            onToggleSound = { enabled ->
                onSoundToggled(enabled)
                audioAdapter.isMuted = !enabled
                if (enabled) {
                    audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                }
            },
            onToggleHaptic = { enabled ->
                onHapticToggled(enabled)
                hapticAdapter.isEnabled = enabled
                if (enabled) {
                    hapticAdapter.trigger(HapticFeedbackType.LIGHT)
                }
            },
            onSelectWorld = { world ->
                coordinator.switchWorld(world)
                onResume()
            },
            onResume = onResume,
            onRestart = onRestart,
            onExitToMenu = onExitToMenu
        )
    }

    if (isGameOver) {
        DinoGameOverDialog(
            score = score,
            distanceMeters = distance,
            colorRecoveryPercent = (colorProgress * 100).toInt(),
            onRestart = onRestart,
            onExitToMenu = onExitToMenu
        )
    }
}
