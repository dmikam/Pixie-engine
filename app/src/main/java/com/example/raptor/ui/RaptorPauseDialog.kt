package com.example.raptor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.window.Dialog
import com.pixie.engine.core.context.DifficultyPreset

/**
 * Tactical Pause Dialog for Raptor game sessions.
 */
@Composable
fun RaptorPauseDialog(
    selectedDifficulty: DifficultyPreset,
    isSoundEnabled: Boolean,
    isHapticEnabled: Boolean,
    currentLevel: Int,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onGoToHangar: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onSelectDifficulty: (DifficultyPreset) -> Unit,
    onSelectLevel: (Int) -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
            border = BorderStroke(1.5.dp, Color(0xFFFF9100)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TACTICAL PAUSE",
                    color = Color(0xFFFF9100),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(14.dp))
                RaptorDifficultySelector(
                    selectedDifficulty = selectedDifficulty,
                    onSelectDifficulty = onSelectDifficulty
                )

                Spacer(modifier = Modifier.height(14.dp))
                RaptorLevelSelector(
                    currentLevel = currentLevel,
                    onSelectLevel = onSelectLevel
                )

                Spacer(modifier = Modifier.height(14.dp))
                RaptorAudioHapticControls(
                    isSoundEnabled = isSoundEnabled,
                    isHapticEnabled = isHapticEnabled,
                    onToggleSound = onToggleSound,
                    onToggleHaptic = onToggleHaptic
                )

                Spacer(modifier = Modifier.height(18.dp))
                RaptorPauseActionButtons(
                    onResume = onResume,
                    onRestart = onRestart,
                    onGoToHangar = onGoToHangar,
                    onExit = onExit
                )
            }
        }
    }
}

@Composable
private fun RaptorDifficultySelector(
    selectedDifficulty: DifficultyPreset,
    onSelectDifficulty: (DifficultyPreset) -> Unit
) {
    Text(
        text = "DIFFICULTY PRESET (0..100)",
        color = Color(0xFF90A4AE),
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DifficultyPreset.DEFAULT_PRESETS.forEach { preset ->
            val isSelected = preset == selectedDifficulty
            Button(
                onClick = { onSelectDifficulty(preset) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFFFF9100) else Color(0xFF263238)
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .height(36.dp)
            ) {
                Text(
                    text = "${preset.difficultyValue}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.Black else Color.White
                )
            }
        }
    }
}

@Composable
private fun RaptorLevelSelector(
    currentLevel: Int,
    onSelectLevel: (Int) -> Unit
) {
    Text(
        text = "CAMPAIGN LEVEL",
        color = Color(0xFF90A4AE),
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        (1..3).forEach { lvl ->
            val isSelected = lvl == currentLevel
            Button(
                onClick = { onSelectLevel(lvl) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF263238)
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .height(36.dp)
            ) {
                Text(
                    text = "LVL $lvl",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.Black else Color.White
                )
            }
        }
    }
}

@Composable
private fun RaptorAudioHapticControls(
    isSoundEnabled: Boolean,
    isHapticEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit
) {
    Text(
        text = "SOUND & HAPTIC RESPONSE",
        color = Color(0xFFFF9100),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace
    )
    Spacer(modifier = Modifier.height(6.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F131C), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF263238), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SoundToggleRow(isSoundEnabled = isSoundEnabled, onToggleSound = onToggleSound)
        HorizontalDivider(color = Color(0xFF263238), thickness = 1.dp)
        HapticToggleRow(isHapticEnabled = isHapticEnabled, onToggleHaptic = onToggleHaptic)
    }
}

@Composable
private fun SoundToggleRow(
    isSoundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onToggleSound(!isSoundEnabled) }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                contentDescription = "Combat Audio",
                tint = if (isSoundEnabled) Color(0xFFFF9100) else Color(0xFF78909C),
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = "Sound Effects (SFX)",
                    color = if (isSoundEnabled) Color.White else Color(0xFF78909C),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isSoundEnabled) "Cannons, missiles, explosions" else "Audio Muted",
                    color = if (isSoundEnabled) Color(0xFFFFB74D) else Color(0xFF546E7A),
                    fontSize = 10.sp
                )
            }
        }
        Switch(
            checked = isSoundEnabled,
            onCheckedChange = { onToggleSound(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFFF9100),
                checkedTrackColor = Color(0xFF5D2400),
                uncheckedThumbColor = Color(0xFF78909C),
                uncheckedTrackColor = Color(0xFF263238)
            ),
            modifier = Modifier.testTag("raptor_sound_switch")
        )
    }
}

@Composable
private fun HapticToggleRow(
    isHapticEnabled: Boolean,
    onToggleHaptic: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onToggleHaptic(!isHapticEnabled) }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Filled.Vibration,
                contentDescription = "Haptic Feedback",
                tint = if (isHapticEnabled) Color(0xFF00E5FF) else Color(0xFF78909C),
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = "Haptic Response",
                    color = if (isHapticEnabled) Color.White else Color(0xFF78909C),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isHapticEnabled) "Vibrations on impacts & weapons" else "Haptics Disabled",
                    color = if (isHapticEnabled) Color(0xFF80DEEA) else Color(0xFF546E7A),
                    fontSize = 10.sp
                )
            }
        }
        Switch(
            checked = isHapticEnabled,
            onCheckedChange = { onToggleHaptic(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00E5FF),
                checkedTrackColor = Color(0xFF004D40),
                uncheckedThumbColor = Color(0xFF78909C),
                uncheckedTrackColor = Color(0xFF263238)
            ),
            modifier = Modifier.testTag("raptor_haptic_switch")
        )
    }
}

@Composable
private fun RaptorPauseActionButtons(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onGoToHangar: () -> Unit,
    onExit: () -> Unit
) {
    Button(
        onClick = onResume,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("resume_button")
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text("RESUME COMBAT", fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onRestart,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("raptor_restart_button")
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("RESTART", fontSize = 12.sp)
        }

        OutlinedButton(
            onClick = onGoToHangar,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("raptor_hangar_button")
        ) {
            Icon(
                Icons.Filled.MonetizationOn,
                contentDescription = null,
                tint = Color(0xFFFF9100),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "HANGAR",
                color = Color(0xFFFF9100),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = onExit,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("raptor_exit_button")
    ) {
        Text("EXIT TO ARCADE HUB", color = Color(0xFFFF5252))
    }
}
