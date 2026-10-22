package com.example.raptor.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.engine.core.ports.AudioEvent

/**
 * Authentic graphical intro / title screen for Raptor: Call of the Shadows.
 *
 * Displays the iconic planetary and mercenary pilot artwork with retro amber/bronze
 * menu selections and atmospheric CRT scanlines.
 */
@Composable
fun RaptorIntroTitleScreen(
    audioAdapter: AndroidSoundPoolAudioAdapter?,
    onNewMission: () -> Unit,
    onOpenSupplyDepot: () -> Unit,
    onOpenBriefing: () -> Unit,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "title_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030509))
    ) {
        // Hero Background Artwork (Raptor Title Graphic)
        Image(
            painter = painterResource(id = R.drawable.raptor_intro_title),
            contentDescription = "Raptor Call of the Shadows Title Artwork",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark gradient overlay for menu readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x66000000),
                            Color(0xCC050811),
                            Color(0xF0030509)
                        ),
                        startY = 200f
                    )
                )
        )

        // Title Header & Interactive Retro Menu
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top branding badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xAA0A0E17),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = glowAlpha)),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "MERCENARY TACTICAL FLIGHT INTERCEPTOR",
                    color = Color(0xFFFF9800),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Main Menu Options in retro metallic orange/bronze styling
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. NEW MISSION (Takes pilot to Difficulty Selection & Briefing desk)
                RetroMenuButton(
                    label = "NEW MISSION",
                    subtitle = "Select Pilot Rank & Deploy to Combat",
                    icon = Icons.Filled.FlightTakeoff,
                    primaryColor = Color(0xFFFF6D00),
                    onClick = {
                        audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                        onNewMission()
                    },
                    modifier = Modifier.testTag("raptor_menu_new_mission")
                )

                // 2. SUPPLY DEPOT & WEAPON SHOP (The iconic CRT Computer Terminal)
                RetroMenuButton(
                    label = "SUPPLY DEPOT (HANGAR)",
                    subtitle = "ODIN Laser, Deathray, Missiles & Armor",
                    icon = Icons.Filled.Build,
                    primaryColor = Color(0xFF00E5FF),
                    onClick = {
                        audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                        onOpenSupplyDepot()
                    },
                    modifier = Modifier.testTag("raptor_menu_supply_depot")
                )

                // 3. SECTOR BRIEFING & DIFFICULTY OPTIONS
                RetroMenuButton(
                    label = "TACTICAL BRIEFING",
                    subtitle = "Pilot Callsign, Sector Maps & Difficulty",
                    icon = Icons.Filled.MilitaryTech,
                    primaryColor = Color(0xFFFFD54F),
                    onClick = {
                        audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                        onOpenBriefing()
                    },
                    modifier = Modifier.testTag("raptor_menu_briefing")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 4. Return to Arcade Hub
                OutlinedButton(
                    onClick = {
                        audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                        onExitToMenu()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF90A4AE)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(42.dp)
                        .testTag("raptor_menu_exit")
                ) {
                    Text(
                        text = "EXIT TO ARCADE HUB",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFCFD8DC)
                    )
                }
            }

            // Bottom copyright banner
            Text(
                text = "RAPTOR: CALL OF THE SHADOWS • ARCADE EDITION",
                color = Color(0xFF546E7A),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}

@Composable
private fun RetroMenuButton(
    label: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primaryColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xEE121824),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.8f)),
        modifier = modifier
            .fillMaxWidth(0.92f)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = primaryColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF90A4AE),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
