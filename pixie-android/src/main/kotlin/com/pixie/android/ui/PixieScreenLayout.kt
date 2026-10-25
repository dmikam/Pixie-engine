package com.pixie.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.pixie.android.input.ArcadeButtonConfig
import com.pixie.android.input.DockedBottomConsoleDeck
import com.pixie.android.input.FlexArcadeButtonRow
import com.pixie.android.input.SplitWingsArcadeDeck

/**
 * Declarative screen layout specification for a specific orientation and breakpoint.
 *
 * Supports both docked (isolated non-overlapping) and overlay (floating over canvas) slots.
 */
class PixieScreenLayout {
    var topStatusBar: (@Composable () -> Unit)? = null
        private set
    var topOverlayStatusBar: (@Composable () -> Unit)? = null
        private set
    var bottomDeck: (@Composable () -> Unit)? = null
        private set
    var bottomOverlayDeck: (@Composable () -> Unit)? = null
        private set
    var leftWing: (@Composable () -> Unit)? = null
        private set
    var rightWing: (@Composable () -> Unit)? = null
        private set
    var floatingButtons: List<ArcadeButtonConfig> = emptyList()
        private set

    /**
     * Top Status Bar docked above the game canvas.
     * Guaranteed to never overlap gameplay elements (e.g. bricks, top hazards).
     */
    fun topStatusBar(content: @Composable () -> Unit) = apply {
        this.topStatusBar = content
    }

    /**
     * Top Status Bar floating directly over the game canvas.
     * Ideal for scroller games (e.g. Dinorun, Raptor) where the sky/background should extend to the edge.
     */
    fun topOverlayStatusBar(content: @Composable () -> Unit) = apply {
        this.topOverlayStatusBar = content
    }

    /**
     * Bottom Action Deck docked below the game canvas.
     * Guaranteed to never overlap gameplay elements (e.g. paddle, player, ground).
     */
    fun bottomDeck(content: @Composable () -> Unit) = apply {
        this.bottomDeck = content
    }

    /**
     * Convenience method to configure a bottom console deck with automatic flex/proportional
     * button distribution so buttons never overflow or push buttons off-screen.
     */
    fun bottomButtons(vararg buttons: ArcadeButtonConfig) = apply {
        bottomButtons(buttons.toList())
    }

    fun bottomButtons(buttons: List<ArcadeButtonConfig>) = apply {
        this.bottomDeck = {
            DockedBottomConsoleDeck(buttons = buttons)
        }
    }

    /**
     * Bottom Deck floating directly over the bottom of the game canvas.
     */
    fun bottomOverlayDeck(content: @Composable () -> Unit) = apply {
        this.bottomOverlayDeck = content
    }

    /**
     * Left lateral control wing for wide / landscape layouts.
     */
    fun leftWing(content: @Composable () -> Unit) = apply {
        this.leftWing = content
    }

    /**
     * Right lateral control wing for wide / landscape layouts.
     */
    fun rightWing(content: @Composable () -> Unit) = apply {
        this.rightWing = content
    }

    /**
     * Convenience to configure both lateral wings with headers and action buttons.
     */
    fun wings(
        leftButtons: List<ArcadeButtonConfig>,
        rightButtons: List<ArcadeButtonConfig>,
        leftHeader: @Composable (() -> Unit)? = null,
        rightHeader: @Composable (() -> Unit)? = null
    ) = apply {
        this.leftWing = {
            com.pixie.android.input.ArcadeWingPillar(
                title = "STEER",
                titleColor = androidx.compose.ui.graphics.Color(0xFF80DEEA),
                borderColor = androidx.compose.ui.graphics.Color(0x3300E5FF),
                buttons = leftButtons,
                header = leftHeader
            )
        }
        this.rightWing = {
            com.pixie.android.input.ArcadeWingPillar(
                title = "ACTION",
                titleColor = androidx.compose.ui.graphics.Color(0xFFFF80AB),
                borderColor = androidx.compose.ui.graphics.Color(0x33FF4081),
                buttons = rightButtons,
                header = rightHeader
            )
        }
    }

    /**
     * On-screen floating draggable buttons.
     */
    fun floatingButtons(vararg buttons: ArcadeButtonConfig) = apply {
        this.floatingButtons = buttons.toList()
    }

    fun floatingButtons(buttons: List<ArcadeButtonConfig>) = apply {
        this.floatingButtons = buttons
    }

    // =========================================================================
    // FLUENT PRESETS FOR ARCADE FORM FACTORS
    // =========================================================================

    /**
     * Preset: Arcade Cabinet.
     * Configures a docked top status bar and a custom docked bottom control deck.
     * Guaranteed never to overlap the letterboxed game viewport.
     */
    fun arcadeCabinet(
        topBar: @Composable () -> Unit,
        bottomDeck: @Composable () -> Unit,
        floatingButtons: List<ArcadeButtonConfig> = emptyList()
    ) = apply {
        this.topStatusBar = topBar
        this.bottomDeck = bottomDeck
        if (floatingButtons.isNotEmpty()) {
            this.floatingButtons = floatingButtons
        }
    }

    /**
     * Preset: Arcade Cabinet with flex console buttons.
     * Automatically distributes buttons horizontally with proportional sizing.
     */
    fun arcadeCabinet(
        topBar: @Composable () -> Unit,
        vararg buttons: ArcadeButtonConfig
    ) = arcadeCabinet(topBar, buttons.toList())

    fun arcadeCabinet(
        topBar: @Composable () -> Unit,
        buttons: List<ArcadeButtonConfig>,
        floatingButtons: List<ArcadeButtonConfig> = emptyList()
    ) = apply {
        this.topStatusBar = topBar
        bottomButtons(buttons)
        if (floatingButtons.isNotEmpty()) {
            this.floatingButtons = floatingButtons
        }
    }

