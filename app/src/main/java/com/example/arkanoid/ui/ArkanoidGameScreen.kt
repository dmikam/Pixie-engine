package com.example.arkanoid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.arkanoid.ArkanoidGameCoordinator
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.android.input.DualModeInputAdapter
import com.pixie.android.ui.PixieGame

/**
 * Arkanoid Game Screen built with the PixieGame fluent builder DSL.
 *
 * Responsively isolates Viewport and HUD across Portrait and Landscape:
 * - Portrait: Docked top HUD above game canvas + docked bottom flex arcade buttons.
 * - Landscape: 0px vertical space wasted! HUD is absorbed into lateral wings so the
 *   playscreen gets 100% of the screen height.
 *
 * Canvas gestures, frame ticks, and serve overlays are fully parameterized via fluent methods.
 */
@Composable
fun ArkanoidGameScreen(
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioAdapter = remember { AndroidSoundPoolAudioAdapter(context) }
    val hapticAdapter = remember { AndroidHapticAdapter(context) }
    val inputAdapter = remember { DualModeInputAdapter() }

    val coordinator = remember {
        ArkanoidGameCoordinator(
            audioPort = audioAdapter,
            hapticPort = hapticAdapter,
            inputPort = inputAdapter
        ).apply {
            engine.start()
        }
    }

    val state by coordinator.uiState.collectAsState()

    DisposableEffect(coordinator) {
        onDispose {
            coordinator.engine.stop()
            audioAdapter.release()
            inputAdapter.reset()
        }
    }

    BackHandler {
        if (!state.isPaused && !state.isGameOver && !state.isVictory) {
            coordinator.togglePause()
        } else {
            onExitToMenu()
        }
    }

    // Arcade Button configurations
    val steerLeftBtn = remember { ArkanoidControls.createSteerLeftButton(inputAdapter) }
    val steerRightBtn = remember { ArkanoidControls.createSteerRightButton(inputAdapter) }
    val actionBtn = ArkanoidControls.createActionButton(state, inputAdapter)
    val pauseBtn = remember { ArkanoidControls.createPauseButton(coordinator) }
    val floatingBtns = remember(state.isBallDocked, state.activePowerUp) {
        ArkanoidControls.createFloatingButtons(steerLeftBtn, actionBtn, steerRightBtn)
    }

    // Fluent PixieGame assembly
    PixieGame(coordinator.engine)
        .input(inputAdapter)
        .haptic(hapticAdapter)
        .onTap { coordinator.onActionTriggered() }
        .onDragDelta { dx, _ -> coordinator.movePaddleByDelta(dx * 0.45f) }
        .onFrameTick { dt -> coordinator.update(dt) }
        .canvasOverlay {
            if (state.isBallDocked && !state.isPaused && !state.isGameOver) {
                ArkanoidServeHintOverlay()
            }
        }
        // ---------- PORTRAIT LAYOUT ----------
        .portrait(maxWidth = 600.dp) {
            topStatusBar {
                ArkanoidPortraitStatusBar(
                    state = state,
                    onExitToMenu = onExitToMenu,
                    onPause = { coordinator.togglePause() }
                )
            }
            bottomButtons(
                steerLeftBtn,
                actionBtn,
                steerRightBtn,
                pauseBtn
            )
            floatingButtons(floatingBtns)
        }
        // ---------- LANDSCAPE LAYOUT ----------
        .landscape(maxHeight = 600.dp) {
            leftWing {
                ArkanoidLandscapeLeftWing(
                    state = state,
                    onExitToMenu = onExitToMenu,
                    leftBtn = steerLeftBtn,
                    rightBtn = steerRightBtn
                )
            }
            rightWing {
                ArkanoidLandscapeRightWing(
                    state = state,
                    onPause = { coordinator.togglePause() },
                    actionBtn = actionBtn
                )
            }
            floatingButtons(floatingBtns)
        }
        // ---------- MODAL OVERLAYS ----------
        .overlays {
            ArkanoidModals(
                state = state,
                coordinator = coordinator,
                onExitToMenu = onExitToMenu
            )
        }
        // ---------- RENDER CANVAS & LAYOUT ----------
        .Render(modifier = modifier)
}
