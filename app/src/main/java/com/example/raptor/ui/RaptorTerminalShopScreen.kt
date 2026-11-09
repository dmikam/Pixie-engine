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
    val category: String,
    val description: String,
    val cost: Long,
    val iconRes: Int? = null,
    val weaponId: String? = null,
    val isPerk: Boolean = false,
    val isConsumable: Boolean = false
)

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

    val filtered = remember(selectedTab) {
        when (selectedTab) {
            TerminalTab.ALL -> catalog
            TerminalTab.WEAPONS -> catalog.filter { it.weaponId != null }
            TerminalTab.SUPPLIES -> catalog.filter { it.isConsumable }
            TerminalTab.PERKS -> catalog.filter { it.isPerk }
        }
    }
    val safeIndex = selectedIndex.coerceIn(0, (filtered.size - 1).coerceAtLeast(0))
    val currentItem = filtered.getOrElse(safeIndex) { catalog.first() }
    val crtPulse by rememberCrtPulse()
    val countOwned = getOwnershipCount(currentItem, coordinator, ship, context)
    val isAlreadyOwned = currentItem.weaponId != null && coordinator.unlockedWeapons.contains(currentItem.weaponId)
    val isEquipped = currentItem.weaponId != null && coordinator.activeWeaponId == currentItem.weaponId
    val canAfford = context.credits >= currentItem.cost || isAlreadyOwned

    TerminalShopContent(
        coordinator, audioAdapter, onLaunchCombat, onBackToTitle, selectedTab, isBuyMode,
        canAfford, currentItem, countOwned, isAlreadyOwned, isEquipped, crtPulse,
        onSelectTab = { selectedTab = it; selectedIndex = 0 },
        onToggleBuy = { isBuyMode = it },
        onPrev = { selectedIndex = if (safeIndex > 0) safeIndex - 1 else filtered.size - 1 },
        onNext = { selectedIndex = (safeIndex + 1) % filtered.size },
        modifier = modifier
    )
}

@Composable
private fun rememberCrtPulse(): androidx.compose.runtime.State<Float> {
    val transition = rememberInfiniteTransition(label = "crt_flicker")
    return transition.animateFloat(
        initialValue = 0.94f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "crt_alpha"
    )
}

@Composable
private fun TerminalShopContent(
    coordinator: RaptorGameCoordinator,
    audio: AndroidSoundPoolAudioAdapter?,
    onLaunch: () -> Unit,
    onBack: () -> Unit,
    selectedTab: TerminalTab,
    isBuyMode: Boolean,
    canAfford: Boolean,
    item: ArmoryItem,
    countOwned: Int,
    isAlreadyOwned: Boolean,
    isEquipped: Boolean,
    crtPulse: Float,
    onSelectTab: (TerminalTab) -> Unit,
    onToggleBuy: (Boolean) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0C1017)).statusBarsPadding()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TerminalTopBar(
                onBackClick = { audio?.playSound(AudioEvent.BUTTON_CLICK); onBack() },
                onFlyClick = { audio?.playSound(AudioEvent.POWERUP_PICKUP); onLaunch() }
            )
            Spacer(modifier = Modifier.height(6.dp))
            TerminalTabHeader(selectedTab = selectedTab, onTabSelected = { audio?.playSound(AudioEvent.BUTTON_CLICK); onSelectTab(it) })
            Spacer(modifier = Modifier.height(6.dp))
            TerminalItemPreviewRow(coordinator, item, countOwned, crtPulse)
            Spacer(modifier = Modifier.height(8.dp))
            TerminalControlDeck(
                isBuyMode = isBuyMode, onToggleBuyMode = { audio?.playSound(AudioEvent.BUTTON_CLICK); onToggleBuy(it) },
                canAfford = canAfford, currentItem = item,
                isAlreadyOwned = isAlreadyOwned, isEquipped = isEquipped,
                onPrevItem = { audio?.playSound(AudioEvent.BUTTON_CLICK); onPrev() },
                onNextItem = { audio?.playSound(AudioEvent.BUTTON_CLICK); onNext() },
                onActionClick = {
                    if (isBuyMode) handlePurchase(item, canAfford, coordinator, audio)
                    else handleSale(item, countOwned, coordinator, audio)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            TerminalScrambleButton(onLaunchCombat = { audio?.playSound(AudioEvent.POWERUP_PICKUP); onLaunch() })
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TerminalItemPreviewRow(
    coordinator: RaptorGameCoordinator,
    item: ArmoryItem,
    countOwned: Int,
    crtPulse: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1E2530), RoundedCornerShape(10.dp))
            .border(2.dp, Color(0xFF37474F), RoundedCornerShape(10.dp)).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TerminalPilotStatusHeader(coordinator.engine.context.credits, coordinator.playerShip.hull, coordinator.playerShip.shield)
        Box(modifier = Modifier.weight(1f)) {
            when {
                item.isPerk -> TerminalPerkCard(item, countOwned > 0, crtPulse)
                item.isConsumable -> TerminalSupplyCard(item, countOwned, crtPulse)
                else -> TerminalWeaponCard(item, countOwned, crtPulse)
            }
        }
    }
}