    /**
     * Preset: Split Wings.
     * Flanks the game canvas with left and right lateral pillars in wide/landscape screens,
     * ensuring zero vertical screen space is lost to HUD or buttons.
     */
    fun splitWings(
        leftWing: @Composable () -> Unit,
        rightWing: @Composable () -> Unit,
        floatingButtons: List<ArcadeButtonConfig> = emptyList()
    ) = apply {
        this.leftWing = leftWing
        this.rightWing = rightWing
        if (floatingButtons.isNotEmpty()) {
            this.floatingButtons = floatingButtons
        }
    }

    /**
     * Preset: Split Wings with standard arcade pillars.
     * Configures left steer pillar and right action pillar flanking the center viewport.
     */
    fun splitWings(
        leftButtons: List<ArcadeButtonConfig>,
        rightButtons: List<ArcadeButtonConfig>,
        leftHeader: @Composable (() -> Unit)? = null,
        rightHeader: @Composable (() -> Unit)? = null,
        floatingButtons: List<ArcadeButtonConfig> = emptyList()
    ) = apply {
        wings(leftButtons, rightButtons, leftHeader, rightHeader)
        if (floatingButtons.isNotEmpty()) {
            this.floatingButtons = floatingButtons
        }
    }

    /**
     * Preset: Full Canvas.
     * Maximizes gameplay viewport (ideal for runners like DinoRun, vertical shmups like Raptor,
     * or scrollers) with optional non-blocking HUD overlays and bottom controls.
     */
    fun fullCanvas(
        topOverlay: (@Composable () -> Unit)? = null,
        bottomOverlay: (@Composable () -> Unit)? = null,
        floatingButtons: List<ArcadeButtonConfig> = emptyList()
    ) = apply {
        this.topOverlayStatusBar = topOverlay
        this.bottomOverlayDeck = bottomOverlay
        if (floatingButtons.isNotEmpty()) {
            this.floatingButtons = floatingButtons
        }
    }

    /**
     * Preset: Full Canvas with top overlay HUD and flex console buttons.
     */
    fun fullCanvas(
        topOverlay: (@Composable () -> Unit)? = null,
        vararg buttons: ArcadeButtonConfig
    ) = fullCanvas(topOverlay, buttons.toList())

    fun fullCanvas(
        topOverlay: (@Composable () -> Unit)? = null,
        buttons: List<ArcadeButtonConfig>,
        floatingButtons: List<ArcadeButtonConfig> = emptyList()
    ) = apply {
        this.topOverlayStatusBar = topOverlay
        this.bottomOverlayDeck = {
            DockedBottomConsoleDeck(buttons = buttons)
        }
        if (floatingButtons.isNotEmpty()) {
            this.floatingButtons = floatingButtons
        }
    }

    companion object {
        /**
         * Creates an Arcade Cabinet layout preset with docked top status bar and bottom buttons.
         */
        fun arcadeCabinet(
            topBar: @Composable () -> Unit,
            vararg buttons: ArcadeButtonConfig
        ): PixieScreenLayout = PixieScreenLayout().arcadeCabinet(topBar, *buttons)

        fun arcadeCabinet(
            topBar: @Composable () -> Unit,
            bottomDeck: @Composable () -> Unit,
            floatingButtons: List<ArcadeButtonConfig> = emptyList()
        ): PixieScreenLayout = PixieScreenLayout().arcadeCabinet(topBar, bottomDeck, floatingButtons)

        fun arcadeCabinet(
            topBar: @Composable () -> Unit,
            buttons: List<ArcadeButtonConfig>,
            floatingButtons: List<ArcadeButtonConfig> = emptyList()
        ): PixieScreenLayout = PixieScreenLayout().arcadeCabinet(topBar, buttons, floatingButtons)

        /**
         * Creates a Split Wings layout preset flanking the viewport.
         */
        fun splitWings(
            leftWing: @Composable () -> Unit,
            rightWing: @Composable () -> Unit,
            floatingButtons: List<ArcadeButtonConfig> = emptyList()
        ): PixieScreenLayout = PixieScreenLayout().splitWings(leftWing, rightWing, floatingButtons)

        fun splitWings(
            leftButtons: List<ArcadeButtonConfig>,
            rightButtons: List<ArcadeButtonConfig>,
            leftHeader: @Composable (() -> Unit)? = null,
            rightHeader: @Composable (() -> Unit)? = null,
            floatingButtons: List<ArcadeButtonConfig> = emptyList()
        ): PixieScreenLayout = PixieScreenLayout().splitWings(
            leftButtons,
            rightButtons,
            leftHeader,
            rightHeader,
            floatingButtons
        )

        /**
         * Creates a Full Canvas layout preset with optional overlay HUD and controls.
         */
        fun fullCanvas(
            topOverlay: (@Composable () -> Unit)? = null,
            bottomOverlay: (@Composable () -> Unit)? = null,
            floatingButtons: List<ArcadeButtonConfig> = emptyList()
        ): PixieScreenLayout = PixieScreenLayout().fullCanvas(topOverlay, bottomOverlay, floatingButtons)

        fun fullCanvas(
            topOverlay: (@Composable () -> Unit)? = null,
            vararg buttons: ArcadeButtonConfig
        ): PixieScreenLayout = PixieScreenLayout().fullCanvas(topOverlay, *buttons)

        fun fullCanvas(
            topOverlay: (@Composable () -> Unit)? = null,
            buttons: List<ArcadeButtonConfig>,
            floatingButtons: List<ArcadeButtonConfig> = emptyList()
        ): PixieScreenLayout = PixieScreenLayout().fullCanvas(topOverlay, buttons, floatingButtons)
    }
}
