package com.example.dinorun.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixie.engine.core.layer.PixieLayer
import com.pixie.engine.core.world.WorldDefinition

@Composable
internal fun DinoPauseHeader() {
    Text(
        text = "GAME PAUSED (v0.3)",
        color = Color(0xFFFFD54F),
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
    )
}

@Composable
internal fun DinoWorldSelector(
    currentWorld: WorldDefinition,
    availableWorlds: List<WorldDefinition>,
    onSelectWorld: (WorldDefinition) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "ACTIVE WORLD DEFINITION",
            color = Color(0xFFFF8A65),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            availableWorlds.forEach { world ->
                DinoWorldRow(
                    world = world,
                    isSelected = world.id == currentWorld.id,
                    onClick = { onSelectWorld(world) }
                )
            }
        }
    }
}

@Composable
private fun DinoWorldRow(
    world: WorldDefinition,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF263238) else Color(0xFF141414))
            .border(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0xFF333333), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = world.displayName,
            color = if (isSelected) Color(0xFF00E5FF) else Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
        if (isSelected) {
            Text(
                text = "ACTIVE",
                color = Color(0xFF00E5FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun DinoAudioHapticSettings(
    isSoundEnabled: Boolean,
    isHapticEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "SOUND & HAPTIC RESPONSE",
            color = Color(0xFF00E5FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Text(text = "Toggle retro sound effects & tactile vibrations", color = Color(0xFF9E9E9E), fontSize = 10.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121212), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DinoSettingToggleRow(
                icon = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                title = "Sound Effects (SFX)",
                subtitle = if (isSoundEnabled) "Enabled (8-bit arcade audio)" else "Muted",
                isChecked = isSoundEnabled,
                onCheckedChange = onToggleSound,
                testTag = "dino_sound_switch"
            )
            HorizontalDivider(color = Color(0xFF263238), thickness = 1.dp)
            DinoSettingToggleRow(
                icon = Icons.Filled.Vibration,
                title = "Haptic Response",
                subtitle = if (isHapticEnabled) "Enabled (Jump & impact vibrations)" else "Disabled",
                isChecked = isHapticEnabled,
                onCheckedChange = onToggleHaptic,
                testTag = "dino_haptic_switch"
            )
        }
    }
}

@Composable
private fun DinoSettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onCheckedChange(!isChecked) }
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
                imageVector = icon,
                contentDescription = title,
                tint = if (isChecked) Color(0xFF00E5FF) else Color(0xFF757575),
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    color = if (isChecked) Color.White else Color(0xFF757575),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = subtitle,
                    color = if (isChecked) Color(0xFF80DEEA) else Color(0xFF616161),
                    fontSize = 10.sp
                )
            }
        }
        DinoSettingSwitch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            testTag = testTag
        )
    }
}

@Composable
private fun DinoSettingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color(0xFF00E5FF),
            checkedTrackColor = Color(0xFF004D40),
            uncheckedThumbColor = Color(0xFF757575),
            uncheckedTrackColor = Color(0xFF263238)
        ),
        modifier = Modifier.testTag(testTag)
    )
}

@Composable
internal fun DinoLayerInspector(
    layers: List<PixieLayer>,
    layerStates: Map<String, Boolean>,
    layerGridStates: Map<String, Boolean>,
    onToggleLayer: (String, Boolean) -> Unit,
    onToggleGrid: (String, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "PIXIE ENGINE - LAYER STACK INSPECTOR",
            color = Color(0xFF90CAF9),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Text(text = "Toggle visibility & Grid/Solid mode per layer", color = Color(0xFF9E9E9E), fontSize = 10.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121212), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            layers.forEach { layer ->
                val isChecked = layerStates[layer.name] ?: layer.isEnabled
                val isGrid = layerGridStates[layer.name] ?: layer.isGridEnabled
                DinoLayerRow(
                    layerName = layer.name,
                    isEnabled = isChecked,
                    isGrid = isGrid,
                    onToggleEnabled = { onToggleLayer(layer.name, it) },
                    onToggleGrid = { onToggleGrid(layer.name, !isGrid) }
                )
            }
        }
    }
}

@Composable
internal fun DinoPixelGapBadge(
    isGrid: Boolean,
    onToggleGrid: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isGrid) Color(0xFF2E7D32) else Color(0xFF424242),
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onToggleGrid)
    ) {
        Text(
            text = if (isGrid) "GRID" else "SOLID",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun DinoLayerRow(
    layerName: String,
    isEnabled: Boolean,
    isGrid: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleGrid: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = layerName.replace("Layer", "L"),
            color = if (isEnabled) Color.White else Color(0xFF757575),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DinoPixelGapBadge(isGrid = isGrid, onToggleGrid = onToggleGrid)
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00E5FF),
                    checkedTrackColor = Color(0xFF004D40)
                ),
                modifier = Modifier.testTag("switch_$layerName")
            )
        }
    }
}

@Composable
internal fun DinoPauseActions(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier.weight(1f).testTag("pause_restart_button")
            ) {
                Text("Restart", color = Color.White)
            }

            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier.weight(1f).testTag("pause_resume_button")
            ) {
                Text("Resume", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onExitToMenu,
            border = BorderStroke(1.dp, Color(0xFF37474F)),
            modifier = Modifier.fillMaxWidth().testTag("pause_menu_button")
        ) {
            Text("Exit to Menu", color = Color(0xFF90A4AE))
        }
    }
}
