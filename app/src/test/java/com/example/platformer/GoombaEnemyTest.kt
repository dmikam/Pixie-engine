package com.example.platformer

import com.example.platformer.entities.GoombaEnemy
import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.platformer.PlatformerEnemyState
import com.pixie.engine.core.ports.RenderBatchPort
import com.pixie.engine.core.ports.VirtualResolution
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.tilemap.TilemapChunkLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoombaEnemyTest {

    @Test
    fun testGoombaInitialStateAndPatrol() {
        val goomba = GoombaEnemy(
            position = Vec2(100f, 50f),
            direction = -1f,
            patrolSpeed = 35f
        )

        assertEquals(PlatformerEnemyState.PATROL, goomba.state)
        assertEquals(Vec2(16f, 16f), goomba.size)
        assertEquals(-35f, goomba.velocity.x, 0.001f)
        assertEquals(-1f, goomba.direction, 0.001f)
        assertTrue(goomba.isAlive)
        assertFalse(goomba.isDefeated)
        assertTrue(goomba.dealsContactDamage)
    }

    @Test
    fun testTwoFrameWalkingWobbleAnimation() {
        val goomba = GoombaEnemy(position = Vec2(0f, 0f))
        val context = GameContext()

        // At t = 0, first walk frame is shown
        val frame1 = goomba.getCurrentSprite()
        assertEquals(GoombaEnemy.WALK_FRAME_1, frame1)

        // Advance 0.21s -> second frame (wobble) is shown
        goomba.update(0.21f, context)
        val frame2 = goomba.getCurrentSprite()
        assertEquals(GoombaEnemy.WALK_FRAME_2, frame2)

        // Advance another 0.21s -> loops back to frame 1
        goomba.update(0.21f, context)
        val frame3 = goomba.getCurrentSprite()
        assertEquals(GoombaEnemy.WALK_FRAME_1, frame3)
    }

    @Test
    fun testGoombaStompSquashAndAwardPoints() {
        val context = GameContext(score = 500L)
        val floorY = 80f
        // Goomba resting on floor at floorY (so top is at 64f, bottom is at 80f)
        val goomba = GoombaEnemy(
            position = Vec2(100f, 64f),
            stompedDuration = 0.4f
        )
        val stomper = PixieObject(id = "mario", position = Vec2(100f, 50f))

        var stompedEvent = false
        goomba.onStomped { stompedEvent = true }

        // Stomp Goomba
        goomba.onStomped(stomper, context)

        assertTrue("Stomp callback should trigger", stompedEvent)
        assertEquals(PlatformerEnemyState.STOMPED, goomba.state)
        assertEquals(600L, context.score) // 500 + 100 awarded points
        assertEquals(0f, goomba.velocity.x, 0.001f)
        assertFalse("Squashed Goomba should not deal contact damage", goomba.dealsContactDamage)

        // Goomba height squashed to 8f and shifted down to stay flush with floor at 80f
        assertEquals(8f, goomba.size.y, 0.001f)
        assertEquals(72f, goomba.position.y, 0.001f) // 64 + 8 = 72, bottom is 72 + 8 = 80f
        assertEquals(GoombaEnemy.SQUASHED_SPRITE, goomba.getCurrentSprite())
        assertEquals(16, goomba.getCurrentSprite().width)
        assertEquals(8, goomba.getCurrentSprite().height)

        // Advance 0.2s -> still alive and squashed
        goomba.update(0.2f, context)
        assertTrue(goomba.isAlive)
        assertFalse(goomba.isDefeated)
        assertEquals(GoombaEnemy.SQUASHED_SPRITE, goomba.getCurrentSprite())

        // Advance remaining 0.25s -> despawns
        goomba.update(0.25f, context)
        assertFalse("Goomba should despawn after stomped duration", goomba.isAlive)
        assertTrue(goomba.isDefeated)
    }

    @Test
    fun testDefeatedFallShowsUpsideDownSprite() {
        val goomba = GoombaEnemy(position = Vec2(50f, 50f))
        goomba.defeatWithFall(hitDirection = 1f)

        assertEquals(PlatformerEnemyState.DEFEATED_FALL, goomba.state)
        assertEquals(GoombaEnemy.UPSIDE_DOWN_SPRITE, goomba.getCurrentSprite())
        assertEquals(40f, goomba.velocity.x, 0.001f)
        assertEquals(-160f, goomba.velocity.y, 0.001f)
    }

    @Test
    fun testGoombaWallBounceWithTilemap() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(
            listOf(
                "#...#",
                "#...#",
                "#####"
            )
        )

        // Spawn Goomba at x=32 moving left (dir=-1f) towards wall at col 0 (x=16)
        val goomba = GoombaEnemy(
            position = Vec2(32f, 16f),
            direction = -1f,
            patrolSpeed = 35f,
            tilemap = layer
        )

        var bounces = 0
        goomba.onDirectionChanged { bounces++ }

        val dt = 1f / 60f
        for (i in 0 until 40) {
            goomba.updatePhysics(dt, layer)
            if (bounces > 0) break
        }

        assertTrue("Goomba should bounce off solid wall", bounces > 0)
        assertEquals(1f, goomba.direction, 0.001f)
        assertEquals(35f, goomba.velocity.x, 0.001f)
    }

    @Test
    fun testFluentBuildersAndSpawnHelper() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        val goomba = GoombaEnemy.spawn(col = 5, row = 10, tileSize = 16f, direction = 1f, tilemap = layer)
            .withPoints(200L)
            .withDamage(25f)
            .withSquashDuration(0.5f)

        assertEquals(80f, goomba.position.x, 0.001f)
        assertEquals(160f, goomba.position.y, 0.001f)
        assertEquals(1f, goomba.direction, 0.001f)
        assertEquals(200L, goomba.scoreAward)
        assertEquals(25f, goomba.contactDamage, 0.001f)
        assertEquals(0.5f, goomba.stompedDuration, 0.001f)
    }

    @Test
    fun testRenderDrawsSprite() {
        val goomba = GoombaEnemy(position = Vec2(30f, 40f))
        val camera = PixieCamera().apply { setPositionImmediate(Vec2(10f, 10f)) }

        var drawnSprite: PixelSprite? = null
        var drawnX = 0f
        var drawnY = 0f

        val mockBatch = object : RenderBatchPort {
            override val resolution: VirtualResolution = VirtualResolution(320, 240)
            override var pixelsGap: Int = 0
            override var isCurrentLayerGridEnabled: Boolean = false

            override fun fillBackground(color: Int) {}
            override fun drawPixelRect(x: Float, y: Float, width: Float, height: Float, color: Int, saturation: Float, alpha: Float) {}
            override fun drawSprite(sprite: PixelSprite, x: Float, y: Float, saturation: Float, alpha: Float) {
                drawnSprite = sprite
                drawnX = x
                drawnY = y
            }
            override fun drawMotionBlur(sprite: PixelSprite, trail: List<Vec2>, strength: Float, saturation: Float) {}
            override fun drawText(text: String, x: Float, y: Float, color: Int, sizePx: Float) {}
        }

        goomba.render(mockBatch, camera)

        assertEquals(GoombaEnemy.WALK_FRAME_1, drawnSprite)
        assertEquals(20f, drawnX, 0.001f) // 30 - 10 = 20
        assertEquals(30f, drawnY, 0.001f) // 40 - 10 = 30
    }
}
