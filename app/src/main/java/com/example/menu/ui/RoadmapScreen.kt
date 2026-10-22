package com.example.menu.ui

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.menu.model.PocGameRegistry

@Composable
fun RoadmapScreen(
    onBack: () -> Unit,
    onLaunchGame: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("GAMES ROADMAP", "CAPABILITIES MATRIX", "LOG & DOCS")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115))
            .statusBarsPadding()
    ) {
        // App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF161920),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("roadmap_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Menu",
                        tint = Color(0xFF00E5FF)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ENGINE ROADMAP",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "DOCS/roadmap.md • Multi-Genre Validation",
                        color = Color(0xFF90A4AE),
                        fontSize = 11.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF263238)
                ) {
                    Text(
                        text = "v0.6.0",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF161920),
            contentColor = Color(0xFF00E5FF),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF00E5FF)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = if (selectedTab == index) Color(0xFF00E5FF) else Color(0xFF90A4AE)
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            when (selectedTab) {
                0 -> GamesRoadmapTab(onLaunchGame = onLaunchGame)
                1 -> CapabilitiesMatrixTab()
                2 -> LogAndDocsTab()
            }
        }
    }
}

@Composable
private fun GamesRoadmapTab(onLaunchGame: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Strategy notice
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1A212E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "GRADUAL ENGINE VALIDATION STRATEGY",
                    color = Color(0xFF64B5F6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Each proof-of-concept game tests a distinct camera mode, physics model, and layer paradigm. We implement them gradually one by one to ensure engine stability.",
                    color = Color(0xFFCFD8DC),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // 4 Game Roadmap Cards
        PocGameRegistry.games.forEach { game ->
            RoadmapGameCard(game = game, onLaunchGame = { onLaunchGame(game.id) })
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun RoadmapGameCard(game: PocGameInfo, onLaunchGame: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181C24)),
        border = androidx.compose.foundation.BorderStroke(1.dp, game.primaryColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (game.status) {
                            GameStatus.PLAYABLE -> Icons.Default.CheckCircle
                            GameStatus.NEXT_UP -> Icons.Default.HourglassTop
                            GameStatus.PLANNED -> Icons.Default.Schedule
                        },
                        contentDescription = null,
                        tint = game.status.badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = game.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = game.status.badgeColor
                ) {
                    Text(
                        text = game.status.label,
                        color = game.status.textColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${game.genre} (${game.paradigm})",
                color = game.primaryColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = game.tagline,
                color = Color(0xFFB0BEC5),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subsystems
            Text(
                text = "ENGINE MILESTONE FOCUS:",
                color = Color(0xFFFFD54F),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                game.subsystemsTested.forEach { sub ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "•",
                            color = game.primaryColor,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = sub,
                            color = Color(0xFFECEFF1),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            if (game.status == GameStatus.PLAYABLE) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onLaunchGame,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LAUNCH ${game.title.uppercase()} NOW",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun CapabilitiesMatrixTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "ENGINE CAPABILITIES MATRIX",
            color = Color(0xFFFFD54F),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Comparison of how each proof-of-concept exercises the core engine modules:",
            color = Color(0xFFB0BEC5),
            fontSize = 12.sp
        )

        val matrixRows = listOf(
            Triple("Camera Mode", "HorizontalScroller", "VerticalScroller"),
            Triple("Camera (cont.)", "FixedScreen (Arkanoid)", "2D Follow (Chiki)"),
            Triple("Physics Query", "AABB Obstacle/Meteors", "High-Density Bullet Pool"),
            Triple("Collisions (cont.)", "Continuous Sub-Step (CCD)", "Tilemap Solid & Ledges"),
            Triple("Layer Stack", "5-Layer Dynamic Horizon", "Multi-Pass Cloud & Starfield"),
            Triple("Input Paradigm", "Virtual Pad / Gestures", "Touch Drag / Sticks"),
            Triple("Post-Process", "ColorMatrix Saturation", "Energy Shield Glow / Bloom")
        )

        matrixRows.forEach { (feature, specA, specB) ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181C24)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = feature,
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = specA,
                            color = Color(0xFFECEFF1),
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = specB,
                            color = Color(0xFFFFD54F),
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun LogAndDocsTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "DOCS/ROADMAP.MD TRACKER",
            color = Color(0xFFFFD54F),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181C24)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "FILE LOCATION: /DOCS/roadmap.md",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This documentation file tracks all planification, current statuses, engine capabilities validated, and change logs. It is updated after implementing every part of the roadmap.",
                    color = Color(0xFFB0BEC5),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Text(
            text = "RELEASE HISTORY",
            color = Color(0xFF81D4FA),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        val changelog = listOf(
            "v0.6.0" to "Released Arkanoid fixed-screen precision brick breaker with FixedScreenViewMode, Continuous Collision Detection (CCD) preventing tunneling, 2D DestructibleMatrixLayer with explosive chains, angle modulation, and falling power capsules.",
            "v0.5.0" to "Released Raptor vertical combat scroller with continuous difficulty (0..100), zero-allocation projectile pooling, procedural particle explosions, destructible ground buildings/naval craft, and offline 16-bit PCM retro sound synthesis.",
            "v0.4.0" to "Added Starting Menu with PoC game selector (Dinorun, Raptor, Arkanoid, Chiki), specs preview modal, in-app Roadmap Viewer, and created DOCS/roadmap.md.",
            "v0.3.0" to "Released Dinorun endless runner with 5-layer parallax stack, ColorMatrix grayscale-to-color recovery, and dual-mode controls.",
            "v0.2.0" to "Multi-module architecture split (:pixie-engine pure Kotlin, :pixie-android adapters).",
            "v0.1.0" to "Initial 2D pixel-art fixed-timestep engine loop and Canvas renderer."
        )

        changelog.forEach { (version, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141820), RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = version,
                    color = Color(0xFF00E5FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = desc,
                    color = Color(0xFFCFD8DC),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
