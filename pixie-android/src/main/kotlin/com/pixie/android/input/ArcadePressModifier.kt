package com.pixie.android.input

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Reusable Compose modifier for arcade game buttons.
 *
 * Triggers [onPressDown] immediately upon pointer down (mousedown / touch press),
 * and [onPressUp] upon release or cancel, eliminating the "mouseup-only" lag of standard click buttons.
 */
fun Modifier.arcadePress(
    onPressDown: () -> Unit,
    onPressUp: () -> Unit = {}
): Modifier = this.pointerInput(Unit) {
    detectTapGestures(
        onPress = {
            onPressDown()
            try {
                tryAwaitRelease()
            } finally {
                onPressUp()
            }
        }
    )
}

/**
 * High-responsiveness tactile arcade button supporting immediate mousedown firing
 * and continuous holding states.
 */
@Composable
fun ArcadeButton(
    onPressDown: () -> Unit,
    onPressUp: () -> Unit = {},
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    backgroundColor: Color = Color(0x99263238),
    pressedColor: Color = Color(0xDD37474F),
    border: BorderStroke? = BorderStroke(1.dp, Color(0x44FFFFFF)),
    content: @Composable (isPressed: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .scale(if (isPressed) 0.95f else 1.0f)
            .clip(shape)
            .background(if (isPressed) pressedColor else backgroundColor)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPressDown()
                        try {
                            tryAwaitRelease()
                        } finally {
                            isPressed = false
                            onPressUp()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content(isPressed)
    }
}
