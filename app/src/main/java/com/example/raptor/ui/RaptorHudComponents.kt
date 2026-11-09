package com.example.raptor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixie.android.input.ArcadeButtonConfig
import com.pixie.android.input.ArcadeWingPillar
import com.pixie.android.input.TactileArcadeButton
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
 * Top HUD Overlay for Raptor combat displaying mission status, level progress, score, active weapon, and actions.
 */
@Composable
fun RaptorTopHud(
    score: Long,
    levelName: String,
    difficulty: DifficultyPreset,
    distance: Float,
    targetDistance: Float,
    activeWeaponId: String = "vulcan",
    weaponTier: Int = 1,
    onPauseClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xCC0A0E17),
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RaptorDifficultyChip(difficulty = difficulty)
                Spacer(modifier = Modifier.width(6.dp))
                RaptorActiveWeaponBadge(activeWeaponId = activeWeaponId, weaponTier = weaponTier)
            }
        }
    }
}

@Composable
fun RaptorActiveWeaponBadge(
    activeWeaponId: String,
    weaponTier: Int,
    modifier: Modifier = Modifier
) {
    val shortName = com.example.raptor.entities.RaptorWeapons.getShortName(activeWeaponId)
    val label = if (activeWeaponId == "vulcan") "$shortName T$weaponTier" else shortName
    val (chipBg, chipBorder, chipText) = when (activeWeaponId) {
        "twin_plasma" -> Triple(Color(0xFF003366), Color(0xFF00E5FF), Color(0xFFE0F7FA))
        "odin_laser" -> Triple(Color(0xFF4A148C), Color(0xFFE040FB), Color(0xFFF3E5F5))
        "deathray" -> Triple(Color(0xFF5D1049), Color(0xFFFF4081), Color(0xFFFCE4EC))
        else -> Triple(Color(0xFF004D40), Color(0xFF26A69A), Color(0xFFE0F2F1))
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = chipBg,
        border = BorderStroke(1.dp, chipBorder),
        modifier = modifier.testTag("raptor_active_weapon_badge")
    ) {
        Text(
            text = "GUN: $label",
            color = chipText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
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
    val chipColor = when (difficulty) {
        DifficultyPreset.EASY -> Color(0xFF1B5E20)
        DifficultyPreset.MEDIUM -> Color(0xFF0D47A1)
        DifficultyPreset.DIFFICULT -> Color(0xFFE65100)
        DifficultyPreset.HEAVY_METAL -> Color(0xFFB71C1C)
        else -> Color(0xFF263238)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = chipColor,
        modifier = modifier.testTag("raptor_difficulty_chip")
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

/**
 * Left Lateral Wing in Landscape mode (houses HUD integrity metrics + Auto-Fire toggle button + Weapon Switch).
 */
@Composable
fun RaptorLandscapeLeftWing(
    shieldHp: Float,
    hullHp: Float,
    autoFireBtn: ArcadeButtonConfig,
    weaponBtn: ArcadeButtonConfig? = null,
    modifier: Modifier = Modifier
) {
    val buttonList = mutableListOf(autoFireBtn.copy(widthDp = 108.dp, weight = null))
    weaponBtn?.let { buttonList.add(it.copy(widthDp = 108.dp, weight = null)) }

    ArcadeWingPillar(
        width = 124.dp,
        title = "SYSTEMS",
        titleColor = Color(0xFF80DEEA),
        borderColor = Color(0x3300E5FF),
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "INTEGRITY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF80DEEA)
                )
                Spacer(modifier = Modifier.height(6.dp))
                RaptorIntegrityGauges(shieldHp = shieldHp, hullHp = hullHp)
            }
        },
        buttons = buttonList,
        modifier = modifier
    )
}

/**
 * Right Lateral Wing in Landscape mode (houses tactical actions: Missiles, Mega-Bomb, Pause).
 */
@Composable
fun RaptorLandscapeRightWing(
    missileBtn: ArcadeButtonConfig,
    bombBtn: ArcadeButtonConfig,
    pauseBtn: ArcadeButtonConfig,
    modifier: Modifier = Modifier
) {
    ArcadeWingPillar(
        width = 124.dp,
        title = "TACTICAL",
        titleColor = Color(0xFFFF80AB),
        borderColor = Color(0x33FF4081),
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TactileArcadeButton(config = pauseBtn)
            }
        },
        buttons = listOf(
            missileBtn.copy(widthDp = 108.dp, weight = null),
            bombBtn.copy(widthDp = 108.dp, weight = null)
        ),
        modifier = modifier
    )
}

/**
 * Full Split Wings Arcade Deck for Landscape mode.
 */
@Composable
fun RaptorSplitWingsDeck(
    shieldHp: Float,
    hullHp: Float,
    autoFireBtn: ArcadeButtonConfig,
    missileBtn: ArcadeButtonConfig,
    bombBtn: ArcadeButtonConfig,
    pauseBtn: ArcadeButtonConfig,
    weaponBtn: ArcadeButtonConfig? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RaptorLandscapeLeftWing(
            shieldHp = shieldHp,
            hullHp = hullHp,
            autoFireBtn = autoFireBtn,
            weaponBtn = weaponBtn
        )

        Spacer(modifier = Modifier.weight(1f))

        RaptorLandscapeRightWing(
            missileBtn = missileBtn,
            bombBtn = bombBtn,
            pauseBtn = pauseBtn
        )
    }
}
