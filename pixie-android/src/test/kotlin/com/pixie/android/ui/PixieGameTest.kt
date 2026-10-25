package com.pixie.android.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixie.android.input.ArcadeActionType
import com.pixie.android.input.ArcadeButtonConfig
import com.pixie.engine.core.PixieEngine
import com.pixie.engine.core.ports.VirtualResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Automated tests for [PixieGame] fluent builder API, responsive breakpoint matching,
 * slot resolution, and arcade layout presets across portrait and landscape form factors.
 */
class PixieGameTest {

    private lateinit var engine: PixieEngine
    private lateinit var game: PixieGame

    @Before
    fun setUp() {
        engine = PixieEngine(virtualResolution = VirtualResolution(320, 240))
        game = PixieGame(engine)
    }

    // =========================================================================
    // 1. FLUENT BUILDER & CHAINABILITY
    // =========================================================================

    @Test
    fun `fluent builder methods return the same PixieGame instance`() {
        var tapped = false
        var dragDeltaX = 0f
        var virtualStartX = 0f
        var tickDelta = 0f

        val chained = game
            .onTap { tapped = true }
            .onDragDelta { dx, _ -> dragDeltaX = dx }
            .onVirtualDragStart { vx, _ -> virtualStartX = vx }
            .onFrameTick { dt -> tickDelta = dt }
            .canvasOverlay { }
            .overlays { }

        assertSame("All builder methods should return PixieGame instance", game, chained)

        // Verify registered handlers are functional
        game.getTapHandler()?.invoke(Offset.Zero)
        assertTrue(tapped)

        game.getDragDeltaHandler()?.invoke(15f, 0f)
        assertEquals(15f, dragDeltaX, 0.001f)

        game.getVirtualDragStartHandler()?.invoke(80f, 120f)
        assertEquals(80f, virtualStartX, 0.001f)

        game.getFrameTickHandler()?.invoke(0.016f)
        assertEquals(0.016f, tickDelta, 0.001f)

        assertEquals(1, game.getCanvasOverlays().size)
        assertNotNull(game.getOverlaysContent())
    }

    // =========================================================================
    // 2. PORTRAIT BREAKPOINT MATCHING
    // =========================================================================

    @Test
    fun `resolveActiveLayout returns default layout when no portrait breakpoints registered`() {
        val layout = game.resolveActiveLayout(
            isLandscape = false,
            screenWidthDp = 360.dp,
            screenHeightDp = 640.dp
        )
        assertNotNull(layout)
        assertNull(layout.topStatusBar)
        assertNull(layout.bottomDeck)
        assertNull(layout.leftWing)
        assertNull(layout.rightWing)
        assertTrue(layout.floatingButtons.isEmpty())
    }

    @Test
    fun `resolveActiveLayout matches single portrait breakpoint for any screen width`() {
        val portraitLayout = PixieScreenLayout().topStatusBar { }
        game.portrait(maxWidth = Dp.Infinity, layout = portraitLayout)

        val compact = game.resolveActiveLayout(false, 360.dp, 640.dp)
        val expanded = game.resolveActiveLayout(false, 1024.dp, 1366.dp)

        assertSame(portraitLayout, compact)
        assertSame(portraitLayout, expanded)
    }

