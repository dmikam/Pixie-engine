package com.example.arkanoid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.arkanoid.ArkanoidGameCoordinator
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.android.input.ArcadeButton
import com.pixie.android.input.DualModeInputAdapter
import com.pixie.android.ui.PixieGameCanvas

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090C10))
    ) {
        // 1. Fullscreen Main Game Canvas with full-screen touch drag & mousedown tap
        PixieGameCanvas(
            engine = coordinator.engine,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            coordinator.onActionTriggered()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Move paddle by horizontal drag distance
                        coordinator.movePaddleByDelta(dragAmount.x * 0.45f)
                    }
                },
            onFrameTick = { dt ->
                coordinator.update(dt)
            }
        )

        // 2. Floating Sleek Top HUD Overlay
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            color = Color(0xD910141C),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF)),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onExitToMenu,
                            modifier = Modifier.testTag("arkanoid_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit to Menu",
                                tint = Color(0xFF00E5FF)
                            )
                        }
                        Column {
                            Text(
                                text = "ARKANOID",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "L${state.levelNumber}: ${state.levelName.uppercase()}",
                                color = Color(0xFF00E5FF),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Score & High Score
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "SCORE: ${state.score}",
                            color = Color(0xFFFFD54F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "BEST: ${state.highScore}",
                            color = Color(0xFF90A4AE),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Pause Button
                    IconButton(
                        onClick = { coordinator.togglePause() },
                        modifier = Modifier.testTag("arkanoid_pause_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color.White
                        )
                    }
                }

                // Sub-HUD Row: Lives, Remaining Bricks, and Active Powerup
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lives Hearts
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "LIVES:",
                            color = Color(0xFFCFD8DC),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        repeat(3) { index ->
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = if (index < state.lives) Color(0xFFFF1744) else Color(0xFF37474F),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Combo Multiplier
                    if (state.combo > 1) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE65100)
                        ) {
                            Text(
                                text = "COMBO ${state.comboMultiplier}X (${state.combo})",
                                color = Color(0xFFFFF3E0),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Active Power-up Pill
                    state.activePowerUp?.let { pill ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF00B0FF)
                        ) {
                            Text(
                                text = pill,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Serve Hint Overlay when ball is docked
        if (state.isBallDocked && !state.isPaused && !state.isGameOver) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xD9000000),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFFFD600),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TAP SCREEN OR ACTION TO SERVE",
                        color = Color(0xFFE0F7FA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 4. Floating Bottom Controls Deck (Overlay over bottom)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            color = Color(0xB810141C),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Steer Left Button (continuous motion on hold, triggers on mousedown)
                ArcadeButton(
                    onPressDown = { inputAdapter.setButtonMoveX(-1f) },
                    onPressUp = { if (inputAdapter.moveX < 0f) inputAdapter.setButtonMoveX(0f) },
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = Color(0x99263238),
                    pressedColor = Color(0xDD37474F),
                    modifier = Modifier
                        .size(width = 68.dp, height = 48.dp)
                        .testTag("paddle_left_button")
                ) { isPressed ->
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Steer Left",
                        tint = if (isPressed) Color(0xFF00E5FF) else Color.White
                    )
                }

                // Center Action Button (triggers immediately on mousedown, continuous fire for lasers)
                ArcadeButton(
                    onPressDown = { inputAdapter.setButtonAction(true) },
                    onPressUp = { inputAdapter.setButtonAction(false) },
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = if (state.activePowerUp?.contains("LASER") == true) Color(0xD9D50000) else Color(0xD90091EA),
                    pressedColor = if (state.activePowerUp?.contains("LASER") == true) Color(0xFFFF1744) else Color(0xFF00E5FF),
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .testTag("paddle_action_button")
                ) { isPressed ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isBallDocked) "SERVE BALL" else if (state.activePowerUp?.contains("LASER") == true) "FIRE LASERS" else "ACTION",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }

                // Steer Right Button (continuous motion on hold, triggers on mousedown)
                ArcadeButton(
                    onPressDown = { inputAdapter.setButtonMoveX(1f) },
                    onPressUp = { if (inputAdapter.moveX > 0f) inputAdapter.setButtonMoveX(0f) },
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = Color(0x99263238),
                    pressedColor = Color(0xDD37474F),
                    modifier = Modifier
                        .size(width = 68.dp, height = 48.dp)
                        .testTag("paddle_right_button")
                ) { isPressed ->
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Steer Right",
                        tint = if (isPressed) Color(0xFF00E5FF) else Color.White
                    )
                }
            }
        }
    }

    // 4. Modals & Dialogs
    // Pause Dialog
    if (state.isPaused) {
        Dialog(onDismissRequest = { coordinator.togglePause() }) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161920)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GAME PAUSED",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { coordinator.togglePause() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESUME", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { coordinator.restartGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESTART LEVEL", fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onExitToMenu,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("EXIT TO MENU", fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }

    // Level Cleared Dialog
    if (state.isLevelCleared && !state.isVictory) {
        Dialog(onDismissRequest = {}) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161920)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "LEVEL ${state.levelNumber} CLEARED!",
                        color = Color(0xFF00E676),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Matrix Completely Shattered",
                        color = Color(0xFFB0BEC5),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "CURRENT SCORE: ${state.score}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { coordinator.nextLevel() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ADVANCE TO NEXT LEVEL",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }

    // Game Over Dialog
    if (state.isGameOver) {
        Dialog(onDismissRequest = {}) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161920)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF1744))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BALL LOST — GAME OVER",
                        color = Color(0xFFFF1744),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "FINAL SCORE: ${state.score}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "HIGH SCORE: ${state.highScore}",
                        color = Color(0xFFFFD54F),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { coordinator.restartGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TRY AGAIN", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onExitToMenu,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("RETURN TO MENU", fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }

    // Victory Dialog
    if (state.isVictory) {
        Dialog(onDismissRequest = {}) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161920)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD600))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD600),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "CAMPAIGN VICTORY!",
                        color = Color(0xFFFFD600),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All 3 Cyber Defense Grids Demolished!",
                        color = Color(0xFFB0BEC5),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "VICTORY SCORE: ${state.score}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { coordinator.restartGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "PLAY AGAIN",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onExitToMenu,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("EXIT TO MENU", fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
