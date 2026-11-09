package com.example.raptor

import com.example.raptor.entities.PickupType
import com.example.raptor.entities.RaptorShip
import com.pixie.android.audio.RetroAudioPresets
import com.pixie.android.audio.RetroAudioSynthesizer
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.weapon.ProjectilePool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying audio event triggers in Raptor and the validity of
 * procedural retro arcade synthesized presets in [RetroAudioPresets].
 */
class RaptorAudioPresetsTest {

    private class TestAudioPort : AudioPort {
        val playedEvents = mutableListOf<AudioEvent>()
        override var isMuted: Boolean = false
        override fun playSound(event: AudioEvent) {
            playedEvents.add(event)
        }
    }

    @Test
    fun `all raptor audio events synthesize non-empty valid 16-bit PCM waveforms`() {
        val requiredEvents = listOf(
            AudioEvent.FIRE_PRIMARY,
            AudioEvent.FIRE_MISSILE,
            AudioEvent.EXPLOSION_SMALL,
            AudioEvent.EXPLOSION_LARGE,
            AudioEvent.SHIELD_HIT,
            AudioEvent.HIT,
            AudioEvent.POWERUP_PICKUP,
            AudioEvent.BUTTON_CLICK,
            AudioEvent.SCORE_MILESTONE
        )

        val buffer = ShortArray(RetroAudioSynthesizer.SAMPLE_RATE)
        for (event in requiredEvents) {
            val samplesWritten = RetroAudioPresets.synthesizeEvent(event, buffer, RetroAudioSynthesizer.SAMPLE_RATE)
            assertTrue("Event $event must produce non-zero sample count", samplesWritten > 0)

            var hasNonZero = false
            for (i in 0 until samplesWritten) {
                if (buffer[i].toInt() != 0) {
                    hasNonZero = true
                    break
                }
            }
            assertTrue("Event $event waveform must have audible amplitude", hasNonZero)
        }
    }

    @Test
    fun `shield impact triggers SHIELD_HIT and hull impact triggers HIT`() {
        val audio = TestAudioPort()
        val pool = ProjectilePool(16)
        val ship = RaptorShip(
            initialPosition = Vec2(100f, 200f),
            projectilePool = pool,
            audio = audio
        )
        ship.isInvulnerable = false
        ship.shield = 100f
        ship.hull = 100f

        // Shield hit (40 damage against 100 shield)
        ship.takeDamage(40f, particleLayer = null)
        assertTrue("Audio events should contain SHIELD_HIT", audio.playedEvents.contains(AudioEvent.SHIELD_HIT))
        assertEquals(1, audio.playedEvents.count { it == AudioEvent.SHIELD_HIT })

        // Clear invulnerability and breach shield (100 damage against 60 shield)
        ship.isInvulnerable = false
        audio.playedEvents.clear()
        ship.takeDamage(100f, particleLayer = null)
        assertTrue("Breached shield should trigger SHIELD_HIT", audio.playedEvents.contains(AudioEvent.SHIELD_HIT))
        assertTrue("Breached shield bleed should trigger HIT", audio.playedEvents.contains(AudioEvent.HIT))

        // Bare hull hit with zero shield
        ship.isInvulnerable = false
        audio.playedEvents.clear()
        ship.takeDamage(15f, particleLayer = null)
        assertEquals(listOf(AudioEvent.HIT), audio.playedEvents)
    }

    @Test
    fun `shield recharge and hull repair play POWERUP_PICKUP sound`() {
        val audio = TestAudioPort()
        val pool = ProjectilePool(16)
        val ship = RaptorShip(
            initialPosition = Vec2(100f, 200f),
            projectilePool = pool,
            audio = audio
        )

        ship.rechargeShield(30f)
        assertEquals(listOf(AudioEvent.POWERUP_PICKUP), audio.playedEvents)

        audio.playedEvents.clear()
        ship.repairHull(20f)
        assertEquals(listOf(AudioEvent.POWERUP_PICKUP), audio.playedEvents)
    }

    @Test
    fun `score crossing 5000 point interval triggers SCORE_MILESTONE fanfare`() {
        val audio = TestAudioPort()
        val coordinator = RaptorGameCoordinator(audio = audio)

        assertEquals(0, audio.playedEvents.count { it == AudioEvent.SCORE_MILESTONE })

        // Score 4000 (no milestone yet)
        coordinator.engine.context.score = 4000L
        coordinator.update(0.016f)
        assertEquals(0, audio.playedEvents.count { it == AudioEvent.SCORE_MILESTONE })

        // Score reaches 5200 (crosses 5000 boundary)
        coordinator.engine.context.score = 5200L
        coordinator.update(0.016f)
        assertEquals(1, audio.playedEvents.count { it == AudioEvent.SCORE_MILESTONE })

        // Score reaches 10500 (crosses 10000 boundary)
        coordinator.engine.context.score = 10500L
        coordinator.update(0.016f)
        assertEquals(2, audio.playedEvents.count { it == AudioEvent.SCORE_MILESTONE })
    }

    @Test
    fun `boss defeat triggers victory SCORE_MILESTONE fanfare`() {
        val audio = TestAudioPort()
        val coordinator = RaptorGameCoordinator(audio = audio)

        // Spawn boss
        coordinator.levelDistanceTraveled = 1600f
        coordinator.update(0.016f)
        assertTrue(audio.playedEvents.contains(AudioEvent.SCORE_MILESTONE))

        audio.playedEvents.clear()
        val boss = coordinator.boss
        boss?.takeDamage(2000f, Vec2(120f, 50f), null)
        coordinator.update(0.016f)

        assertTrue("Defeated boss should trigger SCORE_MILESTONE victory fanfare", audio.playedEvents.contains(AudioEvent.SCORE_MILESTONE))
        assertTrue(coordinator.isLevelCompleted)
        assertTrue(coordinator.engine.context.isVictory)
    }
}
