package com.example.raptor.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.raptor.RaptorGameCoordinator
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.engine.core.ports.AudioEvent

data class ArmoryItem(
    val id: String,
    val name: String,
    val category: String, // "SELECTABLE", "PRIMARY", "AIR-TO-AIR", "DEFENSIVE", "CYBER PERK"
    val description: String,
    val cost: Long,
    val iconRes: Int,
    val weaponId: String? = null,
    val isPerk: Boolean = false,
    val isConsumable: Boolean = false
)

/**
 * Authentic graphical Hangar & Supply Depot Computer Terminal ("HDF MAIN DISPLAY").
 *
 * Faithfully reproduces the retro CRT monitor console, pilot module with hazard stripes,
 * digital odometer credits display, and tactile mechanical controls from Raptor: Call of the Shadows.
 */
@Composable
fun RaptorTerminalShopScreen(
    coordinator: RaptorGameCoordinator,
    audioAdapter: AndroidSoundPoolAudioAdapter?,
    onLaunchCombat: () -> Unit,
    onBackToTitle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ship = coordinator.playerShip
    val context = coordinator.engine.context
    val credits = context.credits

    // Armory catalog containing all classic Raptor weapons & supplies
    val catalog = remember {
        listOf(
            ArmoryItem(
                id = "odin_laser",
                name = "LASER TURRET",
                category = "SELECTABLE",
                description = "THE OD55 ODIN LASER TURRET IS A PILOT'S BEST CHOICE FOR AIR COMBAT. SIMILAR TO THE TH19 THOR, THE ODIN HAS A FASTER RESPONSE TIME WITH THE LASER'S INCREASED FIREPOWER.",
                cost = 1200L,
                iconRes = R.drawable.raptor_terminal_laser,
                weaponId = "odin_laser"
            ),
            ArmoryItem(
                id = "deathray",
                name = "DEATHRAY",
                category = "SELECTABLE",
                description = "THE MSIL-ATLAS IS ONE OF THE MOST POWERFUL WEAPONS AVAILABLE. EMITTING A PARTICLE STREAM AT INCREDIBLE SPEEDS, ITS AWESOME FIREPOWER IS USUALLY ENOUGH TO DESTROY MOST TARGETS IN ONE SHOT.",
                cost = 2500L,
                iconRes = R.drawable.raptor_terminal_deathray,
                weaponId = "deathray"
            ),
            ArmoryItem(
                id = "vulcan_upgrade",
                name = "VULCAN AUTOCANNON",
                category = "PRIMARY",
                description = "HEAVY HIGH-VELOCITY ARMOR-PIERCING KINETIC ROUNDS. EXCELLENT FOR CONTINUOUS SUSTAINED SUPPRESSION FIRE AGAINST AIR SQUADRONS AND GROUND INSTALLATIONS.",
                cost = 500L,
                iconRes = R.drawable.raptor_terminal_laser,
                weaponId = "vulcan"
            ),
            ArmoryItem(
                id = "twin_plasma",
                name = "TWIN PLASMA SPREADER",
                category = "SELECTABLE",
                description = "DUAL HIGH-ENERGY ION PLASMA BOLTS FIRED IN SPREAD TRAJECTORY. DEVASTATES ADVANCING ENEMY FORMATIONS WITH OVERLAPPING CONES OF DESTRUCTIVE FIRE.",
                cost = 900L,
                iconRes = R.drawable.raptor_terminal_laser,
                weaponId = "twin_plasma"
            ),
            ArmoryItem(
                id = "missiles_pack",
                name = "MICRO-HOMING MISSILES",
                category = "AIR-TO-AIR",
                description = "SMART LOCK-ON TARGETING ORDNANCE. AUTOMATICALLY TRACKS AND PURSUES ENEMY FIGHTERS AND HEAVY DREADNOUGHTS ACROSS THE ENTIRE COMBAT AIRSPACE.",
                cost = 400L,
                iconRes = R.drawable.raptor_terminal_laser,
                isConsumable = true
            ),
            ArmoryItem(
                id = "mega_bomb",
                name = "MEGA-BOMB DISRUPTOR",
                category = "AIR-TO-GROUND",
                description = "TACTICAL EMP ATOMIC RESONANCE SHOCKWAVE. CLEARS INCOMING HOSTILE BULLETS AND DISINTEGRATES ALL VISIBLE THREATS ON SCREEN.",
                cost = 800L,
                iconRes = R.drawable.raptor_terminal_deathray,
                isConsumable = true
            ),
            ArmoryItem(
                id = "ion_shield",
                name = "ION SHIELD CELL",
                category = "DEFENSIVE",
                description = "HIGH-CAPACITY ELECTROMAGNETIC FORCEFIELD CELLS. RECHARGES SHIELD EMITTERS TO ABSORB HOSTILE LASER AND PROJECTILE DAMAGE.",
                cost = 350L,
                iconRes = R.drawable.raptor_terminal_laser,
                isConsumable = true
            ),
            ArmoryItem(
                id = "hull_repair",
                name = "TITANIUM HULL REPAIR",
                category = "DEFENSIVE",
                description = "REINFORCED TITANIUM COMPOSITE ARMOR PLATING. REPAIRS STRUCTURAL INTEGRITY TO ENSURE SURVIVAL AGAINST PROLONGED COMBAT STRESS.",
                cost = 300L,
                iconRes = R.drawable.raptor_terminal_deathray,
                isConsumable = true
            ),
            ArmoryItem(
                id = "magnet_beam",
                name = "MAGNETIC BEAM TRACTOR",
                category = "CYBER PERK",
                description = "GRAVITATIONAL TRACTOR FIELD EMITTER. AUTOMATICALLY PULLS FLOATING CREDIT CONTAINERS, REPAIR PODS, AND WEAPON UPGRADES DIRECTLY TO THE FIGHTER.",
                cost = 600L,
                iconRes = R.drawable.raptor_terminal_laser,
                isPerk = true
            )
        )
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    var isBuyMode by remember { mutableStateOf(true) }
    val currentItem = catalog[selectedIndex]

    val infiniteTransition = rememberInfiniteTransition(label = "crt_flicker")
    val crtPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crt_alpha"
    )

    // Calculate current ownership quantity for display
    val countOwned = when (currentItem.id) {
        "odin_laser" -> if (ship.activeWeaponId == "odin_laser") 1 else 0
        "deathray" -> if (ship.activeWeaponId == "deathray") 1 else 0
        "twin_plasma" -> if (ship.activeWeaponId == "twin_plasma") 1 else 0
        "vulcan_upgrade" -> ship.weaponTier
        "missiles_pack" -> ship.missileSalvoes
        "mega_bomb" -> ship.megaBombs
        "ion_shield" -> (ship.shield / 25f).toInt()
        "hull_repair" -> (ship.hull / 25f).toInt()
        "magnet_beam" -> if (context.hasPerk("MAGNET_BEAM")) 1 else 0
        else -> 0
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C1017))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation & Title Bar
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
                    modifier = Modifier.testTag("terminal_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFCFD8DC)
                    )
                }

                Text(
                    text = "HDF MAIN DISPLAY • SUPPLY DEPOT",
                    color = Color(0xFF00E5FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                // Scramble / Launch Mission Quick Button
                Button(
                    onClick = {
                        audioAdapter?.playSound(AudioEvent.POWERUP_PICKUP)
                        onLaunchCombat()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("terminal_quick_launch")
                ) {
                    Icon(
                        imageVector = Icons.Filled.FlightTakeoff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "FLY",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Terminal Console Body (Pilot module on left + CRT display on right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2530), RoundedCornerShape(10.dp))
                    .border(2.dp, Color(0xFF37474F), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left Column: Pilot ID Unit with hazard stripes & Credits LED
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .background(Color(0xFF141923), RoundedCornerShape(8.dp))
                        .border(1.5.dp, Color(0xFF263238), RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Hazard stripe header
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = Color(0xFFFFB300),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    ) {}

                    Spacer(modifier = Modifier.height(4.dp))

                    // Pilot Portrait Box
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF0A0E17), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF455A64), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.raptor_terminal_laser),
                            contentDescription = "Pilot Portrait",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Callsign badge
                    Text(
                        text = "PRV. 6:15",
                        color = Color(0xFFFF5252),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "REG",
                        color = Color(0xFFCFD8DC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 7-segment / Digital Odometer Credits Display
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF000000),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CREDITS",
                                color = Color(0xFF90A4AE),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "$credits",
                                color = Color(0xFFFFD54F),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Ship Hull & Shield mini-meters
                    Text(
                        text = "HULL ${ship.hull.toInt()}%",
                        color = if (ship.hull > 40f) Color(0xFF00E676) else Color(0xFFFF5252),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "SHD ${ship.shield.toInt()}%",
                        color = Color(0xFF00E5FF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Right Column: CRT Monitor Screen (Item Preview, Lore & Status)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF05070B), RoundedCornerShape(8.dp))
                        .border(2.dp, Color(0xFF00E5FF).copy(alpha = crtPulse), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    // Header of CRT: Item Title & Category
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentItem.name,
                            color = Color(0xFFFFEB3B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = currentItem.category,
                            color = Color(0xFFFF1744),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Item Graphical Preview Image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(75.dp)
                            .background(Color(0xFF0A0F1A), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = currentItem.iconRes),
                            contentDescription = currentItem.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Authentic Lore / Technical Description (Amber CRT Font)
                    Text(
                        text = currentItem.description,
                        color = Color(0xFFFFB74D),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ownership & Cost Readouts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "YOU HAVE",
                                color = Color(0xFF76FF03),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "%02d".format(countOwned),
                                color = Color(0xFF76FF03),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "COST",
                                color = Color(0xFFFFD54F),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${currentItem.cost}",
                                color = Color(0xFFFFD54F),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Console Control Deck: Mode selector, Arrow controls & Buy Button
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF263238)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Row 1: BUY / SELL mode toggle and item selector arrows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // BUY / SELL switch
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF0F141D), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF37474F), RoundedCornerShape(6.dp))
                                .padding(2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isBuyMode) Color(0xFFD32F2F) else Color.Transparent,
                                modifier = Modifier.clickable {
                                    audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                                    isBuyMode = true
                                }
                            ) {
                                Text(
                                    text = "BUY",
                                    color = if (isBuyMode) Color.White else Color(0xFF78909C),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (!isBuyMode) Color(0xFFD32F2F) else Color.Transparent,
                                modifier = Modifier.clickable {
                                    audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                                    isBuyMode = false
                                }
                            ) {
                                Text(
                                    text = "SELL",
                                    color = if (!isBuyMode) Color.White else Color(0xFF78909C),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Arrow buttons << and >> to cycle catalog
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                                    selectedIndex = if (selectedIndex > 0) selectedIndex - 1 else catalog.size - 1
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .size(width = 44.dp, height = 36.dp)
                                    .testTag("terminal_prev_item")
                            ) {
                                Text(
                                    text = "<<",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                            }

                            Button(
                                onClick = {
                                    audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                                    selectedIndex = (selectedIndex + 1) % catalog.size
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .size(width = 44.dp, height = 36.dp)
                                    .testTag("terminal_next_item")
                            ) {
                                Text(
                                    text = ">>",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2: Large tactile green [BUY ITEM] mechanical push-button
                    val canAfford = credits >= currentItem.cost
                    Button(
                        onClick = {
                            if (isBuyMode) {
                                if (canAfford) {
                                    context.spendCredits(currentItem.cost)
                                    audioAdapter?.playSound(AudioEvent.POWERUP_PICKUP)

                                    // Apply purchase to ship loadout
                                    when (currentItem.id) {
                                        "odin_laser" -> ship.equipWeapon("odin_laser")
                                        "deathray" -> ship.equipWeapon("deathray")
                                        "twin_plasma" -> ship.equipWeapon("twin_plasma")
                                        "vulcan_upgrade" -> ship.upgradeWeapon()
                                        "missiles_pack" -> ship.addMissiles(4)
                                        "mega_bomb" -> ship.addBomb(1)
                                        "ion_shield" -> ship.rechargeShield(40f)
                                        "hull_repair" -> ship.repairHull(35f)
                                        "magnet_beam" -> context.addPerk("MAGNET_BEAM")
                                    }
                                } else {
                                    audioAdapter?.playSound(AudioEvent.HIT)
                                }
                            } else {
                                // Sell consumable/refund
                                if (countOwned > 0) {
                                    val refund = currentItem.cost / 2
                                    context.earnCredits(refund)
                                    audioAdapter?.playSound(AudioEvent.COLLECT)
                                    when (currentItem.id) {
                                        "missiles_pack" -> if (ship.missileSalvoes >= 4) ship.missileSalvoes -= 4
                                        "mega_bomb" -> if (ship.megaBombs > 0) ship.megaBombs--
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBuyMode) {
                                if (canAfford) Color(0xFF2E7D32) else Color(0xFF37474F)
                            } else Color(0xFFC62828)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            if (isBuyMode && canAfford) Color(0xFF00E676) else Color(0xFF78909C)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("terminal_action_button")
                    ) {
                        Text(
                            text = if (isBuyMode) {
                                if (canAfford) "BUY ITEM ($${currentItem.cost})" else "INSUFFICIENT FUNDS ($${currentItem.cost})"
                            } else "SELL ITEM (REFUND)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Big red illuminated [FLY MISSION / SCRAMBLE] launch button
            Button(
                onClick = {
                    audioAdapter?.playSound(AudioEvent.POWERUP_PICKUP)
                    onLaunchCombat()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF5722)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("terminal_scramble_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.FlightTakeoff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SCRAMBLE (FLY MISSION)",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
