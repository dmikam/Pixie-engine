package com.example.raptor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.raptor.RaptorGameCoordinator
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.engine.core.context.DifficultyPreset
import com.pixie.engine.core.ports.AudioEvent

/**
 * Tactical Hangar & Supply Depot screen inspired by the classic Raptor: Call of the Shadows.
 *
 * Allows pilots to:
 * - View earned bounty credits from downed combat craft and ground installations.
 * - Inspect craft status (Hull, Shield, Weapon Tier, Missiles, Mega-Bombs, Active Perks).
 * - Purchase weapon upgrades, missile pods, hull repairs, shield cells, and cyber perks.
 * - Select sector mission targets and scramble fighter craft into combat.
 */
@Composable
fun RaptorHangarBaseScreen(
    coordinator: RaptorGameCoordinator,
    audioAdapter: AndroidSoundPoolAudioAdapter,
    selectedDifficulty: DifficultyPreset,
    currentLevelIndex: Int,
    onSelectDifficulty: (DifficultyPreset) -> Unit,
    onSelectLevel: (Int) -> Unit,
    onLaunchCombat: () -> Unit,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ship = coordinator.playerShip
    val context = coordinator.engine.context
    val credits = context.credits

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D14))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Top Header: Navigation & Credits Bank
            HangarTopBar(
                credits = credits,
                onBackClick = {
                    audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                    onExitToMenu()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Craft Status Card
            HangarCraftStatusCard(
                ship = ship,
                activePerks = context.activePerks
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Armory & Supply Weapon Shop
            Text(
                text = "ARMORY & MUNITIONS DEPOT",
                color = Color(0xFFFFD54F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))

            HangarArmoryDepot(
                ship = ship,
                context = context,
                audioAdapter = audioAdapter
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Mission Dispatch Computer
            Text(
                text = "MISSION DISPATCH COMPUTER",
                color = Color(0xFFFF9100),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))

            HangarSectorSelector(
                currentLevelIndex = currentLevelIndex,
                onSelectLevel = onSelectLevel,
                audioAdapter = audioAdapter
            )

            Spacer(modifier = Modifier.height(8.dp))

            HangarDifficultySelector(
                selectedDifficulty = selectedDifficulty,
                onSelectDifficulty = onSelectDifficulty,
                audioAdapter = audioAdapter
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Scramble / Launch Button
            HangarScrambleButton(
                onLaunchCombat = onLaunchCombat,
                audioAdapter = audioAdapter
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
