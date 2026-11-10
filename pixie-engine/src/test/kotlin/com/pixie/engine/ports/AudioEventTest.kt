package com.pixie.engine.ports

import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioEventTest {

    @Test
    fun testPlatformerAudioEventsPresence() {
        val platformerEvents = listOf(
            AudioEvent.PLATFORMER_JUMP,
            AudioEvent.PLATFORMER_JUMP_SUPER,
            AudioEvent.COIN_DING,
            AudioEvent.BLOCK_BUMP,
            AudioEvent.BRICK_BREAK,
            AudioEvent.POWERUP_SPAWN,
            AudioEvent.POWERUP_COLLECT,
            AudioEvent.ENEMY_STOMP,
            AudioEvent.CRATE_THROW,
            AudioEvent.STAGE_CLEAR
        )

        for (event in platformerEvents) {
            assertTrue("AudioEvent entries should contain $event", AudioEvent.entries.contains(event))
        }
        assertEquals("Platformer events must be distinct", 10, platformerEvents.distinct().size)
    }

    @Test
    fun testAudioPortReceivesPlatformerEvents() {
        val playedEvents = mutableListOf<AudioEvent>()
        val mockPort = object : AudioPort {
            override var isMuted: Boolean = false
            override fun playSound(event: AudioEvent) {
                if (!isMuted) {
                    playedEvents.add(event)
                }
            }
        }

        mockPort.playSound(AudioEvent.PLATFORMER_JUMP)
        mockPort.playSound(AudioEvent.COIN_DING)
        mockPort.playSound(AudioEvent.ENEMY_STOMP)

        assertEquals(3, playedEvents.size)
        assertEquals(AudioEvent.PLATFORMER_JUMP, playedEvents[0])
        assertEquals(AudioEvent.COIN_DING, playedEvents[1])
        assertEquals(AudioEvent.ENEMY_STOMP, playedEvents[2])

        mockPort.isMuted = true
        mockPort.playSound(AudioEvent.STAGE_CLEAR)
        assertEquals("Muted port should not record sound", 3, playedEvents.size)
    }
}
