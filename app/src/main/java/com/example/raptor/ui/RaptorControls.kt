package com.example.raptor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom control deck for Raptor vertical scrolling combat.
 * Hosts Auto-Fire toggle, Secondary Missiles launch, and Mega-Bomb detonator.
 */
@Composable
fun RaptorBottomControls(
    isAutoFire: Boolean,
    weaponTier: Int,
    missilesLeft: Int,
    bombsLeft: Int,
    activePerks: List<String>,
    onToggleAutoFire: () -> Unit,
    onFireMissiles: () -> Unit,
    onTriggerBomb: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (activePerks.isNotEmpty()) {
            RaptorActivePerksBadges(activePerks = activePerks)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AutoFireButton(
                isAutoFire = isAutoFire,
                weaponTier = weaponTier,
                onClick = onToggleAutoFire
            )

            MissileFireButton(
                missilesLeft = missilesLeft,
                onClick = onFireMissiles
            )

            MegaBombDetonatorButton(
                bombsLeft = bombsLeft,
                onClick = onTriggerBomb
            )
        }
    }
}

@Composable
fun RaptorActivePerksBadges(
    activePerks: List<String>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        activePerks.forEach { perk ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF1B5E20),
                border = BorderStroke(1.dp, Color(0xFF00E676)),
                modifier = Modifier.padding(horizontal = 3.dp)
            ) {
                Text(
                    text = perk,
                    color = Color(0xFFE8F5E9),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun AutoFireButton(
    isAutoFire: Boolean,
    weaponTier: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isAutoFire) Color(0xFF1565C0) else Color(0xFF263238)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .height(48.dp)
            .testTag("auto_fire_toggle")
    ) {
        Icon(
            imageVector = Icons.Filled.Bolt,
            contentDescription = "Auto Fire",
            tint = if (isAutoFire) Color(0xFF00E5FF) else Color(0xFF78909C),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "AUTO (T$weaponTier)",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun MissileFireButton(
    missilesLeft: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = missilesLeft > 0,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .height(48.dp)
            .testTag("missile_fire_button")
    ) {
        Icon(
            imageVector = Icons.Filled.RocketLaunch,
            contentDescription = "Launch Missiles",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "MISSILE ($missilesLeft)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun MegaBombDetonatorButton(
    bombsLeft: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = bombsLeft > 0,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .height(48.dp)
            .border(1.dp, Color(0xFFD500F9), RoundedCornerShape(10.dp))
            .testTag("mega_bomb_button")
    ) {
        Icon(
            imageVector = Icons.Filled.CrisisAlert,
            contentDescription = "Detonate Mega-Bomb",
            tint = Color(0xFFFFD54F),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "BOMB ($bombsLeft)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
