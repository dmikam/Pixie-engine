package com.pixie.android.ui

import android.content.res.Configuration
import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixie.android.input.ArcadeButtonConfig
import com.pixie.android.input.ArcadeDeckPlacement
import com.pixie.android.input.DockedBottomConsoleDeck
import com.pixie.android.input.DualModeInputAdapter
import com.pixie.android.input.FloatingAdjustableDeck
import com.pixie.android.input.InputPreference
import com.pixie.android.input.InputSourceType
import com.pixie.android.input.TactileArcadeButton
import com.pixie.android.input.ViewportMode
import com.pixie.engine.core.PixieEngine

/**
 * Universal Game Scaffold for Pixie Engine games.
 *
 * Provides:
 * 1. Safe Viewport isolation (prevents Status Bar and Action Bar from occluding game elements).
 * 2. 3 Switchable Landscape Viewport Presentation Modes:
 *    - CENTERED_WINGS: Centered aspect-ratio game canvas with left/right control wings.
 *    - CABINET_BEZEL: Embedded within an authentic CRT Arcade Cabinet bezel.
 *    - ADAPTIVE_FIT: Dynamically bounded strictly between top status bar and bottom bar.
 * 3. 3 Switchable Arcade Button Placements:
 *    - SPLIT_WINGS: Left D-pad/steer, Right action cluster.
 *    - BOTTOM_STRIP: Traditional horizontal arcade cabinet shelf.
 *    - FLOATING: Draggable on-screen arcade buttons.
 * 4. Dual Input Support:
 *    - Intercepts hardware KeyEvents from gamepads and keyboards and forwards to [inputAdapter].
 *    - Visual indicator of active input method (Touch, Gamepad, Keyboard).
 * 5. Built-in live showcase switcher panel for rapid inspection and user tuning.
 */
