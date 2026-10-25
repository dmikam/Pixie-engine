package com.example.raptor.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.raptor.RaptorGameCoordinator
import com.example.raptor.entities.RaptorShip
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.engine.core.context.GameContext
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
 * Orchestrates armory transactions, category tabs, CRT preview cards, and pilot status.
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
    val catalog = remember { createArmoryCatalog() }

    var selectedTab by remember { mutableStateOf(TerminalTab.ALL) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var isBuyMode by remember { mutableStateOf(true) }

    val filteredCatalog = remember(selectedTab) {
        when (selectedTab) {
            TerminalTab.ALL -> catalog
            TerminalTab.WEAPONS -> catalog.filter { it.weaponId != null }
            TerminalTab.SUPPLIES -> catalog.filter { it.isConsumable }
            TerminalTab.PERKS -> catalog.filter { it.isPerk }
        }
    }

    val safeIndex = selectedIndex.coerceIn(0, (filteredCatalog.size - 1).coerceAtLeast(0))
    val currentItem = filteredCatalog.getOrElse(safeIndex) { catalog.first() }

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

    val countOwned = getOwnershipCount(currentItem, ship, context)
    val canAfford = context.credits >= currentItem.cost

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
            TerminalTopBar(
                onBackClick = {
                    audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                    onBackToTitle()
                },
                onFlyClick = {
                    audioAdapter?.playSound(AudioEvent.POWERUP_PICKUP)
                    onLaunchCombat()
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            TerminalTabHeader(
                selectedTab = selectedTab,
                onTabSelected = { newTab ->
                    audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                    selectedTab = newTab
                    selectedIndex = 0
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2530), RoundedCornerShape(10.dp))
                    .border(2.dp, Color(0xFF37474F), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TerminalPilotStatusHeader(
                    credits = context.credits,
                    hull = ship.hull,
                    shield = ship.shield
                )

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        currentItem.isPerk -> TerminalPerkCard(
                            item = currentItem,
                            isInstalled = countOwned > 0,
                            crtPulse = crtPulse
                        )
                        currentItem.isConsumable -> TerminalSupplyCard(
                            item = currentItem,
                            stockCount = countOwned,
                            crtPulse = crtPulse
                        )
                        else -> TerminalWeaponCard(
                            item = currentItem,
                            countOwned = countOwned,
                            crtPulse = crtPulse
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TerminalControlDeck(
                isBuyMode = isBuyMode,
                onToggleBuyMode = { mode ->
                    audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                    isBuyMode = mode
                },
                canAfford = canAfford,
                currentItem = currentItem,
                onPrevItem = {
                    audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                    selectedIndex = if (safeIndex > 0) safeIndex - 1 else filteredCatalog.size - 1
                },
                onNextItem = {
                    audioAdapter?.playSound(AudioEvent.BUTTON_CLICK)
                    selectedIndex = (safeIndex + 1) % filteredCatalog.size
                },
                onActionClick = {
                    if (isBuyMode) {
                        handlePurchase(currentItem, canAfford, coordinator, audioAdapter)
                    } else {
                        handleSale(currentItem, countOwned, coordinator, audioAdapter)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            TerminalScrambleButton(
                onLaunchCombat = {
                    audioAdapter?.playSound(AudioEvent.POWERUP_PICKUP)
                    onLaunchCombat()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun handlePurchase(
    item: ArmoryItem,
    canAfford: Boolean,
    coordinator: RaptorGameCoordinator,
    audioAdapter: AndroidSoundPoolAudioAdapter?
) {
    val context = coordinator.engine.context
    val ship = coordinator.playerShip
    if (canAfford) {
        context.spendCredits(item.cost)
        audioAdapter?.playSound(AudioEvent.POWERUP_PICKUP)
        when (item.id) {
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
}

private fun handleSale(
    item: ArmoryItem,
    countOwned: Int,
    coordinator: RaptorGameCoordinator,
    audioAdapter: AndroidSoundPoolAudioAdapter?
) {
    val context = coordinator.engine.context
    val ship = coordinator.playerShip
    if (countOwned > 0) {
        val refund = item.cost / 2
        context.earnCredits(refund)
        audioAdapter?.playSound(AudioEvent.COLLECT)
        when (item.id) {
            "missiles_pack" -> if (ship.missileSalvoes >= 4) ship.missileSalvoes -= 4
            "mega_bomb" -> if (ship.megaBombs > 0) ship.megaBombs--
        }
    }
}

private fun getOwnershipCount(item: ArmoryItem, ship: RaptorShip, context: GameContext): Int = when (item.id) {
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

private fun createArmoryCatalog(): List<ArmoryItem> = listOf(
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
