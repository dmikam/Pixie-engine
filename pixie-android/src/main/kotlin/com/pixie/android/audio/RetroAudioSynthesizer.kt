package com.pixie.android.audio

import com.pixie.engine.core.ports.AudioEvent
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Pure Kotlin procedural retro audio synthesizer for Pixie Engine.
 *
 * Generates 16-bit 22050Hz mono WAV audio files entirely offline and in-memory,
 * replicating authentic vintage 8-bit/16-bit arcade sound effects.
 *
 * Sound design waveforms, envelopes, and frequency definitions are encapsulated
 * within [RetroAudioPresets].
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
                val sampleCount = RetroAudioPresets.synthesizeEvent(event, sampleBuffer, SAMPLE_RATE)
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
        return RetroAudioPresets.synthesizeEvent(event, out, SAMPLE_RATE)
    }

    /**
     * Convenience method returning a newly allocated ShortArray containing synthesized samples.
     * Primarily used for unit testing and offline diagnostics.
     */
    @Synchronized
    fun synthesize(event: AudioEvent): ShortArray {
        val count = RetroAudioPresets.synthesizeEvent(event, sampleBuffer, SAMPLE_RATE)
        return sampleBuffer.copyOf(count)
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
