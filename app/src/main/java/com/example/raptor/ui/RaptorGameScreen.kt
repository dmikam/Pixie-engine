package com.example.raptor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.raptor.RaptorGameCoordinator
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.android.ui.PixieGameCanvas
import com.pixie.engine.core.context.DifficultyPreset
import com.pixie.engine.core.math.Vec2
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
            // Render active combat below
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B12))
            .statusBarsPadding()
    ) {
        // Direct Touch-drag Controller over the whole screen with letterbox-aware scaling
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val renderScale = kotlin.math.min(size.width / 240f, size.height / 320f)
                            val renderOffsetX = (size.width - 240f * renderScale) * 0.5f
                            val renderOffsetY = (size.height - 320f * renderScale) * 0.5f

                            val virtualTouchX = (offset.x - renderOffsetX) / renderScale
                            val virtualTouchY = (offset.y - renderOffsetY) / renderScale

                            val dist = (Vec2(virtualTouchX, virtualTouchY) - coordinator.playerShip.position).length()
                            if (dist < 75f) {
                                coordinator.playerShip.targetPosition = Vec2(
                                    virtualTouchX.coerceIn(4f, 218f),
                                    virtualTouchY.coerceIn(24f, 290f)
                                )
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val renderScale = kotlin.math.min(size.width / 240f, size.height / 320f)
                            val currentTarget = coordinator.playerShip.targetPosition
                            coordinator.playerShip.targetPosition = Vec2(
                                (currentTarget.x + dragAmount.x / renderScale).coerceIn(4f, 218f),
                                (currentTarget.y + dragAmount.y / renderScale).coerceIn(24f, 290f)
                            )
                        }
                    )
                }
        ) {
            // Main Retro Arcade Game Canvas synchronized to screen vsync
            PixieGameCanvas(
                engine = coordinator.engine,
                modifier = Modifier.fillMaxSize(),
                onFrameTick = { deltaSeconds ->
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
            )

            // Top HUD Overlay
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

            // Left Side Status Meters (Shield & Hull)
            RaptorIntegrityGauges(
                shieldHp = shieldHp,
                hullHp = hullHp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            )

            // Bottom Controls Bar (Auto-Fire, Missile trigger, Mega-Bomb detonator)
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
                onTriggerBomb = { coordinator.triggerMegaBomb() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Pause Modal
        if (isPaused) {
            RaptorPauseDialog(
                selectedDifficulty = selectedDifficulty,
                isSoundEnabled = isSoundEnabled,
                isHapticEnabled = isHapticEnabled,
                currentLevel = currentLevelIndex,
                onResume = {
                    if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    isPaused = false
                },
                onRestart = {
                    if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    coordinator.setupGame()
                    isPaused = false
                },
                onGoToHangar = {
                    if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    isPaused = false
                    currentScreenMode = RaptorScreenMode.TERMINAL_SHOP
                },
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
                onSelectDifficulty = { diff ->
                    if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    selectedDifficulty = diff
                    coordinator.setDifficulty(diff)
                },
                onSelectLevel = { lvl ->
                    if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    currentLevelIndex = lvl
                    coordinator.setLevel(lvl)
                },
                onExit = {
                    if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    isPaused = false
                    currentScreenMode = RaptorScreenMode.TITLE
                }
            )
        }

        // Game Over Dialog
        if (isGameOver) {
            RaptorGameOverDialog(
                score = currentScore,
                levelName = coordinator.currentLevel.name,
                difficulty = selectedDifficulty,
                onRestart = {
                    coordinator.setupGame()
                    isGameOver = false
                },
                onGoToHangar = {
                    audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    isGameOver = false
                    currentScreenMode = RaptorScreenMode.TERMINAL_SHOP
                },
                onExit = {
                    isGameOver = false
                    currentScreenMode = RaptorScreenMode.TITLE
                }
            )
        }

        // Victory Dialog
        if (isVictory) {
            RaptorVictoryDialog(
                score = currentScore,
                levelName = coordinator.currentLevel.name,
                hasNextLevel = currentLevelIndex < 3,
                onNextLevel = {
                    currentLevelIndex++
                    coordinator.setLevel(currentLevelIndex)
                    isVictory = false
                    currentScreenMode = RaptorScreenMode.TERMINAL_SHOP // Return to Hangar shop with earned bounty before next sector!
                },
                onGoToHangar = {
                    audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    isVictory = false
                    currentScreenMode = RaptorScreenMode.TERMINAL_SHOP
                },
                onRestart = {
                    coordinator.setupGame()
                    isVictory = false
                },
                onExit = {
                    isVictory = false
                    currentScreenMode = RaptorScreenMode.TITLE
                }
            )
        }
    }
}

