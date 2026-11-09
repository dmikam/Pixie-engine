package com.example.raptor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixie.android.input.ArcadeActionType
import com.pixie.android.input.ArcadeButtonConfig
import com.pixie.android.input.ArcadeButtonShape
import com.pixie.android.input.DockedBottomConsoleDeck
import com.pixie.android.input.TactileArcadeButton

/**
 * Arcade control button factories and UI components for Raptor combat.
 */
object RaptorControls {

    fun createAutoFireButton(
        isAutoFire: Boolean, weaponTier: Int, onToggle: () -> Unit
    ): ArcadeButtonConfig = ArcadeButtonConfig(
        id = "auto_fire_toggle", label = "AUTO (T$weaponTier)", iconVector = Icons.Filled.Bolt,
        shape = ArcadeButtonShape.PILL, weight = 1.0f, minWidthDp = 80.dp, heightDp = 48.dp,
        baseColor = if (isAutoFire) Color(0xFF1565C0) else Color(0xFF263238),
        pressedColor = if (isAutoFire) Color(0xFF00B0FF) else Color(0xFF37474F),
        rimColor = if (isAutoFire) Color(0xFF00E5FF) else Color(0xFF546E7A),
        actionType = ArcadeActionType.ACTION_PRIMARY, onPressDown = onToggle
    )

    fun createMissileButton(missilesLeft: Int, onFire: () -> Unit): ArcadeButtonConfig {
        val hasAmmo = missilesLeft > 0
        return ArcadeButtonConfig(
            id = "missile_fire_button", label = "MISSILE ($missilesLeft)", iconVector = Icons.Filled.RocketLaunch,
            shape = ArcadeButtonShape.PILL, weight = 1.0f, minWidthDp = 80.dp, heightDp = 48.dp,
            baseColor = if (hasAmmo) Color(0xFFD84315) else Color(0xFF2E2421),
            pressedColor = if (hasAmmo) Color(0xFFFF5722) else Color(0xFF3E2723),
            rimColor = if (hasAmmo) Color(0xFFFF8A65) else Color(0xFF4E342E),
            actionType = ArcadeActionType.ACTION_SECONDARY, onPressDown = { if (hasAmmo) onFire() }
        )
    }

    fun createMegaBombButton(bombsLeft: Int, onTrigger: () -> Unit): ArcadeButtonConfig {
        val hasBombs = bombsLeft > 0
        return ArcadeButtonConfig(
            id = "mega_bomb_button", label = "BOMB ($bombsLeft)", iconVector = Icons.Filled.CrisisAlert,
            shape = ArcadeButtonShape.OCTAGON, weight = 1.0f, minWidthDp = 80.dp, heightDp = 48.dp,
            baseColor = if (hasBombs) Color(0xFF7B1FA2) else Color(0xFF261D2E),
            pressedColor = if (hasBombs) Color(0xFFAA00FF) else Color(0xFF311B92),
            rimColor = if (hasBombs) Color(0xFFFFD54F) else Color(0xFF4A148C),
            actionType = ArcadeActionType.ACTION_SPECIAL, onPressDown = { if (hasBombs) onTrigger() }
        )
    }

    fun createPauseButton(onPause: () -> Unit): ArcadeButtonConfig = ArcadeButtonConfig(
        id = "raptor_pause_button", iconVector = Icons.Filled.Pause, shape = ArcadeButtonShape.CIRCLE,
        sizeDp = 44.dp, widthDp = 44.dp, minWidthDp = 44.dp,
        baseColor = Color(0xFF263238), pressedColor = Color(0xFF455A64), rimColor = Color(0xFF90A4AE),
        actionType = ArcadeActionType.PAUSE, onPressDown = onPause
    )

