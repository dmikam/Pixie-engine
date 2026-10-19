package com.example.raptor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.raptor.RaptorGameCoordinator
import com.example.raptor.world.RaptorLevelCatalog
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.android.ui.PixieGameCanvas
import com.pixie.engine.core.context.DifficultyPreset
import com.pixie.engine.core.math.Vec2
import kotlinx.coroutines.delay

@Composable
fun RaptorGameScreen(
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioAdapter = remember { AndroidSoundPoolAudioAdapter(context) }
    val hapticAdapter = remember { AndroidHapticAdapter(context) }

    var selectedDifficulty by remember { mutableStateOf(DifficultyPreset.MEDIUM) }
    var currentLevelIndex by remember { mutableIntStateOf(1) }

    val coordinator = remember {
        RaptorGameCoordinator(
            audio = audioAdapter,
            haptics = hapticAdapter,
            selectedDifficulty = selectedDifficulty,
            currentLevelIndex = currentLevelIndex
        )
    }

    var isPaused by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var isVictory by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    // HUD reactive state
    var currentScore by remember { mutableLongStateOf(0L) }
    var hullHp by remember { mutableFloatStateOf(100f) }
    var shieldHp by remember { mutableFloatStateOf(100f) }
    var missilesLeft by remember { mutableIntStateOf(6) }
    var bombsLeft by remember { mutableIntStateOf(2) }
    var weaponTier by remember { mutableIntStateOf(1) }
    var activePerksList by remember { mutableStateOf(listOf<String>()) }
    var levelDistance by remember { mutableFloatStateOf(0f) }

    // Handle back button
    BackHandler {
        if (!isPaused && !isGameOver && !isVictory) {
            isPaused = true
        } else {
            onExitToMenu()
        }
    }

    coordinator.onGameOver = { isGameOver = true }
    coordinator.onVictory = { isVictory = true }

    // 60 FPS Game Loop Driver
    LaunchedEffect(isPaused, isGameOver, isVictory) {
        if (!isPaused && !isGameOver && !isVictory) {
            coordinator.engine.start()
            var lastTimeNanos = System.nanoTime()

            while (true) {
                val now = System.nanoTime()
                val dt = ((now - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTimeNanos = now

                coordinator.update(dt)

                // Sync HUD reactive values
                currentScore = coordinator.engine.context.score
                hullHp = coordinator.playerShip.hull
                shieldHp = coordinator.playerShip.shield
                missilesLeft = coordinator.playerShip.missileSalvoes
                bombsLeft = coordinator.playerShip.megaBombs
                weaponTier = coordinator.playerShip.weaponTier
                activePerksList = coordinator.engine.context.activePerks.toList()
                levelDistance = coordinator.engine.context.distance

                delay(16L) // ~60fps fixed step
            }
        } else {
            coordinator.engine.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            coordinator.engine.stop()
            audioAdapter.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B12))
            .statusBarsPadding()
    ) {
        // Direct Touch-drag Controller over the whole screen
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val scaleX = 240f / size.width.toFloat()
                            val scaleY = 320f / size.height.toFloat()
                            val touchVirtual = Vec2(offset.x * scaleX, (offset.y * scaleY) - 20f)
                            val distToShip = (touchVirtual - coordinator.playerShip.position).length()
                            if (distToShip < 65f) {
                                coordinator.playerShip.targetPosition = Vec2(
                                    touchVirtual.x.coerceIn(4f, 218f),
                                    touchVirtual.y.coerceIn(24f, 290f)
                                )
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val scaleX = 240f / size.width.toFloat()
                            val scaleY = 320f / size.height.toFloat()
                            val currentTarget = coordinator.playerShip.targetPosition
                            coordinator.playerShip.targetPosition = Vec2(
                                (currentTarget.x + dragAmount.x * scaleX).coerceIn(4f, 218f),
                                (currentTarget.y + dragAmount.y * scaleY).coerceIn(24f, 290f)
                            )
                        }
                    )
                }
        ) {
            // Main Retro Arcade Game Canvas
            PixieGameCanvas(
                engine = coordinator.engine,
                modifier = Modifier.fillMaxSize()
            )

            // Top HUD Overlay
            RaptorTopHud(
                score = currentScore,
                levelName = coordinator.currentLevel.name,
                difficulty = selectedDifficulty,
                distance = levelDistance,
                targetDistance = coordinator.currentLevel.targetDistance,
                onPauseClick = { isPaused = true },
                onBackClick = { onExitToMenu() }
            )

            // Left Side Status Meters (Shield & Hull)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            ) {
                // Shield Gauge
                StatusGauge(
                    label = "SHIELD",
                    value = shieldHp,
                    maxValue = 100f,
                    activeColor = Color(0xFF00E5FF),
                    backgroundColor = Color(0xFF002233)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Hull Integrity Gauge
                StatusGauge(
                    label = "HULL",
                    value = hullHp,
                    maxValue = 100f,
                    activeColor = if (hullHp > 40f) Color(0xFF00E676) else Color(0xFFFF3D00),
                    backgroundColor = Color(0xFF2E1500)
                )
            }

            // Bottom Controls Bar (Auto-Fire, Missile trigger, Mega-Bomb detonator)
            RaptorBottomControls(
                isAutoFire = coordinator.isAutoFireEnabled,
                weaponTier = weaponTier,
                missilesLeft = missilesLeft,
                bombsLeft = bombsLeft,
                activePerks = activePerksList,
                onToggleAutoFire = { coordinator.isAutoFireEnabled = !coordinator.isAutoFireEnabled },
                onFireMissiles = { coordinator.fireSecondaryMissiles() },
                onTriggerBomb = { coordinator.triggerMegaBomb() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Pause Modal
        if (isPaused) {
            RaptorPauseDialog(
                selectedDifficulty = selectedDifficulty,
                isMuted = isMuted,
                currentLevel = currentLevelIndex,
                onResume = { isPaused = false },
                onRestart = {
                    coordinator.setupGame()
                    isPaused = false
                },
                onToggleMute = {
                    isMuted = !isMuted
                    audioAdapter.isMuted = isMuted
                },
                onSelectDifficulty = { diff ->
                    selectedDifficulty = diff
                    coordinator.setDifficulty(diff)
                },
                onSelectLevel = { lvl ->
                    currentLevelIndex = lvl
                    coordinator.setLevel(lvl)
                },
                onExit = onExitToMenu
            )
        }

        // Game Over Dialog
        if (isGameOver) {
            RaptorGameOverDialog(
                score = currentScore,
                levelName = coordinator.currentLevel.name,
                difficulty = selectedDifficulty,
                onRestart = {
                    coordinator.setupGame()
                    isGameOver = false
                },
                onExit = onExitToMenu
            )
        }

        // Victory Dialog
        if (isVictory) {
            RaptorVictoryDialog(
                score = currentScore,
                levelName = coordinator.currentLevel.name,
                hasNextLevel = currentLevelIndex < 3,
                onNextLevel = {
                    currentLevelIndex++
                    coordinator.setLevel(currentLevelIndex)
                    isVictory = false
                },
                onRestart = {
                    coordinator.setupGame()
                    isVictory = false
                },
                onExit = onExitToMenu
            )
        }
    }
}

@Composable
private fun RaptorTopHud(
    score: Long,
    levelName: String,
    difficulty: DifficultyPreset,
    distance: Float,
    targetDistance: Float,
    onPauseClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Surface(
        color = Color(0xCC0A0E17),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("raptor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Menu",
                        tint = Color(0xFFCFD8DC)
                    )
                }

                // Level Name and Progress
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = levelName,
                        color = Color(0xFFFF9100),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    val progress = (distance / targetDistance).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .width(120.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFFFF9100),
                        trackColor = Color(0xFF263238)
                    )
                }

                // Score Display
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SCORE",
                        color = Color(0xFF78909C),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "%06d".format(score),
                        color = Color(0xFF00E5FF),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Pause Button
                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("raptor_pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = "Pause Game",
                        tint = Color(0xFFCFD8DC)
                    )
                }
            }

            // Difficulty chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (difficulty) {
                        DifficultyPreset.EASY -> Color(0xFF1B5E20)
                        DifficultyPreset.MEDIUM -> Color(0xFF0D47A1)
                        DifficultyPreset.DIFFICULT -> Color(0xFFE65100)
                        DifficultyPreset.HEAVY_METAL -> Color(0xFFB71C1C)
                        else -> Color(0xFF263238)
                    }
                ) {
                    Text(
                        text = "${difficulty.name.uppercase()} (${difficulty.difficultyValue})",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusGauge(
    label: String,
    value: Float,
    maxValue: Float,
    activeColor: Color,
    backgroundColor: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xCC0A0E17),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263238)),
        modifier = Modifier.width(68.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = activeColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { (value / maxValue).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = activeColor,
                trackColor = backgroundColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${value.toInt()}",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun RaptorBottomControls(
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
        // Active Perks Badges
        if (activePerks.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                activePerks.forEach { perk ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1B5E20),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676)),
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

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Auto-fire Toggle
            Button(
                onClick = onToggleAutoFire,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAutoFire) Color(0xFF1565C0) else Color(0xFF263238)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
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

            // Homing Missiles Button
            Button(
                onClick = onFireMissiles,
                enabled = missilesLeft > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
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

            // Mega-Bomb Detonator
            Button(
                onClick = onTriggerBomb,
                enabled = bombsLeft > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
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
    }
}

@Composable
private fun RaptorPauseDialog(
    selectedDifficulty: DifficultyPreset,
    isMuted: Boolean,
    currentLevel: Int,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onToggleMute: () -> Unit,
    onSelectDifficulty: (DifficultyPreset) -> Unit,
    onSelectLevel: (Int) -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF9100)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TACTICAL PAUSE",
                    color = Color(0xFFFF9100),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Difficulty Preset Selection
                Text(
                    text = "DIFFICULTY PRESET (0..100)",
                    color = Color(0xFF90A4AE),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DifficultyPreset.DEFAULT_PRESETS.forEach { preset ->
                        val isSelected = preset == selectedDifficulty
                        Button(
                            onClick = { onSelectDifficulty(preset) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFFF9100) else Color(0xFF263238)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .height(36.dp)
                        ) {
                            Text(
                                text = "${preset.difficultyValue}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Campaign Level Select
                Text(
                    text = "CAMPAIGN LEVEL",
                    color = Color(0xFF90A4AE),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (1..3).forEach { lvl ->
                        val isSelected = lvl == currentLevel
                        Button(
                            onClick = { onSelectLevel(lvl) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF263238)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .height(36.dp)
                        ) {
                            Text(
                                text = "LVL $lvl",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("resume_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESUME COMBAT", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onToggleMute,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isMuted) "UNMUTE" else "MUTE", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onRestart,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESTART", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onExit,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("EXIT TO ARCADE HUB", color = Color(0xFFFF5252))
                }
            }
        }
    }
}

@Composable
private fun RaptorGameOverDialog(
    score: Long,
    levelName: String,
    difficulty: DifficultyPreset,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1215)),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF1744)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "INTERCEPTOR DESTROYED",
                    color = Color(0xFFFF1744),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = levelName,
                    color = Color(0xFFB0BEC5),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F0A0C),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("FINAL SCORE", color = Color(0xFF78909C), fontSize = 11.sp)
                        Text(
                            text = "$score",
                            color = Color(0xFFFFD54F),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "DIFFICULTY: ${difficulty.name} (${difficulty.difficultyValue})",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("game_over_retry_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DEPLOY REPLACEMENT INTERCEPTOR")
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
        }
    }
}

@Composable
private fun RaptorVictoryDialog(
    score: Long,
    levelName: String,
    hasNextLevel: Boolean,
    onNextLevel: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101B17)),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E676)),
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

                Spacer(modifier = Modifier.height(20.dp))

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
        }
    }
}
