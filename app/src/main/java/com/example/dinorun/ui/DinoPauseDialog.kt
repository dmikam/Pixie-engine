package com.example.dinorun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.dinorun.world.PrehistoricWorldDefinition
import com.example.dinorun.world.VolcanicCraterWorldDefinition
import com.pixie.engine.core.layer.LayerStack
import com.pixie.engine.core.layer.PixieLayer
import com.pixie.engine.core.world.WorldDefinition

/**
 * Pause dialog coordinator for DinoRun, displaying world selection,
 * sound/haptic controls, layer visibility toggles, and pause action buttons.
 */
@Composable
fun DinoPauseDialog(
    layerStack: LayerStack,
    currentWorld: WorldDefinition,
    isSoundEnabled: Boolean,
    isHapticEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onSelectWorld: (WorldDefinition) -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit
) {
    val layers = layerStack.allLayers()
    val layerStates = remember {
        mutableStateMapOf<String, Boolean>().apply { layers.forEach { put(it.name, it.isEnabled) } }
    }
    val layerGridStates = remember {
        mutableStateMapOf<String, Boolean>().apply { layers.forEach { put(it.name, it.isGridEnabled) } }
    }
    val availableWorlds = remember {
        listOf(PrehistoricWorldDefinition(), VolcanicCraterWorldDefinition())
    }

    Dialog(onDismissRequest = onResume) {
        DinoPauseDialogContent(
            currentWorld = currentWorld,
            availableWorlds = availableWorlds,
            layers = layers,
            layerStates = layerStates,
            layerGridStates = layerGridStates,
            isSoundEnabled = isSoundEnabled,
            isHapticEnabled = isHapticEnabled,
            onToggleSound = onToggleSound,
            onToggleHaptic = onToggleHaptic,
            onSelectWorld = onSelectWorld,
            onToggleLayer = { name, enabled ->
                layerStates[name] = enabled
                layerStack.setLayerEnabled(name, enabled)
            },
            onToggleGrid = { name, isGrid ->
                layerGridStates[name] = isGrid
                layerStack.setLayerGridEnabled(name, isGrid)
            },
            onResume = onResume,
            onRestart = onRestart,
            onExitToMenu = onExitToMenu
        )
    }
}

@Composable
private fun DinoPauseDialogContent(
    currentWorld: WorldDefinition,
    availableWorlds: List<WorldDefinition>,
    layers: List<PixieLayer>,
    layerStates: Map<String, Boolean>,
    layerGridStates: Map<String, Boolean>,
    isSoundEnabled: Boolean,
    isHapticEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onSelectWorld: (WorldDefinition) -> Unit,
    onToggleLayer: (String, Boolean) -> Unit,
    onToggleGrid: (String, Boolean) -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DinoPauseHeader()

            Spacer(modifier = Modifier.height(14.dp))

            DinoWorldSelector(
                currentWorld = currentWorld,
                availableWorlds = availableWorlds,
                onSelectWorld = onSelectWorld
            )

            Spacer(modifier = Modifier.height(16.dp))

            DinoAudioHapticSettings(
                isSoundEnabled = isSoundEnabled,
                isHapticEnabled = isHapticEnabled,
                onToggleSound = onToggleSound,
                onToggleHaptic = onToggleHaptic
            )

            Spacer(modifier = Modifier.height(16.dp))

            DinoLayerInspector(
                layers = layers,
                layerStates = layerStates,
                layerGridStates = layerGridStates,
                onToggleLayer = onToggleLayer,
                onToggleGrid = onToggleGrid
            )

            Spacer(modifier = Modifier.height(20.dp))

            DinoPauseActions(
                onResume = onResume,
                onRestart = onRestart,
                onExitToMenu = onExitToMenu
            )
        }
    }
}
