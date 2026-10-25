package com.pixie.android.audio

import com.pixie.engine.core.ports.AudioEvent
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.PI
import kotlin.math.asin
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
 *
 * Buffer Allocation Optimization:
 * Uses pre-allocated static synthesis, header, and streaming buffers to avoid
 * memory allocations, heap thrashing, and GC pauses on lower-end mobile devices.
 */
object RetroAudioSynthesizer {

    const val SAMPLE_RATE: Int = 22050
    private const val MAX_SYNTH_SECONDS = 1.0f
    private const val BUFFER_CAPACITY = (SAMPLE_RATE * MAX_SYNTH_SECONDS).toInt()
    private const val STREAM_CHUNK_SIZE = 2048 // 1024 samples = 2048 bytes

    // Shared pre-allocated buffers guarded by @Synchronized to avoid runtime heap allocations
    private val sampleBuffer = ShortArray(BUFFER_CAPACITY)
    private val headerBuffer = ByteArray(44)
    private val chunkBuffer = ByteArray(STREAM_CHUNK_SIZE)

    /**
     * Synthesizes and streams WAV audio files for all supported [AudioEvent] values
     * into the specified directory without creating temporary large heap arrays.
     *
     * @return Map of AudioEvent to absolute File path of the generated WAV file.
     */
    @Synchronized
    fun generateAllSounds(targetDir: File): Map<AudioEvent, File> {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val result = mutableMapOf<AudioEvent, File>()
        for (event in AudioEvent.entries) {
            val file = File(targetDir, "${event.name.lowercase()}.wav")
            // Generate if file doesn't exist or is empty/corrupt
            if (!file.exists() || file.length() < 100) {
                val sampleCount = synthesizeEvent(event, sampleBuffer)
                BufferedOutputStream(FileOutputStream(file), STREAM_CHUNK_SIZE * 2).use { stream ->
                    writeWavToStream(stream, sampleBuffer, sampleCount, SAMPLE_RATE)
                }
            }
            result[event] = file
        }
        return result
    }

    /**
     * Synthesizes an [AudioEvent] directly into a caller-supplied buffer.
     *
     * @return The number of audio samples written into [out].
     */
    fun synthesizeToBuffer(event: AudioEvent, out: ShortArray): Int {
        return synthesizeEvent(event, out)
    }

    /**
     * Convenience method returning a newly allocated ShortArray containing synthesized samples.
     * Primarily used for unit testing and offline diagnostics.
     */
    @Synchronized
    fun synthesize(event: AudioEvent): ShortArray {
        val count = synthesizeEvent(event, sampleBuffer)
        return sampleBuffer.copyOf(count)
    }