    fun createWeaponSwitchButton(
        activeWeaponId: String, unlockedCount: Int, onCycle: () -> Unit
    ): ArcadeButtonConfig {
        val shortName = com.example.raptor.entities.RaptorWeapons.getShortName(activeWeaponId)
        val label = if (unlockedCount > 1) "WPN: $shortName" else shortName
        return ArcadeButtonConfig(
            id = "weapon_switch_button",
            label = label,
            iconVector = Icons.Filled.Refresh,
            shape = ArcadeButtonShape.PILL,
            weight = 1.0f,
            minWidthDp = 76.dp,
            heightDp = 48.dp,
            baseColor = Color(0xFF00695C),
            pressedColor = Color(0xFF00897B),
            rimColor = Color(0xFF26A69A),
            actionType = ArcadeActionType.CUSTOM,
            onPressDown = onCycle
        )
    }

    fun createFloatingButtons(
        autoFireBtn: ArcadeButtonConfig,
        missileBtn: ArcadeButtonConfig,
        bombBtn: ArcadeButtonConfig,
        pauseBtn: ArcadeButtonConfig? = null,
        weaponBtn: ArcadeButtonConfig? = null
    ): List<ArcadeButtonConfig> {
        val list = mutableListOf(
            autoFireBtn.copy(id = "fl_auto_fire", widthDp = 72.dp, weight = null, floatingOffset = Offset(12f, 440f))
        )
        weaponBtn?.let {
            list.add(it.copy(id = "fl_weapon", widthDp = 72.dp, weight = null, floatingOffset = Offset(88f, 440f)))
        }
        val missileX = if (weaponBtn != null) 164f else 104f
        val bombX = if (weaponBtn != null) 240f else 192f
        list.add(missileBtn.copy(id = "fl_missile", widthDp = 72.dp, weight = null, floatingOffset = Offset(missileX, 440f)))
        list.add(bombBtn.copy(id = "fl_bomb", widthDp = 72.dp, weight = null, floatingOffset = Offset(bombX, 440f)))
        pauseBtn?.let { list.add(it.copy(id = "fl_pause", widthDp = 44.dp, weight = null, floatingOffset = Offset(240f, 20f))) }
        return list
    }
}

// Top-level aliases for direct function access
fun createAutoFireButton(isAutoFire: Boolean, weaponTier: Int, onToggle: () -> Unit) =
    RaptorControls.createAutoFireButton(isAutoFire, weaponTier, onToggle)

fun createWeaponSwitchButton(activeWeaponId: String, unlockedCount: Int, onCycle: () -> Unit) =
    RaptorControls.createWeaponSwitchButton(activeWeaponId, unlockedCount, onCycle)

fun createMissileButton(missilesLeft: Int, onFire: () -> Unit) =
    RaptorControls.createMissileButton(missilesLeft, onFire)

fun createMegaBombButton(bombsLeft: Int, onTrigger: () -> Unit) =
    RaptorControls.createMegaBombButton(bombsLeft, onTrigger)

fun createPauseButton(onPause: () -> Unit) =
    RaptorControls.createPauseButton(onPause)

fun createFloatingButtons(
    autoFireBtn: ArcadeButtonConfig,
    missileBtn: ArcadeButtonConfig,
    bombBtn: ArcadeButtonConfig,
    pauseBtn: ArcadeButtonConfig? = null,
    weaponBtn: ArcadeButtonConfig? = null
) = RaptorControls.createFloatingButtons(autoFireBtn, missileBtn, bombBtn, pauseBtn, weaponBtn)

@Composable
fun AutoFireButton(isAutoFire: Boolean, weaponTier: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val config = remember(isAutoFire, weaponTier, onClick) { RaptorControls.createAutoFireButton(isAutoFire, weaponTier, onClick) }
    TactileArcadeButton(config = config, modifier = modifier.testTag("auto_fire_toggle"))
}

@Composable
fun WeaponSwitchButton(
    activeWeaponId: String,
    unlockedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = remember(activeWeaponId, unlockedCount, onClick) {
        RaptorControls.createWeaponSwitchButton(activeWeaponId, unlockedCount, onClick)
    }
    TactileArcadeButton(config = config, modifier = modifier.testTag("weapon_switch_button"))
}

