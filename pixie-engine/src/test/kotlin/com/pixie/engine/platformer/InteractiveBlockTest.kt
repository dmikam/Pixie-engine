package com.pixie.engine.platformer

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.platformer.BlockContent
import com.pixie.engine.core.platformer.BlockHitResult
import com.pixie.engine.core.platformer.InteractiveBlockController
import com.pixie.engine.core.platformer.MushroomState
import com.pixie.engine.core.platformer.PlayerTier
import com.pixie.engine.core.platformer.SuperMushroom
import com.pixie.engine.core.tilemap.TileType
import com.pixie.engine.core.tilemap.TilemapChunkLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

class InteractiveBlockTest {

    @Test
    fun testTriggerBlockBonkRecoilPeakAndReturn() {
        val layer = TilemapChunkLayer(tileSize = 16f, zIndex = 200)
        layer.loadAscii(
            listOf(
                "....",
                ".?B.",
                "####"
            )
        )

        // Ground is not bumpable
        assertFalse(layer.triggerBlockBonk(col = 0, row = 2))

        // Question block is bumpable
        val bonked = layer.triggerBlockBonk(col = 1, row = 1, velocity = -90f)
        assertTrue("Question block should be bumpable", bonked)

        val tile = layer.getTile(1, 1)
        assertNotNull(tile)
        assertEquals(-90f, tile!!.bumpVelocityY, 0.001f)

        // Step through simulation at 60Hz (dt = 1/60s) and track peak upward recoil
        var peakOffset = 0f
        var timeElapsed = 0f
        val context = GameContext()

        while (timeElapsed < 0.35f) {
            val dt = 1f / 60f
            layer.update(dt, context)
            timeElapsed += dt
            peakOffset = min(peakOffset, tile.bumpOffsetY)
            if (tile.bumpOffsetY == 0f && tile.bumpVelocityY == 0f && timeElapsed > 0.1f) {
                break
            }
        }

        // Peak recoil should be between 5.0 and 6.0 pixels upward (negative Y)
        assertTrue(
            "Peak recoil ($peakOffset) should be between -6.0 and -5.0 pixels",
            peakOffset in -6.0f..-5.0f
        )
        // Snaps back to resting state
        assertEquals(0f, tile.bumpOffsetY, 0.001f)
        assertEquals(0f, tile.bumpVelocityY, 0.001f)
    }

    @Test
    fun testTriggerBlockBonkKnocksStandingEntityUpward() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(
            listOf(
                "......",
                "..??..",
                "######"
            )
        )
        val standingGoomba = PixieObject(
            id = "goomba_1",
            position = Vec2(32f, 0f),
            size = Vec2(16f, 16f),
            velocity = Vec2(20f, 0f)
        )

        val distantGoomba = PixieObject(
            id = "goomba_2",
            position = Vec2(64f, 0f),
            size = Vec2(16f, 16f),
            velocity = Vec2(-20f, 0f)
        )

        val airborneEntity = PixieObject(
            id = "airborne",
            position = Vec2(32f, -50f),
            size = Vec2(16f, 16f),
            velocity = Vec2(0f, 10f)
        )

        var bonkedTileRecorded: Pair<Int, Int>? = null
        var knockedEntityRecorded: PixieObject? = null

        layer.withEntityProvider {
            listOf(standingGoomba, distantGoomba, airborneEntity)
        }.onBlockBonk { col, row, _ ->
            bonkedTileRecorded = col to row
        }.onEntityKnocked { entity, _, _ ->
            knockedEntityRecorded = entity
        }

        val success = layer.triggerBlockBonk(col = 2, row = 1)
        assertTrue(success)

        assertEquals(-120f, standingGoomba.velocity.y, 0.001f)
        assertEquals(20f, standingGoomba.velocity.x, 0.001f)
        assertEquals(standingGoomba, knockedEntityRecorded)

        assertEquals(0f, distantGoomba.velocity.y, 0.001f)
        assertEquals(10f, airborneEntity.velocity.y, 0.001f)

