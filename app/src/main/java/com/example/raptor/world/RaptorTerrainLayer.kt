package com.example.raptor.world

import com.example.raptor.entities.RaptorShip
import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.layer.PixieLayer
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.RenderBatchPort
import kotlin.math.sin
import kotlin.random.Random

data class ScrollingIsland(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val sandColor: Int,
    val jungleColor: Int,
    val hasAirstrip: Boolean = false
)

data class ParallaxStar(
    var x: Float,
    var y: Float,
    val speedFactor: Float,
    val size: Float,
    val color: Int
)

/**
 * High-performance procedural vertical scrolling terrain layer for Raptor.
 *
 * Implements [PixieLayer] at order 150, rendering distinct planetary surfaces based on current level:
 * - Level 1: Oceanic surface with wave ripples, sand atolls, and jungle islands.
 * - Level 2: Arid canyon ridges, asphalt roads, and oil refinery conduits.
 * - Level 3: Deep space starfield with 2-tier parallax stars and orbital space station panels.
 */
class RaptorTerrainLayer(
    var currentLevelGetter: () -> RaptorLevel,
    override val order: Int = 150,
    override var isEnabled: Boolean = true,
    override var isGridEnabled: Boolean = false
) : PixieLayer {
    override val name: String = "Raptor_Vertical_Terrain"

    var scrollSpeed: Float = 36f
    private var scrollOffset: Float = 0f
    private var waveTime: Float = 0f

    // Level 1 Islands Pool
    private val islands = mutableListOf<ScrollingIsland>()

    // Level 3 Stars Pool
    private val stars = mutableListOf<ParallaxStar>()

    init {
        // Pre-populate initial islands across the vertical screen
        initIslands()
        initStars()
    }

    private fun initIslands() {
        islands.clear()
        val rng = Random(42)
        var spawnY = -40f
        while (spawnY < 360f) {
            val w = rng.nextFloat() * 40f + 30f
            val h = rng.nextFloat() * 35f + 25f
            val x = rng.nextFloat() * (240f - w - 20f) + 10f
            islands.add(
                ScrollingIsland(
                    x = x,
                    y = spawnY,
                    width = w,
                    height = h,
                    sandColor = 0xFFFFF59D.toInt(),
                    jungleColor = 0xFF43A047.toInt(),
                    hasAirstrip = rng.nextBoolean()
                )
            )
            spawnY += rng.nextFloat() * 80f + 60f
        }
    }

    private fun initStars() {
        stars.clear()
        val rng = Random(1337)
        for (i in 0 until 45) {
            val isNear = rng.nextBoolean()
            stars.add(
                ParallaxStar(
                    x = rng.nextFloat() * 240f,
                    y = rng.nextFloat() * 320f,
                    speedFactor = if (isNear) 0.85f else 0.35f,
                    size = if (isNear) 2f else 1f,
                    color = if (isNear) 0xFFE0F7FA.toInt() else 0xFF78909C.toInt()
                )
            )
        }
    }

    override fun update(dt: Float, context: GameContext) {
        if (!isEnabled) return

        scrollOffset = (scrollOffset + scrollSpeed * dt) % 320f
        waveTime += dt

        val level = currentLevelGetter()

        when (level.levelIndex) {
            1 -> {
                // Update and recycle islands downwards
                for (island in islands) {
                    island.y += scrollSpeed * dt
                    if (island.y > 340f) {
                        island.y = -60f - Random.nextFloat() * 40f
                        island.x = Random.nextFloat() * (240f - island.width - 20f) + 10f
                    }
                }
            }
            3 -> {
                // Update parallax stars
                for (star in stars) {
                    star.y += (scrollSpeed * star.speedFactor) * dt
                    if (star.y > 330f) {
                        star.y = -10f
                        star.x = Random.nextFloat() * 240f
                    }
                }
            }
        }
    }

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        if (!isEnabled) return

        val level = currentLevelGetter()
        val width = 240f
        val height = 320f

        // 1. Base Biome Fill
        batch.fillBackground(level.deepColor)

        when (level.levelIndex) {
            1 -> renderArchipelago(batch, width, height)
            2 -> renderCanyon(batch, width, height)
            3 -> renderOrbitalCitadel(batch, width, height)
            else -> renderArchipelago(batch, width, height)
        }
    }

    private fun renderArchipelago(batch: RenderBatchPort, width: Float, height: Float) {
        // Subtle moving ocean waves (horizontal shimmer stripes)
        val waveStep = 32f
        var waveY = (scrollOffset % waveStep) - waveStep
        while (waveY < height + waveStep) {
            val shimmerX = sin((waveY + waveTime * 40f) * 0.05f) * 15f + 40f
            batch.drawPixelRect(shimmerX, waveY, 50f, 2f, 0xFF1976D2.toInt(), alpha = 0.45f)
            batch.drawPixelRect(shimmerX + 100f, waveY + 12f, 35f, 2f, 0xFF29B6F6.toInt(), alpha = 0.35f)
            waveY += waveStep
        }

        // Render Islands (Sand beaches with lush jungle interior & occasional military airstrip)
        for (island in islands) {
            if (island.y + island.height >= -10f && island.y <= height + 10f) {
                // Sand beach outline
                batch.drawPixelRect(
                    island.x,
                    island.y,
                    island.width,
                    island.height,
                    island.sandColor,
                    alpha = 0.9f
                )
                // Inner green jungle
                val margin = 4f
                if (island.width > margin * 2 && island.height > margin * 2) {
                    batch.drawPixelRect(
                        island.x + margin,
                        island.y + margin,
                        island.width - (margin * 2),
                        island.height - (margin * 2),
                        island.jungleColor,
                        alpha = 1.0f
                    )
                }
                // Optional military asphalt runway on island
                if (island.hasAirstrip && island.height > 20f) {
                    val runwayWidth = 8f
                    batch.drawPixelRect(
                        island.x + (island.width * 0.5f) - (runwayWidth * 0.5f),
                        island.y + 2f,
                        runwayWidth,
                        island.height - 4f,
                        0xFF37474F.toInt(),
                        alpha = 1.0f
                    )
                }
            }
        }
    }

    private fun renderCanyon(batch: RenderBatchPort, width: Float, height: Float) {
        // Red rock canyon ridges along the edges
        val ridgeStep = 40f
        var y = (scrollOffset % ridgeStep) - ridgeStep
        while (y < height + ridgeStep) {
            // Left canyon wall
            val leftWallWidth = 35f + sin((y + scrollOffset) * 0.03f) * 10f
            batch.drawPixelRect(0f, y, leftWallWidth, ridgeStep + 2f, 0xFF5D4037.toInt())
            batch.drawPixelRect(leftWallWidth - 6f, y, 6f, ridgeStep + 2f, 0xFF8D6E63.toInt())

            // Right canyon wall
            val rightWallWidth = 35f + sin((y * 1.2f + scrollOffset) * 0.03f) * 10f
            batch.drawPixelRect(width - rightWallWidth, y, rightWallWidth, ridgeStep + 2f, 0xFF5D4037.toInt())
            batch.drawPixelRect(width - rightWallWidth, y, 6f, ridgeStep + 2f, 0xFF8D6E63.toInt())

            // Center industrial pipeline or highway
            batch.drawPixelRect(114f, y, 12f, ridgeStep + 2f, 0xFF212121.toInt())
            batch.drawPixelRect(118f, y, 4f, ridgeStep + 2f, 0xFFFF9800.toInt(), alpha = 0.6f)

            y += ridgeStep
        }
    }

    private fun renderOrbitalCitadel(batch: RenderBatchPort, width: Float, height: Float) {
        // 2-tier parallax starfield
        for (star in stars) {
            batch.drawPixelRect(star.x, star.y, star.size, star.size, star.color, alpha = 0.85f)
        }

        // Giant space station structural hull girder scrolling on the right
        val girderStep = 80f
        var y = (scrollOffset * 0.5f % girderStep) - girderStep
        while (y < height + girderStep) {
            // Solar panel matrix segment
            batch.drawPixelRect(width - 45f, y, 40f, 65f, 0xFF0D47A1.toInt(), alpha = 0.7f)
            batch.drawPixelRect(width - 45f, y + 20f, 40f, 2f, 0xFF00E5FF.toInt(), alpha = 0.9f)
            batch.drawPixelRect(width - 45f, y + 42f, 40f, 2f, 0xFF00E5FF.toInt(), alpha = 0.9f)
            // Left girder truss
            batch.drawPixelRect(width - 48f, y, 3f, 75f, 0xFF78909C.toInt())
            y += girderStep
        }
    }

    fun reset() {
        scrollOffset = 0f
        waveTime = 0f
        initIslands()
        initStars()
    }
}
