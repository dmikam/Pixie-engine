package com.example.raptor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.window.Dialog

/**
 * Victory Dialog when a Raptor level/sector is accomplished.
 */
@Composable
fun RaptorVictoryDialog(
    score: Long,
    levelName: String,
    hasNextLevel: Boolean,
    onNextLevel: () -> Unit,
    onGoToHangar: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101B17)),
            border = BorderStroke(2.dp, Color(0xFF00E676)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MISSION ACCOMPLISHED!",
                    color = Color(0xFF00E676),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$levelName Cleared",
                    color = Color(0xFFB0BEC5),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                VictoryScoreCard(score = score)

                Spacer(modifier = Modifier.height(20.dp))
                VictoryActionButtons(
                    hasNextLevel = hasNextLevel,
                    onNextLevel = onNextLevel,
                    onGoToHangar = onGoToHangar,
                    onRestart = onRestart,
                    onExit = onExit
                )
            }
        }
    }
}

@Composable
private fun VictoryScoreCard(score: Long) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF09120F),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("MISSION SCORE", color = Color(0xFF78909C), fontSize = 11.sp)
            Text(
                text = "$score",
                color = Color(0xFF00E5FF),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun VictoryActionButtons(
    hasNextLevel: Boolean,
    onNextLevel: () -> Unit,
    onGoToHangar: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Button(
        onClick = onGoToHangar,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("victory_hangar_button")
    ) {
        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = Color.Black)
        Spacer(modifier = Modifier.width(6.dp))
        Text("SUPPLY DEPOT & WEAPON SHOP", color = Color.Black, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (hasNextLevel) {
        Button(
            onClick = onNextLevel,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("next_mission_button")
        ) {
            Text("NEXT CAMPAIGN SECTOR", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    OutlinedButton(
        onClick = onRestart,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("REPLAY SECTOR")
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = onExit,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("RETURN TO ARCADE HUB")
    }
}
