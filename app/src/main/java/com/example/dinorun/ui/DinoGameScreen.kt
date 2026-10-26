package com.example.dinorun.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.unit.dp
import com.example.dinorun.DinoGameCoordinator
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.android.input.ControlMode
import com.pixie.android.input.DualModeInputAdapter
import com.pixie.android.ui.PixieGame
import com.pixie.engine.core.ports.AudioEvent

/**
 * Dinorun Game Screen refactored to use the PixieGame fluent builder DSL.
 *
 * Adheres to AGENTS.md rules:
 * - Fluent, chainable builder assembly.
 * - Declarative simplicity without manual focus/hardware key boilerplate.
 * - Responsive layouts for portrait and landscape form factors.
 * - Modular modal and HUD delegation.
 */
@Composable
fun DinoGameScreen(
    modifier: Modifier = Modifier,
    onExitToMenu: () -> Unit = {}
) {
    val context = LocalContext.current
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
            inputAdapter.reset()
        }
    }

    BackHandler {
        if (!isPaused && !isGameOver) {
            isPaused = true
            coordinator.engine.pause()
        } else {
            coordinator.engine.pause()
            onExitToMenu()
        }
    }

    val topHudContent: @Composable () -> Unit = {
        DinoTopHudBar(
            score = score,
            distanceMeters = distance,
            livesLeft = livesLeft,
            colorProgress = colorProgress,
            fps = fps,
            currentMode = currentControlMode,
            pixelsGap = pixelsGap,
            audioAdapter = audioAdapter,
            onToggleControlMode = {
                val nextMode = if (currentControlMode == ControlMode.VIRTUAL_BUTTONS) {
                    ControlMode.TOUCH_GESTURES
                } else {
                    ControlMode.VIRTUAL_BUTTONS
                }
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
            }
        )
    }

    val controlDeckContent: @Composable () -> Unit = {
        if (currentControlMode == ControlMode.VIRTUAL_BUTTONS) {
            DinoVirtualControlDeck(
                inputAdapter = inputAdapter,
                audioAdapter = audioAdapter,
                player = coordinator.player
            )
        }
    }

    // Fluent PixieGame assembly
    PixieGame(coordinator.engine)
        .input(inputAdapter)
        .haptic(hapticAdapter)
        .renderer(coordinator.renderer)
        .onTap {
            if (currentControlMode == ControlMode.TOUCH_GESTURES && !isPaused && !isGameOver) {
                val wasGrounded = coordinator.player.isGrounded
                coordinator.player.jump()
                if (wasGrounded && !coordinator.player.isGrounded) {
                    audioAdapter.playSound(AudioEvent.JUMP)
                }
            }
        }
        .onFrameTick {
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
        // ---------- PORTRAIT LAYOUT ----------
        .portrait(maxWidth = 600.dp) {
            fullCanvas(
                topOverlay = topHudContent,
                bottomOverlay = controlDeckContent
            )
        }
        // ---------- LANDSCAPE LAYOUT ----------
        .landscape(maxHeight = 600.dp) {
            fullCanvas(
                topOverlay = topHudContent,
                bottomOverlay = controlDeckContent
            )
        }
        // ---------- MODAL OVERLAYS ----------
        .overlays {
            DinoModals(
                isPaused = isPaused,
                isGameOver = isGameOver,
                coordinator = coordinator,
                audioAdapter = audioAdapter,
                hapticAdapter = hapticAdapter,
                isSoundEnabled = isSoundEnabled,
                isHapticEnabled = isHapticEnabled,
                score = score,
                distance = distance,
                colorProgress = colorProgress,
                onSoundToggled = { isSoundEnabled = it },
                onHapticToggled = { isHapticEnabled = it },
                onResume = {
                    isPaused = false
                    coordinator.engine.resume()
                },
                onRestart = {
                    isPaused = false
                    isGameOver = false
                    coordinator.restart()
                },
                onExitToMenu = {
                    isPaused = false
                    coordinator.engine.pause()
                    onExitToMenu()
                }
            )
        }
        // ---------- RENDER CANVAS & LAYOUT ----------
        .Render(modifier = modifier)
}
