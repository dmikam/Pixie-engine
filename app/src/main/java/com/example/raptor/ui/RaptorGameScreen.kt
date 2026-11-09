package com.example.raptor.ui

import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.raptor.RaptorGameCoordinator
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.android.input.ArcadeButtonConfig
import com.pixie.android.ui.PixieGame
import com.pixie.engine.core.context.DifficultyPreset
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.HapticFeedbackType

enum class RaptorScreenMode {
    TITLE, BRIEFING, TERMINAL_SHOP, COMBAT
}

private class RaptorCombatButtons(
    val autoFireBtn: ArcadeButtonConfig,
    val weaponBtn: ArcadeButtonConfig,
    val missileBtn: ArcadeButtonConfig,
    val bombBtn: ArcadeButtonConfig,
    val pauseBtn: ArcadeButtonConfig,
    val floatingBtns: List<ArcadeButtonConfig>
)

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
        RaptorGameCoordinator(audioAdapter, hapticAdapter, selectedDifficulty = selectedDifficulty, currentLevelIndex = currentLevelIndex)
    }

    var currentScreenMode by remember { mutableStateOf(RaptorScreenMode.TITLE) }
    var isPaused by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var isVictory by remember { mutableStateOf(false) }
    var isSoundEnabled by remember { mutableStateOf(!audioAdapter.isMuted) }
    var isHapticEnabled by remember { mutableStateOf(hapticAdapter.isEnabled) }

    HandleRaptorBack(currentScreenMode, isPaused, isGameOver, isVictory, onExitToMenu) {
        mode, pause, over, vic -> currentScreenMode = mode; isPaused = pause; isGameOver = over; isVictory = vic
    }

    coordinator.onGameOver = { isGameOver = true }
    coordinator.onVictory = { isVictory = true }

    LaunchedEffect(currentScreenMode, isPaused, isGameOver, isVictory) {
        if (currentScreenMode == RaptorScreenMode.COMBAT && !isPaused && !isGameOver && !isVictory) coordinator.engine.start()
        else coordinator.engine.pause()
    }
    DisposableEffect(Unit) { onDispose { coordinator.engine.stop(); audioAdapter.release() } }

    when (currentScreenMode) {
        RaptorScreenMode.TITLE -> RaptorIntroTitleScreen(
            audioAdapter, { currentScreenMode = RaptorScreenMode.BRIEFING },
            { currentScreenMode = RaptorScreenMode.TERMINAL_SHOP },
            { currentScreenMode = RaptorScreenMode.BRIEFING }, onExitToMenu, modifier
        )
        RaptorScreenMode.BRIEFING -> RaptorBriefingScreen(
            selectedDifficulty, currentLevelIndex, coordinator.engine.context.credits, audioAdapter,
            { selectedDifficulty = it; coordinator.setDifficulty(it) },
            { currentLevelIndex = it; coordinator.setLevel(it) },
            { coordinator.setupGame(); currentScreenMode = RaptorScreenMode.COMBAT },
            { currentScreenMode = RaptorScreenMode.TERMINAL_SHOP },
            { currentScreenMode = RaptorScreenMode.TITLE }, modifier
        )
        RaptorScreenMode.TERMINAL_SHOP -> RaptorTerminalShopScreen(
            coordinator, audioAdapter,
            { coordinator.setupGame(); currentScreenMode = RaptorScreenMode.COMBAT },
            { currentScreenMode = RaptorScreenMode.TITLE }, modifier
        )
        RaptorScreenMode.COMBAT -> RaptorCombatScreen(
            coordinator, audioAdapter, hapticAdapter, selectedDifficulty, currentLevelIndex,
            isPaused, isGameOver, isVictory, isSoundEnabled, isHapticEnabled, onExitToMenu,
            onSetMode = { currentScreenMode = it }, onSetPaused = { isPaused = it },
            onSetGameOver = { isGameOver = it }, onSetVictory = { isVictory = it },
            onSetSound = { isSoundEnabled = it }, onSetHaptic = { isHapticEnabled = it },
            onSetDifficulty = { selectedDifficulty = it }, onSetLevel = { currentLevelIndex = it },
            modifier = modifier
        )
    }
}

