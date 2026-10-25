package com.pixie.android.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Flexible Arcade Button Row distributing widths proportionally via weights and fixed bounds.
 * Prevents buttons from overflowing or disappearing on narrow mobile displays.
 */
@Composable
fun FlexArcadeButtonRow(
    buttons: List<ArcadeButtonConfig>,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp)
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        buttons.forEach { btn ->
            val childModifier = when {
                btn.weight != null -> {
                    Modifier
                        .weight(btn.weight)
                        .then(
                            if (btn.minWidthDp != null || btn.maxWidthDp != null) {
                                Modifier.widthIn(
                                    min = btn.minWidthDp ?: 0.dp,
                                    max = btn.maxWidthDp ?: Dp.Infinity
                                )
                            } else Modifier
                        )
                }
                btn.widthDp != null -> Modifier.width(btn.widthDp)
                else -> Modifier.width(btn.sizeDp)
            }

            TactileArcadeButton(
                config = btn,
                modifier = childModifier
            )
        }
    }
}

/**
 * Docked bottom horizontal arcade console shelf with flex width distribution.
 */
@Composable
fun DockedBottomConsoleDeck(
    buttons: List<ArcadeButtonConfig>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        color = Color(0xD90E131C),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E5FF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlexArcadeButtonRow(buttons = buttons)
        }
    }
}

@Composable
fun DockedBottomConsoleDeck(
    leftButtons: List<ArcadeButtonConfig> = emptyList(),
    centerButtons: List<ArcadeButtonConfig> = emptyList(),
    rightButtons: List<ArcadeButtonConfig> = emptyList(),
    modifier: Modifier = Modifier
) {
    DockedBottomConsoleDeck(
        buttons = leftButtons + centerButtons + rightButtons,
        modifier = modifier
    )
}

/**
 * Default content layout for lateral wing pillar when custom slot is not provided.
 */
@Composable
private fun ArcadeWingDefaultContent(
    title: String?,
    titleColor: Color,
    header: @Composable (() -> Unit)?,
    buttons: List<ArcadeButtonConfig>,
    footer: @Composable (() -> Unit)?
) {
    if (header != null) {
        header()
    } else if (title != null) {
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = titleColor
        )
    }

    if (buttons.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            buttons.forEach { btn ->
                TactileArcadeButton(config = btn)
            }
        }
    }

    footer?.invoke()
}

/**
 * Single lateral wing pillar for Landscape mode.
 * Houses HUD metrics at top/bottom and tactile arcade buttons.
 */
@Composable
fun ArcadeWingPillar(
    modifier: Modifier = Modifier,
    width: Dp = 120.dp,
    title: String? = null,
    titleColor: Color = Color(0xFF80DEEA),
    borderColor: Color = Color(0x3300E5FF),
    header: @Composable (() -> Unit)? = null,
    buttons: List<ArcadeButtonConfig> = emptyList(),
    footer: @Composable (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xD910141D),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .padding(vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (content != null) {
                content()
            } else {
                ArcadeWingDefaultContent(
                    title = title,
                    titleColor = titleColor,
                    header = header,
                    buttons = buttons,
                    footer = footer
                )
            }
        }
    }
}

/**
 * Split Side Wings Deck for Landscape mode:
 * Left wing houses directional navigation, right wing houses action buttons.
 */
@Composable
fun SplitWingsArcadeDeck(
    leftButtons: List<ArcadeButtonConfig>,
    rightButtons: List<ArcadeButtonConfig>,
    modifier: Modifier = Modifier,
    leftHeader: @Composable (() -> Unit)? = null,
    rightHeader: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArcadeWingPillar(
            title = "STEER",
            titleColor = Color(0xFF80DEEA),
            borderColor = Color(0x3300E5FF),
            buttons = leftButtons,
            header = leftHeader
        )

        Spacer(modifier = Modifier.weight(1f))

        ArcadeWingPillar(
            title = "ACTION",
            titleColor = Color(0xFFFF80AB),
            borderColor = Color(0x33FF4081),
            buttons = rightButtons,
            header = rightHeader
        )
    }
}
