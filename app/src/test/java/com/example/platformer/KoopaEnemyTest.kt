package com.example.platformer

import com.example.platformer.entities.GoombaEnemy
import com.example.platformer.entities.KoopaEnemy
import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.entity.PixiePlayer
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.platformer.BlockContent
import com.pixie.engine.core.platformer.InteractiveBlockController
import com.pixie.engine.core.platformer.PlatformerEnemyState
import com.pixie.engine.core.ports.RenderBatchPort
import com.pixie.engine.core.ports.VirtualResolution
import com.pixie.engine.core.sprite.PixelSprite
import com.pixie.engine.core.tilemap.TileType
import com.pixie.engine.core.tilemap.TilemapChunkLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KoopaEnemyTest {

    @Test
    fun testKoopaInitialStateAndPatrol() {
        val koopa = KoopaEnemy(
            position = Vec2(100f, 56f),
            direction = -1f,
            patrolSpeed = 35f
        )

        assertEquals(PlatformerEnemyState.PATROL, koopa.state)
        assertEquals(Vec2(16f, 24f), koopa.size)
        assertEquals(-35f, koopa.velocity.x, 0.001f)
        assertEquals(-1f, koopa.direction, 0.001f)
        assertTrue(koopa.isAlive)
        assertFalse(koopa.isDefeated)
        assertTrue(koopa.dealsContactDamage)
        assertFalse(koopa.isShellStationary)
        assertFalse(koopa.isShellSliding)
    }

    @Test
    fun testTwoFrameWalkingAnimationAndFlipping() {
        val koopa = KoopaEnemy(position = Vec2(0f, 0f), direction = -1f)
        val context = GameContext()

        // t = 0 -> Walk Frame 1
        val frame1 = koopa.getCurrentSprite()
        assertEquals(KoopaEnemy.WALK_FRAME_1, frame1)

        // t = 0.21s -> Walk Frame 2
        koopa.update(0.21f, context)
        val frame2 = koopa.getCurrentSprite()
        assertEquals(KoopaEnemy.WALK_FRAME_2, frame2)

        // Change direction to 1f (facing right) -> sprite should be horizontally flipped
        koopa.reverseDirection()
        assertEquals(1f, koopa.direction, 0.001f)
        val flipped = koopa.getCurrentSprite()
        assertEquals(KoopaEnemy.WALK_FRAME_2.flipHorizontal(), flipped)
    }

    @Test
    fun testFirstStompRetractsIntoStationaryShell() {
        val context = GameContext(score = 200L)
        // Resting on floor at 80f: position.y = 80 - 24 = 56f
        val koopa = KoopaEnemy(position = Vec2(100f, 56f))
        val stomper = PixiePlayer(id = "mario", position = Vec2(100f, 40f))

        var stompEventFired = false
        var playedSound: String? = null
        koopa.onStomped { stompEventFired = true }
        koopa.onPlaySound { playedSound = it }

        koopa.onStomped(stomper, context)

        assertTrue(stompEventFired)
        assertEquals("stomp", playedSound)
        assertEquals(PlatformerEnemyState.SHELL_STATIONARY, koopa.state)
        assertEquals(300L, context.score) // 200 + 100
        assertEquals(0f, koopa.velocity.x, 0.001f)
        assertFalse("Stationary shell must not deal contact damage", koopa.dealsContactDamage)
        assertTrue(koopa.canBeKicked)

        // Height retracted from 24f to 16f, Y shifted down by 8f (56 + 8 = 64f, bottom is 64 + 16 = 80f)
        assertEquals(16f, koopa.size.y, 0.001f)
        assertEquals(64f, koopa.position.y, 0.001f)
        assertEquals(KoopaEnemy.SHELL_IDLE_SPRITE, koopa.getCurrentSprite())
    }

    @Test
    fun testPlayerTouchingStationaryShellKicksSliding() {
        val context = GameContext(score = 0L)
        val koopa = KoopaEnemy(position = Vec2(100f, 64f))
            .withEnemyState(PlatformerEnemyState.SHELL_STATIONARY)

        val player = PixiePlayer(id = "mario", position = Vec2(90f, 64f)) // Player on left
        var kickedDir = 0f
        koopa.onShellKicked { _, dir -> kickedDir = dir }

        // Player touches stationary shell
        koopa.onCollision(player, context)

        assertEquals(PlatformerEnemyState.SHELL_SLIDING, koopa.state)
        assertEquals(1f, koopa.direction, 0.001f) // Kicked to the right
        assertEquals(1f, kickedDir, 0.001f)
        assertEquals(180f, koopa.velocity.x, 0.001f)
        assertEquals(400L, context.score) // 400 pts for kick
        assertTrue(koopa.kickSafetyTimer > 0f)
        assertEquals(player, koopa.lastKicker)

        // Player takes NO contact damage on initial kick window
        assertEquals(player.maxHealth, player.health, 0.001f)
    }

    @Test
    fun testSecondStompHaltsSlidingShell() {
        val context = GameContext(score = 1000L)
        val koopa = KoopaEnemy(position = Vec2(100f, 64f), direction = 1f)
            .withEnemyState(PlatformerEnemyState.SHELL_SLIDING)

        val stomper = PixiePlayer(id = "mario", position = Vec2(100f, 50f))
        var stompEventFired = false
        koopa.onStomped { stompEventFired = true }

        koopa.onStomped(stomper, context)

        assertTrue(stompEventFired)
        assertEquals(PlatformerEnemyState.SHELL_STATIONARY, koopa.state)
        assertEquals(0f, koopa.velocity.x, 0.001f)
        assertEquals(1100L, context.score) // 1000 + 100
    }

    @Test
    fun testSlidingShellBouncesOffWallsAndShattersBricks() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(
            listOf(
                "######",
                "#...B#", // Row 1: col 0 is wall, col 4 is BRICK (B), col 5 is wall
                "######"
            )
        )
        val blockController = InteractiveBlockController(layer)

        val koopa = KoopaEnemy(
            direction = 1f, // Moving right towards brick at col 4 (x = 64f)
            shellSlideSpeed = 180f,
            tilemap = layer,
            blockController = blockController
        ).withEnemyState(PlatformerEnemyState.SHELL_SLIDING)
            .withPosition(Vec2(32f, 16f))

        var brickShattered = false
        koopa.onBrickShattered { col, row ->
            if (col == 4 && row == 1) brickShattered = true
        }

        // Advance simulation for 20 frames until brick impact
        val dt = 1f / 60f
        for (i in 0 until 20) {
            koopa.updatePhysics(dt, layer)
            if (brickShattered) break
        }

        assertTrue("Brick at col 4 row 1 should be shattered by sliding shell", brickShattered)
        org.junit.Assert.assertNull("Brick tile should be cleared", layer.getTile(4, 1))
        assertEquals("Shell should rebound to the left", -1f, koopa.direction, 0.001f)
        assertEquals(-180f, koopa.velocity.x, 0.001f)
    }

    @Test
    fun testSlidingShellDefeatsEnemiesInPathWithChainScore() {
        val context = GameContext(score = 0L)
        val koopa = KoopaEnemy(
            position = Vec2(50f, 64f),
            direction = 1f,
            shellSlideSpeed = 180f
        ).withEnemyState(PlatformerEnemyState.SHELL_SLIDING)

        val goomba1 = GoombaEnemy(position = Vec2(80f, 64f))
        val goomba2 = GoombaEnemy(position = Vec2(120f, 64f))

        var defeatedCount = 0
        koopa.onEnemyDefeated { _, _ -> defeatedCount++ }

        // Shell strikes 1st Goomba
        koopa.handleEnemyContact(goomba1, context)

        assertEquals(1, defeatedCount)
        assertEquals(PlatformerEnemyState.DEFEATED_FALL, goomba1.state)
        assertEquals(500L, context.score) // 1st enemy in chain = 500

        // Shell strikes 2nd Goomba
        koopa.handleEnemyContact(goomba2, context)

        assertEquals(2, defeatedCount)
        assertEquals(PlatformerEnemyState.DEFEATED_FALL, goomba2.state)
        assertEquals(1300L, context.score) // 500 + 800 = 1300
    }

    @Test
    fun testKoopaDefeatedFallShowsUpsideDownSprite() {
        val koopa = KoopaEnemy(position = Vec2(50f, 50f))
        koopa.defeatWithFall(hitDirection = -1f)

        assertEquals(PlatformerEnemyState.DEFEATED_FALL, koopa.state)
        assertEquals(KoopaEnemy.UPSIDE_DOWN_SHELL_SPRITE, koopa.getCurrentSprite())
        assertEquals(-40f, koopa.velocity.x, 0.001f)
        assertEquals(-160f, koopa.velocity.y, 0.001f)
        assertTrue(koopa.isDefeated)
    }

    @Test
    fun testSpawnHelperAndFluentBuilders() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        val blockController = InteractiveBlockController(layer)

        val koopa = KoopaEnemy.spawn(
            col = 6,
            row = 4,
            tileSize = 16f,
            direction = 1f,
            tilemap = layer,
            blockController = blockController
        ).withPatrolSpeedRate(40f)
            .withShellSpeedRate(200f)

        // Spawn Y: row 4 * 16 - (24 - 16) = 64 - 8 = 56f
        assertEquals(96f, koopa.position.x, 0.001f)
        assertEquals(56f, koopa.position.y, 0.001f)
        assertEquals(1f, koopa.direction, 0.001f)
        assertEquals(40f, koopa.patrolSpeed, 0.001f)
        assertEquals(200f, koopa.shellSlideSpeed, 0.001f)
        assertNotNull(koopa.blockController)
        assertNotNull(koopa.tilemap)
    }

    @Test
    fun testRenderBatchPortDrawsSprite() {
        val koopa = KoopaEnemy(position = Vec2(40f, 60f))
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

        koopa.render(mockBatch, camera)

        assertEquals(KoopaEnemy.WALK_FRAME_1, drawnSprite)
        assertEquals(30f, drawnX, 0.001f) // 40 - 10 = 30
        assertEquals(50f, drawnY, 0.001f) // 60 - 10 = 50
    }
}
