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
        assertEquals(1.0f, obj.trailFadeAlpha, 0.001f)

        // Test smooth fade out when disabled (does not abruptly vanish)
        obj.motionBlur = MotionBlurConfig(enabled = false)
        obj.update(0.05f, context)
        assertTrue("Trail alpha should decay gracefully", obj.trailFadeAlpha < 1.0f && obj.trailFadeAlpha > 0f)
        assertTrue("Trail history remains during fade-out", obj.trailHistory.isNotEmpty())

        // Update until fully faded out to nothing
        obj.update(0.25f, context)
        assertEquals(0, obj.trailHistory.size)
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

    @Test
    fun testContinuousIntegerDifficultyAndPresets() {
        val context = GameContext()
        // Default is 25 (Medium)
        assertEquals(25, context.difficulty)
        assertEquals(0.25f, context.normalizedDifficulty, 0.001f)
        assertEquals(1.0f, context.difficultyLevel, 0.001f)

        // Clamping to 0..100
        context.difficulty = 150
        assertEquals(100, context.difficulty)
        assertEquals(1.0f, context.normalizedDifficulty, 0.001f)

        context.difficulty = -50
        assertEquals(0, context.difficulty)
        assertEquals(0.0f, context.normalizedDifficulty, 0.001f)

        // Presets verification
        assertEquals(10, com.pixie.engine.core.context.DifficultyPreset.EASY.difficultyValue)
        assertEquals(25, com.pixie.engine.core.context.DifficultyPreset.MEDIUM.difficultyValue)
        assertEquals(50, com.pixie.engine.core.context.DifficultyPreset.DIFFICULT.difficultyValue)
        assertEquals(100, com.pixie.engine.core.context.DifficultyPreset.HEAVY_METAL.difficultyValue)

        context.difficulty = com.pixie.engine.core.context.DifficultyPreset.HEAVY_METAL.difficultyValue
        assertEquals(100, context.difficulty)
        assertEquals(4.0f, context.difficultyLevel, 0.001f)
    }

    @Test
    fun testDynamicPropertyCalculationWithContextFactors() {
        val context = GameContext(
            currentLevel = 1,
            livesLeft = 3
        ).apply {
            difficulty = 25 // standard baseline (0.25 normalized)
        }

        val baseSpeed = 100f
        val calculatedStandard = context.calculateDynamicProperty(baseSpeed)
        assertEquals(100f, calculatedStandard, 0.01f)

        // Scaling with higher difficulty (e.g. Heavy Metal = 100 -> diffRatio = 1.0)
        context.difficulty = 100
        val heavyMetalSpeed = context.calculateDynamicProperty(baseSpeed, difficultyWeight = 0.8f)
        assertTrue("Heavy metal difficulty should scale property higher", heavyMetalSpeed > baseSpeed)

        // Scaling with progression level (e.g. Level 3)
        context.currentLevel = 3
        val level3Speed = context.calculateDynamicProperty(baseSpeed, levelWeight = 0.2f)
        assertTrue("Higher level should increase calculated property", level3Speed > heavyMetalSpeed)

        // Scaling with active perk
        context.addPerk("OVERCLOCK")
        assertTrue(context.hasPerk("OVERCLOCK"))
        val perkBonus = if (context.hasPerk("OVERCLOCK")) 1.5f else 1.0f
        val overclockedSpeed = context.calculateDynamicProperty(baseSpeed, perkBonus = perkBonus)
        assertTrue("Perk should boost calculated property by 50%", overclockedSpeed > level3Speed)

        // Lives pressure factor (rubber-banding buff when at 1 life left)
        context.livesLeft = 1
        val criticalLifePower = context.calculateDynamicProperty(baseSpeed, perkBonus = perkBonus, livesInfluence = 0.15f)
        assertTrue("Critical life should trigger survival factor boost", criticalLifePower > overclockedSpeed)
    }

    @Test
    fun testZeroAllocationPixiePool() {
        var createCount = 0
        class MockBullet : com.pixie.engine.core.pool.Poolable {
            var active = false
            override fun reset() { active = false }
        }

        val pool = com.pixie.engine.core.pool.PixiePool(initialCapacity = 10, maxCapacity = 50) {
            createCount++
            MockBullet()
        }

        assertEquals(10, pool.freeCount)
        assertEquals(10, createCount)

        // Obtain items
        val b1 = pool.obtain()
        val b2 = pool.obtain()
        b1.active = true
        b2.active = true
        assertEquals(8, pool.freeCount)

        // Recycle b1
        pool.recycle(b1)
        assertFalse("Recycled bullet should be reset", b1.active)
        assertEquals(9, pool.freeCount)

        // Recycle b2
        pool.recycle(b2)
        assertEquals(10, pool.freeCount)
    }

    @Test
    fun testParticleLifecycleAndProceduralExplosions() {
        val pool = com.pixie.engine.core.particle.ParticlePool(initialCapacity = 30)
        val activeParticles = mutableListOf<com.pixie.engine.core.particle.Particle>()

        // Trigger procedural spark hit
        com.pixie.engine.core.particle.ProceduralExplosion.trigger(
            type = com.pixie.engine.core.particle.ExplosionType.SPARK_HIT,
            position = Vec2(50f, 50f),
            pool = pool,
            activeList = activeParticles,
            scale = 1.0f
        )
        assertTrue("Sparks should be spawned", activeParticles.isNotEmpty())
        val initialCount = activeParticles.size

        // Update physics and verify lifespan progression
        for (p in activeParticles) {
            assertTrue("Particle should be alive", p.isAlive)
        }

        // Advance time until particles expire
        val deadParticles = mutableListOf<com.pixie.engine.core.particle.Particle>()
        for (p in activeParticles) {
            val alive = p.update(0.5f)
            if (!alive) deadParticles.add(p)
        }
        assertEquals(initialCount, deadParticles.size)

        // Recycle all back to pool
        pool.recycleAll(deadParticles)
        assertEquals(30, pool.freeCount)
    }

    @Test
    fun testParticleLayerInPixieEngine() {
        val engine = PixieEngine()
        assertTrue("Engine should contain particleLayer", engine.layerStack.getLayer("Layer_Particles") != null)
        assertEquals(550, engine.particleLayer.order)

        // Spawn explosion via engine's particleLayer
        engine.particleLayer.spawnExplosion(
            type = com.pixie.engine.core.particle.ExplosionType.PLASMA_BURST,
            position = Vec2(100f, 100f)
        )
        assertTrue(engine.particleLayer.activeParticles.size > 0)

        // Test engine reset clears particles
        engine.resetGame()
        assertEquals(0, engine.particleLayer.activeParticles.size)
    }

    @Test
    fun testWeaponDefinitionDynamicCalculations() {
        val vulcan = com.pixie.engine.core.weapon.StandardWeapons.VULCAN_CANNON
        val context = GameContext(currentLevel = 1).apply { difficulty = 25 }

        val standardDamage = vulcan.calculateDynamicHitPower(context, isPlayerWeapon = true)
        val standardRate = vulcan.calculateDynamicFireRate(context, isPlayerWeapon = true)

        assertEquals(20f, standardDamage, 0.01f)
        assertEquals(2.8f, standardRate, 0.01f)

        // Add OVERCLOCK perk
        context.addPerk("OVERCLOCK")
        val overclockedDamage = vulcan.calculateDynamicHitPower(context, isPlayerWeapon = true)
        assertTrue("OVERCLOCK should increase damage", overclockedDamage > standardDamage)

        // Add RAPID_FIRE perk
        context.addPerk("RAPID_FIRE")
        val rapidFireRate = vulcan.calculateDynamicFireRate(context, isPlayerWeapon = true)
        assertTrue("RAPID_FIRE should increase rate of fire", rapidFireRate > standardRate)
    }

    @Test
    fun testProjectileSpawnAndTrajectories() {
        val pool = com.pixie.engine.core.weapon.ProjectilePool(initialCapacity = 10)
        val proj = pool.obtain()
        val vulcan = com.pixie.engine.core.weapon.StandardWeapons.VULCAN_CANNON

        // 1. Linear trajectory straight up
        val linearTraj = com.pixie.engine.core.weapon.Trajectory.Linear(-1.5707963f)
        proj.spawn(
            pos = Vec2(100f, 100f),
            definition = vulcan,
            isPlayer = true,
            traj = linearTraj,
            speed = 200f,
            damage = 15f
        )
        assertTrue(proj.isAlive)
        assertEquals(15f, proj.hitPower, 0.001f)

        // Update step
        proj.update(0.1f, GameContext())
        assertTrue("Projectile should have moved upward", proj.position.y < 100f)

        // 2. Homing trajectory steering toward target
        val target = PixieObject("target", position = Vec2(150f, 50f))
        val homingTraj = com.pixie.engine.core.weapon.Trajectory.Homing(
            target = target,
            turnRateRadPerSec = 5.0f,
            currentHeadingRad = -1.5707963f // pointing straight up initially
        )
        val homingProj = pool.obtain()
        homingProj.spawn(
            pos = Vec2(100f, 100f),
            definition = com.pixie.engine.core.weapon.StandardWeapons.HOMING_MISSILES,
            isPlayer = true,
            traj = homingTraj,
            speed = 200f,
            damage = 35f
        )

        // After updating, heading should turn right toward target (x=150, y=50)
        homingProj.update(0.1f, GameContext())
        assertTrue("Homing velocity x should be positive heading toward target", homingProj.velocity.x > 0f)

        // Clean up
        pool.recycle(proj)
        pool.recycle(homingProj)
    }

    @Test
    fun testWeaponSystemCooldownAndMultiShot() {
        val pool = com.pixie.engine.core.weapon.ProjectilePool(initialCapacity = 20)
        val weaponSys = com.pixie.engine.core.weapon.WeaponSystem(
            weapon = com.pixie.engine.core.weapon.StandardWeapons.TWIN_PLASMA,
            pool = pool,
            isPlayer = true
        )
        weaponSys.shotCount = 2 // Twin shot

        val spawned = mutableListOf<com.pixie.engine.core.weapon.Projectile>()
        val fired1 = weaponSys.fire(
            origin = Vec2(100f, 100f),
            headingRad = -1.5707963f,
            context = GameContext(),
            outList = spawned
        )
        assertTrue("First fire should succeed", fired1)
        assertEquals(2, spawned.size)
        assertTrue("Cooldown should be active", weaponSys.cooldownTimer > 0f)

        // Immediate second fire must fail due to cooldown
        val firedImmediate = weaponSys.fire(
            origin = Vec2(100f, 100f),
            headingRad = -1.5707963f,
            context = GameContext(),
            outList = spawned
        )
        assertFalse("Immediate second fire should be blocked by cooldown", firedImmediate)

        // Advance time to expire cooldown (TWIN_PLASMA rate 1.8 -> cooldown ~0.555s)
        weaponSys.update(0.6f)
        assertEquals(0f, weaponSys.cooldownTimer, 0.001f)
        val firedAfterCooldown = weaponSys.fire(
            origin = Vec2(100f, 100f),
            headingRad = -1.5707963f,
            context = GameContext(),
            outList = spawned
        )
        assertTrue("Fire after cooldown should succeed", firedAfterCooldown)
        assertEquals(4, spawned.size)

        // Recycle all
        pool.recycleAll(spawned)
    }

    @Test
    fun testProjectileImpactAndProceduralExplosion() {
        val pool = com.pixie.engine.core.weapon.ProjectilePool(initialCapacity = 5)
        val particleLayer = com.pixie.engine.core.particle.ParticleLayer()
        val proj = pool.obtain()
        proj.spawn(
            pos = Vec2(50f, 50f),
            definition = com.pixie.engine.core.weapon.StandardWeapons.TWIN_PLASMA,
            isPlayer = true,
            traj = com.pixie.engine.core.weapon.Trajectory.Linear(-1.57f),
            speed = 200f,
            damage = 18f,
            pierce = 1
        )

        // On impact
        proj.onImpact(proj.position, particleLayer)
        assertFalse("Single-pierce bullet should die on impact", proj.isAlive)
        assertTrue("Impact should trigger particles", particleLayer.activeParticles.size > 0)

        pool.recycle(proj)
    }

    @Test
    fun testContinuousCollisionSweptCircleAndReflection() {
        val box = RectF.fromTopLeft(100f, 100f, 40f, 10f) // thin brick

        // Fast moving ball heading downward straight into the brick
        val ballStart = Vec2(120f, 80f)
        val ballVel = Vec2(0f, 300f) // 300 px/sec
        val dt = 0.1f // ball would travel 30 px, completely skipping across a 10px brick in discrete physics!

        val hit = com.pixie.engine.core.physics.ContinuousCollision.sweepCircleVsAABB(
            center = ballStart,
            radius = 3f,
            velocity = ballVel,
            dt = dt,
            box = box
        )

        assertTrue("High-speed swept circle should detect impact before tunneling", hit.hasHit)
        assertTrue("Hit time must be within [0, 1]", hit.timeOfImpact in 0f..1f)
        assertEquals("Hit normal should point upward (Vec2.UP)", Vec2.UP, hit.normal)

        // Verify reflection
        val reflectedVel = ballVel.reflect(hit.normal)
        assertEquals(0f, reflectedVel.x, 0.001f)
        assertEquals(-300f, reflectedVel.y, 0.001f) // Bounced straight back up
    }

    @Test
    fun testDestructibleMatrixDestructionAndExplosiveChain() {
        val matrix = com.pixie.engine.core.matrix.DestructibleMatrixLayer(
            originX = 10f,
            originY = 20f,
            brickWidth = 20f,
            brickHeight = 10f,
            gap = 2f
        )

        val cell00 = com.pixie.engine.core.matrix.BrickCell(0, 0, type = com.pixie.engine.core.matrix.BrickType.STANDARD, hp = 1)
        val cell01 = com.pixie.engine.core.matrix.BrickCell(0, 1, type = com.pixie.engine.core.matrix.BrickType.EXPLOSIVE, hp = 1)
        val cell02 = com.pixie.engine.core.matrix.BrickCell(0, 2, type = com.pixie.engine.core.matrix.BrickType.ARMORED, hp = 2, maxHp = 2)

        matrix.loadLayout(1, 3, listOf(cell00, cell01, cell02))
        assertEquals(3, matrix.remainingDestructibles())

        // Hit the central explosive brick (0, 1)
        val hitRes = matrix.hitCell(0, 1)
        assertTrue(hitRes.hit)
        assertTrue(hitRes.wasDestroyed)
        assertTrue("Explosive brick should trigger adjacent chained destruction", hitRes.chainedExplosions.isNotEmpty())

        // Verify neighbors were destroyed in explosion
        assertTrue(cell00.isDestroyed)
        assertTrue(cell02.isDestroyed)
        assertEquals(0, matrix.remainingDestructibles())
    }
}
