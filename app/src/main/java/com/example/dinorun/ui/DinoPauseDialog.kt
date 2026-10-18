package com.example.dinorun.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
import com.example.dinorun.world.PrehistoricWorldDefinition
import com.example.dinorun.world.VolcanicCraterWorldDefinition
import com.pixie.engine.core.layer.LayerStack
import com.pixie.engine.core.world.WorldDefinition

@Composable
fun DinoPauseDialog(
    layerStack: LayerStack,
    currentWorld: WorldDefinition,
    onSelectWorld: (WorldDefinition) -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit
) {
    val layers = layerStack.allLayers()
    val layerStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            layers.forEach { put(it.name, it.isEnabled) }
        }
    }
    val layerGridStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            layers.forEach { put(it.name, it.isGridEnabled) }
        }
    }

    val availableWorlds = listOf(
        PrehistoricWorldDefinition(),
        VolcanicCraterWorldDefinition()
    )

    Dialog(onDismissRequest = onResume) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GAME PAUSED (v0.3)",
                    color = Color(0xFFFFD54F),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(14.dp))

                // World / Level Selector
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
                        val isSelected = world.id == currentWorld.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF263238) else Color(0xFF141414))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF00E5FF) else Color(0xFF333333),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectWorld(world) }
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Layer toggles
                Text(
                    text = "PIXIE ENGINE - LAYER STACK INSPECTOR",
                    color = Color(0xFF90CAF9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Toggle visibility & Grid/Solid mode per layer",
                    color = Color(0xFF9E9E9E),
                    fontSize = 10.sp
                )

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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = layer.name.replace("Layer", "L"),
                                color = if (isChecked) Color.White else Color(0xFF757575),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Tap to toggle layer grid/solid mode
                                androidx.compose.material3.Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isGrid) Color(0xFF2E7D32) else Color(0xFF424242),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            val newGrid = !isGrid
                                            layerGridStates[layer.name] = newGrid
                                            layerStack.setLayerGridEnabled(layer.name, newGrid)
                                        }
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

                                Switch(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        layerStates[layer.name] = checked
                                        layerStack.setLayerEnabled(layer.name, checked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00E5FF),
                                        checkedTrackColor = Color(0xFF004D40)
                                    ),
                                    modifier = Modifier.testTag("switch_${layer.name}")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRestart,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pause_restart_button")
                    ) {
                        Text("Restart", color = Color.White)
                    }

                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pause_resume_button")
                    ) {
                        Text("Resume", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onExitToMenu,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF37474F)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pause_menu_button")
                ) {
                    Text("Exit to Menu", color = Color(0xFF90A4AE))
                }
            }
        }
    }
}
