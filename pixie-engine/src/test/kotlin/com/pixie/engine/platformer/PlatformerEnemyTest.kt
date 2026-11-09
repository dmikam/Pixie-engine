package com.pixie.engine.platformer

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.entity.PixiePlayer
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.platformer.PlatformerEnemy
import com.pixie.engine.core.platformer.PlatformerEnemyState
import com.pixie.engine.core.tilemap.TilemapChunkLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformerEnemyTest {

    @Test
    fun testPatrolWallBounceReversesDirection() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(
            listOf(
                "....#",
                "....#",
                "#####"
            )
        )

        // Enemy starts at x=32, y=16 (above floor at row 2) moving right (dir=1f) towards wall at col 4 (x=64)
        val enemy = PlatformerEnemy(
            position = Vec2(32f, 16f),
            state = PlatformerEnemyState.PATROL,
            direction = 1f,
            patrolSpeed = 40f,
            tilemap = layer
        )
        enemy.isGrounded = true

        var directionReversals = 0
        enemy.onDirectionChanged { directionReversals++ }

        val dt = 1f / 60f
        for (i in 0 until 60) {
            enemy.updatePhysics(dt, layer)
        }

        // Enemy hit the wall at x=64, clamped to x=48 (64 - 16), and reversed direction to -1f
        assertEquals(-1f, enemy.direction, 0.001f)
        assertEquals(-40f, enemy.velocity.x, 0.001f)
        assertTrue("Direction change listener should fire", directionReversals >= 1)
        assertTrue("Enemy should now be moving left away from wall", enemy.position.x <= 48f)
    }

    @Test
    fun testStageBoundaryReflection() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(
            listOf(
                "........",
                "........",
                "########"
            )
        )

        // Enemy starts near left boundary at x=4, y=16 moving left (dir=-1f)
        val enemy = PlatformerEnemy(
            position = Vec2(4f, 16f),
            state = PlatformerEnemyState.PATROL,
            direction = -1f,
            patrolSpeed = 50f,
            tilemap = layer
        )
        enemy.isGrounded = true

        val dt = 1f / 60f
        for (i in 0 until 10) {
            enemy.updatePhysics(dt, layer)
            if (enemy.direction > 0f) break
        }

        // Hit stage boundary x=0, clamped and reversed to dir=1f
        assertEquals(0f, enemy.position.x, 0.001f)
        assertEquals(1f, enemy.direction, 0.001f)
        assertEquals(50f, enemy.velocity.x, 0.001f)
    }

    @Test
    fun testGravityAndGroundLanding() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(
            listOf(
                "........",
                "........",
                "........",
                "########"
            )
        )

        // Enemy spawns in air at y=0, floor is at y=48 (row 3)
        val enemy = PlatformerEnemy(
            position = Vec2(32f, 0f),
            state = PlatformerEnemyState.PATROL,
            tilemap = layer
        )
        assertFalse(enemy.isGrounded)

        val dt = 1f / 60f
        for (i in 0 until 60) {
            enemy.updatePhysics(dt, layer)
            if (enemy.isGrounded) break
        }

        assertTrue("Enemy should have landed on ground", enemy.isGrounded)
        assertEquals(32f, enemy.position.y, 0.001f) // row 3 top (48) - 16 (height) = 32
        assertEquals(0f, enemy.velocity.y, 0.001f)
    }

    @Test
    fun testStompedSquashTimerAndDespawn() {
        val context = GameContext(score = 200L)
        val enemy = PlatformerEnemy(
            position = Vec2(20f, 20f),
            state = PlatformerEnemyState.PATROL,
            scoreAward = 100L,
            stompedDuration = 0.4f
        )
        val player = PixieObject(id = "player_hero", position = Vec2(20f, 10f))

        var stompEventFired = false
        enemy.onStomped { stompEventFired = true }

        enemy.onStomped(player, context)

        assertTrue(stompEventFired)
        assertEquals(PlatformerEnemyState.STOMPED, enemy.state)
        assertEquals(0.4f, enemy.stompedTimer, 0.001f)
        assertEquals(300L, context.score)
        assertTrue(enemy.isAlive)
        assertFalse(enemy.dealsContactDamage)

        // Advance 0.2s -> still alive and squashed
        enemy.update(0.2f, context)
        assertTrue(enemy.isAlive)
        assertFalse(enemy.isDefeated)

        // Advance remaining 0.25s -> despawns
        enemy.update(0.25f, context)
        assertFalse(enemy.isAlive)
        assertTrue(enemy.isDefeated)
    }

    @Test
    fun testShellKickedAndRebound() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(
            listOf(
                "....#",
                "....#",
                "#####"
            )
        )

        val enemy = PlatformerEnemy(
            position = Vec2(20f, 16f),
            state = PlatformerEnemyState.SHELL_STATIONARY,
            shellSlideSpeed = 180f,
            tilemap = layer
        )
        enemy.isGrounded = true

        assertTrue(enemy.canBeKicked)
        assertFalse(enemy.dealsContactDamage)
        assertFalse(enemy.isHazardToEnemies)

        var kickCallbackFired = false
        var kickDir = 0f
        enemy.onShellKicked { _, dir ->
            kickCallbackFired = true
            kickDir = dir
        }

        // Kick shell to the right
        enemy.kickShell(1f)

        assertTrue(kickCallbackFired)
        assertEquals(1f, kickDir, 0.001f)
        assertEquals(PlatformerEnemyState.SHELL_SLIDING, enemy.state)
        assertTrue(enemy.isHazardToEnemies)
        assertTrue(enemy.dealsContactDamage)
        assertEquals(180f, enemy.velocity.x, 0.001f)

        // Simulate sliding towards wall at x=64
        var wallBounceFired = false
        enemy.onWallBounce { _, _ -> wallBounceFired = true }

        val dt = 1f / 60f
        for (i in 0 until 40) {
            enemy.updatePhysics(dt, layer)
            if (wallBounceFired) break
        }

        assertTrue("Shell should rebound off solid wall", wallBounceFired)
        assertEquals(-1f, enemy.direction, 0.001f)
        assertEquals(-180f, enemy.velocity.x, 0.001f)

        // Stomping sliding shell stops it
        val stomper = PixieObject(id = "player_hero", position = Vec2(enemy.position.x, 0f))
        enemy.onStomped(stomper)
        assertEquals(PlatformerEnemyState.SHELL_STATIONARY, enemy.state)
        assertEquals(0f, enemy.velocity.x, 0.001f)
    }

    @Test
    fun testDefeatWithFall() {
        val enemy = PlatformerEnemy(
            position = Vec2(50f, 50f),
            state = PlatformerEnemyState.PATROL
        )

        enemy.defeatWithFall(hitDirection = 1f, verticalImpulse = -200f)

        assertEquals(PlatformerEnemyState.DEFEATED_FALL, enemy.state)
        assertTrue(enemy.isDefeated)
        assertEquals(40f, enemy.velocity.x, 0.001f)
        assertEquals(-200f, enemy.velocity.y, 0.001f)
        assertFalse(enemy.dealsContactDamage)

        val context = GameContext()
        // Simulate falling off stage
        for (i in 0 until 180) {
            enemy.update(1f / 60f, context)
            if (!enemy.isAlive) break
        }
        assertFalse("Enemy should despawn after falling into deep pit", enemy.isAlive)
    }

    @Test
    fun testContactDamageOnlyWhenActive() {
        val enemy = PlatformerEnemy(
            position = Vec2(20f, 20f),
            state = PlatformerEnemyState.PATROL,
            contactDamage = 15f
        )
        val player = PixiePlayer(id = "hero", initialHealth = 100f)
        val context = GameContext()

        // In PATROL, contact deals damage
        enemy.onCollision(player, context)
        assertEquals(85f, player.health, 0.001f)

        // In SHELL_STATIONARY, contact deals NO damage (can be safely kicked)
        enemy.transitionTo(PlatformerEnemyState.SHELL_STATIONARY)
        enemy.onCollision(player, context)
        assertEquals(85f, player.health, 0.001f) // Unchanged!

        // In STOMPED, contact deals NO damage
        enemy.transitionTo(PlatformerEnemyState.STOMPED)
        enemy.onCollision(player, context)
        assertEquals(85f, player.health, 0.001f) // Unchanged!
    }
}