private fun handlePurchase(
    item: ArmoryItem, canAfford: Boolean, coordinator: RaptorGameCoordinator, audioAdapter: AndroidSoundPoolAudioAdapter?
) {
    val context = coordinator.engine.context
    val ship = coordinator.playerShip

    // If already owned, equipping is free!
    if (item.weaponId != null && coordinator.unlockedWeapons.contains(item.weaponId)) {
        coordinator.switchWeapon(item.weaponId)
        audioAdapter?.playSound(AudioEvent.POWERUP_PICKUP)
        return
    }

    if (canAfford) {
        context.spendCredits(item.cost)
        audioAdapter?.playSound(AudioEvent.POWERUP_PICKUP)
        when (item.id) {
            "odin_laser" -> coordinator.unlockWeapon("odin_laser")
            "deathray" -> coordinator.unlockWeapon("deathray")
            "twin_plasma" -> coordinator.unlockWeapon("twin_plasma")
            "vulcan_upgrade" -> {
                ship.upgradeWeapon()
                coordinator.savedWeaponTier = ship.weaponTier
            }
            "missiles_pack" -> {
                ship.addMissiles(4)
                coordinator.savedMissiles = ship.missileSalvoes
            }
            "mega_bomb" -> {
                ship.addBomb(1)
                coordinator.savedBombs = ship.megaBombs
            }
            "ion_shield" -> ship.rechargeShield(40f)
            "hull_repair" -> ship.repairHull(35f)
            "magnet_beam" -> {
                coordinator.persistentPerks.add("MAGNET_BEAM")
                context.addPerk("MAGNET_BEAM")
            }
        }
    } else {
        audioAdapter?.playSound(AudioEvent.HIT)
    }
}

private fun handleSale(
    item: ArmoryItem, countOwned: Int, coordinator: RaptorGameCoordinator, audioAdapter: AndroidSoundPoolAudioAdapter?
) {
    val context = coordinator.engine.context
    val ship = coordinator.playerShip
    if (countOwned > 0) {
        val refund = item.cost / 2
        context.earnCredits(refund)
        audioAdapter?.playSound(AudioEvent.COLLECT)
        when (item.id) {
            "missiles_pack" -> if (ship.missileSalvoes >= 4) {
                ship.missileSalvoes -= 4
                coordinator.savedMissiles = ship.missileSalvoes
            }
            "mega_bomb" -> if (ship.megaBombs > 0) {
                ship.megaBombs--
                coordinator.savedBombs = ship.megaBombs
            }
        }
    }
}

private fun getOwnershipCount(
    item: ArmoryItem,
    coordinator: RaptorGameCoordinator,
    ship: RaptorShip,
    context: GameContext
): Int = when (item.id) {
    "odin_laser" -> if (coordinator.unlockedWeapons.contains("odin_laser")) 1 else 0
    "deathray" -> if (coordinator.unlockedWeapons.contains("deathray")) 1 else 0
    "twin_plasma" -> if (coordinator.unlockedWeapons.contains("twin_plasma")) 1 else 0
    "vulcan_upgrade" -> ship.weaponTier
    "missiles_pack" -> ship.missileSalvoes
    "mega_bomb" -> ship.megaBombs
    "ion_shield" -> (ship.shield / 25f).toInt()
    "hull_repair" -> (ship.hull / 25f).toInt()
    "magnet_beam" -> if (context.hasPerk("MAGNET_BEAM") || coordinator.persistentPerks.contains("MAGNET_BEAM")) 1 else 0
    else -> 0
}

