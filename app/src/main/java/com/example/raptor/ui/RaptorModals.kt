package com.example.raptor.ui

import androidx.compose.runtime.Composable
import com.pixie.engine.core.context.DifficultyPreset

/**
 * Encapsulated dialog overlays for active Raptor combat sessions
 * (Tactical Pause, Interceptor Destroyed Game Over, and Mission Victory).
 */
@Composable
fun RaptorModals(
    isPaused: Boolean,
    isGameOver: Boolean,
    isVictory: Boolean,
    selectedDifficulty: DifficultyPreset,
    isSoundEnabled: Boolean,
    isHapticEnabled: Boolean,
    currentLevelIndex: Int,
    currentScore: Long,
    levelName: String,
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
            levelName = levelName,
            difficulty = selectedDifficulty,
            onRestart = onRestart,
            onGoToHangar = onGoToHangar,
            onExit = onExitToTitle
        )
    }

    if (isVictory) {
        RaptorVictoryDialog(
            score = currentScore,
            levelName = levelName,
            hasNextLevel = currentLevelIndex < 3,
            onNextLevel = onNextLevel,
            onGoToHangar = onGoToHangar,
            onRestart = onRestart,
            onExit = onExitToTitle
        )
    }
}
