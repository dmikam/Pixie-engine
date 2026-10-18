package com.pixie.android.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.RenderBatchPort
import com.pixie.engine.core.ports.VirtualResolution
import com.pixie.engine.core.sprite.PixelSprite
import kotlin.math.min

/**
 * High-performance Android Canvas adapter for Pixie Engine.
 *
 * Implements nearest-neighbor integer or uniform aspect-ratio scaling from virtual pixel
 * coordinates (e.g. 320x180) to physical display pixels, with configurable [pixelsGap].
 */
class AndroidCanvasRenderer(
    override val resolution: VirtualResolution = VirtualResolution(320, 180),
    override var pixelsGap: Int = 0
) : RenderBatchPort {

    var targetCanvas: Canvas? = null
    var scale: Float = 1.0f
    var offsetX: Float = 0f
    var offsetY: Float = 0f

    private val paint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        isFakeBoldText = true
        style = Paint.Style.FILL
    }

    private val colorFilterCache = mutableMapOf<Int, ColorMatrixColorFilter>()

    fun setupViewport(canvas: Canvas, screenWidth: Float, screenHeight: Float) {
        targetCanvas = canvas
        val scaleX = screenWidth / resolution.width
        val scaleY = screenHeight / resolution.height
        scale = min(scaleX, scaleY)
        offsetX = (screenWidth - resolution.width * scale) * 0.5f
        offsetY = (screenHeight - resolution.height * scale) * 0.5f
    }

    override fun fillBackground(color: Int) {
        val canvas = targetCanvas ?: return
        canvas.drawColor(color)
    }

    override fun drawPixelRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Int,
        saturation: Float,
        alpha: Float
    ) {
        val canvas = targetCanvas ?: return
        paint.color = applySaturationToColor(color, saturation)
        paint.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()

        val rx = offsetX + x * scale
        val ry = offsetY + y * scale
        val rw = width * scale
        val rh = height * scale

        if (pixelsGap <= 0) {
            canvas.drawRect(rx, ry, rx + rw, ry + rh, paint)
        } else {
            // Retro grid gap effect
            val gapPx = pixelsGap.toFloat()
            canvas.drawRect(rx, ry, (rx + rw - gapPx).coerceAtLeast(rx + 1f), (ry + rh - gapPx).coerceAtLeast(ry + 1f), paint)
        }
    }

    override fun drawSprite(
        sprite: PixelSprite,
        x: Float,
        y: Float,
        saturation: Float,
        alpha: Float
    ) {
        val canvas = targetCanvas ?: return
        if (sprite == PixelSprite.EMPTY) return

        val gap = pixelsGap.toFloat()
        paint.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()

        for (py in 0 until sprite.height) {
            val destY = offsetY + (y + py) * scale
            for (px in 0 until sprite.width) {
                val color = sprite.getPixel(px, py)
                if ((color ushr 24) == 0) continue // Skip transparent pixels

                paint.color = applySaturationToColor(color, saturation)
                val destX = offsetX + (x + px) * scale
                val pixelSize = scale

                if (gap <= 0f) {
                    canvas.drawRect(destX, destY, destX + pixelSize, destY + pixelSize, paint)
                } else {
                    canvas.drawRect(destX, destY, destX + pixelSize - gap, destY + pixelSize - gap, paint)
                }
            }
        }
    }

    override fun drawMotionBlur(
        sprite: PixelSprite,
        trail: List<Vec2>,
        strength: Float,
        saturation: Float
    ) {
        if (trail.isEmpty()) return
        val total = trail.size
        for (i in trail.indices) {
            val factor = (i + 1).toFloat() / total
            val alpha = factor * strength * 0.45f
            val pos = trail[i]
            drawSprite(sprite, pos.x, pos.y, saturation, alpha)
        }
    }

    override fun drawText(text: String, x: Float, y: Float, color: Int, sizePx: Float) {
        val canvas = targetCanvas ?: return
        textPaint.color = color
        textPaint.textSize = sizePx * scale
        val destX = offsetX + x * scale
        val destY = offsetY + y * scale
        canvas.drawText(text, destX, destY, textPaint)
    }

    private fun applySaturationToColor(color: Int, saturation: Float): Int {
        if (saturation >= 0.99f) return color
        val a = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        // Standard ITU-R BT.601 luma weights
        val gray = (0.299f * r + 0.587f * g + 0.114f * b)

        val newR = (gray + (r - gray) * saturation).coerceIn(0f, 255f).toInt()
        val newG = (gray + (g - gray) * saturation).coerceIn(0f, 255f).toInt()
        val newB = (gray + (b - gray) * saturation).coerceIn(0f, 255f).toInt()

        return Color.argb(a, newR, newG, newB)
    }
}
