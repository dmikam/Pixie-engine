package com.pixie.engine

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
            override fun update(dt: Float, context: GameContext) { layer1Updated = true }
            override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {}
        }

        val layer2 = object : PixieLayer {
            override val name = "Layer2"
            override val order = 200
            override var isEnabled = false
            override fun update(dt: Float, context: GameContext) { layer2Updated = true }
            override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {}
        }

        stack.addLayer(layer1)
        stack.addLayer(layer2)

        stack.update(0.016f, GameContext())
        assertTrue("Enabled layer should update", layer1Updated)
        assertFalse("Disabled layer should not update", layer2Updated)
    }
}