    @Test
    fun `resolveActiveLayout sorts portrait breakpoints and matches smallest satisfying threshold`() {
        val compactLayout = PixieScreenLayout().topStatusBar { }
        val tabletLayout = PixieScreenLayout().topStatusBar { }
        val expandedLayout = PixieScreenLayout().topStatusBar { }

        // Register out-of-order to verify ascending sort behavior
        game.portrait(maxWidth = 840.dp, layout = tabletLayout)
        game.portrait(maxWidth = 400.dp, layout = compactLayout)
        game.portrait(maxWidth = Dp.Infinity, layout = expandedLayout)

        // <= 400.dp should select compactLayout
        val phoneMatch = game.resolveActiveLayout(false, 360.dp, 800.dp)
        assertSame("360dp should match compact breakpoint (<= 400dp)", compactLayout, phoneMatch)

        val boundaryMatch = game.resolveActiveLayout(false, 400.dp, 800.dp)
        assertSame("400dp boundary should match compact breakpoint", compactLayout, boundaryMatch)

        // 401dp - 840dp should select tabletLayout
        val tabletMatch = game.resolveActiveLayout(false, 600.dp, 900.dp)
        assertSame("600dp should match tablet breakpoint (<= 840dp)", tabletLayout, tabletMatch)

        val tabletBoundaryMatch = game.resolveActiveLayout(false, 840.dp, 1000.dp)
        assertSame("840dp boundary should match tablet breakpoint", tabletLayout, tabletBoundaryMatch)

        // > 840dp should select expandedLayout
        val expandedMatch = game.resolveActiveLayout(false, 1024.dp, 1366.dp)
        assertSame("1024dp should match expanded breakpoint", expandedLayout, expandedMatch)
    }

    @Test
    fun `resolveActiveLayout portrait falls back to last defined layout when screen exceeds all bounded thresholds`() {
        val smallLayout = PixieScreenLayout().topStatusBar { }
        val mediumLayout = PixieScreenLayout().topStatusBar { }

        game.portrait(maxWidth = 320.dp, layout = smallLayout)
        game.portrait(maxWidth = 480.dp, layout = mediumLayout)

        val largeScreen = game.resolveActiveLayout(false, 720.dp, 1280.dp)
        assertSame("When no threshold is >= width, fallback to last layout", mediumLayout, largeScreen)
    }

    // =========================================================================
    // 3. LANDSCAPE BREAKPOINT MATCHING
    // =========================================================================

    @Test
    fun `resolveActiveLayout returns default layout when no landscape breakpoints registered`() {
        val layout = game.resolveActiveLayout(
            isLandscape = true,
            screenWidthDp = 800.dp,
            screenHeightDp = 360.dp
        )
        assertNotNull(layout)
        assertNull(layout.leftWing)
        assertNull(layout.rightWing)
    }

    @Test
    fun `resolveActiveLayout sorts landscape breakpoints and matches smallest satisfying height threshold`() {
        val compactLandscape = PixieScreenLayout().leftWing { }
        val expandedLandscape = PixieScreenLayout().leftWing { }

        // Register out-of-order
        game.landscape(maxHeight = Dp.Infinity, layout = expandedLandscape)
        game.landscape(maxHeight = 420.dp, layout = compactLandscape)

        val phoneLandscape = game.resolveActiveLayout(true, 840.dp, 360.dp)
        assertSame("360dp height should match compact landscape (<= 420dp)", compactLandscape, phoneLandscape)

        val boundaryLandscape = game.resolveActiveLayout(true, 840.dp, 420.dp)
        assertSame("420dp boundary should match compact landscape", compactLandscape, boundaryLandscape)

        val tabletLandscape = game.resolveActiveLayout(true, 1280.dp, 800.dp)
        assertSame("800dp height should match expanded landscape", expandedLandscape, tabletLandscape)
    }

    @Test
    fun `resolveActiveLayout landscape falls back to last defined layout when height exceeds bounded thresholds`() {
        val lowHeightLayout = PixieScreenLayout().leftWing { }
        val midHeightLayout = PixieScreenLayout().leftWing { }

        game.landscape(maxHeight = 300.dp, layout = lowHeightLayout)
        game.landscape(maxHeight = 450.dp, layout = midHeightLayout)

        val tallLandscape = game.resolveActiveLayout(true, 1000.dp, 600.dp)
        assertSame("When no threshold is >= height, fallback to last landscape layout", midHeightLayout, tallLandscape)
    }

    // =========================================================================
    // 4. ORIENTATION DISCRIMINATION
    // =========================================================================

