package com.pixie.android.input

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Individual draggable wrapper for a floating arcade button in edit mode.
 */
@Composable
fun FloatingDraggableButton(
    btn: ArcadeButtonConfig,
    isEditMode: Boolean,
    modifier: Modifier = Modifier
) {
    var dragOffset by remember { mutableStateOf(btn.floatingOffset) }

    Box(
        modifier = modifier
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .then(
                if (isEditMode) {
                    Modifier
                        .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                        .pointerInput(btn.id) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                            }
                        }
                } else Modifier
            )
    ) {
        TactileArcadeButton(
            config = btn,
            isInteractive = !isEditMode
        )
    }
}

/**
 * Floating badge allowing users to toggle Edit Mode to reposition controls.
 */
@Composable
fun FloatingEditModeBadge(
    isEditMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onToggle() },
        shape = CircleShape,
        color = if (isEditMode) Color(0xFF00E5FF) else Color(0xCC10141D),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.OpenWith,
                contentDescription = if (isEditMode) "Save Button Positions" else "Move Buttons",
                tint = if (isEditMode) Color.Black else Color(0xFF00E5FF),
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = if (isEditMode) "DONE" else "MOVE",
                color = if (isEditMode) Color.Black else Color(0xFFE0F7FA),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Floating and draggable arcade buttons.
 * Includes an Edit Mode toggle so users can freely reposition buttons, then lock them in place.
 */
@Composable
fun FloatingAdjustableDeck(
    buttons: List<ArcadeButtonConfig>,
    modifier: Modifier = Modifier
) {
    var isEditMode by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        buttons.forEach { btn ->
            FloatingDraggableButton(
                btn = btn,
                isEditMode = isEditMode
            )
        }

        FloatingEditModeBadge(
            isEditMode = isEditMode,
            onToggle = { isEditMode = !isEditMode },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
        )
    }
}
