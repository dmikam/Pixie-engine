package com.example.arkanoid.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.arkanoid.ArkanoidGameCoordinator
import com.example.arkanoid.ArkanoidUiState
import com.pixie.android.input.ArcadeActionType
import com.pixie.android.input.ArcadeButtonConfig
import com.pixie.android.input.ArcadeButtonShape
import com.pixie.android.input.DualModeInputAdapter

/**
 * Arcade control button factories for Arkanoid.
 * Keeps control definitions modular and cleanly parameterized.
 */
object ArkanoidControls {

    fun createSteerLeftButton(inputAdapter: DualModeInputAdapter): ArcadeButtonConfig {
        return ArcadeButtonConfig(
            id = "paddle_steer_left",
            iconVector = Icons.Default.ChevronLeft,
            shape = ArcadeButtonShape.PILL,
            weight = 1.0f,
            minWidthDp = 54.dp,
            heightDp = 48.dp,
            baseColor = Color(0xCC263238),
            pressedColor = Color(0xFF00E5FF),
            rimColor = Color(0xFF80DEEA),
            actionType = ArcadeActionType.MOVE_LEFT,
            onPressDown = { inputAdapter.setButtonMoveX(-1f) },
            onPressUp = { if (inputAdapter.moveX < 0f) inputAdapter.setButtonMoveX(0f) }
        )
    }

    fun createSteerRightButton(inputAdapter: DualModeInputAdapter): ArcadeButtonConfig {
        return ArcadeButtonConfig(
            id = "paddle_steer_right",
            iconVector = Icons.Default.ChevronRight,
            shape = ArcadeButtonShape.PILL,
            weight = 1.0f,
            minWidthDp = 54.dp,
            heightDp = 48.dp,
            baseColor = Color(0xCC263238),
            pressedColor = Color(0xFF00E5FF),
            rimColor = Color(0xFF80DEEA),
            actionType = ArcadeActionType.MOVE_RIGHT,
            onPressDown = { inputAdapter.setButtonMoveX(1f) },
            onPressUp = { if (inputAdapter.moveX > 0f) inputAdapter.setButtonMoveX(0f) }
        )
    }

    fun createActionButton(state: ArkanoidUiState, inputAdapter: DualModeInputAdapter): ArcadeButtonConfig {
        val isLaser = state.activePowerUp?.contains("LASER") == true
        return ArcadeButtonConfig(
            id = "paddle_action_serve",
            label = if (state.isBallDocked) "SERVE" else if (isLaser) "LASER" else "ACTION",
            iconVector = Icons.Default.Bolt,
            shape = ArcadeButtonShape.PILL,
            weight = 1.3f,
            minWidthDp = 68.dp,
            heightDp = 48.dp,
            baseColor = if (isLaser) Color(0xD9D50000) else Color(0xD90091EA),
            pressedColor = if (isLaser) Color(0xFFFF1744) else Color(0xFF00E5FF),
            rimColor = Color.White,
            actionType = ArcadeActionType.ACTION_PRIMARY,
            onPressDown = { inputAdapter.setButtonAction(true) },
            onPressUp = { inputAdapter.setButtonAction(false) }
        )
    }

    fun createPauseButton(coordinator: ArkanoidGameCoordinator): ArcadeButtonConfig {
        return ArcadeButtonConfig(
            id = "paddle_pause_btn",
            iconVector = Icons.Default.Pause,
            shape = ArcadeButtonShape.CIRCLE,
            sizeDp = 48.dp,
            widthDp = 48.dp,
            minWidthDp = 48.dp,
            baseColor = Color(0xFF37474F),
            pressedColor = Color(0xFF78909C),
            rimColor = Color.White,
            actionType = ArcadeActionType.PAUSE,
            onPressDown = { coordinator.togglePause() }
        )
    }

    fun createFloatingButtons(
        steerLeft: ArcadeButtonConfig,
        action: ArcadeButtonConfig,
        steerRight: ArcadeButtonConfig
    ): List<ArcadeButtonConfig> {
        return listOf(
            steerLeft.copy(id = "fl_left", widthDp = 76.dp, weight = null, floatingOffset = Offset(24f, 440f)),
            action.copy(id = "fl_action", widthDp = 100.dp, weight = null, floatingOffset = Offset(130f, 440f)),
            steerRight.copy(id = "fl_right", widthDp = 76.dp, weight = null, floatingOffset = Offset(260f, 440f))
        )
    }
}
