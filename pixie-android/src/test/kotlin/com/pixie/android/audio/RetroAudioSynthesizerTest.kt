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

    @Test
    fun `platformer 8-bit waveform generators produce valid PCM samples`() {
        val buffer = ShortArray(22050)
        val sampleRate = RetroAudioSynthesizer.SAMPLE_RATE

        // Coin Chime
        val coinSamples = RetroAudioPresets.generateCoinChime(buffer, sampleRate)
        assertTrue("Coin chime must write samples", coinSamples > 0)
        assertTrue("Coin chime must not exceed buffer", coinSamples <= buffer.size)

        // Jump Chirp (Normal & Super)
        val jumpSamples = RetroAudioPresets.generateJumpChirp(buffer, sampleRate, isSuper = false)
        assertTrue("Jump chirp must write samples", jumpSamples > 0)
        val superJumpSamples = RetroAudioPresets.generateJumpChirp(buffer, sampleRate, isSuper = true)
        assertTrue("Super jump chirp must write samples", superJumpSamples > jumpSamples)

        // Stomp Thud
        val stompSamples = RetroAudioPresets.generateStompThud(buffer, sampleRate)
        assertTrue("Stomp thud must write samples", stompSamples > 0)

        // Brick Break
        val brickSamples = RetroAudioPresets.generateBrickBreak(buffer, sampleRate)
        assertTrue("Brick break must write samples", brickSamples > 0)

        // Stage Clear Fanfare
        val fanfareSamples = RetroAudioPresets.generateStageClearFanfare(buffer, sampleRate)
        assertTrue("Stage clear fanfare must write samples", fanfareSamples > 0)

        // Block Bump
        val bumpSamples = RetroAudioPresets.generateBlockBump(buffer, sampleRate)
        assertTrue("Block bump must write samples", bumpSamples > 0)

        // Powerup Spawn & Collect
        val spawnSamples = RetroAudioPresets.generatePowerupSpawn(buffer, sampleRate)
        assertTrue("Powerup spawn must write samples", spawnSamples > 0)
        val collectSamples = RetroAudioPresets.generatePowerupCollect(buffer, sampleRate)
        assertTrue("Powerup collect must write samples", collectSamples > 0)
    }

    @Test
    fun `all platformer AudioEvents have corresponding recipes and generate valid PCM WAV buffers`() {
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

        val targetDir = tempFolder.newFolder("platformer_sounds")
        val sampleRate = RetroAudioSynthesizer.SAMPLE_RATE
        val buffer = ShortArray(sampleRate * 2)

        for (event in platformerEvents) {
            val count = RetroAudioPresets.synthesizeEvent(event, buffer, sampleRate)
            assertTrue("Platformer event $event must write > 0 samples", count > 0)
            assertTrue("Platformer event $event must fit in buffer", count <= buffer.size)

            var nonZeroFound = false
            var minVal = Short.MAX_VALUE
            var maxVal = Short.MIN_VALUE
            for (i in 0 until count) {
                val s = buffer[i]
                if (s.toInt() != 0) nonZeroFound = true
                if (s < minVal) minVal = s
                if (s > maxVal) maxVal = s
            }
            assertTrue("Platformer event $event must contain non-zero audio content", nonZeroFound)
            assertTrue("Platformer event $event must have dynamic range", minVal < maxVal)

            val synthesized = RetroAudioSynthesizer.synthesize(event)
            assertEquals("synthesize sample count matches synthesizeEvent for $event", count, synthesized.size)

            val wavFile = File(targetDir, "${event.name.lowercase()}.wav")
            RetroAudioSynthesizer.generateAllSounds(targetDir)
            assertTrue("WAV file for $event must exist", wavFile.exists())
            assertTrue("WAV file for $event must be larger than 44 bytes", wavFile.length() > 44)
            validateWavHeader(wavFile)
        }
    }

    @Test
    fun `platformer waveform generators respect constrained buffer boundaries without out-of-bounds`() {
        val tinyBuffer = ShortArray(64)
        val sampleRate = RetroAudioSynthesizer.SAMPLE_RATE

        assertEquals(64, RetroAudioPresets.generateCoinChime(tinyBuffer, sampleRate))
        assertEquals(64, RetroAudioPresets.generateJumpChirp(tinyBuffer, sampleRate, isSuper = false))
        assertEquals(64, RetroAudioPresets.generateJumpChirp(tinyBuffer, sampleRate, isSuper = true))
        assertEquals(64, RetroAudioPresets.generateStompThud(tinyBuffer, sampleRate))
        assertEquals(64, RetroAudioPresets.generateBrickBreak(tinyBuffer, sampleRate))
        assertEquals(64, RetroAudioPresets.generateBlockBump(tinyBuffer, sampleRate))
        assertEquals(64, RetroAudioPresets.generatePowerupSpawn(tinyBuffer, sampleRate))
        assertEquals(64, RetroAudioPresets.generatePowerupCollect(tinyBuffer, sampleRate))
        assertEquals(64, RetroAudioPresets.generateStageClearFanfare(tinyBuffer, sampleRate))
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
