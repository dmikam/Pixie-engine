package com.example.dinorun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixie.android.input.ControlMode
import com.pixie.android.input.DualModeInputAdapter

@Composable
fun DinoHud(
    score: Long,
    distanceMeters: Float,
    livesLeft: Int,
    colorProgress: Float,
    fps: Float,
    currentMode: ControlMode,
    pixelsGap: Int,
    onToggleControlMode: () -> Unit,
    onTogglePixelGap: () -> Unit,
    onPauseClick: () -> Unit,
    onExitToMenu: () -> Unit = {},
    inputAdapter: DualModeInputAdapter,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DinoTopHudBar(
            score = score,
            distanceMeters = distanceMeters,
            livesLeft = livesLeft,
            colorProgress = colorProgress,
            fps = fps,
            currentMode = currentMode,
            pixelsGap = pixelsGap,
            onToggleControlMode = onToggleControlMode,
            onTogglePixelGap = onTogglePixelGap,
            onPauseClick = onPauseClick,
            onExitToMenu = onExitToMenu
        )

        if (currentMode == ControlMode.VIRTUAL_BUTTONS) {
            DinoVirtualControlDeck(inputAdapter = inputAdapter)
        }
    }
}

@Composable
fun DinoTopHudBar(
    score: Long,
    distanceMeters: Float,
    livesLeft: Int,
    colorProgress: Float,
    fps: Float,
    currentMode: ControlMode,
    pixelsGap: Int,
    onToggleControlMode: () -> Unit,
    onTogglePixelGap: () -> Unit,
    onPauseClick: () -> Unit,
    onExitToMenu: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = Color(0xCC1A1A1A),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DinoDistanceScorePill(distanceMeters = distanceMeters, score = score)
                DinoLivesIndicator(livesLeft = livesLeft)
                DinoQuickActionToggles(
                    pixelsGap = pixelsGap,
                    currentMode = currentMode,
                    onTogglePixelGap = onTogglePixelGap,
                    onToggleControlMode = onToggleControlMode,
                    onPauseClick = onPauseClick,
                    onExitToMenu = onExitToMenu
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            DinoColorRecoveryBar(colorProgress = colorProgress, fps = fps)
        }
    }
}

@Composable
private fun DinoDistanceScorePill(distanceMeters: Float, score: Long) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${distanceMeters.toInt()}m",
                color = Color(0xFFFFD54F),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "v0.3",
                color = Color(0xFFB0BEC5),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = "$score PTS",
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun DinoLivesIndicator(livesLeft: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val isAlive = index < livesLeft
            Text(
                text = "♥",
                color = if (isAlive) Color(0xFFFF5252) else Color(0xFF616161),
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun DinoQuickActionToggles(
    pixelsGap: Int,
    currentMode: ControlMode,
    onTogglePixelGap: () -> Unit,
    onToggleControlMode: () -> Unit,
    onPauseClick: () -> Unit,
    onExitToMenu: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DinoPixelGapPill(pixelsGap = pixelsGap, onToggle = onTogglePixelGap)
        DinoControlModePill(currentMode = currentMode, onToggle = onToggleControlMode)

        IconButton(
            onClick = onPauseClick,
            modifier = Modifier
                .size(36.dp)
                .testTag("pause_button")
        ) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = "Pause Game",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = onExitToMenu,
            modifier = Modifier
                .size(36.dp)
                .testTag("exit_to_menu_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Exit to Menu",
                tint = Color(0xFFB0BEC5),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DinoPixelGapPill(pixelsGap: Int, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .testTag("toggle_pixel_gap"),
        color = if (pixelsGap > 0) Color(0xFF388E3C) else Color(0xFF424242)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = "Toggle Pixel Grid",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = if (pixelsGap > 0) "GRID" else "SOLID",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DinoControlModePill(currentMode: ControlMode, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .testTag("toggle_control_mode"),
        color = Color(0xFF1976D2)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (currentMode == ControlMode.VIRTUAL_BUTTONS)
                    Icons.Default.VideogameAsset else Icons.Default.TouchApp,
                contentDescription = "Toggle Control Mode",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = if (currentMode == ControlMode.VIRTUAL_BUTTONS) "PAD" else "SWIPE",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DinoColorRecoveryBar(colorProgress: Float, fps: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "COLOR: ${(colorProgress * 100).toInt()}%",
            color = Color(0xFF81D4FA),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { colorProgress },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF00E5FF),
            trackColor = Color(0xFF424242)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${fps.toInt()} FPS",
            color = Color(0xFF9E9E9E),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun DinoVirtualControlDeck(inputAdapter: DualModeInputAdapter) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        DinoVirtualDuckButton(
            inputAdapter = inputAdapter,
            modifier = Modifier.align(Alignment.BottomStart)
        )
        DinoVirtualJumpButton(
            inputAdapter = inputAdapter,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun DinoVirtualDuckButton(
    inputAdapter: DualModeInputAdapter,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color(0x99212121))
            .border(2.dp, Color(0xCC616161), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        inputAdapter.setButtonDuck(true)
                        tryAwaitRelease()
                        inputAdapter.setButtonDuck(false)
                    }
                )
            }
            .testTag("duck_button"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "DUCK\n▼",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DinoVirtualJumpButton(
    inputAdapter: DualModeInputAdapter,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color(0xCC2E7D32))
            .border(2.dp, Color(0xFF81C784), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        inputAdapter.setButtonJump(true)
                        tryAwaitRelease()
                        inputAdapter.setButtonJump(false)
                    }
                )
            }
            .testTag("jump_button"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "JUMP\n▲",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
