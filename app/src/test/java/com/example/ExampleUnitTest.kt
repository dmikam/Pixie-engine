package com.example

import androidx.compose.runtime.Composable
import com.pixie.android.input.ArcadeButtonConfig
import com.pixie.android.ui.PixieScreenLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun `pixie screen layout arcadeCabinet preset assigns top and bottom slots`() {
        val dummyTop: @Composable () -> Unit = {}
        val dummyBottom: @Composable () -> Unit = {}
        val btn = ArcadeButtonConfig(id = "action", label = "A")

        val layout = PixieScreenLayout().arcadeCabinet(
            topBar = dummyTop,
            bottomDeck = dummyBottom,
            floatingButtons = listOf(btn)
        )

        assertNotNull(layout.topStatusBar)
        assertNotNull(layout.bottomDeck)
        assertNull(layout.topOverlayStatusBar)
        assertNull(layout.bottomOverlayDeck)
        assertEquals(1, layout.floatingButtons.size)
        assertEquals("action", layout.floatingButtons[0].id)
    }

    @Test
    fun `pixie screen layout splitWings preset assigns left and right wings`() {
        val dummyLeft: @Composable () -> Unit = {}
        val dummyRight: @Composable () -> Unit = {}

        val layout = PixieScreenLayout.splitWings(
            leftWing = dummyLeft,
            rightWing = dummyRight
        )

        assertNotNull(layout.leftWing)
        assertNotNull(layout.rightWing)
        assertNull(layout.topStatusBar)
        assertNull(layout.bottomDeck)
    }

    @Test
    fun `pixie screen layout fullCanvas preset assigns overlay slots without docking`() {
        val dummyTopOverlay: @Composable () -> Unit = {}
        val dummyBottomOverlay: @Composable () -> Unit = {}

        val layout = PixieScreenLayout.fullCanvas(
            topOverlay = dummyTopOverlay,
            bottomOverlay = dummyBottomOverlay
        )

        assertNotNull(layout.topOverlayStatusBar)
        assertNotNull(layout.bottomOverlayDeck)
        assertNull(layout.topStatusBar)
        assertNull(layout.bottomDeck)
    }
}