@Composable
private fun HandleRaptorBack(
    mode: RaptorScreenMode, isPaused: Boolean, isGameOver: Boolean, isVictory: Boolean,
    onExitToMenu: () -> Unit, onUpdate: (RaptorScreenMode, Boolean, Boolean, Boolean) -> Unit
) {
    BackHandler {
        when (mode) {
            RaptorScreenMode.TITLE -> onExitToMenu()
            RaptorScreenMode.BRIEFING, RaptorScreenMode.TERMINAL_SHOP -> onUpdate(RaptorScreenMode.TITLE, false, false, false)
            RaptorScreenMode.COMBAT -> {
                if (!isPaused && !isGameOver && !isVictory) onUpdate(RaptorScreenMode.COMBAT, true, false, false)
                else onUpdate(RaptorScreenMode.TERMINAL_SHOP, false, false, false)
            }
        }
    }
}

@Composable
private fun rememberRaptorButtons(
    coordinator: RaptorGameCoordinator,
    weaponTier: Int,
    activeWeaponId: String,
    unlockedCount: Int,
    missilesLeft: Int,
    bombsLeft: Int,
    audioAdapter: AndroidSoundPoolAudioAdapter,
    onSetPaused: (Boolean) -> Unit
): RaptorCombatButtons {
    val autoFireBtn = remember(coordinator.isAutoFireEnabled, weaponTier) {
        RaptorControls.createAutoFireButton(coordinator.isAutoFireEnabled, weaponTier) {
            coordinator.isAutoFireEnabled = !coordinator.isAutoFireEnabled
            audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
        }
    }
    val weaponBtn = remember(activeWeaponId, unlockedCount) {
        RaptorControls.createWeaponSwitchButton(activeWeaponId, unlockedCount) {
            coordinator.cycleWeapon()
        }
    }
    val missileBtn = remember(missilesLeft) {
        RaptorControls.createMissileButton(missilesLeft) { coordinator.fireSecondaryMissiles() }
    }
    val bombBtn = remember(bombsLeft) {
        RaptorControls.createMegaBombButton(bombsLeft) { coordinator.triggerMegaBomb() }
    }
    val pauseBtn = remember {
        RaptorControls.createPauseButton {
            audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
            onSetPaused(true)
        }
    }
    val floatingBtns = remember(autoFireBtn, weaponBtn, missileBtn, bombBtn) {
        RaptorControls.createFloatingButtons(autoFireBtn, missileBtn, bombBtn, pauseBtn = null, weaponBtn = weaponBtn)
    }
    return remember(autoFireBtn, weaponBtn, missileBtn, bombBtn, pauseBtn, floatingBtns) {
        RaptorCombatButtons(autoFireBtn, weaponBtn, missileBtn, bombBtn, pauseBtn, floatingBtns)
    }
}

@Composable
fun RaptorCombatScreen(
    coordinator: RaptorGameCoordinator,
    audioAdapter: AndroidSoundPoolAudioAdapter,
    hapticAdapter: AndroidHapticAdapter,
    selectedDifficulty: DifficultyPreset,
    currentLevelIndex: Int,
    isPaused: Boolean,
    isGameOver: Boolean,
    isVictory: Boolean,
    isSoundEnabled: Boolean,
    isHapticEnabled: Boolean,
    onExitToMenu: () -> Unit,
    onSetMode: (RaptorScreenMode) -> Unit,
    onSetPaused: (Boolean) -> Unit,
    onSetGameOver: (Boolean) -> Unit,
    onSetVictory: (Boolean) -> Unit,
    onSetSound: (Boolean) -> Unit,
    onSetHaptic: (Boolean) -> Unit,
    onSetDifficulty: (DifficultyPreset) -> Unit,
    onSetLevel: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var score by remember { mutableLongStateOf(0L) }
    var hullHp by remember { mutableFloatStateOf(100f) }
    var shieldHp by remember { mutableFloatStateOf(100f) }
    var missilesLeft by remember { mutableIntStateOf(6) }
    var bombsLeft by remember { mutableIntStateOf(2) }
    var weaponTier by remember { mutableIntStateOf(1) }
    var activeWeaponId by remember { mutableStateOf(coordinator.playerShip.activeWeaponId) }
    var unlockedWeapons by remember { mutableStateOf(coordinator.unlockedWeapons.toList()) }
    var activePerksList by remember { mutableStateOf(listOf<String>()) }
    var levelDistance by remember { mutableFloatStateOf(0f) }

    val btns = rememberRaptorButtons(
        coordinator, weaponTier, activeWeaponId, unlockedWeapons.size, missilesLeft, bombsLeft, audioAdapter, onSetPaused
    )

    RaptorPixieGameView(
        coordinator, audioAdapter, hapticAdapter, selectedDifficulty, currentLevelIndex,
        isPaused, isGameOver, isVictory, isSoundEnabled, isHapticEnabled,
        score, hullHp, shieldHp, missilesLeft, bombsLeft, weaponTier, activeWeaponId, unlockedWeapons,
        activePerksList, levelDistance,
        btns.autoFireBtn, btns.weaponBtn, btns.missileBtn, btns.bombBtn, btns.pauseBtn, btns.floatingBtns,
        onExitToMenu, onSetMode, onSetPaused, onSetGameOver, onSetVictory,
        onSetSound, onSetHaptic, onSetDifficulty, onSetLevel,
        onSyncHud = {
            score = coordinator.engine.context.score
            hullHp = coordinator.playerShip.hull
            shieldHp = coordinator.playerShip.shield
            missilesLeft = coordinator.playerShip.missileSalvoes
            bombsLeft = coordinator.playerShip.megaBombs
            weaponTier = coordinator.playerShip.weaponTier
            activeWeaponId = coordinator.playerShip.activeWeaponId
            unlockedWeapons = coordinator.unlockedWeapons.toList()
            activePerksList = coordinator.engine.context.activePerks.toList()
            levelDistance = coordinator.engine.context.distance
        },
        modifier = modifier
    )
}