@Composable
fun MissileFireButton(missilesLeft: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val config = remember(missilesLeft, onClick) { RaptorControls.createMissileButton(missilesLeft, onClick) }
    TactileArcadeButton(config = config, modifier = modifier.testTag("missile_fire_button"))
}

@Composable
fun MegaBombDetonatorButton(bombsLeft: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val config = remember(bombsLeft, onClick) { RaptorControls.createMegaBombButton(bombsLeft, onClick) }
    TactileArcadeButton(config = config, modifier = modifier.testTag("mega_bomb_button"))
}

@Composable
fun RaptorActivePerksBadges(activePerks: List<String>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        activePerks.forEach { perk ->
            Surface(
                shape = RoundedCornerShape(4.dp), color = Color(0xFF1B5E20),
                border = BorderStroke(1.dp, Color(0xFF00E676)), modifier = Modifier.padding(horizontal = 3.dp)
            ) {
                Text(
                    text = perk, color = Color(0xFFE8F5E9), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Interactive weapon selector bar showing badges for all unlocked weapons.
 * Allows pilot to switch directly to any owned weapon with a single tap during combat.
 */
@Composable
fun RaptorWeaponSelectorBar(
    activeWeaponId: String,
    unlockedWeapons: List<String>,
    onSelectWeapon: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (unlockedWeapons.size <= 1) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        unlockedWeapons.forEach { weaponId ->
            val isSelected = weaponId == activeWeaponId
            val name = com.example.raptor.entities.RaptorWeapons.getShortName(weaponId)
            val chipColor = if (isSelected) Color(0xFF004D40) else Color(0xFF141923)
            val borderColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF37474F)
            val textColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF90A4AE)

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = chipColor,
                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .clickable { onSelectWeapon(weaponId) }
                    .testTag("weapon_chip_$weaponId")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.size(5.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = name,
                        color = textColor,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun RaptorBottomControls(
    isAutoFire: Boolean,
    weaponTier: Int,
    activeWeaponId: String = "vulcan",
    unlockedWeapons: List<String> = listOf("vulcan"),
    missilesLeft: Int = 6,
    bombsLeft: Int = 2,
    activePerks: List<String> = emptyList(),
    onToggleAutoFire: () -> Unit = {},
    onCycleWeapon: () -> Unit = {},
    onSelectWeapon: (String) -> Unit = {},
    onFireMissiles: () -> Unit = {},
    onTriggerBomb: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val autoFireBtn = remember(isAutoFire, weaponTier, onToggleAutoFire) {
        RaptorControls.createAutoFireButton(isAutoFire, weaponTier, onToggleAutoFire)
    }
    val weaponBtn = remember(activeWeaponId, unlockedWeapons.size, onCycleWeapon) {
        RaptorControls.createWeaponSwitchButton(activeWeaponId, unlockedWeapons.size, onCycleWeapon)
    }
    val missileBtn = remember(missilesLeft, onFireMissiles) {
        RaptorControls.createMissileButton(missilesLeft, onFireMissiles)
    }
    val bombBtn = remember(bombsLeft, onTriggerBomb) {
        RaptorControls.createMegaBombButton(bombsLeft, onTriggerBomb)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (activePerks.isNotEmpty()) {
            RaptorActivePerksBadges(activePerks = activePerks, modifier = Modifier.padding(horizontal = 8.dp))
        }
        RaptorWeaponSelectorBar(
            activeWeaponId = activeWeaponId,
            unlockedWeapons = unlockedWeapons,
            onSelectWeapon = onSelectWeapon
        )
        val buttons = listOf(autoFireBtn, weaponBtn, missileBtn, bombBtn)
        DockedBottomConsoleDeck(buttons = buttons, modifier = Modifier.fillMaxWidth())
    }
}
