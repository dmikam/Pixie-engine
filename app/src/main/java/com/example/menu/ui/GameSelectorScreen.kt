package com.example.menu.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.menu.model.GameStatus
import com.example.menu.model.PocGameInfo
import com.example.menu.model.PocGameRegistry

/**
 * Main arcade menu screen allowing selection between different Pixie engine proof of concepts.
 */
@Composable
fun GameSelectorScreen(
    onLaunchDinorun: () -> Unit,
    onLaunchRaptor: () -> Unit,
    onLaunchArkanoid: () -> Unit,
    onOpenRoadmap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedGameForDetails by remember { mutableStateOf<PocGameInfo?>(null) }
    val pulseAlpha = rememberPulseAlpha()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F12))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            ArcadeHeader(onOpenRoadmap = onOpenRoadmap)

            Spacer(modifier = Modifier.height(14.dp))

            ArchitectureFeaturePillsBar()

            Spacer(modifier = Modifier.height(18.dp))

            SelectorSectionHeader(onOpenRoadmap = onOpenRoadmap)

            Spacer(modifier = Modifier.height(12.dp))

            GameCardsList(
                pulseAlpha = pulseAlpha,
                onLaunchGame = { gameId ->
                    launchGameById(
                        gameId = gameId,
                        onLaunchDinorun = onLaunchDinorun,
                        onLaunchRaptor = onLaunchRaptor,
                        onLaunchArkanoid = onLaunchArkanoid
                    )
                },
                onSelectDetails = { game -> selectedGameForDetails = game }
            )

            SelectorFooterNotice()

            Spacer(modifier = Modifier.height(24.dp))
        }

        selectedGameForDetails?.let { game ->
            GameDetailDialog(
                game = game,
                onDismiss = { selectedGameForDetails = null },
                onLaunchPlay = {
                    selectedGameForDetails = null
                    launchGameById(
                        gameId = game.id,
                        onLaunchDinorun = onLaunchDinorun,
                        onLaunchRaptor = onLaunchRaptor,
                        onLaunchArkanoid = onLaunchArkanoid
                    )
                },
                onOpenRoadmap = {
                    selectedGameForDetails = null
                    onOpenRoadmap()
                }
            )
        }
    }
}

@Composable
private fun rememberPulseAlpha(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    return pulseAlpha
}

@Composable
private fun GameCardsList(
    pulseAlpha: Float,
    onLaunchGame: (String) -> Unit,
    onSelectDetails: (PocGameInfo) -> Unit
) {
    PocGameRegistry.games.forEach { game ->
        PocGameCard(
            game = game,
            pulseAlpha = if (game.status == GameStatus.PLAYABLE) pulseAlpha else 1f,
            onPlay = {
                if (game.status == GameStatus.PLAYABLE) {
                    onLaunchGame(game.id)
                } else {
                    onSelectDetails(game)
                }
            },
            onViewDetails = { onSelectDetails(game) }
        )
        Spacer(modifier = Modifier.height(14.dp))
    }
}

private fun launchGameById(
    gameId: String,
    onLaunchDinorun: () -> Unit,
    onLaunchRaptor: () -> Unit,
    onLaunchArkanoid: () -> Unit
) {
    when (gameId) {
        "dinorun" -> onLaunchDinorun()
        "raptor" -> onLaunchRaptor()
        "arkanoid" -> onLaunchArkanoid()
    }
}
