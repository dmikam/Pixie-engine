package com.example.arkanoid.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.arkanoid.ArkanoidGameCoordinator
import com.example.arkanoid.ArkanoidUiState

/**
 * Modals and Dialogs for Arkanoid Game States.
 */
@Composable
fun ArkanoidModals(
    state: ArkanoidUiState,
    coordinator: ArkanoidGameCoordinator,
    onExitToMenu: () -> Unit
) {
    if (state.isPaused) {
        PauseDialog(
            onResume = { coordinator.togglePause() },
            onRestart = { coordinator.restartGame() },
            onExit = onExitToMenu
        )
    }

    if (state.isLevelCleared && !state.isVictory) {
        LevelClearedDialog(
            levelNumber = state.levelNumber,
            score = state.score,
            onNextLevel = { coordinator.nextLevel() }
        )
    }

    if (state.isGameOver) {
        GameOverDialog(
            score = state.score,
            highScore = state.highScore,
            onRetry = { coordinator.restartGame() },
            onExit = onExitToMenu
        )
    }

    if (state.isVictory) {
        VictoryDialog(
            score = state.score,
            onPlayAgain = { coordinator.restartGame() },
            onExit = onExitToMenu
        )
    }
}

@Composable
private fun PauseDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161920)),
            border = BorderStroke(1.dp, Color(0xFF00E5FF))
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
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESUME", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESTART LEVEL", fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("EXIT TO MENU", fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun LevelClearedDialog(
    levelNumber: Int,
    score: Int,
    onNextLevel: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161920)),
            border = BorderStroke(1.dp, Color(0xFF00E676))
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
                    text = "LEVEL $levelNumber CLEARED!",
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
                    text = "CURRENT SCORE: $score",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onNextLevel,
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

@Composable
private fun GameOverDialog(
    score: Int,
    highScore: Int,
    onRetry: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161920)),
            border = BorderStroke(1.dp, Color(0xFFFF1744))
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
                    text = "FINAL SCORE: $score",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "HIGH SCORE: $highScore",
                    color = Color(0xFFFFD54F),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TRY AGAIN", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("RETURN TO MENU", fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun VictoryDialog(
    score: Int,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161920)),
            border = BorderStroke(1.dp, Color(0xFFFFD600))
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
                    text = "VICTORY SCORE: $score",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onPlayAgain,
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
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("EXIT TO MENU", fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
