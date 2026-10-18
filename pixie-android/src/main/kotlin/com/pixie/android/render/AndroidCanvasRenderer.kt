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
    override var pixelsGap: Int = 0,
    override var isCurrentLayerGridEnabled: Boolean = false
) : RenderBatchPort {

    var targetCanvas: Canvas? = null
    var scale: Float = 1.0f
    var offsetX: Float = 0f
    var offsetY: Float = 0f

    val isGridActive: Boolean
        get() = pixelsGap > 0 && isCurrentLayerGridEnabled

    private val paint = Paint().apply {
        isAntiAlias = true
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

        // Apply global scale factor to magnify 1x1 virtual unit blocks into chunky retro pixels
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
    }

    override fun fillBackground(color: Int) {
        val canvas = targetCanvas ?: return
        paint.color = color
        canvas.drawRect(0f, 0f, resolution.width.toFloat(), resolution.height.toFloat(), paint)
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

        if (isGridActive && height >= 1f) {
            val rows = height.toInt()
            val full2DGrid = pixelsGap >= 2 && width >= 1f

            if (full2DGrid) {
                val cols = width.toInt()
                for (r in 0 until rows) {
                    val py = y + r
                    for (c in 0 until cols) {
                        val px = x + c
                        canvas.drawRect(px, py, px + 1.0f, py + 1.0f, paint)
                    }
                }
                val remH = height - rows
                if (remH > 0.01f) {
                    val py = y + rows
                    for (c in 0 until cols) {
                        val px = x + c
                        canvas.drawRect(px, py, px + 1.0f, py + remH, paint)
                    }
                }
            } else {
                // Authentic Dinorun row-by-row ribbon slicing with exact 1-unit tall ribbons (no overlap).
                // GPU anti-aliasing on fractional coordinates produces hairline sub-pixel rasterization seams.
                for (r in 0 until rows) {
                    val py = y + r
                    canvas.drawRect(x, py, x + width, py + 1.0f, paint)
                }
                val remainder = height - rows
                if (remainder > 0.01f) {
                    canvas.drawRect(x, y + rows, x + width, y + rows + remainder, paint)
                }
            }
        } else {
            // Solid mode (e.g. sky, clouds, ground, or Solid toggle):
            // Add +0.4f overlap (the "smoking gun" from drawParallaxScenery) to completely
            // seal sub-pixel rasterization seams and render a solid, gap-free fill.
            canvas.drawRect(x, y, x + width + 0.4f, y + height + 0.4f, paint)
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

        paint.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()

        if (isGridActive) {
            // Authentic Dinorun rendering:
            // Slices row by row into 1-unit tall ribbons (Size(runLength * pixelSize, pixelSize)) with NO overlap.
            // Under fractional scaling, GPU anti-aliasing naturally produces microscopic
            // sub-pixel boundary seams (hairline transparent bleed) between the rows.
            // Pixels maintain their exact 1.0f virtual unit size (never shrunken).
            val full2DGrid = pixelsGap >= 2

            for (py in 0 until sprite.height) {
                val destY = y + py
                var px = 0
                while (px < sprite.width) {
                    val color = sprite.getPixel(px, py)
                    if ((color ushr 24) == 0) {
                        px++
                        continue
                    }

                    if (full2DGrid) {
                        paint.color = applySaturationToColor(color, saturation)
                        val destX = x + px
                        canvas.drawRect(destX, destY, destX + 1.0f, destY + 1.0f, paint)
                        px++
                    } else {
                        var runLength = 1
                        while (px + runLength < sprite.width && sprite.getPixel(px + runLength, py) == color) {
                            runLength++
                        }

                        paint.color = applySaturationToColor(color, saturation)
                        val destX = x + px
                        canvas.drawRect(destX, destY, destX + runLength.toFloat(), destY + 1.0f, paint)

                        px += runLength
                    }
                }
            }
        } else {
            // Solid mode:
            // Horizontal runs with +0.4f overlap (the "smoking gun" from drawParallaxScenery)
            // to seal the hairline rasterization seams, creating a clean solid pixel body.
            for (py in 0 until sprite.height) {
                val destY = y + py
                var px = 0
                while (px < sprite.width) {
                    val color = sprite.getPixel(px, py)
                    if ((color ushr 24) == 0) {
                        px++
                        continue
                    }
                    var runLength = 1
                    while (px + runLength < sprite.width && sprite.getPixel(px + runLength, py) == color) {
                        runLength++
                    }

                    paint.color = applySaturationToColor(color, saturation)
                    val destX = x + px
                    canvas.drawRect(destX, destY, destX + runLength.toFloat() + 0.4f, destY + 1.4f, paint)

                    px += runLength
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
        textPaint.textSize = sizePx
        canvas.drawText(text, x, y, textPaint)
    }

    override fun endFrame() {
        // Pixel rendering and gaps are applied cleanly in virtual coordinate space
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
