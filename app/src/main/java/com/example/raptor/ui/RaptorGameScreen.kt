package com.example.raptor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.raptor.RaptorGameCoordinator
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.android.ui.PixieGame
import com.pixie.engine.core.context.DifficultyPreset
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.HapticFeedbackType

enum class RaptorScreenMode {
    TITLE,         // Iconic title screen with planet Earth and pilot helmet artwork
    BRIEFING,      // Military briefing desk & difficulty selection
    TERMINAL_SHOP, // "HDF MAIN DISPLAY" CRT Computer Terminal Weapon Shop
    COMBAT         // Active vertical scrolling combat
}

@Composable
fun RaptorGameScreen(
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioAdapter = remember { AndroidSoundPoolAudioAdapter(context) }
    val hapticAdapter = remember { AndroidHapticAdapter(context) }

    var selectedDifficulty by remember { mutableStateOf(DifficultyPreset.MEDIUM) }
    var currentLevelIndex by remember { mutableIntStateOf(1) }

    val coordinator = remember {
        RaptorGameCoordinator(
            audio = audioAdapter,
            haptics = hapticAdapter,
            selectedDifficulty = selectedDifficulty,
            currentLevelIndex = currentLevelIndex
        )
    }

    var currentScreenMode by remember { mutableStateOf(RaptorScreenMode.TITLE) }
    var isPaused by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var isVictory by remember { mutableStateOf(false) }
    var isSoundEnabled by remember { mutableStateOf(!audioAdapter.isMuted) }
    var isHapticEnabled by remember { mutableStateOf(hapticAdapter.isEnabled) }

    // HUD reactive state
    var currentScore by remember { mutableLongStateOf(0L) }
    var hullHp by remember { mutableFloatStateOf(100f) }
    var shieldHp by remember { mutableFloatStateOf(100f) }
    var missilesLeft by remember { mutableIntStateOf(6) }
    var bombsLeft by remember { mutableIntStateOf(2) }
    var weaponTier by remember { mutableIntStateOf(1) }
    var activePerksList by remember { mutableStateOf(listOf<String>()) }
    var levelDistance by remember { mutableFloatStateOf(0f) }

    // Handle back button for multi-screen navigation
    BackHandler {
        when (currentScreenMode) {
            RaptorScreenMode.TITLE -> onExitToMenu()
            RaptorScreenMode.BRIEFING -> currentScreenMode = RaptorScreenMode.TITLE
            RaptorScreenMode.TERMINAL_SHOP -> currentScreenMode = RaptorScreenMode.TITLE
            RaptorScreenMode.COMBAT -> {
                if (!isPaused && !isGameOver && !isVictory) {
                    isPaused = true
                } else {
                    currentScreenMode = RaptorScreenMode.TERMINAL_SHOP
                    isPaused = false
                    isGameOver = false
                    isVictory = false
                }
            }
        }
    }

    coordinator.onGameOver = { isGameOver = true }
    coordinator.onVictory = { isVictory = true }

    // Game Engine Pause/Resume Control
    LaunchedEffect(currentScreenMode, isPaused, isGameOver, isVictory) {
        if (currentScreenMode == RaptorScreenMode.COMBAT && !isPaused && !isGameOver && !isVictory) {
            coordinator.engine.start()
        } else {
            coordinator.engine.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            coordinator.engine.stop()
            audioAdapter.release()
        }
    }

    when (currentScreenMode) {
        RaptorScreenMode.TITLE -> {
            RaptorIntroTitleScreen(
                audioAdapter = audioAdapter,
                onNewMission = { currentScreenMode = RaptorScreenMode.BRIEFING },
                onOpenSupplyDepot = { currentScreenMode = RaptorScreenMode.TERMINAL_SHOP },
                onOpenBriefing = { currentScreenMode = RaptorScreenMode.BRIEFING },
                onExitToMenu = onExitToMenu,
                modifier = modifier
            )
            return
        }

        RaptorScreenMode.BRIEFING -> {
            RaptorBriefingScreen(
                selectedDifficulty = selectedDifficulty,
                currentLevelIndex = currentLevelIndex,
                pilotCredits = coordinator.engine.context.credits,
                audioAdapter = audioAdapter,
                onSelectDifficulty = { diff ->
                    selectedDifficulty = diff
                    coordinator.setDifficulty(diff)
                },
                onSelectLevel = { lvl ->
                    currentLevelIndex = lvl
                    coordinator.setLevel(lvl)
                },
                onLaunchMission = {
                    coordinator.setupGame()
                    currentScreenMode = RaptorScreenMode.COMBAT
                },
                onOpenSupplyDepot = { currentScreenMode = RaptorScreenMode.TERMINAL_SHOP },
                onBackToTitle = { currentScreenMode = RaptorScreenMode.TITLE },
                modifier = modifier
            )
            return
        }

        RaptorScreenMode.TERMINAL_SHOP -> {
            RaptorTerminalShopScreen(
                coordinator = coordinator,
                audioAdapter = audioAdapter,
                onLaunchCombat = {
                    coordinator.setupGame()
                    currentScreenMode = RaptorScreenMode.COMBAT
                },
                onBackToTitle = { currentScreenMode = RaptorScreenMode.TITLE },
                modifier = modifier
            )
            return
        }

        RaptorScreenMode.COMBAT -> {
            // Render active combat using fluent PixieGame DSL below
        }
    }

    PixieGame(coordinator.engine)
        .haptic(hapticAdapter)
        .onVirtualDragStart { virtualX, virtualY ->
            coordinator.handleDragStart(virtualX, virtualY)
        }
        .onVirtualDragDelta { dx, dy ->
            coordinator.movePlayer(dx, dy)
        }
        .onFrameTick { deltaSeconds ->
            if (!isPaused && !isGameOver && !isVictory) {
                coordinator.update(deltaSeconds)

                // Sync HUD reactive values
                currentScore = coordinator.engine.context.score
                hullHp = coordinator.playerShip.hull
                shieldHp = coordinator.playerShip.shield
                missilesLeft = coordinator.playerShip.missileSalvoes
                bombsLeft = coordinator.playerShip.megaBombs
                weaponTier = coordinator.playerShip.weaponTier
                activePerksList = coordinator.engine.context.activePerks.toList()
                levelDistance = coordinator.engine.context.distance
            }
        }
        .portrait {
            topOverlayStatusBar {
                RaptorTopHud(
                    score = currentScore,
                    levelName = coordinator.currentLevel.name,
                    difficulty = selectedDifficulty,
                    distance = levelDistance,
                    targetDistance = coordinator.currentLevel.targetDistance,
                    onPauseClick = {
                        audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                        isPaused = true
                    },
                    onBackClick = {
                        audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                        onExitToMenu()
                    }
                )
            }
            bottomOverlayDeck {
                RaptorBottomControls(
                    isAutoFire = coordinator.isAutoFireEnabled,
                    weaponTier = weaponTier,
                    missilesLeft = missilesLeft,
                    bombsLeft = bombsLeft,
                    activePerks = activePerksList,
                    onToggleAutoFire = {
                        coordinator.isAutoFireEnabled = !coordinator.isAutoFireEnabled
                        audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    },
                    onFireMissiles = { coordinator.fireSecondaryMissiles() },
                    onTriggerBomb = { coordinator.triggerMegaBomb() }
                )
            }
        }
        .landscape {
            topOverlayStatusBar {
                RaptorTopHud(
                    score = currentScore,
                    levelName = coordinator.currentLevel.name,
                    difficulty = selectedDifficulty,
                    distance = levelDistance,
                    targetDistance = coordinator.currentLevel.targetDistance,
                    onPauseClick = {
                        audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                        isPaused = true
                    },
                    onBackClick = {
                        audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                        onExitToMenu()
                    }
                )
            }
            bottomOverlayDeck {
                RaptorBottomControls(
                    isAutoFire = coordinator.isAutoFireEnabled,
                    weaponTier = weaponTier,
                    missilesLeft = missilesLeft,
                    bombsLeft = bombsLeft,
                    activePerks = activePerksList,
                    onToggleAutoFire = {
                        coordinator.isAutoFireEnabled = !coordinator.isAutoFireEnabled
                        audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    },
                    onFireMissiles = { coordinator.fireSecondaryMissiles() },
                    onTriggerBomb = { coordinator.triggerMegaBomb() }
                )
            }
        }
        .canvasOverlay {
            RaptorIntegrityGauges(
                shieldHp = shieldHp,
                hullHp = hullHp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            )
        }
        .overlays {
            RaptorCombatModals(
                isPaused = isPaused,
                isGameOver = isGameOver,
                isVictory = isVictory,
                selectedDifficulty = selectedDifficulty,
                isSoundEnabled = isSoundEnabled,
                isHapticEnabled = isHapticEnabled,
                currentLevelIndex = currentLevelIndex,
                currentScore = currentScore,
                coordinator = coordinator,
                audioAdapter = audioAdapter,
                hapticAdapter = hapticAdapter,
                onResume = {
                    if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    isPaused = false
                },
                onRestart = {
                    if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    coordinator.setupGame()
                    isPaused = false
                    isGameOver = false
                },
                onGoToHangar = {
                    if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    isPaused = false
                    isGameOver = false
                    isVictory = false
                    currentScreenMode = RaptorScreenMode.TERMINAL_SHOP
                },
                onNextLevel = {
                    currentLevelIndex++
                    coordinator.setLevel(currentLevelIndex)
                    isVictory = false
                    currentScreenMode = RaptorScreenMode.TERMINAL_SHOP
                },
                onExitToTitle = {
                    if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    isPaused = false
                    isGameOver = false
                    isVictory = false
                    currentScreenMode = RaptorScreenMode.TITLE
                },
                onToggleSound = { enabled ->
                    isSoundEnabled = enabled
                    audioAdapter.isMuted = !enabled
                    if (enabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                },
                onToggleHaptic = { enabled ->
                    isHapticEnabled = enabled
                    hapticAdapter.isEnabled = enabled
                    if (enabled) hapticAdapter.trigger(HapticFeedbackType.LIGHT)
                },
                onSelectDifficulty = { diff ->
                    if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    selectedDifficulty = diff
                    coordinator.setDifficulty(diff)
                },
                onSelectLevel = { lvl ->
                    if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    currentLevelIndex = lvl
                    coordinator.setLevel(lvl)
                }
            )
        }
        .Render(modifier = modifier)
}

/**
 * Modals and alert dialog overlays for active Raptor combat sessions.
 */
@Composable
private fun RaptorCombatModals(
    isPaused: Boolean,
    isGameOver: Boolean,
    isVictory: Boolean,
    selectedDifficulty: DifficultyPreset,
    isSoundEnabled: Boolean,
    isHapticEnabled: Boolean,
    currentLevelIndex: Int,
    currentScore: Long,
    coordinator: RaptorGameCoordinator,
    audioAdapter: AndroidSoundPoolAudioAdapter,
    hapticAdapter: AndroidHapticAdapter,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onGoToHangar: () -> Unit,
    onNextLevel: () -> Unit,
    onExitToTitle: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onSelectDifficulty: (DifficultyPreset) -> Unit,
    onSelectLevel: (Int) -> Unit
) {
    if (isPaused) {
        RaptorPauseDialog(
            selectedDifficulty = selectedDifficulty,
            isSoundEnabled = isSoundEnabled,
            isHapticEnabled = isHapticEnabled,
            currentLevel = currentLevelIndex,
            onResume = onResume,
            onRestart = onRestart,
            onGoToHangar = onGoToHangar,
            onToggleSound = onToggleSound,
            onToggleHaptic = onToggleHaptic,
            onSelectDifficulty = onSelectDifficulty,
            onSelectLevel = onSelectLevel,
            onExit = onExitToTitle
        )
    }

    if (isGameOver) {
        RaptorGameOverDialog(
            score = currentScore,
            levelName = coordinator.currentLevel.name,
            difficulty = selectedDifficulty,
            onRestart = onRestart,
            onGoToHangar = onGoToHangar,
            onExit = onExitToTitle
        )
    }

    if (isVictory) {
        RaptorVictoryDialog(
            score = currentScore,
            levelName = coordinator.currentLevel.name,
            hasNextLevel = currentLevelIndex < 3,
            onNextLevel = onNextLevel,
            onGoToHangar = onGoToHangar,
            onRestart = onRestart,
            onExit = onExitToTitle
        )
    }
}