        assertEquals(2 to 1, bonkedTileRecorded)
    }

    @Test
    fun testNonBumpableTilesCannotBeBonked() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(listOf("#."))

        val entity = PixieObject(id = "player", position = Vec2(0f, -16f))
        layer.withEntityProvider { listOf(entity) }

        assertFalse(layer.triggerBlockBonk(col = 0, row = 0))
        assertEquals(0f, entity.velocity.y, 0.001f)

        assertFalse(layer.triggerBlockBonk(col = 1, row = 0))
        assertFalse(layer.triggerBlockBonk(col = 99, row = 99))
    }

    @Test
    fun testQuestionBlockHitMutatesToUsedBlockAndSpawnsPoppingCoin() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(
            listOf(
                "....",
                ".?B.",
                "####"
            )
        )

        val controller = InteractiveBlockController(layer)
        val context = GameContext(score = 0L)
        context.setAttribute("coins", 0)

        var soundPlayed: String? = null
        controller.onPlaySound { soundPlayed = it }

        var coinPoppedEvent: Pair<Long, Int>? = null
        controller.onCoinPopped { _, _, points, coins ->
            coinPoppedEvent = points to coins
        }

        val hitResult = controller.handleBlockHit(col = 1, row = 1, playerTier = PlayerTier.SMALL, context = context)
        assertTrue(hitResult is BlockHitResult.QuestionCoin)

        // Mutates to USED_BLOCK
        val tile = layer.getTile(1, 1)
        assertNotNull(tile)
        assertEquals(TileType.USED_BLOCK, tile!!.type)

        // Starts recoil bump
        assertEquals(-90f, tile.bumpVelocityY, 0.001f)

        // Spawns 1 popping coin
        assertEquals(1, controller.activeCoins.size)
        val coin = controller.activeCoins.first()
        assertEquals(200L, coin.pointsAwarded)

        // Context updated with 200 pts and 1 coin
        assertEquals(200L, context.score)
        assertEquals(1, context.getAttribute("coins", 0))
        assertEquals(1, controller.coinsAwardedTotal)
        assertEquals(200L, controller.scoreAwardedTotal)
        assertEquals(200L to 1, coinPoppedEvent)
        assertEquals("coin", soundPlayed)

        // Hitting again does nothing
        val secondHit = controller.handleBlockHit(col = 1, row = 1, playerTier = PlayerTier.SMALL, context = context)
        assertEquals(BlockHitResult.None, secondHit)
        assertEquals(1, controller.activeCoins.size)
    }

    @Test
    fun testPoppingCoinTrajectoryArches24PixelsAndDespawns() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(
            listOf(
                "....",
                ".?..",
                "####"
            )
        )

        val controller = InteractiveBlockController(layer)
        val context = GameContext()
        controller.handleBlockHit(col = 1, row = 1, context = context)

        assertEquals(1, controller.activeCoins.size)
        val coin = controller.activeCoins.first()
        val startY = coin.startY

        var highestAscent = 0f
        var time = 0f
        val dt = 1f / 60f

        while (controller.activeCoins.isNotEmpty() && time < 1.0f) {
            controller.update(dt, context)
            time += dt
            if (controller.activeCoins.isNotEmpty()) {
                val currentY = controller.activeCoins.first().position.y
                val deltaY = currentY - startY
                if (deltaY < highestAscent) {
                    highestAscent = deltaY
                }
            }
        }

        // Peak ascent should be ~24.0 pixels above startY (negative delta)
        assertTrue(
            "Peak ascent ($highestAscent) should be between -24.5 and -23.5 pixels",
            highestAscent in -24.5f..-23.5f
        )

        // Coin finished arc and despawned cleanly
        assertTrue(controller.activeCoins.isEmpty())
        assertTrue("Despawn time ($time) should be ~0.53s", time in 0.45f..0.65f)
    }

    @Test
    fun testQuestionBlockSpawnsRisingMushroom() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(
            listOf(
                "....",
                ".?..",
                "####"
            )
        )

        val controller = InteractiveBlockController(layer)
        controller.withMushroomBlock(1, 1)

        var soundPlayed: String? = null
        controller.onPlaySound { soundPlayed = it }

        var emergedMushroomPos: Vec2? = null
        controller.onMushroomEmerged { _, _, pos ->
            emergedMushroomPos = pos
        }

        val result = controller.handleBlockHit(col = 1, row = 1)
        assertTrue(result is BlockHitResult.QuestionMushroom)
        assertEquals("powerup_appears", soundPlayed)

        // Mutates to USED_BLOCK
        assertEquals(TileType.USED_BLOCK, layer.getTile(1, 1)?.type)

        // Rising mushroom added
        assertEquals(1, controller.activeMushrooms.size)
        val mush = controller.activeMushrooms.first()
        assertEquals(16f, mush.startY, 0.001f)
        assertEquals(0f, mush.targetY, 0.001f)

        // Step through emergence simulation
        val context = GameContext()
        var time = 0f
        val dt = 1f / 60f
        while (controller.activeMushrooms.isNotEmpty() && time < 1.5f) {
            controller.update(dt, context)
            time += dt
        }

        // Mushroom emerged and notified callback
        assertTrue(controller.activeMushrooms.isEmpty())
        assertNotNull(emergedMushroomPos)
        assertEquals(16f, emergedMushroomPos!!.x, 0.001f)
        assertEquals(0f, emergedMushroomPos!!.y, 0.001f)
    }

    @Test
    fun testBrickHitBySmallPlayerRecoilsWithoutDestroying() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(
            listOf(
                "....",
                ".B..",
                "####"
            )
        )

        val controller = InteractiveBlockController(layer)
        var bumped = false
        controller.onBrickBumped { _, _ -> bumped = true }

        val result = controller.handleBlockHit(1, 1, playerTier = PlayerTier.SMALL)
        assertTrue(result is BlockHitResult.BrickBumped)
        assertTrue(bumped)

        // Tile is still BRICK and recoiling
        val tile = layer.getTile(1, 1)
        assertNotNull(tile)
        assertEquals(TileType.BRICK, tile!!.type)
        assertEquals(-90f, tile.bumpVelocityY, 0.001f)
    }

    @Test
    fun testBrickHitBySuperPlayerShattersBrickAndSpawnsDebris() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(listOf("....", ".B..", "####"))

        val particleLayer = ParticleLayer()
        val controller = InteractiveBlockController(layer).withParticleLayer(particleLayer)
        val context = GameContext(score = 0L)

        var shatteredColRow: Pair<Int, Int>? = null
        var soundPlayed: String? = null
        controller.onBrickShattered { col, row -> shatteredColRow = col to row }
        controller.onPlaySound { soundPlayed = it }

        val result = controller.handleBlockHit(1, 1, playerTier = PlayerTier.SUPER, context = context)
        assertTrue("Expected BrickShattered result", result is BlockHitResult.BrickShattered)

        // Tile erased to EMPTY
        assertNull("Tile should be erased to empty", layer.getTile(1, 1))

        // State & points updated (+50 points in SMB)
        assertEquals(1, controller.brickShatteredTotal)
        assertEquals(50L, controller.scoreAwardedTotal)
        assertEquals(50L, context.score)
        assertEquals(1 to 1, shatteredColRow)
        assertEquals("break_block", soundPlayed)

        // 4 tumbling brick shards spawned into ParticleLayer
        val shards = particleLayer.activeParticles.filter { it.isTumbling }
        assertEquals("Expected exactly 4 brick debris shards", 4, shards.size)

        // Verify diagonal launch trajectories:
        // Top shards: vy = -160f, vx = -45f and +45f
        val topLeft = shards.firstOrNull { it.velocity.x < 0 && it.velocity.y < -150f }
        val topRight = shards.firstOrNull { it.velocity.x > 0 && it.velocity.y < -150f }
        // Bottom shards: vy = -110f, vx = -35f and +35f
        val botLeft = shards.firstOrNull { it.velocity.x < 0 && it.velocity.y in -120f..-100f }
        val botRight = shards.firstOrNull { it.velocity.x > 0 && it.velocity.y in -120f..-100f }

        assertNotNull("Top-left shard should exist", topLeft)
        assertNotNull("Top-right shard should exist", topRight)
        assertNotNull("Bottom-left shard should exist", botLeft)
        assertNotNull("Bottom-right shard should exist", botRight)

        // All 4 shards must be subject to gravity acceleration
        shards.forEach { shard ->
            assertEquals(600f, shard.acceleration.y, 0.001f)
            assertEquals(4f, shard.size, 0.001f)
        }

        // Advance simulation: verify gravity accelerates shards downward and tumbling animates
        val initialVy = topLeft!!.velocity.y
        particleLayer.update(0.1f, context)
        assertTrue("Gravity should pull shard downward", topLeft.velocity.y > initialVy)
        assertTrue("Shard should have moved horizontally", topLeft.position.x < (16f + 8f))
    }

    @Test
    fun testShatterBrickDirectlyOrViaThrownCrate() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(listOf("....", ".BB?", "####"))

        val particleLayer = ParticleLayer()
        val controller = InteractiveBlockController(layer).withParticleLayer(particleLayer)
        val context = GameContext(score = 100L)

        // Small player strikes brick with forceBreak = true (e.g. struck by thrown crate)
        val crateHitResult = controller.handleBlockHit(1, 1, playerTier = PlayerTier.SMALL, context = context, forceBreak = true)
        assertTrue(crateHitResult is BlockHitResult.BrickShattered)
        assertNull(layer.getTile(1, 1))
        assertEquals(150L, context.score)

        // Direct shatterBrick call
        val directShatter = controller.shatterBrick(2, 1, context)
        assertTrue(directShatter)
        assertNull(layer.getTile(2, 1))
        assertEquals(200L, context.score)

        // Calling shatterBrick on a non-brick (e.g. question block or empty) fails safely
        val nonBrickResult = controller.shatterBrick(3, 1, context)
        assertFalse(nonBrickResult)
        assertNotNull("Question block at (3, 1) should remain intact", layer.getTile(3, 1))

        val emptyResult = controller.shatterBrick(0, 0, context)
        assertFalse(emptyResult)
    }

    @Test
    fun testSuperMushroomEmergesCrawlsAndUpgradesPlayerSize() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(
            listOf(
                "........",
                ".?......",
                "########"
            )
        )

        val controller = InteractiveBlockController(layer)
        controller.withMushroomBlock(1, 1)

        val mushroom = controller.spawnMushroomEntity(1, 1, direction = 1f)
        assertEquals(MushroomState.EMERGING, mushroom.state)
        assertEquals(16f, mushroom.position.y, 0.001f)

        // Step simulation until mushroom emerges (16 pixels / 24 px/s ~ 0.67s)
        var time = 0f
        val dt = 1f / 60f
        while (mushroom.state == MushroomState.EMERGING && time < 1.0f) {
            mushroom.update(dt, GameContext())
            time += dt
        }

        assertEquals(MushroomState.MOVING, mushroom.state)
        assertEquals(0f, mushroom.position.y, 0.001f)
        assertEquals(45f, mushroom.velocity.x, 0.001f)

        // Crawl along the floor
        for (i in 0 until 10) {
            mushroom.updatePhysics(dt, layer)
        }
        assertTrue("Mushroom should move to the right", mushroom.position.x > 16f)

        // Player collects the mushroom
        val context = GameContext(score = 0L)
        val player = PixieObject(id = "player", position = Vec2(mushroom.position.x, 0f), size = Vec2(16f, 16f))

        var soundTriggered: String? = null
        mushroom.onPlaySound { soundTriggered = it }

        val collected = mushroom.checkAndCollect(player, context)
        assertTrue("Mushroom should be collected", collected)
        assertEquals(MushroomState.COLLECTED, mushroom.state)
        assertEquals(1000L, context.score)
        assertEquals("SUPER", context.getAttribute("player_tier", ""))
        assertEquals("powerup", soundTriggered)

        // Player size transformed from 16x16 to 16x24
        assertEquals(14f, player.size.x, 0.001f)
        assertEquals(24f, player.size.y, 0.001f)
    }
}

