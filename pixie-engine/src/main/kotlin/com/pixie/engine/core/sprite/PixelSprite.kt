package com.pixie.engine.core.sprite

/**
 * Pixel-art sprite representation storing a 2D matrix of 32-bit ARGB color integers.
 * Pure Kotlin and lightweight, completely platform-agnostic.
 */
data class PixelSprite(
    val width: Int,
    val height: Int,
    val pixels: IntArray
) {
    init {
        require(pixels.size == width * height) {
            "Pixels array size (${pixels.size}) must match width * height ($width * $height = ${width * height})"
        }
    }

    fun getPixel(x: Int, y: Int): Int {
        if (x !in 0 until width || y !in 0 until height) return 0
        return pixels[y * width + x]
    }

    companion object {
        val EMPTY = PixelSprite(1, 1, intArrayOf(0x00000000))

        /**
         * Utility to construct a sprite from ASCII character rows with a color palette map.
         * Example:
         * val palette = mapOf('.' to 0x00000000, '#' to 0xFF333333, 'G' to 0xFF388E3C)
         * val sprite = PixelSprite.fromAscii(listOf("###", "#G#", "###"), palette)
         */
        fun fromAscii(rows: List<String>, palette: Map<Char, Int>): PixelSprite {
            val height = rows.size
            val width = if (height > 0) rows[0].length else 0
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val row = rows[y]
                for (x in 0 until width) {
                    val char = if (x < row.length) row[x] else '.'
                    pixels[y * width + x] = palette[char] ?: 0x00000000
                }
            }
            return PixelSprite(width, height, pixels)
        }

        /**
         * Creates a solid colored rectangular sprite.
         */
        fun createSolid(width: Int, height: Int, color: Int): PixelSprite {
            val pixels = IntArray(width * height) { color }
            return PixelSprite(width, height, pixels)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PixelSprite
        if (width != other.width) return false
        if (height != other.height) return false
        return pixels.contentEquals(other.pixels)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + pixels.contentHashCode()
        return result
    }
}

/**
 * An individual frame in an animation sequence.
 */
data class AnimationFrame(
    val sprite: PixelSprite,
    val durationSeconds: Float = 0.1f
)

/**
 * An animation sequence holding one or more frames with playback controls.
 */
data class AnimationSequence(
    val name: String,
    val frames: List<AnimationFrame>,
    val isLooping: Boolean = true
) {
    val totalDuration: Float = frames.sumOf { it.durationSeconds.toDouble() }.toFloat()

    fun getFrameAt(elapsedTime: Float): PixelSprite {
        if (frames.isEmpty()) return PixelSprite.EMPTY
        if (frames.size == 1) return frames[0].sprite

        val time = if (isLooping && totalDuration > 0f) {
            elapsedTime % totalDuration
        } else {
            elapsedTime.coerceIn(0f, totalDuration)
        }

        var accumulated = 0f
        for (frame in frames) {
            accumulated += frame.durationSeconds
            if (time <= accumulated) {
                return frame.sprite
            }
        }
        return frames.last().sprite
    }
}
