package com.pixie.android.ui

import android.content.res.Configuration
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import com.pixie.android.input.DualModeInputAdapter
import com.pixie.android.input.FloatingAdjustableDeck
import com.pixie.android.input.InputSourceType
import com.pixie.engine.core.PixieEngine
import com.pixie.engine.core.ports.HapticPort

data class PortraitBreakpoint(val maxWidth: Dp, val layout: PixieScreenLayout)
data class LandscapeBreakpoint(val maxHeight: Dp, val layout: PixieScreenLayout)

/**
 * Fluent, chainable builder for composing game layouts, controller bindings,
 * and responsive breakpoints.
 */
class PixieGame(val engine: PixieEngine) {

    private var inputAdapter: DualModeInputAdapter? = null
    private var hapticPort: HapticPort? = null
    private val portraitBreakpoints = mutableListOf<PortraitBreakpoint>()
    private val landscapeBreakpoints = mutableListOf<LandscapeBreakpoint>()
    private var overlaysContent: (@Composable () -> Unit)? = null
    private val canvasOverlays = mutableListOf<@Composable BoxScope.() -> Unit>()

    private var tapHandler: ((Offset) -> Unit)? = null
    private var dragDeltaHandler: ((Float, Float) -> Unit)? = null
    private var dragRawHandler: ((PointerInputChange, Offset) -> Unit)? = null
    private var frameTickHandler: ((Float) -> Unit)? = null

    fun input(inputAdapter: DualModeInputAdapter) = apply {
        this.inputAdapter = inputAdapter
    }

    fun haptic(hapticPort: HapticPort) = apply {
        this.hapticPort = hapticPort
    }

    /**
     * Registers a tap gesture on the game canvas.
     */
    fun onTap(action: () -> Unit) = apply {
        this.tapHandler = { _ -> action() }
    }

    fun onTapAt(action: (Offset) -> Unit) = apply {
        this.tapHandler = action
    }

    /**
     * Registers drag movement delta (dx, dy) on the game canvas.
     */
    fun onDragDelta(action: (dx: Float, dy: Float) -> Unit) = apply {
        this.dragDeltaHandler = action
    }

    /**
     * Registers full drag gestures with change and drag amount.
     */
    fun onDrag(action: (PointerInputChange, Offset) -> Unit) = apply {
        this.dragRawHandler = action
    }

    /**
     * Registers a display frame update tick callback.
     */
    fun onFrameTick(action: (Float) -> Unit) = apply {
        this.frameTickHandler = action
    }

    /**
     * Attaches an in-game HUD or hint overlay directly inside the game canvas bounds.
     */
    fun canvasOverlay(content: @Composable BoxScope.() -> Unit) = apply {
        this.canvasOverlays.add(content)
    }

    /**
     * Registers a portrait layout definition matching screens up to [maxWidth].
     */
    fun portrait(maxWidth: Dp = Dp.Infinity, layout: PixieScreenLayout) = apply {
        portraitBreakpoints.add(PortraitBreakpoint(maxWidth, layout))
    }

    fun portrait(maxWidth: Dp = Dp.Infinity, builder: PixieScreenLayout.() -> Unit) = apply {
        val layout = PixieScreenLayout().apply(builder)
        portraitBreakpoints.add(PortraitBreakpoint(maxWidth, layout))
    }

    /**
     * Registers a landscape layout definition matching screens up to [maxHeight].
     */
    fun landscape(maxHeight: Dp = Dp.Infinity, layout: PixieScreenLayout) = apply {
        landscapeBreakpoints.add(LandscapeBreakpoint(maxHeight, layout))
    }

    fun landscape(maxHeight: Dp = Dp.Infinity, builder: PixieScreenLayout.() -> Unit) = apply {
        val layout = PixieScreenLayout().apply(builder)
        landscapeBreakpoints.add(LandscapeBreakpoint(maxHeight, layout))
    }

    /**
     * Modals, pause dialogs, and game over overlays.
     */
    fun overlays(content: @Composable () -> Unit) = apply {
        this.overlaysContent = content
    }

