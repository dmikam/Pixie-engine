package com.pixie.android.audio

import com.pixie.engine.core.ports.AudioEvent
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pure Kotlin procedural retro audio synthesizer for Pixie Engine.
 *
 * Generates 16-bit 22050Hz mono WAV audio files entirely offline and in-memory,
 * replicating authentic vintage 8-bit/16-bit arcade sound effects:
 * - Square wave laser chirps and sweeps
 * - Filtered noise bursts and thunderous low-frequency explosions
 * - Metallic buzzes, deflections, and shield impacts
 * - Multi-tone ascending victory arpeggios and power-up chimes
 * - Responsive UI clicks and retro jump/duck sound cues
 */
object RetroAudioSynthesizer {

    private const val SAMPLE_RATE = 22050

    /**
     * Synthesizes and writes WAV audio files for all supported [AudioEvent] values
     * into the specified directory.
     *
     * @return Map of AudioEvent to absolute File path of the generated WAV file.
     */
    fun generateAllSounds(targetDir: File): Map<AudioEvent, File> {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val result = mutableMapOf<AudioEvent, File>()
        for (event in AudioEvent.entries) {
            val file = File(targetDir, "${event.name.lowercase()}.wav")
            // Generate if file doesn't exist or is empty
            if (!file.exists() || file.length() < 100) {
                val samples = synthesizeEvent(event)
                val wavBytes = encodeWav(samples, SAMPLE_RATE)
                FileOutputStream(file).use { it.write(wavBytes) }
            }
            result[event] = file
        }
        return result
    }

