package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.menu.model.GameStatus
import com.example.menu.model.PocGameRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Pixie Engine", appName)
    }

    @Test
    fun `poc game registry contains all four proof of concept games`() {
        val games = PocGameRegistry.games
        assertEquals(4, games.size)

        val dinorun = PocGameRegistry.getById("dinorun")
        assertNotNull(dinorun)
        assertEquals("Dinorun", dinorun?.title)
        assertEquals(GameStatus.PLAYABLE, dinorun?.status)
        assertEquals("Horizontal Scroller", dinorun?.paradigm)

        val raptor = PocGameRegistry.getById("raptor")
        assertNotNull(raptor)
        assertEquals("Raptor", raptor?.title)
        assertEquals(GameStatus.NEXT_UP, raptor?.status)
        assertEquals("Vertical Scroller", raptor?.paradigm)

        val arkanoid = PocGameRegistry.getById("arkanoid")
        assertNotNull(arkanoid)
        assertEquals("Arkanoid", arkanoid?.title)
        assertEquals(GameStatus.PLANNED, arkanoid?.status)
        assertEquals("Fixed Screen", arkanoid?.paradigm)

        val chiki = PocGameRegistry.getById("chiki")
        assertNotNull(chiki)
        assertEquals("Chiki", chiki?.title)
        assertEquals(GameStatus.PLANNED, chiki?.status)
        assertEquals("Classic Platformer", chiki?.paradigm)
    }
}
