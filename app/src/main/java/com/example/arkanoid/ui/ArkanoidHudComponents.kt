package com.example.arkanoid.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arkanoid.ArkanoidUiState
import com.pixie.android.input.ArcadeButtonConfig
import com.pixie.android.input.ArcadeWingPillar
import com.pixie.android.input.TactileArcadeButton

/**
 * Top Status Bar docked above game canvas in Portrait mode.
 */
@Composable
fun ArkanoidPortraitStatusBar(
    state: ArkanoidUiState,
    onExitToMenu: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = Color(0xEA10141C),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0x3300E5FF))
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onExitToMenu,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("arkanoid_exit_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Exit to Menu",
                        tint = Color(0xFF90A4AE),
                        modifier = Modifier.size(18.dp)
                    )
                }

                ScoreBlock(score = state.score)
                LevelBadge(levelNumber = state.levelNumber)
                HighScoreBlock(highScore = state.highScore)

                IconButton(
                    onClick = onPause,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("arkanoid_pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LivesIndicator(lives = state.lives)
                ComboBadge(combo = state.combo, multiplier = state.comboMultiplier)
                PowerUpPill(pill = state.activePowerUp)
            }
        }
    }
}

/**
 * Left Lateral Wing in Landscape mode (absorbs Left HUD + Steer buttons).
 */
@Composable
fun ArkanoidLandscapeLeftWing(
    state: ArkanoidUiState,
    onExitToMenu: () -> Unit,
    leftBtn: ArcadeButtonConfig,
    rightBtn: ArcadeButtonConfig
) {
    ArcadeWingPillar(
        width = 124.dp,
        borderColor = Color(0x3300E5FF)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onExitToMenu,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("arkanoid_exit_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Exit to Menu",
                    tint = Color(0xFF90A4AE),
                    modifier = Modifier.size(18.dp)
                )
            }
            LevelBadge(levelNumber = state.levelNumber)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            ScoreBlock(score = state.score)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "HI: ${state.highScore}",
                color = Color(0xFFFFD54F),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "STEER",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF80DEEA)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TactileArcadeButton(
                    config = leftBtn.copy(widthDp = 52.dp, weight = null),
                    modifier = Modifier.weight(1f)
                )
                TactileArcadeButton(
                    config = rightBtn.copy(widthDp = 52.dp, weight = null),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Right Lateral Wing in Landscape mode (absorbs Right HUD + Action button).
 */
@Composable
fun ArkanoidLandscapeRightWing(
    state: ArkanoidUiState,
    onPause: () -> Unit,
    actionBtn: ArcadeButtonConfig
) {
    ArcadeWingPillar(
        width = 124.dp,
        borderColor = Color(0x33FF4081)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LivesIndicator(lives = state.lives)
            IconButton(
                onClick = onPause,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("arkanoid_pause_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            ComboBadge(combo = state.combo, multiplier = state.comboMultiplier)
            PowerUpPill(pill = state.activePowerUp)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            TactileArcadeButton(
                config = actionBtn.copy(widthDp = null, weight = null),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * In-game overlay informing the user to tap or press action to serve the ball.
 */
@Composable
fun BoxScope.ArkanoidServeHintOverlay() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xD900838F),
        border = BorderStroke(1.dp, Color(0xFF80DEEA)),
        modifier = Modifier
            .align(Alignment.Center)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "TAP SCREEN OR ACTION TO SERVE",
                color = Color(0xFFE0F7FA),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ================= Compact Sub-Composables =================

@Composable
private fun ScoreBlock(score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "SCORE",
            color = Color(0xFF80DEEA),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = score.toString().padStart(6, '0'),
            color = Color(0xFF00E5FF),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun HighScoreBlock(highScore: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "HI-SCORE",
            color = Color(0xFFFFD54F),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = highScore.toString().padStart(6, '0'),
            color = Color(0xFFFFE082),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun LevelBadge(levelNumber: Int) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF00B0FF)
    ) {
        Text(
            text = "LVL $levelNumber",
            color = Color.Black,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun LivesIndicator(lives: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "LIVES:",
            color = Color(0xFFCFD8DC),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        repeat(3) { index ->
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = if (index < lives) Color(0xFFFF1744) else Color(0xFF37474F),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun ComboBadge(combo: Int, multiplier: Int) {
    if (combo > 1) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFFE65100)
        ) {
            Text(
                text = "COMBO ${multiplier}X",
                color = Color(0xFFFFF3E0),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun PowerUpPill(pill: String?) {
    pill?.let {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF00B0FF)
        ) {
            Text(
                text = it,
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}