    private fun synthesizeEvent(event: AudioEvent, out: ShortArray): Int {
        return when (event) {
            AudioEvent.FIRE_PRIMARY -> synthLaserChirp(out, duration = 0.09f, startFreq = 950f, endFreq = 220f)
            AudioEvent.FIRE_MISSILE -> synthRocketWhoosh(out, duration = 0.22f, startFreq = 180f, endFreq = 65f)
            AudioEvent.EXPLOSION_SMALL -> synthNoiseBlast(out, duration = 0.18f, decayRate = 22f, lowPass = true)
            AudioEvent.EXPLOSION_LARGE -> synthHeavyExplosion(out, duration = 0.45f)
            AudioEvent.HIT -> synthHitCrunch(out, duration = 0.12f)
            AudioEvent.SHIELD_HIT -> synthShieldDeflect(out, duration = 0.14f)
            AudioEvent.POWERUP_PICKUP -> synthArpeggio(
                out = out,
                frequencies = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f),
                noteDuration = 0.055f
            )
            AudioEvent.COLLECT -> synthArpeggio(
                out = out,
                frequencies = floatArrayOf(659.25f, 987.77f),
                noteDuration = 0.07f
            )
            AudioEvent.JUMP -> synthPitchSlide(out, duration = 0.15f, startFreq = 160f, endFreq = 640f, isSquare = true)
            AudioEvent.DUCK -> synthPitchSlide(out, duration = 0.11f, startFreq = 320f, endFreq = 130f, isSquare = false)
            AudioEvent.SCORE_MILESTONE -> synthFanfare(out)
            AudioEvent.GAME_OVER -> synthGameOverTune(out)
            AudioEvent.BUTTON_CLICK -> synthButtonClick(out, duration = 0.025f)
            AudioEvent.DAY_NIGHT_SHIFT -> synthAmbientSweep(out, duration = 0.35f)
        }
    }

    /**
     * Classic rapid laser chirp with frequency drop and square wave harmonics.
     */
    private fun synthLaserChirp(out: ShortArray, duration: Float, startFreq: Float, endFreq: Float): Int {
        val count = (duration * SAMPLE_RATE).toInt().coerceAtMost(out.size)
        val twoPiDt = (2.0 * PI) / SAMPLE_RATE
        var phase = 0.0

        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = startFreq + (endFreq - startFreq) * (progress * progress)
            phase += freq * twoPiDt

            val raw = if (sin(phase) >= 0.0) 0.8 else -0.8
            val envelope = 1.0 - progress
            val sampleVal = (raw * envelope * 24000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sampleVal.toShort()
        }
        return count
    }

    /**
     * Rocket whoosh with low frequency tone + filtered exhaust noise.
     */
    private fun synthRocketWhoosh(out: ShortArray, duration: Float, startFreq: Float, endFreq: Float): Int {
        val count = (duration * SAMPLE_RATE).toInt().coerceAtMost(out.size)
        val twoPiDt = (2.0 * PI) / SAMPLE_RATE
        var phase = 0.0
        val rng = Random(1234)

        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += freq * twoPiDt

            val tone = sin(phase) * 0.6
            val noise = (rng.nextDouble() * 2.0 - 1.0) * 0.4
            val envelope = sin(progress * PI)
            val sampleVal = ((tone + noise) * envelope * 22000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sampleVal.toShort()
        }
        return count
    }

    /**
     * Exponential decay noise blast.
     */
    private fun synthNoiseBlast(out: ShortArray, duration: Float, decayRate: Float, lowPass: Boolean): Int {
        val count = (duration * SAMPLE_RATE).toInt().coerceAtMost(out.size)
        val invSampleRate = 1.0 / SAMPLE_RATE
        val rng = Random(5678)
        var prevNoise = 0.0

        for (i in 0 until count) {
            val t = i * invSampleRate
            val rawNoise = rng.nextDouble() * 2.0 - 1.0
            val noise = if (lowPass) {
                prevNoise = prevNoise * 0.4 + rawNoise * 0.6
                prevNoise
            } else rawNoise

            val envelope = exp(-t * decayRate)
            val sampleVal = (noise * envelope * 26000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sampleVal.toShort()
        }
        return count
    }

    /**
     * Deep thundering explosion with sub-bass punch and distorted crunch.
     */
    private fun synthHeavyExplosion(out: ShortArray, duration: Float): Int {
        val count = (duration * SAMPLE_RATE).toInt().coerceAtMost(out.size)
        val invSampleRate = 1.0 / SAMPLE_RATE
        val twoPiDt = (2.0 * PI) / SAMPLE_RATE
        val rng = Random(9999)
        var lowNoise = 0.0
        var phase = 0.0

        for (i in 0 until count) {
            val t = i * invSampleRate
            val progress = i.toDouble() / count

            val raw = rng.nextDouble() * 2.0 - 1.0
            lowNoise = lowNoise * 0.85 + raw * 0.15

            val freq = 85.0 - (50.0 * progress)
            phase += freq * twoPiDt
            val bass = sin(phase) * 0.55

            val combined = (lowNoise * 0.65 + bass)
            val envelope = exp(-t * 9.0)
            val sampleVal = (combined * envelope * 28000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sampleVal.toShort()
        }
        return count
    }

    /**
     * Metallic hit crunch with short noise burst and decaying saw tone.
     */
    private fun synthHitCrunch(out: ShortArray, duration: Float): Int {
        val count = (duration * SAMPLE_RATE).toInt().coerceAtMost(out.size)
        val invSampleRate = 1.0 / SAMPLE_RATE
        val twoPiDt = (2.0 * PI) / SAMPLE_RATE
        val twoPiInv = 1.0 / (2.0 * PI)
        val rng = Random(42)
        var phase = 0.0

        for (i in 0 until count) {
            val t = i * invSampleRate
            val progress = i.toDouble() / count

            val freq = 140.0 - (60.0 * progress)
            phase += freq * twoPiDt
            val saw = ((phase * twoPiInv) % 1.0) * 2.0 - 1.0
            val noise = (rng.nextDouble() * 2.0 - 1.0) * 0.6

            val envelope = exp(-t * 30.0)
            val sampleVal = ((saw * 0.5 + noise * 0.5) * envelope * 25000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sampleVal.toShort()
        }
        return count
    }

    /**
     * Shield deflection: high-frequency modulated electric zap.
     */
    private fun synthShieldDeflect(out: ShortArray, duration: Float): Int {
        val count = (duration * SAMPLE_RATE).toInt().coerceAtMost(out.size)
        val invSampleRate = 1.0 / SAMPLE_RATE
        val twoPiDt = (2.0 * PI) / SAMPLE_RATE
        val modStep = 85.0 * twoPiDt
        var carrierPhase = 0.0
        var modPhase = 0.0

        for (i in 0 until count) {
            val t = i * invSampleRate
            modPhase += modStep
            val mod = sin(modPhase) * 250.0

            val carrierFreq = (1150.0 + mod - (t * 2000.0)).coerceAtLeast(100.0)
            carrierPhase += carrierFreq * twoPiDt

            val wave = sin(carrierPhase)
            val envelope = exp(-t * 24.0)
            val sampleVal = (wave * envelope * 24000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sampleVal.toShort()
        }
        return count
    }

    /**
     * Ascending harmonic arpeggio (powerups, collectibles).
     */
    private fun synthArpeggio(out: ShortArray, frequencies: FloatArray, noteDuration: Float): Int {
        val totalDuration = frequencies.size * noteDuration
        val count = (totalDuration * SAMPLE_RATE).toInt().coerceAtMost(out.size)
        val noteSamples = (noteDuration * SAMPLE_RATE).toInt().coerceAtLeast(1)
        val twoPiDt = (2.0 * PI) / SAMPLE_RATE
        var phase = 0.0

        for (i in 0 until count) {
            val noteIdx = (i / noteSamples).coerceIn(0, frequencies.size - 1)
            val noteSampleIdx = i % noteSamples
            val noteProgress = noteSampleIdx.toDouble() / noteSamples

            val freq = frequencies[noteIdx].toDouble()
            phase += freq * twoPiDt

            val tone = sin(phase) * 0.75 + sin(phase * 2.0) * 0.25
            val envelope = 1.0 - noteProgress * 0.6
            val sampleVal = (tone * envelope * 22000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sampleVal.toShort()
        }
        return count
    }

    /**
     * Pitch slide for Jump / Duck.
     */
    private fun synthPitchSlide(
        out: ShortArray,
        duration: Float,
        startFreq: Float,
        endFreq: Float,
        isSquare: Boolean
    ): Int {
        val count = (duration * SAMPLE_RATE).toInt().coerceAtMost(out.size)
        val twoPiDt = (2.0 * PI) / SAMPLE_RATE
        var phase = 0.0

        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += freq * twoPiDt

            val wave = if (isSquare) {
                if (sin(phase) >= 0.0) 0.75 else -0.75
            } else {
                sin(phase)
            }
            val envelope = 1.0 - progress * 0.5
            val sampleVal = (wave * envelope * 22000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sampleVal.toShort()
        }
        return count
    }

    /**
     * Triumphant 4-chord fanfare for milestone.
     */
    private fun synthFanfare(out: ShortArray): Int {
        val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f)
        return synthArpeggio(out, notes, noteDuration = 0.085f)
    }

    /**
     * Descending minor game over melody.
     */
    private fun synthGameOverTune(out: ShortArray): Int {
        val notes = floatArrayOf(440f, 392f, 349.23f, 261.63f)
        val noteDuration = 0.11f
        val count = (notes.size * noteDuration * SAMPLE_RATE).toInt().coerceAtMost(out.size)
        val noteSamples = (noteDuration * SAMPLE_RATE).toInt().coerceAtLeast(1)
        val twoPiDt = (2.0 * PI) / SAMPLE_RATE
        val invPi = 1.0 / PI
        var phase = 0.0

        for (i in 0 until count) {
            val noteIdx = (i / noteSamples).coerceIn(0, notes.size - 1)
            val noteSampleIdx = i % noteSamples
            val noteProgress = noteSampleIdx.toDouble() / noteSamples

            val freq = notes[noteIdx].toDouble()
            phase += freq * twoPiDt

            val tri = (2.0 * invPi) * asin(sin(phase))
            val envelope = 1.0 - noteProgress * 0.7
            val sampleVal = (tri * envelope * 24000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sampleVal.toShort()
        }
        return count
    }

    /**
     * Ultra snappy UI button click.
     */
    private fun synthButtonClick(out: ShortArray, duration: Float): Int {
        val count = (duration * SAMPLE_RATE).toInt().coerceAtMost(out.size)
        val invSampleRate = 1.0 / SAMPLE_RATE
        val twoPiDt = (2.0 * PI) / SAMPLE_RATE
        val step = 1400.0 * twoPiDt
        var phase = 0.0

        for (i in 0 until count) {
            val t = i * invSampleRate
            phase += step
            val wave = sin(phase)
            val envelope = exp(-t * 120.0)
            val sampleVal = (wave * envelope * 20000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sampleVal.toShort()
        }
        return count
    }

    /**
     * Ethereal sweep for Day/Night shift.
     */
    private fun synthAmbientSweep(out: ShortArray, duration: Float): Int {
        val count = (duration * SAMPLE_RATE).toInt().coerceAtMost(out.size)
        val twoPiDt = (2.0 * PI) / SAMPLE_RATE
        var phase1 = 0.0
        var phase2 = 0.0

        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val sinProg = sin(progress * PI)
            val freq1 = 440.0 + sinProg * 120.0
            val freq2 = 880.0 + sinProg * 240.0

            phase1 += freq1 * twoPiDt
            phase2 += freq2 * twoPiDt

            val wave = sin(phase1) * 0.5 + sin(phase2) * 0.3
            val envelope = sinProg
            val sampleVal = (wave * envelope * 20000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sampleVal.toShort()
        }
        return count
    }

    /**
     * Streams 16-bit PCM samples into standard 44-byte RIFF/WAVE format without large heap allocations.
     */
    private fun writeWavToStream(
        outStream: OutputStream,
        samples: ShortArray,
        sampleCount: Int,
        sampleRate: Int
    ) {
        val totalAudioLen = sampleCount * 2
        fillWavHeader(headerBuffer, totalAudioLen, sampleRate)
        outStream.write(headerBuffer, 0, 44)

        var sampleIdx = 0
        while (sampleIdx < sampleCount) {
            val chunkSamples = minOf(STREAM_CHUNK_SIZE / 2, sampleCount - sampleIdx)
            var byteIdx = 0
            for (i in 0 until chunkSamples) {
                val v = samples[sampleIdx + i].toInt()
                chunkBuffer[byteIdx++] = (v and 0xff).toByte()
                chunkBuffer[byteIdx++] = ((v shr 8) and 0xff).toByte()
            }
            outStream.write(chunkBuffer, 0, byteIdx)
            sampleIdx += chunkSamples
        }
        outStream.flush()
    }

    /**
     * Populates a 44-byte standard RIFF/WAVE header in-place.
     */
    private fun fillWavHeader(header: ByteArray, totalAudioLen: Int, sampleRate: Int) {
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

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
        header[20] = 1 // PCM format
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
    }
}
