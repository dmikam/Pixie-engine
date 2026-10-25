package com.pixie.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
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
import com.pixie.android.input.ArcadeDeckPlacement
import com.pixie.android.input.InputPreference
import com.pixie.android.input.ViewportMode

/**
 * Modal dialog for inspecting and live-tuning arcade controls, viewport presentation,
 * and physical/touch input modes in [PixieGameScaffold].
 */
@Composable
fun PixieScaffoldShowcaseDialog(
    isLandscape: Boolean,
    viewportMode: ViewportMode,
    onViewportModeChange: (ViewportMode) -> Unit,
    effectivePlacement: ArcadeDeckPlacement,
    onSelectPlacement: (ArcadeDeckPlacement) -> Unit,
    inputPreference: InputPreference,
    onInputPreferenceChange: (InputPreference) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xF010141E),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
            modifier = modifier
                .fillMaxWidth(0.95f)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ShowcaseHeader(onDismiss = onDismiss)
                HorizontalDivider(color = Color(0x3300E5FF))
                ShowcaseViewportModeSection(
                    viewportMode = viewportMode,
                    onViewportModeChange = onViewportModeChange
                )
                ShowcasePlacementSection(
                    isLandscape = isLandscape,
                    effectivePlacement = effectivePlacement,
                    onSelectPlacement = onSelectPlacement
                )
                ShowcaseInputPreferenceSection(
                    inputPreference = inputPreference,
                    onInputPreferenceChange = onInputPreferenceChange
                )
                ShowcasePhysicalControlsTip()
            }
        }
    }
}

@Composable
private fun ShowcaseHeader(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🎮 ARCADE & VIEWPORT SETUP",
            color = Color(0xFF00E5FF),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "TAP TO CLOSE",
            color = Color.Gray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.clickable { onDismiss() }
        )
    }
}

@Composable
private fun ShowcaseViewportModeSection(
    viewportMode: ViewportMode,
    onViewportModeChange: (ViewportMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "1. LANDSCAPE VIEWPORT PRESENTATION:",
            color = Color(0xFFB0BEC5),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = viewportMode == ViewportMode.CENTERED_WINGS,
                onClick = { onViewportModeChange(ViewportMode.CENTERED_WINGS) },
                label = { Text("Side Wings", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00B0FF),
                    selectedLabelColor = Color.Black
                )
            )
            FilterChip(
                selected = viewportMode == ViewportMode.ADAPTIVE_FIT,
                onClick = { onViewportModeChange(ViewportMode.ADAPTIVE_FIT) },
                label = { Text("Adaptive Fit", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00B0FF),
                    selectedLabelColor = Color.Black
                )
            )
        }
    }
}

@Composable
private fun ShowcasePlacementSection(
    isLandscape: Boolean,
    effectivePlacement: ArcadeDeckPlacement,
    onSelectPlacement: (ArcadeDeckPlacement) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "2. ARCADE CONTROLS PLACEMENT (${if (isLandscape) "LANDSCAPE" else "PORTRAIT"}):",
            color = Color(0xFFB0BEC5),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isLandscape) {
                FilterChip(
                    selected = effectivePlacement == ArcadeDeckPlacement.SPLIT_WINGS,
                    onClick = { onSelectPlacement(ArcadeDeckPlacement.SPLIT_WINGS) },
                    label = { Text("Split Wings", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF4081),
                        selectedLabelColor = Color.White
                    )
                )
            }
            FilterChip(
                selected = effectivePlacement == ArcadeDeckPlacement.BOTTOM_STRIP,
                onClick = { onSelectPlacement(ArcadeDeckPlacement.BOTTOM_STRIP) },
                label = { Text("Bottom Strip", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFF4081),
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = effectivePlacement == ArcadeDeckPlacement.FLOATING,
                onClick = { onSelectPlacement(ArcadeDeckPlacement.FLOATING) },
                label = { Text("Floating Draggable", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFF4081),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun ShowcaseInputPreferenceSection(
    inputPreference: InputPreference,
    onInputPreferenceChange: (InputPreference) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "3. INPUT CONTROLLER METHOD:",
            color = Color(0xFFB0BEC5),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = inputPreference == InputPreference.HYBRID_AUTO,
                onClick = { onInputPreferenceChange(InputPreference.HYBRID_AUTO) },
                label = { Text("Hybrid Auto", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00E676),
                    selectedLabelColor = Color.Black
                )
            )
            FilterChip(
                selected = inputPreference == InputPreference.TOUCH_ONLY,
                onClick = { onInputPreferenceChange(InputPreference.TOUCH_ONLY) },
                label = { Text("Touch Only", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00E676),
                    selectedLabelColor = Color.Black
                )
            )
            FilterChip(
                selected = inputPreference == InputPreference.PHYSICAL_ONLY,
                onClick = { onInputPreferenceChange(InputPreference.PHYSICAL_ONLY) },
                label = { Text("Gamepad / Keys", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00E676),
                    selectedLabelColor = Color.Black
                )
            )
        }
    }
}

@Composable
private fun ShowcasePhysicalControlsTip(
    modifier: Modifier = Modifier
) {
    Text(
        text = "💡 Physical Controls: Arrow Keys / A-D to steer, Space / Enter / Button A to serve & fire, Shift / Button B for secondary.",
        color = Color(0xFF80CBC4),
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = modifier
    )
}