@Composable
private fun RaptorPixieGameView(
    coordinator: RaptorGameCoordinator,
    audioAdapter: AndroidSoundPoolAudioAdapter,
    hapticAdapter: AndroidHapticAdapter,
    selectedDifficulty: DifficultyPreset,
    currentLevelIndex: Int,
    isPaused: Boolean,
    isGameOver: Boolean,
    isVictory: Boolean,
    isSoundEnabled: Boolean,
    isHapticEnabled: Boolean,
    score: Long,
    hullHp: Float,
    shieldHp: Float,
    missilesLeft: Int,
    bombsLeft: Int,
    weaponTier: Int,
    activeWeaponId: String,
    unlockedWeapons: List<String>,
    activePerksList: List<String>,
    levelDistance: Float,
    autoFireBtn: ArcadeButtonConfig,
    weaponBtn: ArcadeButtonConfig,
    missileBtn: ArcadeButtonConfig,
    bombBtn: ArcadeButtonConfig,
    pauseBtn: ArcadeButtonConfig,
    floatingBtns: List<ArcadeButtonConfig>,
    onExitToMenu: () -> Unit,
    onSetMode: (RaptorScreenMode) -> Unit,
    onSetPaused: (Boolean) -> Unit,
    onSetGameOver: (Boolean) -> Unit,
    onSetVictory: (Boolean) -> Unit,
    onSetSound: (Boolean) -> Unit,
    onSetHaptic: (Boolean) -> Unit,
    onSetDifficulty: (DifficultyPreset) -> Unit,
    onSetLevel: (Int) -> Unit,
    onSyncHud: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topHud: @Composable () -> Unit = {
        RaptorTopHud(
            score = score, levelName = coordinator.currentLevel.name, difficulty = selectedDifficulty,
            distance = levelDistance, targetDistance = coordinator.currentLevel.targetDistance,
            activeWeaponId = activeWeaponId, weaponTier = weaponTier,
            onPauseClick = { audioAdapter.playSound(AudioEvent.BUTTON_CLICK); onSetPaused(true) },
            onBackClick = { audioAdapter.playSound(AudioEvent.BUTTON_CLICK); onExitToMenu() }
        )
    }
    val bottomDeck: @Composable () -> Unit = {
        RaptorBottomControls(
            isAutoFire = coordinator.isAutoFireEnabled,
            weaponTier = weaponTier,
            activeWeaponId = activeWeaponId,
            unlockedWeapons = unlockedWeapons,
            missilesLeft = missilesLeft,
            bombsLeft = bombsLeft,
            activePerks = activePerksList,
            onToggleAutoFire = {
                coordinator.isAutoFireEnabled = !coordinator.isAutoFireEnabled
                audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
            },
            onCycleWeapon = { coordinator.cycleWeapon() },
            onSelectWeapon = { coordinator.switchWeapon(it) },
            onFireMissiles = { coordinator.fireSecondaryMissiles() },
            onTriggerBomb = { coordinator.triggerMegaBomb() }
        )
    }

    PixieGame(coordinator.engine)
        .input(coordinator.inputAdapter)
        .haptic(hapticAdapter)
        .onVirtualDragStart { vx, vy -> coordinator.handleDragStart(vx, vy) }
        .onVirtualDragDelta { dx, dy -> coordinator.movePlayer(dx, dy) }
        .onFrameTick { dt ->
            if (!isPaused && !isGameOver && !isVictory) {
                coordinator.update(dt)
                onSyncHud()
            }
        }
        .portrait {
            arcadeCabinet(
                topBar = topHud,
                bottomDeck = bottomDeck
            )
        }
        .landscape {
            topStatusBar(topHud)
            splitWings(
                leftWing = { RaptorLandscapeLeftWing(shieldHp, hullHp, autoFireBtn, weaponBtn) },
                rightWing = { RaptorLandscapeRightWing(missileBtn, bombBtn, pauseBtn) },
                floatingButtons = floatingBtns
            )
        }
        .canvasOverlay {
            val config = LocalConfiguration.current
            if (config.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                RaptorIntegrityGauges(shieldHp, hullHp, Modifier.align(Alignment.CenterStart).padding(start = 12.dp))
            }
        }
        .overlays {
            RaptorCombatModalsOverlay(
                isPaused, isGameOver, isVictory, selectedDifficulty, isSoundEnabled, isHapticEnabled,
                currentLevelIndex, score, coordinator, audioAdapter, hapticAdapter,
                onSetMode, onSetPaused, onSetGameOver, onSetVictory, onSetSound, onSetHaptic, onSetDifficulty, onSetLevel
            )
        }
        .Render(modifier = modifier)
}