    @Test
    fun `orientation flag strictly isolates portrait and landscape breakpoint evaluation`() {
        val portraitLayout = PixieScreenLayout().topStatusBar { }
        val landscapeLayout = PixieScreenLayout().leftWing { }

        game.portrait(maxWidth = Dp.Infinity, layout = portraitLayout)
        game.landscape(maxHeight = Dp.Infinity, layout = landscapeLayout)

        val activeInPortrait = game.resolveActiveLayout(isLandscape = false, 800.dp, 400.dp)
        assertSame("Portrait mode must evaluate portrait breakpoints only", portraitLayout, activeInPortrait)

        val activeInLandscape = game.resolveActiveLayout(isLandscape = true, 800.dp, 400.dp)
        assertSame("Landscape mode must evaluate landscape breakpoints only", landscapeLayout, activeInLandscape)
    }

    // =========================================================================
    // 5. SCREEN LAYOUT PRESETS & SLOT RESOLUTION
    // =========================================================================

    @Test
    fun `arcadeCabinet preset configures docked top bar, bottom deck, and optional floating buttons`() {
        val btn = ArcadeButtonConfig(id = "fire", label = "FIRE", actionType = ArcadeActionType.ACTION_PRIMARY)
        val layout = PixieScreenLayout.arcadeCabinet(
            topBar = { },
            buttons = listOf(btn),
            floatingButtons = listOf(btn)
        )

        assertNotNull("Arcade cabinet must have topStatusBar", layout.topStatusBar)
        assertNotNull("Arcade cabinet must have bottomDeck", layout.bottomDeck)
        assertEquals(1, layout.floatingButtons.size)
        assertEquals("fire", layout.floatingButtons.first().id)

        // Verify docked layout does not activate lateral wings or overlay bars
        assertNull(layout.leftWing)
        assertNull(layout.rightWing)
        assertNull(layout.topOverlayStatusBar)
        assertNull(layout.bottomOverlayDeck)
    }

    @Test
    fun `splitWings preset configures left and right lateral pillars with zero vertical loss`() {
        val steerBtn = ArcadeButtonConfig(id = "left", label = "<", actionType = ArcadeActionType.MOVE_LEFT)
        val fireBtn = ArcadeButtonConfig(id = "shoot", label = "A", actionType = ArcadeActionType.ACTION_PRIMARY)

        val layout = PixieScreenLayout.splitWings(
            leftButtons = listOf(steerBtn),
            rightButtons = listOf(fireBtn)
        )

        assertNotNull("Split wings must have leftWing", layout.leftWing)
        assertNotNull("Split wings must have rightWing", layout.rightWing)

        // Lateral wings guarantee 0 vertical space lost
        assertNull("Docked top status bar should remain null in split wings", layout.topStatusBar)
        assertNull("Docked bottom deck should remain null in split wings", layout.bottomDeck)
    }

    @Test
    fun `fullCanvas preset configures non-blocking overlays and preserves full viewport`() {
        val layout = PixieScreenLayout.fullCanvas(
            topOverlay = { },
            bottomOverlay = { }
        )

        assertNotNull("Full canvas should configure topOverlayStatusBar", layout.topOverlayStatusBar)
        assertNotNull("Full canvas should configure bottomOverlayDeck", layout.bottomOverlayDeck)

        // Viewport is maximized: docked top and bottom bars remain null
        assertNull(layout.topStatusBar)
        assertNull(layout.bottomDeck)
        assertNull(layout.leftWing)
        assertNull(layout.rightWing)
    }

    @Test
    fun `custom PixieScreenLayout fluent builder sets all configured slots independently`() {
        val floatBtn = ArcadeButtonConfig(id = "pause", label = "||", actionType = ArcadeActionType.PAUSE)
        val layout = PixieScreenLayout()
            .topStatusBar { }
            .topOverlayStatusBar { }
            .bottomDeck { }
            .bottomOverlayDeck { }
            .leftWing { }
            .rightWing { }
            .floatingButtons(floatBtn)

        assertNotNull(layout.topStatusBar)
        assertNotNull(layout.topOverlayStatusBar)
        assertNotNull(layout.bottomDeck)
        assertNotNull(layout.bottomOverlayDeck)
        assertNotNull(layout.leftWing)
        assertNotNull(layout.rightWing)
        assertEquals(1, layout.floatingButtons.size)
        assertEquals("pause", layout.floatingButtons.first().id)
    }
}