@Composable
fun PixieGameScaffold(
    engine: PixieEngine,
    inputAdapter: DualModeInputAdapter? = null,
    modifier: Modifier = Modifier,
    // Control Button configurations
    leftButtons: List<ArcadeButtonConfig> = emptyList(),
    centerButtons: List<ArcadeButtonConfig> = emptyList(),
    rightButtons: List<ArcadeButtonConfig> = emptyList(),
    floatingButtons: List<ArcadeButtonConfig> = emptyList(),
    // Custom slots
    topStatusBar: @Composable () -> Unit = {},
    overlays: @Composable () -> Unit = {},
    // Main Game Canvas content
    gameCanvas: @Composable (modifier: Modifier) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Showcase Switcher state
    var viewportMode by remember { mutableStateOf(ViewportMode.CENTERED_WINGS) }
    var userPortraitPlacement by remember { mutableStateOf(ArcadeDeckPlacement.BOTTOM_STRIP) }
    var userLandscapePlacement by remember { mutableStateOf<ArcadeDeckPlacement?>(null) }
    var inputPreference by remember { mutableStateOf(InputPreference.HYBRID_AUTO) }
    var showShowcaseDialog by remember { mutableStateOf(false) }

    // Derive placement safely so rotating back and forth NEVER drops the bottom buttons bar
    val effectivePlacement = if (!isLandscape) {
        userPortraitPlacement
    } else {
        userLandscapePlacement ?: when (viewportMode) {
            ViewportMode.CENTERED_WINGS -> ArcadeDeckPlacement.SPLIT_WINGS
            ViewportMode.ADAPTIVE_FIT -> ArcadeDeckPlacement.BOTTOM_STRIP
        }
    }

    // Focus requester for capturing hardware gamepads and keyboards
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F))
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (inputPreference != InputPreference.TOUCH_ONLY && inputAdapter != null) {
                    val nativeEvent = keyEvent.nativeKeyEvent
                    val isDown = nativeEvent.action == KeyEvent.ACTION_DOWN
                    inputAdapter.handlePhysicalKeyEvent(nativeEvent.keyCode, isDown)
                } else false
            }
    ) {
        if (!isLandscape) {
            // ================= PORTRAIT LAYOUT =================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                // Top Status Bar Slot (Cleanly isolated, cannot overlap top game field)
                topStatusBar()

                // Center Game Canvas (safely occupies the remaining height above bottom deck)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    gameCanvas(Modifier.fillMaxSize())

                    // Floating buttons if selected in portrait
                    if (effectivePlacement == ArcadeDeckPlacement.FLOATING && floatingButtons.isNotEmpty()) {
                        FloatingAdjustableDeck(buttons = floatingButtons)
                    }

                    // Any in-game popups/overlays
                    overlays()
                }

                // Bottom Action Deck (Cleanly docked below game canvas, protected by safeDrawingPadding)
                if (effectivePlacement == ArcadeDeckPlacement.BOTTOM_STRIP) {
                    DockedBottomConsoleDeck(
                        leftButtons = leftButtons,
                        centerButtons = centerButtons,
                        rightButtons = rightButtons
                    )
                }
            }
        } else {
            // ================= LANDSCAPE LAYOUT =================
            when (viewportMode) {
                ViewportMode.CENTERED_WINGS -> {
                    // MODE A: Centered aspect ratio with dedicated side control wings
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Wing Control Deck
                        if (effectivePlacement == ArcadeDeckPlacement.SPLIT_WINGS && leftButtons.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xC810141D),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF)),
                                modifier = Modifier
                                    .width(108.dp)
                                    .fillMaxHeight()
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    verticalArrangement = Arrangement.SpaceEvenly,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "STEER",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF80DEEA)
                                    )
                                    leftButtons.forEach { btn ->
                                        com.pixie.android.input.TactileArcadeButton(config = btn)
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Center Safe Playfield
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            topStatusBar()

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(
                                        engine.virtualResolution.width.toFloat() /
                                                engine.virtualResolution.height.toFloat()
                                    )
                                    .border(1.dp, Color(0x2200E5FF), RoundedCornerShape(6.dp))
                                    .clip(RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                gameCanvas(Modifier.fillMaxSize())

                                if (effectivePlacement == ArcadeDeckPlacement.FLOATING && floatingButtons.isNotEmpty()) {
                                    FloatingAdjustableDeck(buttons = floatingButtons)
                                }

                                overlays()
                            }

                            // If user specifically picked bottom strip in centered wings mode
                            if (effectivePlacement == ArcadeDeckPlacement.BOTTOM_STRIP) {
                                DockedBottomConsoleDeck(
                                    leftButtons = leftButtons,
                                    centerButtons = centerButtons,
                                    rightButtons = rightButtons,
                                    modifier = Modifier.height(68.dp)
                                )
                            }
                        }

                        // Right Wing Control Deck
                        if (effectivePlacement == ArcadeDeckPlacement.SPLIT_WINGS && rightButtons.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xC810141D),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FF4081)),
                                modifier = Modifier
                                    .width(118.dp)
                                    .fillMaxHeight()
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    verticalArrangement = Arrangement.SpaceEvenly,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "ACTION",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFFF80AB)
                                    )
                                    // Combine center and right actions
                                    (centerButtons + rightButtons).forEach { btn ->
                                        com.pixie.android.input.TactileArcadeButton(config = btn)
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }


                ViewportMode.ADAPTIVE_FIT -> {
                    // MODE C: Dynamic letterboxed fit bounded strictly between Top Status Bar and Bottom Deck
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                    ) {
                        topStatusBar()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            gameCanvas(Modifier.fillMaxSize())

                            if (effectivePlacement == ArcadeDeckPlacement.FLOATING && floatingButtons.isNotEmpty()) {
                                FloatingAdjustableDeck(buttons = floatingButtons)
                            }

                            overlays()
                        }

                        // Docked bottom bar (always placed safely below, never covers carriage)
                        if (effectivePlacement == ArcadeDeckPlacement.BOTTOM_STRIP) {
                            DockedBottomConsoleDeck(
                                leftButtons = leftButtons,
                                centerButtons = centerButtons,
                                rightButtons = rightButtons,
                                modifier = Modifier.height(68.dp)
                            )
                        }
                    }
                }
            }
        }

        // ================= SHOWCASE SETTINGS / INPUT STATUS CHIP =================
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .safeDrawingPadding()
                .padding(end = 8.dp, bottom = if (effectivePlacement == ArcadeDeckPlacement.BOTTOM_STRIP && !isLandscape) 84.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Input Device Indicator Chip
            Surface(
                shape = CircleShape,
                color = Color(0xD910141C),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val inputSource = inputAdapter?.activeInputSource ?: InputSourceType.TOUCH
                    Icon(
                        imageVector = when (inputSource) {
                            InputSourceType.TOUCH -> Icons.Default.TouchApp
                            InputSourceType.GAMEPAD -> Icons.Default.Gamepad
                            InputSourceType.KEYBOARD -> Icons.Default.Keyboard
                        },
                        contentDescription = "Active Input",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = when (inputSource) {
                            InputSourceType.TOUCH -> "TOUCH"
                            InputSourceType.GAMEPAD -> "GAMEPAD"
                            InputSourceType.KEYBOARD -> "KEYS"
                        },
                        color = Color(0xFFE0F7FA),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Quick Showcase Switcher Button
            Surface(
                shape = CircleShape,
                color = Color(0xE600B0FF),
                modifier = Modifier
                    .size(32.dp)
                    .clickable { showShowcaseDialog = true }
                    .testTag("arcade_settings_button"),
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Arcade & Viewport Settings",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ================= SHOWCASE SWITCHER DIALOG =================
        if (showShowcaseDialog) {
            PixieScaffoldShowcaseDialog(
                isLandscape = isLandscape,
                viewportMode = viewportMode,
                onViewportModeChange = { viewportMode = it },
                effectivePlacement = effectivePlacement,
                onSelectPlacement = { placement ->
                    if (isLandscape) userLandscapePlacement = placement
                    else userPortraitPlacement = placement
                },
                inputPreference = inputPreference,
                onInputPreferenceChange = { inputPreference = it },
                onDismiss = { showShowcaseDialog = false }
            )
        }
    }
}
