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
        assertEquals(GameStatus.PLAYABLE, raptor?.status)
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

    @Test
    fun `raptor coordinator initializes with continuous difficulty and fires weapons`() {
        val coordinator = com.example.raptor.RaptorGameCoordinator(
            selectedDifficulty = com.pixie.engine.core.context.DifficultyPreset.DIFFICULT,
            currentLevelIndex = 1
        )

        assertEquals(50, coordinator.engine.context.difficulty)
        assertEquals(1, coordinator.currentLevelIndex)
        assertEquals(100f, coordinator.playerShip.hull, 0.001f)
        assertEquals(100f, coordinator.playerShip.shield, 0.001f)

        // Test primary weapon fire
        val bullets = mutableListOf<com.pixie.engine.core.weapon.Projectile>()
        val fired = coordinator.playerShip.firePrimary(coordinator.engine.context, bullets)
        org.junit.Assert.assertTrue("Primary fire should succeed", fired)
        org.junit.Assert.assertTrue("Bullet spawned", bullets.isNotEmpty())

        // Test secondary missiles
        val initialMissiles = coordinator.playerShip.missileSalvoes
        coordinator.fireSecondaryMissiles()
        assertEquals(initialMissiles - 1, coordinator.playerShip.missileSalvoes)
    }

    @Test
    fun `audio and haptic adapters toggle mute and enabled correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioAdapter = com.pixie.android.audio.AndroidSoundPoolAudioAdapter(context)
        val hapticAdapter = com.pixie.android.haptic.AndroidHapticAdapter(context)

        // Initial default states
        org.junit.Assert.assertFalse("Audio should not be muted initially", audioAdapter.isMuted)
        org.junit.Assert.assertTrue("Haptics should be enabled initially", hapticAdapter.isEnabled)

        // Toggle mute
        audioAdapter.isMuted = true
        org.junit.Assert.assertTrue("Audio is now muted", audioAdapter.isMuted)
        audioAdapter.playSound(com.pixie.engine.core.ports.AudioEvent.BUTTON_CLICK) // should not crash

        audioAdapter.isMuted = false
        org.junit.Assert.assertFalse("Audio is unmuted", audioAdapter.isMuted)

        // Toggle haptics
        hapticAdapter.isEnabled = false
        org.junit.Assert.assertFalse("Haptics disabled", hapticAdapter.isEnabled)
        hapticAdapter.trigger(com.pixie.engine.core.ports.HapticFeedbackType.LIGHT) // should safely return

        hapticAdapter.isEnabled = true
        org.junit.Assert.assertTrue("Haptics re-enabled", hapticAdapter.isEnabled)

        audioAdapter.release()
    }

    @Test
    fun `dinorun and raptor pause settings support sound and haptics toggling`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioAdapter = com.pixie.android.audio.AndroidSoundPoolAudioAdapter(context)
        val hapticAdapter = com.pixie.android.haptic.AndroidHapticAdapter(context)
        val inputAdapter = com.pixie.android.input.DualModeInputAdapter()

        val dinoCoordinator = com.example.dinorun.DinoGameCoordinator(
            audioAdapter = audioAdapter,
            hapticAdapter = hapticAdapter,
            inputAdapter = inputAdapter
        )
        assertNotNull(dinoCoordinator.engine.audio)
        assertNotNull(dinoCoordinator.engine.haptics)

        val raptorCoordinator = com.example.raptor.RaptorGameCoordinator(
            audio = audioAdapter,
            haptics = hapticAdapter
        )
        assertNotNull(raptorCoordinator.engine.audio)
        assertNotNull(raptorCoordinator.engine.haptics)

        // Toggle off
        audioAdapter.isMuted = true
        hapticAdapter.isEnabled = false

        assertEquals(true, dinoCoordinator.audioAdapter.isMuted)
        assertEquals(false, dinoCoordinator.hapticAdapter.isEnabled)
        assertEquals(true, raptorCoordinator.audio?.isMuted)
        assertEquals(false, raptorCoordinator.haptics?.isEnabled)

        audioAdapter.release()
    }
}
