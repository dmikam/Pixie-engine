package com.example.raptor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixie.engine.core.context.DifficultyPreset

/**
 * Top HUD Overlay for Raptor combat displaying mission status, level progress, score, and actions.
 */
@Composable
fun RaptorTopHud(
    score: Long,
    levelName: String,
    difficulty: DifficultyPreset,
    distance: Float,
    targetDistance: Float,
    onPauseClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xCC0A0E17),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RaptorHudBackButton(onClick = onBackClick)
                RaptorLevelProgress(levelName = levelName, distance = distance, targetDistance = targetDistance)
                RaptorScoreDisplay(score = score)
                RaptorPauseButton(onClick = onPauseClick)
            }

            Spacer(modifier = Modifier.height(4.dp))
            RaptorDifficultyChip(difficulty = difficulty)
        }
    }
}

@Composable
fun RaptorHudBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
            .testTag("raptor_back_button")
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Menu",
            tint = Color(0xFFCFD8DC)
        )
    }
}

@Composable
fun RaptorLevelProgress(
    levelName: String,
    distance: Float,
    targetDistance: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = levelName,
            color = Color(0xFFFF9100),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        val progress = if (targetDistance > 0f) (distance / targetDistance).coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .width(120.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = Color(0xFFFF9100),
            trackColor = Color(0xFF263238)
        )
    }
}

@Composable
fun RaptorScoreDisplay(
    score: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "SCORE",
            color = Color(0xFF78909C),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "%06d".format(score),
            color = Color(0xFF00E5FF),
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun RaptorPauseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
            .testTag("raptor_pause_button")
    ) {
        Icon(
            imageVector = Icons.Filled.Pause,
            contentDescription = "Pause Game",
            tint = Color(0xFFCFD8DC)
        )
    }
}

@Composable
fun RaptorDifficultyChip(
    difficulty: DifficultyPreset,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        val chipColor = when (difficulty) {
            DifficultyPreset.EASY -> Color(0xFF1B5E20)
            DifficultyPreset.MEDIUM -> Color(0xFF0D47A1)
            DifficultyPreset.DIFFICULT -> Color(0xFFE65100)
            DifficultyPreset.HEAVY_METAL -> Color(0xFFB71C1C)
            else -> Color(0xFF263238)
        }

        Surface(
            shape = RoundedCornerShape(4.dp),
            color = chipColor
        ) {
            Text(
                text = "${difficulty.name.uppercase()} (${difficulty.difficultyValue})",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun StatusGauge(
    label: String,
    value: Float,
    maxValue: Float,
    activeColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xCC0A0E17),
        border = BorderStroke(1.dp, Color(0xFF263238)),
        modifier = modifier.width(68.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = activeColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { if (maxValue > 0f) (value / maxValue).coerceIn(0f, 1f) else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = activeColor,
                trackColor = backgroundColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${value.toInt()}",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun RaptorIntegrityGauges(
    shieldHp: Float,
    hullHp: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        StatusGauge(
            label = "SHIELD",
            value = shieldHp,
            maxValue = 100f,
            activeColor = Color(0xFF00E5FF),
            backgroundColor = Color(0xFF002233)
        )

        Spacer(modifier = Modifier.height(8.dp))

        StatusGauge(
            label = "HULL",
            value = hullHp,
            maxValue = 100f,
            activeColor = if (hullHp > 40f) Color(0xFF00E676) else Color(0xFFFF3D00),
            backgroundColor = Color(0xFF2E1500)
        )
    }
}
