package com.example.platformer

import com.example.platformer.entities.GoombaEnemy
import com.example.platformer.entities.KoopaEnemy
import com.example.platformer.entities.PlatformerPlayer
import com.example.platformer.entities.crate.CrateImpactReason
import com.example.platformer.entities.crate.CrateImpactResolver
import com.example.platformer.entities.crate.CrateImpactResult
import com.example.platformer.entities.crate.CrateState
import com.example.platformer.entities.crate.ThrowableCrate
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.particle.ParticleLayer
import com.pixie.engine.core.platformer.InteractiveBlockController
import com.pixie.engine.core.platformer.PlatformerEnemyState
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.AudioPort
import com.pixie.engine.core.tilemap.TileType
import com.pixie.engine.core.tilemap.TilemapChunkLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrateImpactResolverTest {

    private class TestAudioPort : AudioPort {
        val played = mutableListOf<AudioEvent>()
        override var isMuted: Boolean = false
        override fun playSound(event: AudioEvent) {
            played.add(event)
        }
    }

    @Test
    fun testFluentBuildersAndDefaults() {
        val resolver = CrateImpactResolver()
            .withEnemyScoreAward(300L)
            .withBrickScoreAward(100L)

        assertEquals(300L, resolver.enemyScoreAward)
        assertEquals(100L, resolver.brickScoreAward)
    }

    @Test
    fun testAudioPortAndSoundsOnEnemyImpact() {
        val audio = TestAudioPort()
        val resolver = CrateImpactResolver()
            .withAudioPort(audio)

        var soundKey: String? = null
        resolver.onPlaySound { soundKey = it }

        val crate = ThrowableCrate(position = Vec2(50f, 50f))
            .withState(CrateState.THROWN_AIRBORNE)
        val goomba = GoombaEnemy(position = Vec2(54f, 50f))

        val result = resolver.resolveCrateVsEnemy(crate, goomba)

        assertTrue(result is CrateImpactResult.EnemySmash)
        assertEquals("crate_smash", soundKey)
        assertTrue(audio.played.contains(AudioEvent.HIT))
    }

    @Test
    fun testBatchUpdateResolvesMultipleAirborneCrates() {
        val resolver = CrateImpactResolver()
        val context = GameContext(score = 0L)

        val crate1 = ThrowableCrate(position = Vec2(100f, 50f))
            .withState(CrateState.THROWN_AIRBORNE)
        val enemy1 = GoombaEnemy(position = Vec2(104f, 50f))

        val crate2 = ThrowableCrate(position = Vec2(200f, 50f))
            .withState(CrateState.THROWN_AIRBORNE)
        val enemy2 = KoopaEnemy(position = Vec2(204f, 50f))

        val inertCrate = ThrowableCrate(position = Vec2(300f, 50f))
            .withState(CrateState.PLACED_ON_GROUND)

        val results = resolver.update(
            crates = listOf(crate1, crate2, inertCrate),
            enemies = listOf(enemy1, enemy2),
            tilemap = null,
            context = context
        )

        assertEquals(2, results.size)
        assertTrue(results[0] is CrateImpactResult.EnemySmash)
        assertTrue(results[1] is CrateImpactResult.EnemySmash)
        assertEquals(400L, context.score) // 200 + 200
        assertEquals(CrateState.SHATTERED, crate1.state)
        assertEquals(CrateState.SHATTERED, crate2.state)
        assertEquals(CrateState.PLACED_ON_GROUND, inertCrate.state)
    }

    @Test
    fun testInteractiveBlockControllerIntegrationOnBrickHit() {
        val tilemap = TilemapChunkLayer(tileSize = 16f).loadAscii(
            listOf(
                "...",
                ".B.",
                "..."
            )
        )
        val particleLayer = ParticleLayer()
        val blockController = InteractiveBlockController(tilemap)
            .withParticleLayer(particleLayer)

        val resolver = CrateImpactResolver()
            .withInteractiveBlockController(blockController)
            .withParticleLayer(particleLayer)

        val crate = ThrowableCrate(position = Vec2(14f, 16f))
            .withState(CrateState.THROWN_AIRBORNE)

        val result = resolver.resolveCrateVsTilemap(crate, tilemap)

        assertTrue(result is CrateImpactResult.BrickSmash)
        assertNull("Brick tile should be cleared to empty/null", tilemap.getTile(1, 1))
        assertEquals(1, blockController.brickShatteredTotal)
        assertEquals(CrateState.SHATTERED, crate.state)
    }

    @Test
    fun testCrateDuckingDefenseRepelsSlidingShell() {
        val resolver = CrateImpactResolver()
        val player = PlatformerPlayer(position = Vec2(100f, 64f))
        val crate = ThrowableCrate(position = Vec2(100f, 64f))
        player.pickUpCrate(crate)
        player.isDucking = true

        val koopa = KoopaEnemy(position = Vec2(108f, 64f))
            .withEnemyState(PlatformerEnemyState.SHELL_SLIDING)
            .withSpawnDirection(-1f)

        assertEquals(-1f, koopa.direction, 0.001f)

        val defended = resolver.resolveDuckingDefense(player, koopa)

        assertTrue("Ducking defense should activate against sliding shell", defended)
        assertEquals(1f, koopa.direction, 0.001f) // Direction reversed upon bounce
    }
}
