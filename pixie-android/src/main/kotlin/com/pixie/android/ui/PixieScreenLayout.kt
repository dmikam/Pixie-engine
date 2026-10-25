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
}
