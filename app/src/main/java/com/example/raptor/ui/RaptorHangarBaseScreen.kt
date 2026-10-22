package com.example.raptor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.raptor.RaptorGameCoordinator
import com.example.raptor.world.RaptorLevelCatalog
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                        onExitToMenu()
                    },
                    modifier = Modifier.testTag("hangar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Exit to Menu",
                        tint = Color(0xFFCFD8DC)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MEGACORPS TACTICAL BASE",
                        color = Color(0xFFFF9100),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "INTERCEPTOR HANGAR & WEAPON DEPOT",
                        color = Color(0xFF78909C),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Pilot Credits Wallet
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF141C24),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MonetizationOn,
                            contentDescription = "Credits",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$$credits",
                            color = Color(0xFFFFD54F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Craft Status Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101722)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RAPTOR X-29 INTERCEPTOR",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1B5E20)
                        ) {
                            Text(
                                text = "STATUS: FLIGHT READY",
                                color = Color(0xFFE8F5E9),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusIndicator("HULL", "${ship.hull.toInt()}/100", Color(0xFF00E676))
                        StatusIndicator("SHIELD", "${ship.shield.toInt()}/100", Color(0xFF00E5FF))
                        StatusIndicator("CANNON", "Tier ${ship.weaponTier}", Color(0xFFFF9100))
                        StatusIndicator("MISSILE", "${ship.missileSalvoes}", Color(0xFFFF5252))
                        StatusIndicator("BOMBS", "${ship.megaBombs}", Color(0xFFE040FB))
                    }

                    // Active Perks List
                    if (context.activePerks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "INSTALLED PERKS: ",
                                color = Color(0xFF90A4AE),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            context.activePerks.forEach { perk ->
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = Color(0xFF1B5E20),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = perk,
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

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

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Micro-Missiles
                ShopItemRow(
                    title = "Micro-Missile Pod (+4 Salvoes)",
                    desc = "Smart homing ordnance lock-on package",
                    cost = 400L,
                    canAfford = credits >= 400L,
                    onBuy = {
                        if (context.spendCredits(400L)) {
                            ship.addMissiles(4)
                            audioAdapter.playSound(AudioEvent.COLLECT)
                        }
                    }
                )

                // Mega-Bomb
                ShopItemRow(
                    title = "Mega-Bomb Tactical Warhead (+1 Bomb)",
                    desc = "Screen-clearing shockwave detonator",
                    cost = 800L,
                    canAfford = credits >= 800L,
                    onBuy = {
                        if (context.spendCredits(800L)) {
                            ship.addBomb(1)
                            audioAdapter.playSound(AudioEvent.COLLECT)
                        }
                    }
                )

                // Primary Weapon Upgrade
                ShopItemRow(
                    title = if (ship.weaponTier < 4) "Autocannon Upgrade (Tier ${ship.weaponTier} -> ${ship.weaponTier + 1})" else "Autocannon MAX TIER",
                    desc = when (ship.weaponTier) {
                        1 -> "Upgrades single Vulcan to Dual Kinetic Cannons"
                        2 -> "Upgrades to Twin High-Energy Plasma Spreader"
                        3 -> "Upgrades to 4-Way Overcharged Plasma Matrix"
                        else -> "Maximum weapon yield achieved"
                    },
                    cost = 1200L,
                    canAfford = credits >= 1200L && ship.weaponTier < 4,
                    isSoldOut = ship.weaponTier >= 4,
                    onBuy = {
                        if (ship.weaponTier < 4 && context.spendCredits(1200L)) {
                            ship.upgradeWeapon()
                            audioAdapter.playSound(AudioEvent.POWERUP_PICKUP)
                        }
                    }
                )

                // Hull Field Repair
                ShopItemRow(
                    title = "Hull Structural Repair (+35 HP)",
                    desc = "Emergency nanospray patch for armor plate",
                    cost = 450L,
                    canAfford = credits >= 450L && ship.hull < 100f,
                    isSoldOut = ship.hull >= 100f,
                    onBuy = {
                        if (ship.hull < 100f && context.spendCredits(450L)) {
                            ship.repairHull(35f)
                            audioAdapter.playSound(AudioEvent.COLLECT)
                        }
                    }
                )

                // Shield Battery
                ShopItemRow(
                    title = "Shield Cell Recharge (+40 Shield)",
                    desc = "Restores plasma deflection matrix",
                    cost = 550L,
                    canAfford = credits >= 550L && ship.shield < 100f,
                    isSoldOut = ship.shield >= 100f,
                    onBuy = {
                        if (ship.shield < 100f && context.spendCredits(550L)) {
                            ship.rechargeShield(40f)
                            audioAdapter.playSound(AudioEvent.COLLECT)
                        }
                    }
                )

                // Perk: Titanium Hull Plating
                ShopItemRow(
                    title = "Perk: Titanium Armor Matrix",
                    desc = "Passive continuous shield regeneration boost",
                    cost = 1500L,
                    canAfford = credits >= 1500L && !context.hasPerk("TITANIUM_HULL"),
                    isSoldOut = context.hasPerk("TITANIUM_HULL"),
                    onBuy = {
                        if (!context.hasPerk("TITANIUM_HULL") && context.spendCredits(1500L)) {
                            context.addPerk("TITANIUM_HULL")
                            audioAdapter.playSound(AudioEvent.POWERUP_PICKUP)
                        }
                    }
                )

                // Perk: Overclock Thrusters
                ShopItemRow(
                    title = "Perk: Overclocked Afterburners",
                    desc = "+25% craft steering & lateral strafe speed",
                    cost = 1200L,
                    canAfford = credits >= 1200L && !context.hasPerk("OVERCLOCK"),
                    isSoldOut = context.hasPerk("OVERCLOCK"),
                    onBuy = {
                        if (!context.hasPerk("OVERCLOCK") && context.spendCredits(1200L)) {
                            context.addPerk("OVERCLOCK")
                            audioAdapter.playSound(AudioEvent.POWERUP_PICKUP)
                        }
                    }
                )

                // Perk: Magnet Beam
                ShopItemRow(
                    title = "Perk: Magnetic Tractor Beam",
                    desc = "Draws power-ups and score gems automatically",
                    cost = 1000L,
                    canAfford = credits >= 1000L && !context.hasPerk("MAGNET_BEAM"),
                    isSoldOut = context.hasPerk("MAGNET_BEAM"),
                    onBuy = {
                        if (!context.hasPerk("MAGNET_BEAM") && context.spendCredits(1000L)) {
                            context.addPerk("MAGNET_BEAM")
                            audioAdapter.playSound(AudioEvent.POWERUP_PICKUP)
                        }
                    }
                )
            }

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

            // Sector Select Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..3).forEach { lvl ->
                    val isSelected = lvl == currentLevelIndex
                    val levelInfo = RaptorLevelCatalog.getLevel(lvl)
                    Button(
                        onClick = {
                            audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                            onSelectLevel(lvl)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFFFF9100) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SECTOR $lvl",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White
                            )
                            Text(
                                text = levelInfo.name.take(10),
                                fontSize = 8.sp,
                                color = if (isSelected) Color(0xFF212121) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Difficulty Preset Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DifficultyPreset.DEFAULT_PRESETS.forEach { preset ->
                    val isSelected = preset == selectedDifficulty
                    Button(
                        onClick = {
                            audioAdapter.playSound(AudioEvent.BUTTON_CLICK)
                            onSelectDifficulty(preset)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Text(
                            text = preset.name.take(4),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Scramble / Launch Button
            Button(
                onClick = {
                    audioAdapter.playSound(AudioEvent.SCORE_MILESTONE)
                    onLaunchCombat()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("scramble_mission_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SCRAMBLE INTERCEPTOR (LAUNCH SORTIE)",
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StatusIndicator(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color(0xFF90A4AE), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ShopItemRow(
    title: String,
    desc: String,
    cost: Long,
    canAfford: Boolean,
    isSoldOut: Boolean = false,
    onBuy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF121924),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSoldOut) Color(0xFF78909C) else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = desc,
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onBuy,
                enabled = canAfford && !isSoldOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSoldOut) Color(0xFF263238) else Color(0xFFFF9100),
                    disabledContainerColor = Color(0xFF1E2630)
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = if (isSoldOut) "OWNED" else "$$cost",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSoldOut) Color(0xFF90A4AE) else if (canAfford) Color.Black else Color(0xFF78909C)
                )
            }
        }
    }
}
