package com.example.raptor.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

enum class TerminalTab(val label: String) {
    ALL("ALL"), WEAPONS("WEAPONS"), SUPPLIES("SUPPLIES"), PERKS("PERKS")
}

@Composable
fun TerminalTopBar(
    onBackClick: () -> Unit,
    onFlyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.testTag("terminal_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFCFD8DC))
        }
        Text(
            text = "HDF MAIN DISPLAY • SUPPLY DEPOT",
            color = Color(0xFF00E5FF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Button(
            onClick = onFlyClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(34.dp).testTag("terminal_quick_launch")
        ) {
            Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("FLY", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun TerminalTabHeader(
    selectedTab: TerminalTab,
    onTabSelected: (TerminalTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141923), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF263238), RoundedCornerShape(6.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TerminalTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color.Transparent,
                border = if (isSelected) BorderStroke(1.dp, Color(0xFF00E5FF)) else null,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(tab) }
                    .testTag("terminal_tab_${tab.name.lowercase()}")
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF90A4AE),
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 5.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TerminalPilotStatusHeader(
    credits: Long,
    hull: Float,
    shield: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(100.dp)
            .background(Color(0xFF141923), RoundedCornerShape(8.dp))
            .border(1.5.dp, Color(0xFF263238), RoundedCornerShape(8.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(shape = RoundedCornerShape(2.dp), color = Color(0xFFFFB300), modifier = Modifier.fillMaxWidth().height(6.dp)) {}
        Spacer(modifier = Modifier.height(4.dp))
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
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("PRV. 6:15", color = Color(0xFFFF5252), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text("REG", color = Color(0xFFCFD8DC), fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF000000),
            border = BorderStroke(1.dp, Color(0xFFFFD54F)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CREDITS", color = Color(0xFF90A4AE), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text("$credits", color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text("HULL ${hull.toInt()}%", color = if (hull > 40f) Color(0xFF00E676) else Color(0xFFFF5252), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text("SHD ${shield.toInt()}%", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun TerminalCrtBase(
    title: String,
    titleColor: Color,
    category: String,
    categoryColor: Color,
    borderColor: Color,
    previewBgColor: Color,
    previewBorderColor: Color,
    iconRes: Int,
    description: String,
    descColor: Color,
    statusLabel: String,
    statusValue: String,
    statusValueColor: Color,
    cost: Long,
    crtPulse: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF05070B), RoundedCornerShape(8.dp))
            .border(2.dp, borderColor.copy(alpha = crtPulse), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = titleColor, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            Text(category, color = categoryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp)
                .background(previewBgColor, RoundedCornerShape(4.dp))
                .border(1.dp, previewBorderColor, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(painter = painterResource(id = iconRes), contentDescription = title, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(4.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(description, color = descColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace, lineHeight = 13.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(statusLabel, color = Color(0xFF76FF03), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(statusValue, color = statusValueColor, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("COST", color = Color(0xFFFFD54F), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("$cost", color = Color(0xFFFFD54F), fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun TerminalWeaponCard(item: ArmoryItem, countOwned: Int, crtPulse: Float, modifier: Modifier = Modifier) {
    TerminalCrtBase(
        title = item.name,
        titleColor = Color(0xFFFFEB3B),
        category = item.category,
        categoryColor = Color(0xFFFF1744),
        borderColor = Color(0xFF00E5FF),
        previewBgColor = Color(0xFF0A0F1A),
        previewBorderColor = Color(0xFF1E293B),
        iconRes = item.iconRes,
        description = item.description,
        descColor = Color(0xFFFFB74D),
        statusLabel = if (item.id == "vulcan_upgrade") "TIER LEVEL" else "YOU HAVE",
        statusValue = "%02d".format(countOwned),
        statusValueColor = Color(0xFF76FF03),
        cost = item.cost,
        crtPulse = crtPulse,
        modifier = modifier
    )
}

@Composable
fun TerminalPerkCard(item: ArmoryItem, isInstalled: Boolean, crtPulse: Float, modifier: Modifier = Modifier) {
    TerminalCrtBase(
        title = item.name,
        titleColor = Color(0xFFE040FB),
        category = "CYBER PERK",
        categoryColor = Color(0xFF00E5FF),
        borderColor = Color(0xFFE040FB),
        previewBgColor = Color(0xFF0F0A1A),
        previewBorderColor = Color(0xFF381E48),
        iconRes = item.iconRes,
        description = item.description,
        descColor = Color(0xFFEA80FC),
        statusLabel = "STATUS",
        statusValue = if (isInstalled) "INSTALLED" else "AVAILABLE",
        statusValueColor = if (isInstalled) Color(0xFF76FF03) else Color(0xFFCFD8DC),
        cost = item.cost,
        crtPulse = crtPulse,
        modifier = modifier
    )
}

@Composable
fun TerminalSupplyCard(item: ArmoryItem, stockCount: Int, crtPulse: Float, modifier: Modifier = Modifier) {
    TerminalCrtBase(
        title = item.name,
        titleColor = Color(0xFF69F0AE),
        category = item.category,
        categoryColor = Color(0xFFFFD54F),
        borderColor = Color(0xFF00E676),
        previewBgColor = Color(0xFF07140B),
        previewBorderColor = Color(0xFF1B3D23),
        iconRes = item.iconRes,
        description = item.description,
        descColor = Color(0xFFA5D6A7),
        statusLabel = "RESERVE STOCK",
        statusValue = "%02d".format(stockCount),
        statusValueColor = Color(0xFF76FF03),
        cost = item.cost,
        crtPulse = crtPulse,
        modifier = modifier
    )
}

@Composable
fun TerminalControlDeck(
    isBuyMode: Boolean,
    onToggleBuyMode: (Boolean) -> Unit,
    canAfford: Boolean,
    currentItem: ArmoryItem,
    onPrevItem: () -> Unit,
    onNextItem: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
        border = BorderStroke(1.5.dp, Color(0xFF263238)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TerminalModeToggle(isBuyMode = isBuyMode, onToggleBuyMode = onToggleBuyMode)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onPrevItem,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(width = 44.dp, height = 36.dp).testTag("terminal_prev_item")
                    ) {
                        Text("<<", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                    }
                    Button(
                        onClick = onNextItem,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(width = 44.dp, height = 36.dp).testTag("terminal_next_item")
                    ) {
                        Text(">>", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBuyMode) {
                        if (canAfford) Color(0xFF2E7D32) else Color(0xFF37474F)
                    } else Color(0xFFC62828)
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(2.dp, if (isBuyMode && canAfford) Color(0xFF00E676) else Color(0xFF78909C)),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("terminal_action_button")
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
}

@Composable
private fun TerminalModeToggle(
    isBuyMode: Boolean,
    onToggleBuyMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xFF0F141D), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF37474F), RoundedCornerShape(6.dp))
            .padding(2.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isBuyMode) Color(0xFFD32F2F) else Color.Transparent,
            modifier = Modifier.clickable { onToggleBuyMode(true) }
        ) {
            Text("BUY", color = if (isBuyMode) Color.White else Color(0xFF78909C), fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
        }
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (!isBuyMode) Color(0xFFD32F2F) else Color.Transparent,
            modifier = Modifier.clickable { onToggleBuyMode(false) }
        ) {
            Text("SELL", color = if (!isBuyMode) Color.White else Color(0xFF78909C), fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
        }
    }
}

@Composable
fun TerminalScrambleButton(onLaunchCombat: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onLaunchCombat,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(2.dp, Color(0xFFFF5722)),
        modifier = modifier.fillMaxWidth().height(52.dp).testTag("terminal_scramble_button")
    ) {
        Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("SCRAMBLE (FLY MISSION)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
    }
}
