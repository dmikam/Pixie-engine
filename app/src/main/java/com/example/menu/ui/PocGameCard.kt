package com.example.menu.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menu.model.GameStatus
import com.example.menu.model.PocGameInfo

/**
 * Arcade catalog card representing an individual Pixie engine proof of concept.
 */
@Composable
fun PocGameCard(
    game: PocGameInfo,
    pulseAlpha: Float,
    onPlay: () -> Unit,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlayable = game.status == GameStatus.PLAYABLE

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
        border = BorderStroke(
            if (isPlayable) 1.5.dp else 1.dp,
            if (isPlayable) game.primaryColor else Color(0xFF263238)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onViewDetails() }
            .testTag("game_card_${game.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            GameCardTopHeader(
                game = game,
                pulseAlpha = pulseAlpha,
                isPlayable = isPlayable
            )

            Spacer(modifier = Modifier.height(10.dp))

            GameTagline(tagline = game.tagline)

            Spacer(modifier = Modifier.height(12.dp))

            GameEngineHighlightsTags(highlights = game.engineHighlights)

            Spacer(modifier = Modifier.height(14.dp))

            GameCardActionButtons(
                game = game,
                isPlayable = isPlayable,
                onPlay = onPlay,
                onViewDetails = onViewDetails
            )
        }
    }
}

@Composable
private fun GameCardTopHeader(
    game: PocGameInfo,
    pulseAlpha: Float,
    isPlayable: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = game.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                GameReleaseVersionChip(version = game.releaseVersion)
            }
            Text(
                text = "${game.genre} • ${game.paradigm}",
                color = game.primaryColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        GameStatusBadge(
            status = game.status,
            isPlayable = isPlayable,
            pulseAlpha = pulseAlpha
        )
    }
}

@Composable
private fun GameReleaseVersionChip(version: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF212733)
    ) {
        Text(
            text = version,
            color = Color(0xFF90A4AE),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun GameStatusBadge(
    status: GameStatus,
    isPlayable: Boolean,
    pulseAlpha: Float
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = status.badgeColor.copy(alpha = if (isPlayable) pulseAlpha else 1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isPlayable) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = status.label,
                color = status.textColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun GameTagline(tagline: String) {
    Text(
        text = tagline,
        color = Color(0xFFCFD8DC),
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GameEngineHighlightsTags(highlights: List<String>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        highlights.take(3).forEach { tag ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF101319),
                border = BorderStroke(1.dp, Color(0xFF263238))
            ) {
                Text(
                    text = tag,
                    color = Color(0xFFB0BEC5),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun GameCardActionButtons(
    game: PocGameInfo,
    isPlayable: Boolean,
    onPlay: () -> Unit,
    onViewDetails: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isPlayable) {
            PlayableActions(
                game = game,
                onPlay = onPlay,
                onViewDetails = onViewDetails
            )
        } else {
            UnreleasedActions(
                game = game,
                onViewDetails = onViewDetails
            )
        }
    }
}

@Composable
private fun PlayableActions(
    game: PocGameInfo,
    onPlay: () -> Unit,
    onViewDetails: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onPlay,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("launch_button_${game.id}")
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "PLAY ${game.title.uppercase()} ▶",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        OutlinedButton(
            onClick = onViewDetails,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = "Specs",
                tint = Color(0xFF90A4AE),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "SPECS",
                color = Color(0xFFB0BEC5),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun UnreleasedActions(
    game: PocGameInfo,
    onViewDetails: () -> Unit
) {
    OutlinedButton(
        onClick = onViewDetails,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, game.primaryColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .testTag("view_specs_button_${game.id}")
    ) {
        Icon(
            imageVector = Icons.Default.Visibility,
            contentDescription = null,
            tint = game.primaryColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "VIEW ${game.title.uppercase()} SPECS & ROADMAP",
            color = game.primaryColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
