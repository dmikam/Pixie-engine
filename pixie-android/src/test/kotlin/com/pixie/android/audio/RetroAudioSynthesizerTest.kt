package com.pixie.android.audio

import com.pixie.engine.core.ports.AudioEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream

/**
 * Unit tests verifying [RetroAudioSynthesizer] buffer-optimized sound synthesis,
 * file encoding, valid RIFF/WAVE header generation, and sample validity.
 */
class RetroAudioSynthesizerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `generateAllSounds generates valid WAV files for all AudioEvents`() {
        val targetDir = tempFolder.newFolder("test_sounds")
        val generatedMap = RetroAudioSynthesizer.generateAllSounds(targetDir)

        assertEquals(
            "Every AudioEvent entry should have a corresponding WAV file",
            AudioEvent.entries.size,
            generatedMap.size
        )

        for (event in AudioEvent.entries) {
            val file = generatedMap[event]
            assertTrue("File for $event must exist", file != null && file.exists())
            assertTrue("File for $event must be larger than 44 bytes (header + samples)", file!!.length() > 44)

            // Validate WAV header format
            validateWavHeader(file)
        }
    }

    @Test
    fun `synthesize produces non-empty audio samples within 16-bit PCM amplitude limits`() {
        for (event in AudioEvent.entries) {
            val samples = RetroAudioSynthesizer.synthesize(event)
            assertTrue("Synthesized samples for $event must not be empty", samples.isNotEmpty())

            var hasNonZero = false
            for (sample in samples) {
                if (sample.toInt() != 0) {
                    hasNonZero = true
                }
                assertTrue(
                    "Sample value must be within 16-bit range",
                    sample in -32768..32767
                )
            }
            assertTrue("Audio waveform for $event must contain non-zero audio content", hasNonZero)
        }
    }

    @Test
    fun `synthesizeToBuffer respects buffer capacity and returns accurate sample count`() {
        val testBuffer = ShortArray(16384)
        for (event in AudioEvent.entries) {
            val writtenCount = RetroAudioSynthesizer.synthesizeToBuffer(event, testBuffer)
            assertTrue("Written sample count must be positive for $event", writtenCount > 0)
            assertTrue("Written sample count must fit in buffer", writtenCount <= testBuffer.size)
        }
    }

    @Test
    fun `generateAllSounds skips re-generation when valid files already exist`() {
        val targetDir = tempFolder.newFolder("cached_sounds")
        val firstRun = RetroAudioSynthesizer.generateAllSounds(targetDir)
        val firstModifiedTimestamps = firstRun.mapValues { it.value.lastModified() }

        // Second run should reuse existing files without overwriting
        val secondRun = RetroAudioSynthesizer.generateAllSounds(targetDir)
        for ((event, file) in secondRun) {
            assertEquals(
                "File timestamp should remain identical when cached for $event",
                firstModifiedTimestamps[event],
                file.lastModified()
            )
        }
    }

    private fun validateWavHeader(file: File) {
        val bytes = ByteArray(44)
        FileInputStream(file).use { stream ->
            val read = stream.read(bytes)
            assertEquals("Header must be 44 bytes", 44, read)
        }

        // RIFF descriptor
        assertEquals('R'.code.toByte(), bytes[0])
        assertEquals('I'.code.toByte(), bytes[1])
        assertEquals('F'.code.toByte(), bytes[2])
        assertEquals('F'.code.toByte(), bytes[3])

        // WAVE fmt chunk
        assertEquals('W'.code.toByte(), bytes[8])
        assertEquals('A'.code.toByte(), bytes[9])
        assertEquals('V'.code.toByte(), bytes[10])
        assertEquals('E'.code.toByte(), bytes[11])
        assertEquals('f'.code.toByte(), bytes[12])
        assertEquals('m'.code.toByte(), bytes[13])
        assertEquals('t'.code.toByte(), bytes[14])
        assertEquals(' '.code.toByte(), bytes[15])

        // Audio format PCM (1)
        val format = (bytes[20].toInt() and 0xFF) or ((bytes[21].toInt() and 0xFF) shl 8)
        assertEquals(1, format)

        // Channels (1 = mono)
        val channels = (bytes[22].toInt() and 0xFF) or ((bytes[23].toInt() and 0xFF) shl 8)
        assertEquals(1, channels)

        // Sample rate (22050)
        val sampleRate = (bytes[24].toInt() and 0xFF) or
            ((bytes[25].toInt() and 0xFF) shl 8) or
            ((bytes[26].toInt() and 0xFF) shl 16) or
            ((bytes[27].toInt() and 0xFF) shl 24)
        assertEquals(RetroAudioSynthesizer.SAMPLE_RATE, sampleRate)

        // 'data' chunk marker
        assertEquals('d'.code.toByte(), bytes[36])
        assertEquals('a'.code.toByte(), bytes[37])
        assertEquals('t'.code.toByte(), bytes[38])
        assertEquals('a'.code.toByte(), bytes[39])
    }
}
