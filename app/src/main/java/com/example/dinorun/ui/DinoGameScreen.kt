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
 * - Modular modal and HUD delegation.
 * - Functions strictly under 50 lines.
 */
@Composable
fun DinoGameScreen(
    modifier: Modifier = Modifier,
    onExitToMenu: () -> Unit = {}
) {
    val state = rememberDinoGameState(onExitToMenu = onExitToMenu)

    BackHandler {
        state.handleBack()
    }

    DinoPixieGameView(state = state, modifier = modifier)
}

@Composable
private fun rememberDinoGameState(
    onExitToMenu: () -> Unit
): DinoGameState {
    val context = LocalContext.current
    val audioAdapter = remember { AndroidSoundPoolAudioAdapter(context) }
    val hapticAdapter = remember { AndroidHapticAdapter(context) }
    val inputAdapter = remember { DualModeInputAdapter() }

    val coordinator = remember {
        DinoGameCoordinator(
            audioAdapter = audioAdapter,
            hapticAdapter = hapticAdapter,
            inputAdapter = inputAdapter
        ).apply { engine.start() }
    }

    DisposableEffect(coordinator) {
        onDispose {
            coordinator.engine.stop()
            audioAdapter.release()
            inputAdapter.reset()
        }
    }

    return remember(coordinator) {
        DinoGameState(
            coordinator = coordinator,
            audioAdapter = audioAdapter,
            hapticAdapter = hapticAdapter,
            inputAdapter = inputAdapter,
            onExitToMenu = onExitToMenu
        )
    }
}

@Composable
private fun DinoPixieGameView(
    state: DinoGameState,
    modifier: Modifier = Modifier
) {
    val topHud: @Composable () -> Unit = { DinoGameTopOverlay(state) }
    val bottomDeck: @Composable () -> Unit = { DinoGameBottomDeck(state) }

    PixieGame(state.coordinator.engine)
        .input(state.inputAdapter)
        .haptic(state.hapticAdapter)
        .renderer(state.coordinator.renderer)
        .onTap { state.onCanvasTap() }
        .onFrameTick { state.onFrameTick() }
        .portrait(maxWidth = 600.dp) {
            fullCanvas(topOverlay = topHud, bottomOverlay = bottomDeck)
        }
        .landscape(maxHeight = 600.dp) {
            fullCanvas(topOverlay = topHud, bottomOverlay = bottomDeck)
        }
        .overlays {
            DinoGameOverlays(state)
        }
        .Render(modifier = modifier)
}

@Composable
private fun DinoGameTopOverlay(state: DinoGameState) {
    DinoTopHudBar(
        score = state.score,
        distanceMeters = state.distance,
        livesLeft = state.livesLeft,
        colorProgress = state.colorProgress,
        fps = state.fps,
        currentMode = state.currentControlMode,
        pixelsGap = state.pixelsGap,
        audioAdapter = state.audioAdapter,
        onToggleControlMode = { state.toggleControlMode() },
        onTogglePixelGap = { state.togglePixelGap() },
        onPauseClick = {
            state.isPaused = true
            state.coordinator.engine.pause()
        },
        onExitToMenu = {
            state.coordinator.engine.pause()
            state.onExitToMenu()
        }
    )
}

@Composable
private fun DinoGameBottomDeck(state: DinoGameState) {
    if (state.currentControlMode == ControlMode.VIRTUAL_BUTTONS) {
        DinoVirtualControlDeck(
            inputAdapter = state.inputAdapter,
            audioAdapter = state.audioAdapter,
            player = state.coordinator.player
        )
    }
}

@Composable
private fun DinoGameOverlays(state: DinoGameState) {
    DinoModals(
        isPaused = state.isPaused,
        isGameOver = state.isGameOver,
        coordinator = state.coordinator,
        audioAdapter = state.audioAdapter,
        hapticAdapter = state.hapticAdapter,
        isSoundEnabled = state.isSoundEnabled,
        isHapticEnabled = state.isHapticEnabled,
        score = state.score,
        distance = state.distance,
        colorProgress = state.colorProgress,
        onSoundToggled = { state.isSoundEnabled = it },
        onHapticToggled = { state.isHapticEnabled = it },
        onResume = {
            state.isPaused = false
            state.coordinator.engine.resume()
        },
        onRestart = {
            state.isPaused = false
            state.isGameOver = false
            state.coordinator.restart()
        },
        onExitToMenu = {
            state.isPaused = false
            state.coordinator.engine.pause()
            state.onExitToMenu()
        }
    )
}

/**
 * State holder and action handler for the Dinorun gameplay session.
 */
class DinoGameState(
    val coordinator: DinoGameCoordinator,
    val audioAdapter: AndroidSoundPoolAudioAdapter,
    val hapticAdapter: AndroidHapticAdapter,
    val inputAdapter: DualModeInputAdapter,
    val onExitToMenu: () -> Unit
) {
    var isPaused by mutableStateOf(false)
    var isGameOver by mutableStateOf(false)
    var isSoundEnabled by mutableStateOf(!audioAdapter.isMuted)
    var isHapticEnabled by mutableStateOf(hapticAdapter.isEnabled)
    var score by mutableLongStateOf(0L)
    var distance by mutableFloatStateOf(0f)
    var livesLeft by mutableIntStateOf(3)
    var colorProgress by mutableFloatStateOf(0f)
    var fps by mutableFloatStateOf(60f)
    var currentControlMode by mutableStateOf(ControlMode.VIRTUAL_BUTTONS)
    var pixelsGap by mutableIntStateOf(1)

    init {
        coordinator.onStateChanged = {
            isGameOver = coordinator.engine.context.isGameOver
            livesLeft = coordinator.engine.context.livesLeft
            score = coordinator.engine.context.score
            colorProgress = coordinator.engine.colorRecovery.progress
        }
    }

    fun onFrameTick() {
        if (!isPaused && !isGameOver) {
            coordinator.updateInputs()
            coordinator.checkCustomCollisions()
            score = coordinator.engine.context.score
            distance = coordinator.engine.context.distance
            livesLeft = coordinator.engine.context.livesLeft
            colorProgress = coordinator.engine.colorRecovery.progress
            fps = coordinator.engine.loop.currentFps
            isGameOver = coordinator.engine.context.isGameOver
        }
    }

    fun onCanvasTap() {
        if (currentControlMode == ControlMode.TOUCH_GESTURES && !isPaused && !isGameOver) {
            val wasGrounded = coordinator.player.isGrounded
            coordinator.player.jump()
            if (wasGrounded && !coordinator.player.isGrounded) {
                audioAdapter.playSound(AudioEvent.JUMP)
            }
        }
    }

    fun toggleControlMode() {
        val nextMode = if (currentControlMode == ControlMode.VIRTUAL_BUTTONS) {
            ControlMode.TOUCH_GESTURES
        } else {
            ControlMode.VIRTUAL_BUTTONS
        }
        currentControlMode = nextMode
        inputAdapter.currentMode = nextMode
    }

    fun togglePixelGap() {
        val nextGap = if (pixelsGap == 0) 1 else 0
        pixelsGap = nextGap
        coordinator.setPixelGap(nextGap)
    }

    fun handleBack() {
        if (!isPaused && !isGameOver) {
            isPaused = true
            coordinator.engine.pause()
        } else {
            coordinator.engine.pause()
            onExitToMenu()
        }
    }
}

