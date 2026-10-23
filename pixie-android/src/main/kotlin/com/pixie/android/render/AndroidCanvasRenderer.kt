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
        val colorAlpha = (Color.alpha(color) / 255f) * alpha.coerceIn(0f, 1f)
        paint.alpha = (colorAlpha * 255).toInt()

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
        val canvas = targetCanvas ?: return
        if (trail.isEmpty() || sprite == PixelSprite.EMPTY || strength <= 0.01f) return

        val total = trail.size
        for (i in trail.indices) {
            // Index 0 is the newest position (closest to entity), index (total - 1) is oldest (furthest)
            // Naturally fade to nothing along the trail with an exponential falloff curve:
            val distanceRatio = (i + 1).toFloat() / (total + 1.2f)
            val falloff = (1.0f - distanceRatio).coerceIn(0f, 1f)
            val falloffCurve = falloff * falloff
            val cloneAlpha = (falloffCurve * strength * 0.60f).coerceIn(0f, 1f)
            if (cloneAlpha < 0.015f) continue

            val pos = trail[i]

            // Render ethereal velocity afterimage rather than a duplicate clone
            renderMotionBlurGhost(
                canvas = canvas,
                sprite = sprite,
                x = pos.x,
                y = pos.y,
                ghostIndex = i,
                totalGhosts = total,
                saturation = saturation,
                alpha = cloneAlpha
            )
        }
    }

    private fun renderMotionBlurGhost(
        canvas: Canvas,
        sprite: PixelSprite,
        x: Float,
        y: Float,
        ghostIndex: Int,
        totalGhosts: Int,
        saturation: Float,
        alpha: Float
    ) {
        paint.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()

        for (py in 0 until sprite.height) {
            val destY = y + py
            for (px in 0 until sprite.width) {
                val color = sprite.getPixel(px, py)
                val pixelAlpha = (color ushr 24) and 0xFF
                if (pixelAlpha == 0) continue

                // 1. Natural pixel dissolution along the trail:
                // Lagging ghosts progressively dissolve in density, transforming from solid
                // afterimage into disintegrating speed particles fading into thin air.
                when {
                    ghostIndex == 0 -> {
                        // Nearest ghost: Full silhouette intact
                    }
                    ghostIndex == 1 -> {
                        // Gentle edge softening (drops 20% of pixels in organic scatter)
                        if ((px * 3 + py * 5) % 5 == 0) continue
                    }
                    ghostIndex == 2 -> {
                        // Retro checkerboard dissolution (50% density)
                        if ((px + py) % 2 != 0) continue
                    }
                    else -> {
                        // Sparse dissolving particle mist (25% density)
                        if ((px * 7 + py * 13) % 4 != 0) continue
                    }
                }

                // 2. Stylized ethereal ghost shading ("not being just the same clone"):
                // Unify harsh high-contrast facial accents (e.g. eyes and pupils) into the
                // velocity silhouette so the afterimage behaves as an energetic speed streak.
                val r = (color ushr 16) and 0xFF
                val g = (color ushr 8) and 0xFF
                val b = color and 0xFF
                val luma = 0.299f * r + 0.587f * g + 0.114f * b

                val ghostR: Int
                val ghostG: Int
                val ghostB: Int

                if (luma > 220f) {
                    // Soften stark eye whites into a gentle luminous aura
                    ghostR = (r * 0.50f + 60f).toInt().coerceIn(0, 255)
                    ghostG = (g * 0.65f + 90f).toInt().coerceIn(0, 255)
                    ghostB = (b * 0.50f + 60f).toInt().coerceIn(0, 255)
                } else if (luma < 30f) {
                    // Lift pitch black pupils into soft silhouette shading
                    ghostR = (r + 45).coerceIn(0, 255)
                    ghostG = (g + 80).coerceIn(0, 255)
                    ghostB = (b + 55).coerceIn(0, 255)
                } else {
                    // Ethereal speed aura: older ghosts subtly shift toward cool spectral luminescence
                    val shift = (ghostIndex.toFloat() / (totalGhosts.coerceAtLeast(1))).coerceIn(0f, 1f)
                    ghostR = (r * (1f - shift * 0.2f) + 40f * shift * 0.2f).toInt().coerceIn(0, 255)
                    ghostG = (g * (1f - shift * 0.1f) + 210f * shift * 0.1f).toInt().coerceIn(0, 255)
                    ghostB = (b * (1f - shift * 0.3f) + 180f * shift * 0.3f).toInt().coerceIn(0, 255)
                }

                val ghostColor = Color.argb(pixelAlpha, ghostR, ghostG, ghostB)
                paint.color = applySaturationToColor(ghostColor, saturation)

                val destX = x + px
                if (isGridActive) {
                    canvas.drawRect(destX, destY, destX + 1.0f, destY + 1.0f, paint)
                } else {
                    canvas.drawRect(destX, destY, destX + 1.4f, destY + 1.4f, paint)
                }
            }
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
