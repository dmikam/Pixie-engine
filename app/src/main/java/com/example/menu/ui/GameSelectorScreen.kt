package com.example.menu.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menu.model.GameStatus
import com.example.menu.model.PocGameInfo
import com.example.menu.model.PocGameRegistry

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameSelectorScreen(
    onLaunchDinorun: () -> Unit,
    onOpenRoadmap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedGameForDetails by remember { mutableStateOf<PocGameInfo?>(null) }

    // Pulsing animation for the playable indicator
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

            // Arcade Marquee Header
            ArcadeHeader(
                onOpenRoadmap = onOpenRoadmap
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Architecture pills bar
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF161A22),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3545)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeaturePill(icon = "60", label = "FPS Loop")
                    Text("•", color = Color(0xFF546E7A))
                    FeaturePill(icon = "5L", label = "Parallax")
                    Text("•", color = Color(0xFF546E7A))
                    FeaturePill(icon = "RGB", label = "ColorMatrix")
                    Text("•", color = Color(0xFF546E7A))
                    FeaturePill(icon = "4", label = "PoC Genres")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SELECT PROOF OF CONCEPT",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Gradual multi-genre engine testbed",
                        color = Color(0xFF90A4AE),
                        fontSize = 11.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E2838),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenRoadmap() }
                        .testTag("menu_roadmap_quick_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ROADMAP",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // The 4 PoC Game Cards
            PocGameRegistry.games.forEach { game ->
                PocGameCard(
                    game = game,
                    pulseAlpha = if (game.status == GameStatus.PLAYABLE) pulseAlpha else 1f,
                    onPlay = {
                        if (game.id == "dinorun") {
                            onLaunchDinorun()
                        } else {
                            selectedGameForDetails = game
                        }
                    },
                    onViewDetails = {
                        selectedGameForDetails = game
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Footer info
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF131720),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263238)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PIXIE ENGINE KOTLIN MULTIPLATFORM CORE",
                        color = Color(0xFF78909C),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pure Kotlin core (:pixie-engine) + Android Compose/Canvas adapter (:pixie-android). Developing Raptor, Arkanoid, and Chiki step by step.",
                        color = Color(0xFF546E7A),
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Details dialog
        selectedGameForDetails?.let { game ->
            GameDetailDialog(
                game = game,
                onDismiss = { selectedGameForDetails = null },
                onLaunchPlay = {
                    selectedGameForDetails = null
                    if (game.id == "dinorun") {
                        onLaunchDinorun()
                    }
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
private fun ArcadeHeader(
    onOpenRoadmap: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141924)),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(Color(0xFF00E5FF), Color(0xFFFF9100), Color(0xFFD500F9))
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "◆",
                    color = Color(0xFF00E5FF),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PIXIE ENGINE",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "◆",
                    color = Color(0xFFFF9100),
                    fontSize = 14.sp
                )
            }

            Text(
                text = "RETRO ARCADE PROOF OF CONCEPT HUB",
                color = Color(0xFFFFD54F),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "A multi-genre testbed validating fixed-timestep 2D physics, 5-layer parallax, and real-time ColorMatrix grading across 4 classic paradigms.",
                color = Color(0xFFB0BEC5),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = onOpenRoadmap,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                    modifier = Modifier.testTag("header_view_roadmap_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VIEW DOCS / ROADMAP",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(icon: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = icon,
            color = Color(0xFF00E5FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            color = Color(0xFF90A4AE),
            fontSize = 10.sp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PocGameCard(
    game: PocGameInfo,
    pulseAlpha: Float,
    onPlay: () -> Unit,
    onViewDetails: () -> Unit
) {
    val isPlayable = game.status == GameStatus.PLAYABLE

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
        border = androidx.compose.foundation.BorderStroke(
            if (isPlayable) 1.5.dp else 1.dp,
            if (isPlayable) game.primaryColor else Color(0xFF263238)
        ),
        modifier = Modifier
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
            // Card Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and paradigm
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
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF212733)
                        ) {
                            Text(
                                text = game.releaseVersion,
                                color = Color(0xFF90A4AE),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${game.genre} • ${game.paradigm}",
                        color = game.primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = game.status.badgeColor.copy(alpha = if (isPlayable) pulseAlpha else 1f)
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
                            text = game.status.label,
                            color = game.status.textColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tagline
            Text(
                text = game.tagline,
                color = Color(0xFFCFD8DC),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Engine Highlights Tags
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                game.engineHighlights.take(3).forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF101319),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263238))
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

            Spacer(modifier = Modifier.height(14.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isPlayable) {
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
                } else {
                    OutlinedButton(
                        onClick = onViewDetails,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, game.primaryColor.copy(alpha = 0.5f)),
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
            }
        }
    }
}
