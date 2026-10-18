package com.pixie.android.color

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

/**
 * Creates GPU-accelerated Android ColorMatrix filters driven by the engine's color recovery progress.
 */
object PixieColorMatrixAdapter {

    /**
     * Builds a ColorMatrix for a given saturation level [0.0f..1.0f].
     * At 0.0f, the screen is purely monochrome/ash-gray.
     * At 1.0f, the screen displays full, vibrant colors.
     */
    fun createColorMatrix(saturation: Float): ColorMatrix {
        val sat = saturation.coerceIn(0f, 1f)
        val matrix = ColorMatrix()
        matrix.setSaturation(sat)

        // When nearing full recovery, subtly enhance contrast and warm highlights
        if (sat > 0.8f) {
            val boost = (sat - 0.8f) * 0.15f
            val boostMatrix = ColorMatrix(
                floatArrayOf(
                    1f + boost, 0f, 0f, 0f, 0f,
                    0f, 1f + boost, 0f, 0f, 0f,
                    0f, 0f, 1f + boost, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            matrix.postConcat(boostMatrix)
        }
        return matrix
    }

    fun createColorFilter(saturation: Float): ColorMatrixColorFilter {
        return ColorMatrixColorFilter(createColorMatrix(saturation))
    }
}