@Composable
private fun RaptorCombatModalsOverlay(
    isPaused: Boolean, isGameOver: Boolean, isVictory: Boolean, selectedDifficulty: DifficultyPreset,
    isSoundEnabled: Boolean, isHapticEnabled: Boolean, currentLevelIndex: Int, score: Long,
    coordinator: RaptorGameCoordinator, audioAdapter: AndroidSoundPoolAudioAdapter, hapticAdapter: AndroidHapticAdapter,
    onSetMode: (RaptorScreenMode) -> Unit, onSetPaused: (Boolean) -> Unit, onSetGameOver: (Boolean) -> Unit,
    onSetVictory: (Boolean) -> Unit, onSetSound: (Boolean) -> Unit, onSetHaptic: (Boolean) -> Unit,
    onSetDifficulty: (DifficultyPreset) -> Unit, onSetLevel: (Int) -> Unit
) {
    RaptorModals(
        isPaused = isPaused, isGameOver = isGameOver, isVictory = isVictory,
        selectedDifficulty = selectedDifficulty, isSoundEnabled = isSoundEnabled, isHapticEnabled = isHapticEnabled,
        currentLevelIndex = currentLevelIndex, currentScore = score, levelName = coordinator.currentLevel.name,
        onResume = { if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK); onSetPaused(false) },
        onRestart = {
            if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
            coordinator.setupGame(); onSetPaused(false); onSetGameOver(false)
        },
        onGoToHangar = {
            if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
            onSetPaused(false); onSetGameOver(false); onSetVictory(false)
            onSetMode(RaptorScreenMode.TERMINAL_SHOP)
        },
        onNextLevel = {
            val nextLvl = currentLevelIndex + 1
            onSetLevel(nextLvl); coordinator.setLevel(nextLvl)
            onSetVictory(false); onSetMode(RaptorScreenMode.TERMINAL_SHOP)
        },
        onExitToTitle = {
            if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
            onSetPaused(false); onSetGameOver(false); onSetVictory(false)
            onSetMode(RaptorScreenMode.TITLE)
        },
        onToggleSound = { enabled ->
            onSetSound(enabled); audioAdapter.isMuted = !enabled
            if (enabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
        },
        onToggleHaptic = { enabled ->
            onSetHaptic(enabled); hapticAdapter.isEnabled = enabled
            if (enabled) hapticAdapter.trigger(HapticFeedbackType.LIGHT)
        },
        onSelectDifficulty = { diff ->
            if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
            onSetDifficulty(diff); coordinator.setDifficulty(diff)
        },
        onSelectLevel = { lvl ->
            if (isSoundEnabled) audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
            onSetLevel(lvl); coordinator.setLevel(lvl)
        }
    )
}
