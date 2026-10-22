package com.example.dinorun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.dinorun.DinoGameCoordinator
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import androidx.activity.compose.BackHandler
import com.pixie.android.input.ControlMode
import com.pixie.android.input.DualModeInputAdapter
import com.pixie.android.ui.PixieGameCanvas
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.HapticFeedbackType

@Composable
fun DinoGameScreen(
    modifier: Modifier = Modifier,
    onExitToMenu: () -> Unit = {}
) {
    val context = LocalContext.current

    // Initialize platform adapters
    val audioAdapter = remember { AndroidSoundPoolAudioAdapter(context) }
    val hapticAdapter = remember { AndroidHapticAdapter(context) }
    val inputAdapter = remember { DualModeInputAdapter() }

    val coordinator = remember {
        DinoGameCoordinator(
            audioAdapter = audioAdapter,
            hapticAdapter = hapticAdapter,
            inputAdapter = inputAdapter
        ).apply {
            engine.start()
        }
    }

    // UI observable state
    var isPaused by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var isSoundEnabled by remember { mutableStateOf(!audioAdapter.isMuted) }
    var isHapticEnabled by remember { mutableStateOf(hapticAdapter.isEnabled) }
    var score by remember { mutableLongStateOf(0L) }
    var distance by remember { mutableFloatStateOf(0f) }
    var livesLeft by remember { mutableIntStateOf(3) }
    var colorProgress by remember { mutableFloatStateOf(0f) }
    var fps by remember { mutableFloatStateOf(60f) }
    var currentControlMode by remember { mutableStateOf(ControlMode.VIRTUAL_BUTTONS) }
    var pixelsGap by remember { mutableIntStateOf(1) }

    coordinator.onStateChanged = {
        isGameOver = coordinator.engine.context.isGameOver
        livesLeft = coordinator.engine.context.livesLeft
        score = coordinator.engine.context.score
        colorProgress = coordinator.engine.colorRecovery.progress
    }

    DisposableEffect(coordinator) {
        onDispose {
            coordinator.engine.stop()
            audioAdapter.release()
        }
    }

    BackHandler {
        coordinator.engine.pause()
        onExitToMenu()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        // Main Game Canvas with synchronized frame ticks
        PixieGameCanvas(
            engine = coordinator.engine,
            renderer = coordinator.renderer,
            inputAdapter = inputAdapter,
            modifier = Modifier.fillMaxSize(),
            onFrameTick = { deltaSeconds ->
                if (!isPaused && !isGameOver) {
                    coordinator.updateInputs()
                    coordinator.checkCustomCollisions()

                    // Sync HUD values
                    score = coordinator.engine.context.score
                    distance = coordinator.engine.context.distance
                    livesLeft = coordinator.engine.context.livesLeft
                    colorProgress = coordinator.engine.colorRecovery.progress
                    fps = coordinator.engine.loop.currentFps
                    isGameOver = coordinator.engine.context.isGameOver
                }
            }
        )

        // Overlay HUD
        DinoHud(
            score = score,
            distanceMeters = distance,
            livesLeft = livesLeft,
            colorProgress = colorProgress,
            fps = fps,
            currentMode = currentControlMode,
            pixelsGap = pixelsGap,
            onToggleControlMode = {
                val nextMode = if (currentControlMode == ControlMode.VIRTUAL_BUTTONS)
                    ControlMode.TOUCH_GESTURES else ControlMode.VIRTUAL_BUTTONS
                currentControlMode = nextMode
                inputAdapter.currentMode = nextMode
            },
            onTogglePixelGap = {
                val nextGap = if (pixelsGap == 0) 1 else 0
                pixelsGap = nextGap
                coordinator.setPixelGap(nextGap)
            },
            onPauseClick = {
                isPaused = true
                coordinator.engine.pause()
            },
            onExitToMenu = {
                coordinator.engine.pause()
                onExitToMenu()
            },
            inputAdapter = inputAdapter
        )

        // Pause Dialog with Layer Inspector and World Switcher
        if (isPaused) {
            DinoPauseDialog(
                layerStack = coordinator.engine.layerStack,
                currentWorld = coordinator.currentWorld,
                isSoundEnabled = isSoundEnabled,
                isHapticEnabled = isHapticEnabled,
                onToggleSound = { enabled ->
                    isSoundEnabled = enabled
                    audioAdapter.isMuted = !enabled
                    if (enabled) {
                        audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    }
                },
                onToggleHaptic = { enabled ->
                    isHapticEnabled = enabled
                    hapticAdapter.isEnabled = enabled
                    if (enabled) {
                        hapticAdapter.trigger(HapticFeedbackType.LIGHT)
                    }
                },
                onSelectWorld = { world ->
                    coordinator.switchWorld(world)
                    isPaused = false
                    coordinator.engine.resume()
                },
                onResume = {
                    isPaused = false
                    coordinator.engine.resume()
                },
                onRestart = {
                    isPaused = false
                    coordinator.restart()
                },
                onExitToMenu = {
                    isPaused = false
                    coordinator.engine.pause()
                    onExitToMenu()
                }
            )
        }

        // Game Over Dialog
        if (isGameOver) {
            DinoGameOverDialog(
                score = score,
                distanceMeters = distance,
                colorRecoveryPercent = (colorProgress * 100).toInt(),
                onRestart = {
                    coordinator.restart()
                    isGameOver = false
                },
                onExitToMenu = {
                    coordinator.engine.pause()
                    onExitToMenu()
                }
            )
        }
    }
}
