package com.pixie.engine

import com.pixie.engine.core.PixieEngine
import com.pixie.engine.core.camera.GameViewMode
import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.color.ColorRecoveryPipeline
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.layer.HorizonCoordinator
import com.pixie.engine.core.layer.LayerStack
import com.pixie.engine.core.layer.PixieLayer
import com.pixie.engine.core.loop.PixieGameLoop
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.entity.MotionBlurConfig
import com.pixie.engine.core.entity.ObjectColorMode
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.ports.RenderBatchPort
import com.pixie.engine.core.ports.VirtualResolution
import com.pixie.engine.core.sprite.PixelSprite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PixieEngineTest {

    @Test
    fun testVec2AndAABBCollisions() {
        val v1 = Vec2(10f, 20f)
        val v2 = Vec2(5f, 15f)
        val sum = v1 + v2
        assertEquals(15f, sum.x, 0.001f)
        assertEquals(35f, sum.y, 0.001f)

        val boxA = RectF.fromTopLeft(10f, 10f, 20f, 20f)
        val boxB = RectF.fromTopLeft(25f, 25f, 20f, 20f)
        val boxFar = RectF.fromTopLeft(100f, 100f, 20f, 20f)

        assertTrue("boxA and boxB should intersect", boxA.intersects(boxB))
        assertFalse("boxA and boxFar should not intersect", boxA.intersects(boxFar))
    }

    @Test
    fun testFixedTimeStepLoop() {
        var updateCount = 0
        var renderedAlpha = 0f

        val loop = PixieGameLoop(
            targetUpdatesPerSecond = 60,
            onFixedUpdate = { updateCount++ },
            onRender = { alpha -> renderedAlpha = alpha }
        )

        loop.start()

        // 1/60th second = 0.01667s -> exactly 1 update
        loop.tick(1f / 60f)
        assertEquals(1, updateCount)

        // 2/60th second -> 2 updates
        loop.tick(2f / 60f)
        assertEquals(3, updateCount)
    }

    @Test
    fun testColorRecoveryPipelineOverrides() {
        val pipeline = ColorRecoveryPipeline(initialProgress = 0.25f)

        val defaultObj = PixieObject("obj1")
        assertEquals(0.25f, pipeline.resolveObjectSaturation(defaultObj), 0.001f)

        // Object with AlwaysColor
        val sunObj = PixieObject("sun").apply { colorMode = ObjectColorMode.AlwaysColor }
        assertEquals(1.0f, pipeline.resolveObjectSaturation(sunObj), 0.001f)

        // Class override
        pipeline.setClassOverride("coin", ObjectColorMode.AlwaysColor)
        val coinObj = PixieObject("coin1", initialClasses = setOf("coin"))
        assertEquals(1.0f, pipeline.resolveObjectSaturation(coinObj), 0.001f)

        // Progress progression
        pipeline.addProgress(0.5f)
        assertEquals(0.75f, pipeline.progress, 0.001f)
        assertEquals(0.75f, pipeline.resolveObjectSaturation(defaultObj), 0.001f)
    }

    @Test
    fun testObjectContextParametersAndMotionBlur() {
        val context = GameContext(difficultyLevel = 2.0f, distance = 600f)

        val obj = PixieObject("test").apply {
            parameterCalculator = { o, ctx ->
                o.parameters["speed"] = 100f * ctx.calculateDifficultyMultiplier()
            }
            motionBlur = MotionBlurConfig(enabled = true, strength = 0.8f, maxTrailSamples = 3)
        }

        obj.onContextChanged(context)
        assertTrue(obj.parameters["speed"]!! > 100f)

        // Test motion blur trail history recording
        obj.position = Vec2(10f, 0f)
        obj.update(0.016f, context)
        obj.position = Vec2(20f, 0f)
        obj.update(0.016f, context)

        assertEquals(2, obj.trailHistory.size)
    }

    @Test
    fun testHorizonCoordinationAndLayerStack() {
        val horizon = HorizonCoordinator(horizonY = 120f)
        val stack = LayerStack(horizon = horizon)

        var layer1Updated = false
        var layer2Updated = false

        val layer1 = object : PixieLayer {
            override val name = "Layer1"
            override val order = 100
            override var isEnabled = true
            override var isGridEnabled = false
            override fun update(dt: Float, context: GameContext) { layer1Updated = true }
            override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {}
        }

        val layer2 = object : PixieLayer {
            override val name = "Layer2"
            override val order = 200
            override var isEnabled = false
            override var isGridEnabled = false
            override fun update(dt: Float, context: GameContext) { layer2Updated = true }
            override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {}
        }

        stack.addLayer(layer1)
        stack.addLayer(layer2)

        stack.update(0.016f, GameContext())
        assertTrue("Enabled layer should update", layer1Updated)
        assertFalse("Disabled layer should not update", layer2Updated)
    }

    @Test
    fun testSkyLayerGradualShiftAndCelestialOrbit() {
        val horizon = HorizonCoordinator(horizonY = 135f)
        val sky = com.pixie.engine.core.layer.SkyLayer(
            horizon = horizon,
            celestialCenter = Vec2(160f, 140f),
            celestialRadiusX = 145f,
            celestialRadiusY = 110f,
            riseAngleDeg = 150f,
            setAngleDeg = 8f
        )

        // Test cycle progression update
        sky.cycleProgress = 0.10f
        sky.update(6f, GameContext()) // 6s / 60s = 0.10f progress
        assertEquals(0.20f, sky.cycleProgress, 0.005f)

        // Verify celestial properties are configurable
        sky.celestialCenter = Vec2(170f, 130f)
        assertEquals(170f, sky.celestialCenter.x, 0.001f)
        assertTrue("starsFollowCelestialOrbit should be true by default", sky.starsFollowCelestialOrbit)
        assertEquals(0.95f, sky.starsOrbitSpeedRatio, 0.001f)
        assertTrue("starsMaxRadius should be at least 280f to cover farthest corners", sky.starsMaxRadius >= 280f)
    }

    @Test
    fun testRenderPassesPixelsGapAndInvokesEndFrame() {
        var endFrameInvoked = false
        var batchPixelsGap = -1

        val mockBatch = object : RenderBatchPort {
            override val resolution: VirtualResolution = VirtualResolution(320, 180)
            override var pixelsGap: Int = 0
                set(value) {
                    field = value
                    batchPixelsGap = value
                }
            override var isCurrentLayerGridEnabled: Boolean = false

            override fun fillBackground(color: Int) {}
            override fun drawPixelRect(x: Float, y: Float, width: Float, height: Float, color: Int, saturation: Float, alpha: Float) {}
            override fun drawSprite(sprite: PixelSprite, x: Float, y: Float, saturation: Float, alpha: Float) {}
            override fun drawMotionBlur(sprite: PixelSprite, trail: List<Vec2>, strength: Float, saturation: Float) {}
            override fun drawText(text: String, x: Float, y: Float, color: Int, sizePx: Float) {}
            override fun endFrame() {
                endFrameInvoked = true
            }
        }

        val engine = PixieEngine(pixelsGap = 1)
        engine.render(mockBatch)

        assertEquals(1, batchPixelsGap)
        assertTrue("endFrame should be called at the conclusion of engine.render", endFrameInvoked)
    }

    @Test
    fun testLayerGridModeDefaults() {
        val engine = PixieEngine()

        // By default: Solid for sky, clouds, and ground
        assertFalse("Sky should be solid by default", engine.skyLayer.isGridEnabled)
        assertFalse("Clouds should be solid by default", engine.cloudLayer.isGridEnabled)
        assertFalse("Ground should be solid by default", engine.groundLayer.isGridEnabled)

        // By default: Grid enabled for background, objects & player, and foreground
        assertTrue("Background should have grid enabled by default", engine.backgroundLayer.isGridEnabled)
        assertTrue("Items/player should have grid enabled by default", engine.itemsLayer.isGridEnabled)
        assertTrue("Foreground should have grid enabled by default", engine.foregroundLayer.isGridEnabled)
    }
}
