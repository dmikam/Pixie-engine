package com.example.platformer

import com.example.platformer.entities.GoombaEnemy
import com.example.platformer.entities.KoopaEnemy
import com.example.platformer.entities.PlatformerPlayer
import com.example.platformer.world.CombatCollisionResult
import com.example.platformer.world.PlatformerCombatPlayer
import com.example.platformer.world.PlatformerCombatSystem
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.RectF
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.platformer.PlatformerEnemyState
import com.pixie.engine.core.platformer.PlayerTier
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.tilemap.TileType
import com.pixie.engine.core.tilemap.TilemapChunkLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformerCombatTest {

    private class TestAudioPort : AudioPort {
        val playedEvents = mutableListOf<AudioEvent>()
        override var isMuted: Boolean = false
        override fun playSound(event: AudioEvent) {
            playedEvents.add(event)
        }
    }

    @Test
    fun testFallingOntoEnemyTopTriggersStompAndSquashesGoomba() {
        val combatSystem = PlatformerCombatSystem()
        val context = GameContext(score = 100L)

        // Goomba at (100, 64), size 16x16 -> top is 64f, bottom is 80f
        val goomba = GoombaEnemy(position = Vec2(100f, 64f))

        // Player above Goomba falling downward at 150 px/s
        // Player at (101, 52), height 16 -> bottom is 68f (<= 64 + 8 = 72f)
        val player = PlatformerPlayer(position = Vec2(101f, 52f))
            .withVelocity(Vec2(0f, 150f))
            .withJumpHeld(false)

        var stompNotified = false
        combatSystem.onStomp { enemy, reboundVy, isBoosted ->
            stompNotified = true
            assertEquals(goomba, enemy)
            assertEquals(PlatformerCombatSystem.STOMP_REBOUND_NORMAL_VY, reboundVy, 0.001f)
            assertFalse(isBoosted)
        }

        val result = combatSystem.resolvePlayerEnemyCollision(player, goomba, context)

        assertTrue("Stomp callback should fire", stompNotified)
        assertTrue("Result should be Stomp", result is CombatCollisionResult.Stomp)
        val stompResult = result as CombatCollisionResult.Stomp
        assertEquals(PlatformerCombatSystem.STOMP_REBOUND_NORMAL_VY, stompResult.reboundVelocityY, 0.001f)
        assertFalse(stompResult.isBoosted)

        // Goomba should transition to STOMPED and award 100 points
        assertEquals(PlatformerEnemyState.STOMPED, goomba.state)
        assertEquals(200L, context.score)

        // Player should receive upward rebound impulse
        assertEquals(PlatformerCombatSystem.STOMP_REBOUND_NORMAL_VY, player.velocityY, 0.001f)
        assertEquals(PlatformerCombatSystem.STOMP_REBOUND_NORMAL_VY, player.physics.velocityY, 0.001f)
    }

    @Test
    fun testFallingWithJumpHeldGivesBoostedRebound() {
        val combatSystem = PlatformerCombatSystem()
        val context = GameContext()

        val goomba = GoombaEnemy(position = Vec2(100f, 64f))
        val player = PlatformerPlayer(position = Vec2(101f, 52f))
            .withVelocity(Vec2(0f, 120f))
            .withJumpHeld(true)

        val result = combatSystem.resolvePlayerEnemyCollision(player, goomba, context)

        assertTrue(result is CombatCollisionResult.Stomp)
        val stompResult = result as CombatCollisionResult.Stomp
        assertTrue(stompResult.isBoosted)
        assertEquals(PlatformerCombatSystem.STOMP_REBOUND_BOOSTED_VY, stompResult.reboundVelocityY, 0.001f)
        assertEquals(PlatformerCombatSystem.STOMP_REBOUND_BOOSTED_VY, player.velocityY, 0.001f)
    }

    @Test
    fun testStompOnKoopaRetractsToStationaryShell() {
        val combatSystem = PlatformerCombatSystem()
        val context = GameContext(score = 0L)

        // Koopa 16x24 at (100, 56) -> top is 56f, bottom is 80f
        val koopa = KoopaEnemy(position = Vec2(100f, 56f))
        // Player falling at bottom = 62f (56 + 6 <= 56 + 8)
        val player = PlatformerPlayer(position = Vec2(100f, 46f))
            .withVelocity(Vec2(0f, 100f))

        val result = combatSystem.resolvePlayerEnemyCollision(player, koopa, context)

        assertTrue(result is CombatCollisionResult.Stomp)
        assertEquals(PlatformerEnemyState.SHELL_STATIONARY, koopa.state)
        assertEquals(100L, context.score)
        assertEquals(PlatformerCombatSystem.STOMP_REBOUND_NORMAL_VY, player.velocityY, 0.001f)
    }

    @Test
    fun testWalkingIntoEnemySideDamagesSuperPlayerAndTriggersInvulnerability() {
        val combatSystem = PlatformerCombatSystem()
        val context = GameContext()

        val goomba = GoombaEnemy(position = Vec2(100f, 64f))

        // Super player at (92, 64), moving horizontally rightward at 60 px/s
        val player = PlatformerPlayer(position = Vec2(92f, 64f), initialTier = PlayerTier.SUPER)
            .withVelocity(Vec2(60f, 0f))

        var hurtNotified = false
        combatSystem.onPlayerHurt { enemy, oldTier, newTier ->
            hurtNotified = true
            assertEquals(PlayerTier.SUPER, oldTier)
            assertEquals(PlayerTier.SMALL, newTier)
        }

        val result = combatSystem.resolvePlayerEnemyCollision(player, goomba, context)

        assertTrue("Hurt callback should trigger", hurtNotified)
        assertTrue(result is CombatCollisionResult.PlayerDamaged)
        val damaged = result as CombatCollisionResult.PlayerDamaged
        assertEquals(PlayerTier.SUPER, damaged.oldTier)
        assertEquals(PlayerTier.SMALL, damaged.newTier)

        // Player should be downgraded to SMALL and granted 1.5s invulnerability
        assertEquals(PlayerTier.SMALL, player.tier)
        assertTrue(player.isInvulnerable)
        assertEquals(1.5f, player.invulnerabilityTimer, 0.001f)
        assertTrue(player.isAlive)
        assertFalse(player.isDefeated)
    }

    @Test
    fun testWalkingIntoEnemySideDefeatsSmallPlayerWithDeathHop() {
        val combatSystem = PlatformerCombatSystem()
        val context = GameContext()

        val goomba = GoombaEnemy(position = Vec2(100f, 64f))

        // Small player walking into enemy side
        val player = PlatformerPlayer(position = Vec2(92f, 64f), initialTier = PlayerTier.SMALL)
            .withVelocity(Vec2(50f, 0f))

        var defeatedNotified = false
        combatSystem.onPlayerDefeated { defeatedNotified = true }

        val result = combatSystem.resolvePlayerEnemyCollision(player, goomba, context)

        assertTrue("Defeated callback should trigger", defeatedNotified)
        assertTrue(result is CombatCollisionResult.PlayerDefeated)

        // Small player should be defeated and launched on death hop
        assertFalse(player.isAlive)
        assertTrue(player.isDefeated)
        assertEquals(PlatformerCombatSystem.DEATH_HOP_VELOCITY_Y, player.velocityY, 0.001f)

        // Updating defeated player should simulate parabolic death hop falling
        player.update(0.1f, context)
        assertTrue("Death hop should advance vertical position", player.position.y < 64f)
    }

    @Test
    fun testInvulnerablePlayerIgnoresLateralDamage() {
        val combatSystem = PlatformerCombatSystem()
        val context = GameContext()

        val goomba = GoombaEnemy(position = Vec2(100f, 64f))
        val player = PlatformerPlayer(position = Vec2(92f, 64f), initialTier = PlayerTier.SMALL)
        player.grantInvulnerability(1.2f)

        val result = combatSystem.resolvePlayerEnemyCollision(player, goomba, context)

        assertEquals(CombatCollisionResult.None, result)
        assertTrue(player.isAlive)
        assertFalse(player.isDefeated)
        assertEquals(PlayerTier.SMALL, player.tier)
    }

    @Test
    fun testKickingStationaryShellSetsSlidingVelocityAndDirection() {
        val combatSystem = PlatformerCombatSystem()
        val context = GameContext(score = 0L)

        // Stationary shell at (100, 64)
        val koopa = KoopaEnemy(position = Vec2(100f, 64f))
            .withEnemyState(PlatformerEnemyState.SHELL_STATIONARY)

        // Player approaching from left at (90, 64)
        val player = PlatformerPlayer(position = Vec2(90f, 64f))
            .withVelocity(Vec2(80f, 0f))

        var shellKickNotified = false
        combatSystem.onShellKicked { enemy, direction ->
            shellKickNotified = true
            assertEquals(1f, direction, 0.001f)
        }

        val result = combatSystem.resolvePlayerEnemyCollision(player, koopa, context)

        assertTrue(shellKickNotified)
        assertTrue(result is CombatCollisionResult.ShellKicked)
        val kicked = result as CombatCollisionResult.ShellKicked
        assertEquals(1f, kicked.direction, 0.001f)

        // Shell should now be sliding rightward at 180 px/s
        assertEquals(PlatformerEnemyState.SHELL_SLIDING, koopa.state)
        assertEquals(180f, koopa.velocity.x, 0.001f)
        assertEquals(400L, context.score) // 400 pts awarded for kick
    }

    @Test
    fun testSlidingShellReboundsOffWalls() {
        val tilemap = TilemapChunkLayer(tileSize = 16f).loadAscii(
            listOf(
                "######",
                "#....#", // Col 0 is wall (0px), Col 5 is wall (80px)
                "######"
            )
        )

        // Koopa in shell state resting on ground in row 1 (Y = 16f)
        val koopa = KoopaEnemy(
            direction = 1f,
            tilemap = tilemap
        ).withEnemyState(PlatformerEnemyState.SHELL_SLIDING)
            .withPosition(Vec2(32f, 16f))

        assertEquals(180f, koopa.velocity.x, 0.001f)

        // Advance simulation until wall impact
        val dt = 1f / 60f
        for (i in 0 until 15) {
            koopa.updatePhysics(dt, tilemap)
        }

        // Must rebound and reverse direction to -1f (-180 px/s)
        assertEquals(-1f, koopa.direction, 0.001f)
        assertEquals(-180f, koopa.velocity.x, 0.001f)
    }

    @Test
    fun testSlidingShellDefeatsPatrollingEnemyInEnemyEnemyResolution() {
        val combatSystem = PlatformerCombatSystem()
        val context = GameContext(score = 0L)

        // Sliding shell moving right
        val koopa = KoopaEnemy(position = Vec2(50f, 64f))
            .withEnemyState(PlatformerEnemyState.SHELL_SLIDING)
            .withSpawnDirection(1f)

        // Walking Goomba moving left
        val goomba = GoombaEnemy(position = Vec2(56f, 64f), direction = -1f)

        combatSystem.resolveEnemyCollisions(listOf(koopa, goomba), context)

        // Goomba should be defeated with fall and awarded chain score
        assertTrue(goomba.isDefeatedFall)
        assertEquals(500L, context.score)
    }

    @Test
    fun testAudioAndParticleIntegrationOnStomp() {
        val particleLayer = ParticleLayer()
        val audioPort = TestAudioPort()
        val combatSystem = PlatformerCombatSystem()
            .withParticleLayer(particleLayer)
            .withAudioPort(audioPort)

        var soundTriggered: String? = null
        combatSystem.onPlaySound { soundTriggered = it }

        val goomba = GoombaEnemy(position = Vec2(100f, 64f))
        val player = PlatformerPlayer(position = Vec2(100f, 52f))
            .withVelocity(Vec2(0f, 100f))

        combatSystem.resolvePlayerEnemyCollision(player, goomba, GameContext())

        // Particles should be spawned
        assertTrue(particleLayer.activeParticles.isNotEmpty())
        assertEquals(PlatformerCombatSystem.SPARK_PARTICLE_COUNT, particleLayer.activeParticles.size)

        // Audio should be triggered
        assertEquals("stomp", soundTriggered)
        assertTrue(audioPort.playedEvents.contains(AudioEvent.HIT))
    }
}
