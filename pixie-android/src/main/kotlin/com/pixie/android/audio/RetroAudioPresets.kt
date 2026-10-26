package com.pixie.android.audio

import com.pixie.engine.core.ports.AudioEvent
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural waveform presets and sound effect catalog for retro arcade audio.
 *
 * Encapsulates frequency tables, envelope decay rates, and algorithmic waveform generators
 * for authentic 8-bit/16-bit sound effects. Separated from [RetroAudioSynthesizer] to isolate
 * creative audio design parameters from low-level PCM audio stream and WAV header encoding.
 */
object RetroAudioPresets {

    /**
     * Synthesizes the waveform for the specified [AudioEvent] into [out].
     *
     * @param event The sound event to synthesize.
     * @param out The output sample buffer.
     * @param sampleRate Audio sample rate in Hz.
     * @return Number of samples written to [out].
     */
    fun synthesizeEvent(event: AudioEvent, out: ShortArray, sampleRate: Int): Int {
        return when (event) {
            AudioEvent.FIRE_PRIMARY -> synthLaserChirp(
                out = out,
                sampleRate = sampleRate,
                duration = 0.09f,
                startFreq = 950f,
                endFreq = 220f
            )
            AudioEvent.FIRE_MISSILE -> synthRocketWhoosh(
                out = out,
                sampleRate = sampleRate,
                duration = 0.22f,
                startFreq = 180f,
                endFreq = 65f
            )
            AudioEvent.EXPLOSION_SMALL -> synthNoiseBlast(
                out = out,
                sampleRate = sampleRate,
                duration = 0.18f,
                decayRate = 22f,
                lowPass = true
            )
            AudioEvent.EXPLOSION_LARGE -> synthHeavyExplosion(
                out = out,
                sampleRate = sampleRate,
                duration = 0.45f
            )
            AudioEvent.HIT -> synthHitCrunch(
                out = out,
                sampleRate = sampleRate,
                duration = 0.12f
            )
            AudioEvent.SHIELD_HIT -> synthShieldDeflect(
                out = out,
                sampleRate = sampleRate,
                duration = 0.14f
            )
            AudioEvent.POWERUP_PICKUP -> synthArpeggio(
                out = out,
                sampleRate = sampleRate,
                frequencies = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f),
                noteDuration = 0.055f
            )
            AudioEvent.COLLECT -> synthArpeggio(
                out = out,
                sampleRate = sampleRate,
                frequencies = floatArrayOf(659.25f, 987.77f),
                noteDuration = 0.07f
            )
            AudioEvent.JUMP -> synthPitchSlide(
                out = out,
                sampleRate = sampleRate,
                duration = 0.15f,
                startFreq = 160f,
                endFreq = 640f,
                isSquare = true
            )
            AudioEvent.DUCK -> synthPitchSlide(
                out = out,
                sampleRate = sampleRate,
                duration = 0.11f,
                startFreq = 320f,
                endFreq = 130f,
                isSquare = false
            )
            AudioEvent.SCORE_MILESTONE -> synthFanfare(out, sampleRate)
            AudioEvent.GAME_OVER -> synthGameOverTune(out, sampleRate)
            AudioEvent.BUTTON_CLICK -> synthButtonClick(out, sampleRate, duration = 0.025f)
            AudioEvent.DAY_NIGHT_SHIFT -> synthAmbientSweep(out, sampleRate, duration = 0.35f)
        }
    }

    /**
     * Classic rapid laser chirp with frequency drop and square wave harmonics.
     */
    fun synthLaserChirp(
        out: ShortArray,
        sampleRate: Int,
        duration: Float,
        startFreq: Float,
        endFreq: Float
    ): Int {
        val count = (duration * sampleRate).toInt().coerceAtMost(out.size)
        val twoPiDt = (2.0 * PI) / sampleRate
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
    fun synthRocketWhoosh(
        out: ShortArray,
        sampleRate: Int,
        duration: Float,
        startFreq: Float,
        endFreq: Float
    ): Int {
        val count = (duration * sampleRate).toInt().coerceAtMost(out.size)
        val twoPiDt = (2.0 * PI) / sampleRate
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
    fun synthNoiseBlast(
        out: ShortArray,
        sampleRate: Int,
        duration: Float,
        decayRate: Float,
        lowPass: Boolean
    ): Int {
        val count = (duration * sampleRate).toInt().coerceAtMost(out.size)
        val invSampleRate = 1.0 / sampleRate
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
    fun synthHeavyExplosion(out: ShortArray, sampleRate: Int, duration: Float): Int {
        val count = (duration * sampleRate).toInt().coerceAtMost(out.size)
        val invSampleRate = 1.0 / sampleRate
        val twoPiDt = (2.0 * PI) / sampleRate
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
    fun synthHitCrunch(out: ShortArray, sampleRate: Int, duration: Float): Int {
        val count = (duration * sampleRate).toInt().coerceAtMost(out.size)
        val invSampleRate = 1.0 / sampleRate
        val twoPiDt = (2.0 * PI) / sampleRate
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
    fun synthShieldDeflect(out: ShortArray, sampleRate: Int, duration: Float): Int {
        val count = (duration * sampleRate).toInt().coerceAtMost(out.size)
        val invSampleRate = 1.0 / sampleRate
        val twoPiDt = (2.0 * PI) / sampleRate
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
    fun synthArpeggio(
        out: ShortArray,
        sampleRate: Int,
        frequencies: FloatArray,
        noteDuration: Float
    ): Int {
        val totalDuration = frequencies.size * noteDuration
        val count = (totalDuration * sampleRate).toInt().coerceAtMost(out.size)
        val noteSamples = (noteDuration * sampleRate).toInt().coerceAtLeast(1)
        val twoPiDt = (2.0 * PI) / sampleRate
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
    fun synthPitchSlide(
        out: ShortArray,
        sampleRate: Int,
        duration: Float,
        startFreq: Float,
        endFreq: Float,
        isSquare: Boolean
    ): Int {
        val count = (duration * sampleRate).toInt().coerceAtMost(out.size)
        val twoPiDt = (2.0 * PI) / sampleRate
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
    fun synthFanfare(out: ShortArray, sampleRate: Int): Int {
        val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f)
        return synthArpeggio(out, sampleRate, notes, noteDuration = 0.085f)
    }

    /**
     * Descending minor game over melody.
     */
    fun synthGameOverTune(out: ShortArray, sampleRate: Int): Int {
        val notes = floatArrayOf(440f, 392f, 349.23f, 261.63f)
        val noteDuration = 0.11f
        val count = (notes.size * noteDuration * sampleRate).toInt().coerceAtMost(out.size)
        val noteSamples = (noteDuration * sampleRate).toInt().coerceAtLeast(1)
        val twoPiDt = (2.0 * PI) / sampleRate
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
    fun synthButtonClick(out: ShortArray, sampleRate: Int, duration: Float): Int {
        val count = (duration * sampleRate).toInt().coerceAtMost(out.size)
        val invSampleRate = 1.0 / sampleRate
        val twoPiDt = (2.0 * PI) / sampleRate
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
    fun synthAmbientSweep(out: ShortArray, sampleRate: Int, duration: Float): Int {
        val count = (duration * sampleRate).toInt().coerceAtMost(out.size)
        val twoPiDt = (2.0 * PI) / sampleRate
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
}
