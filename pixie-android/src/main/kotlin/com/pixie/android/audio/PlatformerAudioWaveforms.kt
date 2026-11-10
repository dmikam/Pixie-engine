package com.pixie.android.audio

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * 8-Bit NES procedural waveform synthesizers for Super Pixie platformer audio.
 *
 * Implements low-latency, zero-allocation PCM sample generators reproducing authentic
 * NES sound hardware behaviors (2A03 square, triangle, and pseudo-random noise channels).
 */
internal object PlatformerAudioWaveforms {

    /**
     * Iconic NES coin sound: two-tone pure square wave.
     * Note 1: B5 (987.77 Hz) for 50ms.
     * Note 2: E6 (1318.51 Hz) for 280ms with natural exponential decay.
     */
    fun generateCoinChime(out: ShortArray, sampleRate: Int): Int {
        val count1 = (0.05f * sampleRate).toInt()
        val count2 = (0.28f * sampleRate).toInt()
        val totalCount = (count1 + count2).coerceAtMost(out.size)
        val twoPiDt = (2.0 * PI) / sampleRate
        var phase = 0.0

        for (i in 0 until totalCount) {
            val isSecondTone = i >= count1
            val freq = if (isSecondTone) 1318.51 else 987.77
            phase += freq * twoPiDt

            val square = if (sin(phase) >= 0.0) 0.75 else -0.75
            val envelope = if (!isSecondTone) {
                1.0 - (i.toDouble() / count1) * 0.15
            } else {
                val t2 = (i - count1).toDouble() / sampleRate
                exp(-t2 * 10.0)
            }
            val sampleVal = (square * envelope * 24000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sampleVal.toShort()
        }
        return totalCount
    }

    /**
     * 8-bit frequency slide chirp for jumping.
     * Normal: 150Hz to 600Hz in 120ms.
     * Super: 120Hz to 680Hz in 150ms.
     */
    fun generateJumpChirp(out: ShortArray, sampleRate: Int, isSuper: Boolean = false): Int {
        val duration = if (isSuper) 0.15f else 0.12f
        val startFreq = if (isSuper) 120.0 else 150.0
        val endFreq = if (isSuper) 680.0 else 600.0
        val count = (duration * sampleRate).toInt().coerceAtMost(out.size)
        val twoPiDt = (2.0 * PI) / sampleRate
        var phase = 0.0

        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = startFreq + (endFreq - startFreq) * (progress * (2.0 - progress))
            phase += freq * twoPiDt

            val wave = if (sin(phase) >= 0.0) 0.75 else -0.75
            val envelope = 1.0 - progress * 0.4
            val sampleVal = (wave * envelope * 22000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sampleVal.toShort()
        }
        return count
    }

    /**
     * Low triangle wave thud coupled with initial high-frequency noise tap for enemy stomping.
     */
    fun generateStompThud(out: ShortArray, sampleRate: Int): Int {
        val duration = 0.10f
        val count = (duration * sampleRate).toInt().coerceAtMost(out.size)
        val noiseCount = (0.025f * sampleRate).toInt().coerceAtMost(count)
        val twoPiDt = (2.0 * PI) / sampleRate
        val invPi = 1.0 / PI
        val invSampleRate = 1.0 / sampleRate
        var phase = 0.0
        val rng = Random(777)

        for (i in 0 until count) {
            val t = i * invSampleRate
            val progress = i.toDouble() / count
            val freq = 160.0 - progress * 120.0
            phase += freq * twoPiDt

            val tri = (2.0 * invPi) * asin(sin(phase))
            val noise = if (i < noiseCount) {
                val noiseProg = i.toDouble() / noiseCount
                (rng.nextDouble() * 2.0 - 1.0) * (1.0 - noiseProg) * 0.6
            } else {
                0.0
            }

            val envelope = exp(-t * 26.0)
            val sampleVal = ((tri * 0.7 + noise) * envelope * 26000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sampleVal.toShort()
        }
        return count
    }

    /**
     * Band-pass filtered noise burst simulating crumbling masonry / shattering brick blocks.
     */
    fun generateBrickBreak(out: ShortArray, sampleRate: Int): Int {
        val duration = 0.18f
        val count = (duration * sampleRate).toInt().coerceAtMost(out.size)
        val invSampleRate = 1.0 / sampleRate
        val rng = Random(404)
        var hpState = 0.0
        var lpState = 0.0

        for (i in 0 until count) {
            val t = i * invSampleRate
            val rawNoise = rng.nextDouble() * 2.0 - 1.0

            // High-pass filter removes sub-bass rumble
            val hp = rawNoise - hpState
            hpState = hpState * 0.85 + rawNoise * 0.15

            // Low-pass filter rounds off top end for crunch
            lpState = lpState * 0.6 + hp * 0.4

            val envelope = exp(-t * 20.0)
            val sampleVal = (lpState * envelope * 26000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sampleVal.toShort()
        }
        return count
    }

    /**
     * Triumphant 8-bit fanfare arpeggio upon touching flagpole / clearing stage.
     */
    fun generateStageClearFanfare(out: ShortArray, sampleRate: Int): Int {
        val notes = floatArrayOf(
            392.00f, 0.07f, // G4
            523.25f, 0.07f, // C5
            659.25f, 0.07f, // E5
            783.99f, 0.07f, // G5
            1046.50f, 0.10f, // C6
            1318.51f, 0.10f, // E6
            1567.98f, 0.28f  // G6
        )
        var totalSamples = 0
        for (n in 0 until notes.size step 2) {
            totalSamples += (notes[n + 1] * sampleRate).toInt()
        }
        val count = totalSamples.coerceAtMost(out.size)
        val twoPiDt = (2.0 * PI) / sampleRate

        var currentSample = 0
        for (n in 0 until notes.size step 2) {
            val freq = notes[n].toDouble()
            val noteDuration = notes[n + 1]
            val noteSampleCount = (noteDuration * sampleRate).toInt()
            var phase = 0.0

            for (i in 0 until noteSampleCount) {
                if (currentSample >= count) break
                val noteProg = i.toDouble() / noteSampleCount
                phase += freq * twoPiDt

                val square = if (sin(phase) >= 0.0) 0.7 else -0.7
                val undertone = sin(phase * 0.5) * 0.2
                val envelope = 1.0 - (noteProg * 0.45)
                val sampleVal = ((square + undertone) * envelope * 22000.0).toInt().coerceIn(-32767, 32767)
                out[currentSample++] = sampleVal.toShort()
            }
        }
        return currentSample
    }

    /**
     * Low square wave thud with fast decay (~120Hz to 60Hz) for block bonks.
     */
    fun generateBlockBump(out: ShortArray, sampleRate: Int): Int {
        val duration = 0.08f
        val count = (duration * sampleRate).toInt().coerceAtMost(out.size)
        val twoPiDt = (2.0 * PI) / sampleRate
        val invSampleRate = 1.0 / sampleRate
        var phase = 0.0

        for (i in 0 until count) {
            val t = i * invSampleRate
            val progress = i.toDouble() / count
            val freq = 120.0 - progress * 60.0
            phase += freq * twoPiDt

            val square = if (sin(phase) >= 0.0) 0.75 else -0.75
            val envelope = exp(-t * 38.0)
            val sampleVal = (square * envelope * 22000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sampleVal.toShort()
        }
        return count
    }

    /**
     * Multi-tone rising arpeggio (C-E-G-C-E-G) for mushroom / powerup emergence.
     */
    fun generatePowerupSpawn(out: ShortArray, sampleRate: Int): Int {
        val freqs = floatArrayOf(261.63f, 329.63f, 392.00f, 523.25f, 659.25f, 783.99f)
        return RetroAudioPresets.synthArpeggio(out, sampleRate, freqs, noteDuration = 0.045f)
    }

    /**
     * Celebratory flourish when collecting powerups or level badges.
     */
    fun generatePowerupCollect(out: ShortArray, sampleRate: Int): Int {
        val freqs = floatArrayOf(329.63f, 392.00f, 659.25f, 523.25f, 587.33f, 783.99f)
        return RetroAudioPresets.synthArpeggio(out, sampleRate, freqs, noteDuration = 0.05f)
    }
}