private fun createArmoryCatalog(): List<ArmoryItem> = listOf(
    ArmoryItem(
        id = "odin_laser", name = "LASER TURRET", category = "SELECTABLE",
        description = "THE OD55 ODIN LASER TURRET IS A PILOT'S BEST CHOICE FOR AIR COMBAT. SIMILAR TO THE TH19 THOR, THE ODIN HAS A FASTER RESPONSE TIME WITH THE LASER'S INCREASED FIREPOWER.",
        cost = 1200L, iconRes = R.drawable.raptor_terminal_laser, weaponId = "odin_laser"
    ),
    ArmoryItem(
        id = "deathray", name = "DEATHRAY CANNON", category = "SELECTABLE",
        description = "HIGH-ENERGY CONCENTRATED BEAM EMITTER. TEARS THROUGH CAPITAL CLASS INTERCEPTORS AND HEAVY BARRICADES IN FRACTIONS OF A SECOND.",
        cost = 2400L, iconRes = R.drawable.raptor_terminal_deathray, weaponId = "deathray"
    ),
    ArmoryItem(
        id = "twin_plasma", name = "TWIN PLASMA", category = "SELECTABLE",
        description = "RAPID DUAL ION-PLASMA DISCHARGERS PROVIDING MAXIMUM CLOSE-QUARTERS SPREAD TO ANNIHILATE INCOMING RECON FIGHTER WAVES.",
        cost = 1800L, iconRes = null, weaponId = "twin_plasma"
    ),
    ArmoryItem(
        id = "vulcan_upgrade", name = "CANNON TIER UPGRADE", category = "UPGRADE",
        description = "BOOSTS DEFAULT VULCAN CANNON TIER (+1 BARREL AND ELEVATED MUZZLE VELOCITY). MAX UPGRADE CAPACITY: TIER 4.",
        cost = 800L, isConsumable = true
    ),
    ArmoryItem(
        id = "missiles_pack", name = "AIR-TO-AIR MISSILES", category = "AMMUNITION",
        description = "CARGO POD CONTAINING 4 SEEKER MISSILES. AUTOMATICALLY LOCKS ONTO THE CLOSEST ENEMY INTERCEPTOR OR DREADNOUGHT BOSS.",
        cost = 450L, isConsumable = true
    ),
    ArmoryItem(
        id = "mega_bomb", name = "TACTICAL MEGA-BOMB", category = "AMMUNITION",
        description = "ELECTRO-MAGNETIC HIGH-OCTANE CANISTER. VAPORIZES ALL VISIBLE HOSTILE ORDNANCE AND HEAVILY DAMAGES AIR AND GROUND HOSTILES.",
        cost = 900L, isConsumable = true
    ),
    ArmoryItem(
        id = "ion_shield", name = "SHIELD OVERCHARGE", category = "DEFENSE",
        description = "RECHARGES ENERGY SHIELDING BY 40 UNITS UP TO MAXIMUM OVERCHARGE CAPACITY.",
        cost = 350L, isConsumable = true
    ),
    ArmoryItem(
        id = "hull_repair", name = "HULL REINFORCEMENT", category = "DEFENSE",
        description = "RESTORES STRUCTURAL TITANIUM HULL ARMOR BY 35 INTEGRITY POINTS.",
        cost = 400L, isConsumable = true
    ),
    ArmoryItem(
        id = "magnet_beam", name = "PERK: MAGNET BEAM", category = "PERMANENT PERK",
        description = "EQUIPS PASSIVE TRACTOR ATTRACTOR BEAM. COLLECTIBLE CARGO AND SCORE PODS GRAVITATE TOWARDS SHIP AUTOMATICALLY.",
        cost = 1500L, isPerk = true
    )
)