    private fun synthesizeEvent(event: AudioEvent): ShortArray {
        return when (event) {
            AudioEvent.FIRE_PRIMARY -> synthLaserChirp(duration = 0.09f, startFreq = 950f, endFreq = 220f)
            AudioEvent.FIRE_MISSILE -> synthRocketWhoosh(duration = 0.22f, startFreq = 180f, endFreq = 65f)
            AudioEvent.EXPLOSION_SMALL -> synthNoiseBlast(duration = 0.18f, decayRate = 22f, lowPass = true)
            AudioEvent.EXPLOSION_LARGE -> synthHeavyExplosion(duration = 0.45f)
            AudioEvent.HIT -> synthHitCrunch(duration = 0.12f)
            AudioEvent.SHIELD_HIT -> synthShieldDeflect(duration = 0.14f)
            AudioEvent.POWERUP_PICKUP -> synthArpeggio(
                frequencies = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f),
                noteDuration = 0.055f
            )
            AudioEvent.COLLECT -> synthArpeggio(
                frequencies = floatArrayOf(659.25f, 987.77f),
                noteDuration = 0.07f
            )
            AudioEvent.JUMP -> synthPitchSlide(duration = 0.15f, startFreq = 160f, endFreq = 640f, isSquare = true)
            AudioEvent.DUCK -> synthPitchSlide(duration = 0.11f, startFreq = 320f, endFreq = 130f, isSquare = false)
            AudioEvent.SCORE_MILESTONE -> synthFanfare()
            AudioEvent.GAME_OVER -> synthGameOverTune()
            AudioEvent.BUTTON_CLICK -> synthButtonClick(duration = 0.025f)
            AudioEvent.DAY_NIGHT_SHIFT -> synthAmbientSweep(duration = 0.35f)
        }
    }

    /**
     * Classic rapid laser chirp with frequency drop and square wave harmonics.
     */
    private fun synthLaserChirp(duration: Float, startFreq: Float, endFreq: Float): ShortArray {
        val count = (duration * SAMPLE_RATE).toInt()
        val samples = ShortArray(count)
        var phase = 0.0
        val dt = 1.0 / SAMPLE_RATE

        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = startFreq + (endFreq - startFreq) * (progress * progress)
            phase += 2.0 * PI * freq * dt

            // Square wave with slight smoothing
            val raw = if (sin(phase) >= 0.0) 0.8 else -0.8
            val envelope = (1.0 - progress)
            val sampleVal = (raw * envelope * 24000.0).toInt().coerceIn(-32767, 32767)
            samples[i] = sampleVal.toShort()
        }
        return samples
    }

    /**
     * Rocket whoosh with low frequency tone + filtered exhaust noise.
     */
    private fun synthRocketWhoosh(duration: Float, startFreq: Float, endFreq: Float): ShortArray {
        val count = (duration * SAMPLE_RATE).toInt()
        val samples = ShortArray(count)
        var phase = 0.0
        val dt = 1.0 / SAMPLE_RATE
        val rng = Random(1234)

        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * freq * dt

            val tone = sin(phase) * 0.6
            val noise = (rng.nextDouble() * 2.0 - 1.0) * 0.4
            val envelope = sin(progress * PI) // swells then decays
            val sampleVal = ((tone + noise) * envelope * 22000.0).toInt().coerceIn(-32767, 32767)
            samples[i] = sampleVal.toShort()
        }
        return samples
    }

    /**
     * Exponential decay noise blast.
     */
    private fun synthNoiseBlast(duration: Float, decayRate: Float, lowPass: Boolean): ShortArray {
        val count = (duration * SAMPLE_RATE).toInt()
        val samples = ShortArray(count)
        val rng = Random(5678)
        var prevNoise = 0.0

        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val rawNoise = rng.nextDouble() * 2.0 - 1.0
            val noise = if (lowPass) {
                prevNoise = prevNoise * 0.4 + rawNoise * 0.6
                prevNoise
            } else rawNoise

            val envelope = exp(-t * decayRate)
            val sampleVal = (noise * envelope * 26000.0).toInt().coerceIn(-32767, 32767)
            samples[i] = sampleVal.toShort()
        }
        return samples
    }

    /**
     * Deep thundering explosion with sub-bass punch and distorted crunch.
     */
    private fun synthHeavyExplosion(duration: Float): ShortArray {
        val count = (duration * SAMPLE_RATE).toInt()
        val samples = ShortArray(count)
        val rng = Random(9999)
        var lowNoise = 0.0
        var phase = 0.0
        val dt = 1.0 / SAMPLE_RATE

        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / count

            // Heavy low-pass filtered noise
            val raw = rng.nextDouble() * 2.0 - 1.0
            lowNoise = lowNoise * 0.85 + raw * 0.15

            // Sub-bass frequency drop: 85Hz down to 35Hz
            val freq = 85.0 - (50.0 * progress)
            phase += 2.0 * PI * freq * dt
            val bass = sin(phase) * 0.55

            val combined = (lowNoise * 0.65 + bass)
            val envelope = exp(-t * 9.0)
            val sampleVal = (combined * envelope * 28000.0).toInt().coerceIn(-32767, 32767)
            samples[i] = sampleVal.toShort()
        }
        return samples
    }

    /**
     * Metallic hit crunch with short noise burst and decaying saw tone.
     */
    private fun synthHitCrunch(duration: Float): ShortArray {
        val count = (duration * SAMPLE_RATE).toInt()
        val samples = ShortArray(count)
        val rng = Random(42)
        var phase = 0.0
        val dt = 1.0 / SAMPLE_RATE

        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / count

            val freq = 140.0 - (60.0 * progress)
            phase += 2.0 * PI * freq * dt
            val saw = ((phase / (2.0 * PI)) % 1.0) * 2.0 - 1.0
            val noise = (rng.nextDouble() * 2.0 - 1.0) * 0.6

            val envelope = exp(-t * 30.0)
            val sampleVal = ((saw * 0.5 + noise * 0.5) * envelope * 25000.0).toInt().coerceIn(-32767, 32767)
            samples[i] = sampleVal.toShort()
        }
        return samples
    }

    /**
     * Shield deflection: high-frequency modulated electric zap.
     */
    private fun synthShieldDeflect(duration: Float): ShortArray {
        val count = (duration * SAMPLE_RATE).toInt()
        val samples = ShortArray(count)
        var carrierPhase = 0.0
        var modPhase = 0.0
        val dt = 1.0 / SAMPLE_RATE

        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            modPhase += 2.0 * PI * 85.0 * dt
            val mod = sin(modPhase) * 250.0

            val carrierFreq = 1150.0 + mod - (t * 2000.0)
            carrierPhase += 2.0 * PI * carrierFreq.coerceAtLeast(100.0) * dt

            val wave = sin(carrierPhase)
            val envelope = exp(-t * 24.0)
            val sampleVal = (wave * envelope * 24000.0).toInt().coerceIn(-32767, 32767)
            samples[i] = sampleVal.toShort()
        }
        return samples
    }

    /**
     * Ascending harmonic arpeggio (powerups, collectibles).
     */
    private fun synthArpeggio(frequencies: FloatArray, noteDuration: Float): ShortArray {
        val totalDuration = frequencies.size * noteDuration
        val count = (totalDuration * SAMPLE_RATE).toInt()
        val samples = ShortArray(count)
        val noteSamples = (noteDuration * SAMPLE_RATE).toInt()
        val dt = 1.0 / SAMPLE_RATE

        var currentNote = 0
        var phase = 0.0

        for (i in 0 until count) {
            val noteIdx = (i / noteSamples).coerceIn(0, frequencies.size - 1)
            val noteSampleIdx = i % noteSamples
            val noteProgress = noteSampleIdx.toDouble() / noteSamples

            val freq = frequencies[noteIdx].toDouble()
            phase += 2.0 * PI * freq * dt

            // Sine + subtle octave overtone
            val tone = sin(phase) * 0.75 + sin(phase * 2.0) * 0.25
            val envelope = (1.0 - noteProgress * 0.6)
            val sampleVal = (tone * envelope * 22000.0).toInt().coerceIn(-32767, 32767)
            samples[i] = sampleVal.toShort()
        }
        return samples
    }

    /**
     * Pitch slide for Jump / Duck.
     */
    private fun synthPitchSlide(duration: Float, startFreq: Float, endFreq: Float, isSquare: Boolean): ShortArray {
        val count = (duration * SAMPLE_RATE).toInt()
        val samples = ShortArray(count)
        var phase = 0.0
        val dt = 1.0 / SAMPLE_RATE

        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * freq * dt

            val wave = if (isSquare) {
                if (sin(phase) >= 0.0) 0.75 else -0.75
            } else {
                sin(phase)
            }
            val envelope = (1.0 - progress * 0.5)
            val sampleVal = (wave * envelope * 22000.0).toInt().coerceIn(-32767, 32767)
            samples[i] = sampleVal.toShort()
        }
        return samples
    }

    /**
     * Triumphant 3-chord fanfare for milestone.
     */
    private fun synthFanfare(): ShortArray {
        val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f)
        return synthArpeggio(notes, noteDuration = 0.085f)
    }

    /**
     * Descending minor game over melody.
     */
    private fun synthGameOverTune(): ShortArray {
        val notes = floatArrayOf(440f, 392f, 349.23f, 261.63f)
        val noteDuration = 0.11f
        val count = (notes.size * noteDuration * SAMPLE_RATE).toInt()
        val samples = ShortArray(count)
        val noteSamples = (noteDuration * SAMPLE_RATE).toInt()
        val dt = 1.0 / SAMPLE_RATE

        var phase = 0.0
        for (i in 0 until count) {
            val noteIdx = (i / noteSamples).coerceIn(0, notes.size - 1)
            val noteSampleIdx = i % noteSamples
            val noteProgress = noteSampleIdx.toDouble() / noteSamples

            val freq = notes[noteIdx].toDouble()
            phase += 2.0 * PI * freq * dt

            // Somber triangle wave
            val tri = (2.0 / PI) * kotlin.math.asin(sin(phase))
            val envelope = (1.0 - noteProgress * 0.7)
            val sampleVal = (tri * envelope * 24000.0).toInt().coerceIn(-32767, 32767)
            samples[i] = sampleVal.toShort()
        }
        return samples
    }

    /**
     * Ultra snappy UI button click.
     */
    private fun synthButtonClick(duration: Float): ShortArray {
        val count = (duration * SAMPLE_RATE).toInt()
        val samples = ShortArray(count)
        var phase = 0.0
        val dt = 1.0 / SAMPLE_RATE

        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            phase += 2.0 * PI * 1400.0 * dt
            val wave = sin(phase)
            val envelope = exp(-t * 120.0)
            val sampleVal = (wave * envelope * 20000.0).toInt().coerceIn(-32767, 32767)
            samples[i] = sampleVal.toShort()
        }
        return samples
    }

    /**
     * Ethereal sweep for Day/Night shift.
     */
    private fun synthAmbientSweep(duration: Float): ShortArray {
        val count = (duration * SAMPLE_RATE).toInt()
        val samples = ShortArray(count)
        var phase1 = 0.0
        var phase2 = 0.0
        val dt = 1.0 / SAMPLE_RATE

        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq1 = 440.0 + sin(progress * PI) * 120.0
            val freq2 = 880.0 + sin(progress * PI) * 240.0

            phase1 += 2.0 * PI * freq1 * dt
            phase2 += 2.0 * PI * freq2 * dt

            val wave = sin(phase1) * 0.5 + sin(phase2) * 0.3
            val envelope = sin(progress * PI) // swell and fade
            val sampleVal = (wave * envelope * 20000.0).toInt().coerceIn(-32767, 32767)
            samples[i] = sampleVal.toShort()
        }
        return samples
    }

    /**
     * Encodes 16-bit PCM samples into standard 44-byte RIFF/WAVE byte array.
     */
    private fun encodeWav(samples: ShortArray, sampleRate: Int): ByteArray {
        val totalAudioLen = samples.size * 2
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte() // Block align
        header[33] = 0
        header[34] = 16 // Bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        val out = ByteArray(44 + totalAudioLen)
        System.arraycopy(header, 0, out, 0, 44)
        var byteIdx = 44
        for (s in samples) {
            val v = s.toInt()
            out[byteIdx++] = (v and 0xff).toByte()
            out[byteIdx++] = ((v shr 8) and 0xff).toByte()
        }
        return out
    }
}
