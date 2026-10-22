package com.example.raptor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.raptor.world.RaptorLevelCatalog
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.engine.core.context.DifficultyPreset
import com.pixie.engine.core.ports.AudioEvent

/**
 * Military briefing & difficulty selection screen for Raptor.
 *
 * Backed by the iconic mercenary briefing desk artwork with dossier cards,
 * ID badges, and steel difficulty console.
 */
@Composable
fun RaptorBriefingScreen(
    selectedDifficulty: DifficultyPreset,
    currentLevelIndex: Int,
    pilotCredits: Long,
    audioAdapter: AndroidSoundPoolAudioAdapter?,
    onSelectDifficulty: (DifficultyPreset) -> Unit,
    onSelectLevel: (Int) -> Unit,
    onLaunchMission: () -> Unit,
    onOpenSupplyDepot: () -> Unit,
    onBackToTitle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val level = RaptorLevelCatalog.getLevel(currentLevelIndex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F))
    ) {
        // Briefing Desk Background Artwork
        Image(
            painter = painterResource(id = R.drawable.raptor_difficulty_desk),
            contentDescription = "Mercenary Briefing Desk Artwork",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Semi-transparent tactical dark scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x9905070B))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                        onBackToTitle()
                    },
                    modifier = Modifier.testTag("briefing_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFCFD8DC)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PILOT DOSSIER & BRIEFING",
                        color = Color(0xFFFF9100),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "CALLSIGN: FALCON-1 • MERCENARY SQUADRON",
                        color = Color(0xFF90A4AE),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Pilot Bounty Credits
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF101722),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MonetizationOn,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$$pilotCredits",
                            color = Color(0xFFFFD54F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. "CHOOSE DIFFICULTY" Steel Armored Terminal (Recreating image 2)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF0181F2B)),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE65100)),
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CHOOSE DIFFICULTY",
                        color = Color(0xFFFF6D00),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4 Difficulty Ranks matching original Raptor
                    DifficultyOptionRow(
                        rankTitle = "TRAINING MODE",
                        rankSubtitle = "Gentle combat, reduced enemy speeds, generous shields",
                        rankValue = 25,
                        preset = DifficultyPreset.EASY,
                        isSelected = selectedDifficulty == DifficultyPreset.EASY,
                        onClick = {
                            audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                            onSelectDifficulty(DifficultyPreset.EASY)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DifficultyOptionRow(
                        rankTitle = "ROOKIE",
                        rankSubtitle = "Standard mercenary combat & tactical flight formations",
                        rankValue = 50,
                        preset = DifficultyPreset.MEDIUM,
                        isSelected = selectedDifficulty == DifficultyPreset.MEDIUM,
                        onClick = {
                            audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                            onSelectDifficulty(DifficultyPreset.MEDIUM)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DifficultyOptionRow(
                        rankTitle = "VETERAN",
                        rankSubtitle = "Aggressive squadrons, rapid crossfire & high bounty",
                        rankValue = 75,
                        preset = DifficultyPreset.DIFFICULT,
                        isSelected = selectedDifficulty == DifficultyPreset.DIFFICULT,
                        onClick = {
                            audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                            onSelectDifficulty(DifficultyPreset.DIFFICULT)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DifficultyOptionRow(
                        rankTitle = "ELITE (HEAVY METAL)",
                        rankSubtitle = "Relentless hostility, ruthless dreadnoughts, maximum hazard",
                        rankValue = 95,
                        preset = DifficultyPreset.HEAVY_METAL,
                        isSelected = selectedDifficulty == DifficultyPreset.HEAVY_METAL,
                        onClick = {
                            audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                            onSelectDifficulty(DifficultyPreset.HEAVY_METAL)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Sector Mission Target
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF0101724)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ASSIGNED CAMPAIGN SECTOR",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Sector $currentLevelIndex: ${level.name}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = level.subtitle,
                        color = Color(0xFFB0BEC5),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..3).forEach { lvlIdx ->
                            val isCurrent = lvlIdx == currentLevelIndex
                            Button(
                                onClick = {
                                    audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                                    onSelectLevel(lvlIdx)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrent) Color(0xFF00E5FF) else Color(0xFF1E2838)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Text(
                                    text = "SECTOR $lvlIdx",
                                    color = if (isCurrent) Color.Black else Color(0xFFCFD8DC),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Launch & Shop Actions
            Row(
                modifier = Modifier.fillMaxWidth(0.95f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Open Hangar Shop Terminal
                OutlinedButton(
                    onClick = {
                        audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                        onOpenSupplyDepot()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9100)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("briefing_open_shop")
                ) {
                    Icon(Icons.Filled.Build, contentDescription = null, tint = Color(0xFFFF9100))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SUPPLY DEPOT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Scramble / Launch Mission
                Button(
                    onClick = {
                        audioAdapter?.playSound(AudioEvent.POWERUP_PICKUP)
                        onLaunchMission()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("briefing_launch_button")
                ) {
                    Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SCRAMBLE (FLY)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun DifficultyOptionRow(
    rankTitle: String,
    rankSubtitle: String,
    rankValue: Int,
    preset: DifficultyPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) Color(0xFF2C394E) else Color(0xFF141922),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) Color(0xFFFF9100) else Color(0xFF37474F)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rankTitle,
                    color = if (isSelected) Color(0xFFFF9100) else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = rankSubtitle,
                    color = Color(0xFF90A4AE),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isSelected) Color(0xFFFF9100) else Color(0xFF263238)
            ) {
                Text(
                    text = "$rankValue",
                    color = if (isSelected) Color.Black else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
