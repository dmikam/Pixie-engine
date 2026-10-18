package com.example.dinorun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pixie.engine.core.layer.LayerStack

@Composable
fun DinoPauseDialog(
    layerStack: LayerStack,
    onResume: () -> Unit,
    onRestart: () -> Unit
) {
    val layers = layerStack.allLayers()
    val layerStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            layers.forEach { put(it.name, it.isEnabled) }
        }
    }

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
                    text = "GAME PAUSED",
                    color = Color(0xFFFFD54F),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "PIXIE ENGINE - LAYER STACK INSPECTOR",
                    color = Color(0xFF90CAF9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Toggle layers to inspect modular rendering",
                    color = Color(0xFF9E9E9E),
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Layer toggles
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF121212), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    layers.forEach { layer ->
                        val isChecked = layerStates[layer.name] ?: layer.isEnabled
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
            }
        }
    }
}
