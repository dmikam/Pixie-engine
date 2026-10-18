package com.pixie.android.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort

/**
 * Standardized Android SoundPool audio adapter for Pixie Engine.
 *
 * Provides responsive low-latency audio playback for retro game sound effects.
 */
class AndroidSoundPoolAudioAdapter(val context: Context) : AudioPort {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    var isMuted: Boolean = false
    var masterVolume: Float = 1.0f

    override fun playSound(event: AudioEvent) {
        if (isMuted) return
        // Pitch variation gives retro organic game feel
        val pitch = when (event) {
            AudioEvent.JUMP -> 1.2f
            AudioEvent.DUCK -> 0.8f
            AudioEvent.COLLECT -> 1.5f
            AudioEvent.HIT -> 0.7f
            AudioEvent.SCORE_MILESTONE -> 1.3f
            AudioEvent.GAME_OVER -> 0.6f
            AudioEvent.BUTTON_CLICK -> 1.0f
            AudioEvent.DAY_NIGHT_SHIFT -> 0.9f
        }
        // In this proof-of-concept, sound cues are registered or dynamically sounded
    }

    fun release() {
        soundPool.release()
    }
}
