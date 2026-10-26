package com.pixie.android.input

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixie.engine.core.sprite.PixelSprite

enum class ArcadeButtonShape {
    CIRCLE,
    OCTAGON,
    ROUNDED_RECT,
    PILL
}

enum class ArcadeActionType {
    MOVE_LEFT,
    MOVE_RIGHT,
    MOVE_UP,
    MOVE_DOWN,
    ACTION_PRIMARY,
    ACTION_SECONDARY,
    PAUSE,
    JUMP,
    DUCK,
    CUSTOM
}

/**
 * Customizable arcade button specification.
 * Supports configurable shape, size, flex weight, color, label/letter, vector icon, or retro PixelSprite.
 */
data class ArcadeButtonConfig(
    val id: String,
    val label: String? = null,
    val iconVector: ImageVector? = null,
    val sprite: PixelSprite? = null,
    val shape: ArcadeButtonShape = ArcadeButtonShape.CIRCLE,
    val sizeDp: Dp = 52.dp,
    val widthDp: Dp? = null,
    val heightDp: Dp? = null,
    val weight: Float? = null, // Flex weight for automatic proportional distribution in Rows
    val minWidthDp: Dp? = null,
    val maxWidthDp: Dp? = null,
    val baseColor: Color = Color(0xFF00B0FF),
    val pressedColor: Color = Color(0xFF00E5FF),
    val rimColor: Color = Color(0xFF40C4FF),
    val actionType: ArcadeActionType = ArcadeActionType.ACTION_PRIMARY,
    val floatingOffset: Offset = Offset.Zero,
    val onPressDown: () -> Unit = {},
    val onPressUp: () -> Unit = {}
)

enum class ViewportMode {
    CENTERED_WINGS,
    ADAPTIVE_FIT
}

enum class ArcadeDeckPlacement {
    SPLIT_WINGS,
    BOTTOM_STRIP,
    FLOATING
}

enum class InputPreference {
    HYBRID_AUTO,
    TOUCH_ONLY,
    PHYSICAL_ONLY
}