    /**
     * Default zero-boilerplate render call using parameterized engine canvas.
     */
    @Composable
    fun Render(modifier: Modifier = Modifier) {
        Render(modifier = modifier) { canvasModifier ->
            var finalModifier = canvasModifier
            if (tapHandler != null) {
                finalModifier = finalModifier.pointerInput(tapHandler) {
                    detectTapGestures(
                        onPress = { offset ->
                            tapHandler?.invoke(offset)
                        }
                    )
                }
            }
            if (dragDeltaHandler != null || dragRawHandler != null) {
                finalModifier = finalModifier.pointerInput(dragDeltaHandler, dragRawHandler) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        dragDeltaHandler?.invoke(dragAmount.x, dragAmount.y)
                        dragRawHandler?.invoke(change, dragAmount)
                    }
                }
            }
            PixieGameCanvas(
                engine = engine,
                inputAdapter = inputAdapter,
                modifier = finalModifier,
                onFrameTick = frameTickHandler
            )
        }
    }

    /**
     * Resolves the active layout based on current orientation and screen dimensions,
     * coordinating safe insets, hardware key interceptors, and non-overlapping slots.
     */
    @Composable
    fun Render(
        modifier: Modifier = Modifier,
        gameCanvas: @Composable (canvasModifier: Modifier) -> Unit
    ) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val screenWidthDp = configuration.screenWidthDp.dp
        val screenHeightDp = configuration.screenHeightDp.dp

        // Resolve active layout matching orientation and breakpoint thresholds
        val activeLayout = remember(isLandscape, screenWidthDp, screenHeightDp) {
            if (!isLandscape) {
                portraitBreakpoints
                    .sortedBy { it.maxWidth }
                    .firstOrNull { screenWidthDp <= it.maxWidth }
                    ?.layout
                    ?: portraitBreakpoints.lastOrNull()?.layout
                    ?: PixieScreenLayout()
            } else {
                landscapeBreakpoints
                    .sortedBy { it.maxHeight }
                    .firstOrNull { screenHeightDp <= it.maxHeight }
                    ?.layout
                    ?: landscapeBreakpoints.lastOrNull()?.layout
                    ?: PixieScreenLayout()
            }
        }

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
                    inputAdapter?.let { adapter ->
                        val nativeEvent = keyEvent.nativeKeyEvent
                        val isDown = nativeEvent.action == KeyEvent.ACTION_DOWN
                        adapter.handlePhysicalKeyEvent(nativeEvent.keyCode, isDown)
                    } ?: false
                }
        ) {
            val hasLateralWings = activeLayout.leftWing != null || activeLayout.rightWing != null

            if (hasLateralWings) {
                // ================= LATERAL WINGS ARCHITECTURE =================
                // 0px wasted vertically! Left/Right wings absorb HUD and buttons.
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Control & HUD Wing
                    activeLayout.leftWing?.invoke()

                    // Center Safe Playfield
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Docked Top Status Bar (if any configured for this layout)
                        activeLayout.topStatusBar?.invoke()

                        // Center Playfield constrained to virtual resolution aspect ratio
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

                            // In-game Canvas overlays
                            canvasOverlays.forEach { overlay ->
                                overlay()
                            }

                            // Overlays inside game canvas
                            activeLayout.topOverlayStatusBar?.invoke()
                            activeLayout.bottomOverlayDeck?.invoke()

                            if (activeLayout.floatingButtons.isNotEmpty()) {
                                FloatingAdjustableDeck(buttons = activeLayout.floatingButtons)
                            }

                            overlaysContent?.invoke()
                        }

                        // Docked Bottom Deck (if any configured)
                        activeLayout.bottomDeck?.invoke()
                    }

                    // Right Control & HUD Wing
                    activeLayout.rightWing?.invoke()
                }
            } else {
                // ================= COLUMN DOCKED ARCHITECTURE =================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    // Docked Top Status Bar
                    activeLayout.topStatusBar?.invoke()

                    // Center Playfield Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        gameCanvas(Modifier.fillMaxSize())

                        // In-game Canvas overlays
                        canvasOverlays.forEach { overlay ->
                            overlay()
                        }

                        // Overlay bars over canvas
                        activeLayout.topOverlayStatusBar?.invoke()
                        activeLayout.bottomOverlayDeck?.invoke()

                        if (activeLayout.floatingButtons.isNotEmpty()) {
                            FloatingAdjustableDeck(buttons = activeLayout.floatingButtons)
                        }

                        overlaysContent?.invoke()
                    }

                    // Docked Bottom Deck
                    activeLayout.bottomDeck?.invoke()
                }
            }

            // ================= ACTIVE INPUT BADGE =================
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .safeDrawingPadding()
                    .padding(end = 8.dp, bottom = 8.dp),
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
                        contentDescription = "Active Input Device",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(11.dp)
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
        }
    }
}
