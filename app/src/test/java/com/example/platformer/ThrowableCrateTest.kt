package com.example.platformer

import com.example.platformer.entities.GoombaEnemy
import com.example.platformer.entities.PlatformerPlayer
import com.example.platformer.entities.crate.CrateImpactResolver
import com.example.platformer.entities.crate.CrateImpactResult
import com.example.platformer.entities.crate.CrateState
import com.example.platformer.entities.crate.ThrowableCrate
import com.example.platformer.world.CombatCollisionResult
import com.example.platformer.world.PlatformerCombatSystem
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThrowableCrateTest {

    @Test
    fun testInitialPlacedOnGroundState() {
        val crate = ThrowableCrate(position = Vec2(32f, 64f))

        assertEquals(CrateState.PLACED_ON_GROUND, crate.state)
        assertEquals(Vec2(16f, 16f), crate.size)
        assertEquals(32f, crate.position.x, 0.001f)
        assertEquals(64f, crate.position.y, 0.001f)
        assertTrue("Crate should be solid when placed on ground", crate.isSolid)
        assertTrue("Crate should be alive when placed on ground", crate.isAlive)
        assertTrue("Crate should be visible", crate.isVisible)
        assertTrue("Crate should allow standing upon as a platform", crate.canStandOn())
        assertNull("Carrier should be null when placed", crate.carrier)

        assertTrue(crate.isInClass("crate"))
        assertTrue(crate.isInClass("throwable_crate"))
        assertTrue(crate.isInClass("solid_obstacle"))
        assertTrue(crate.isInClass("interactive_platform"))
    }

    @Test
    fun testPickUpAttachesOverheadToCarrier() {
        val hero = PixieObject(id = "hero", position = Vec2(100f, 80f))
        val crate = ThrowableCrate(position = Vec2(116f, 80f))
        val context = GameContext()

        var pickedUpActor: PixieObject? = null
        crate.onPickedUp = { carrier -> pickedUpActor = carrier }

        var stateTransitionRecorded = false
        crate.onStateChanged = { old, new ->
            if (old == CrateState.PLACED_ON_GROUND && new == CrateState.CARRIED) {
                stateTransitionRecorded = true
            }
        }

        crate.pickUp(hero)

        assertEquals(CrateState.CARRIED, crate.state)
        assertEquals(hero, crate.carrier)
        assertEquals(hero, pickedUpActor)
        assertTrue(stateTransitionRecorded)

        // Overhead offset default is (0f, -14f), so crate sits directly above player's head
        assertEquals(100f, crate.position.x, 0.001f)
        assertEquals(66f, crate.position.y, 0.001f) // 80 - 14 = 66f
        assertFalse("Carried crate should not be solid", crate.isSolid)
        assertFalse("Carried crate cannot be stood upon", crate.canStandOn())
        assertTrue("Carried crate remains alive", crate.isAlive)

        // When hero moves, crate position tracks carrier during update
        hero.position = Vec2(120f, 75f)
        hero.velocity = Vec2(50f, -30f)
        crate.update(0.1f, context)

        assertEquals(120f, crate.position.x, 0.001f)
        assertEquals(61f, crate.position.y, 0.001f) // 75 - 14 = 61f
        assertEquals(50f, crate.velocity.x, 0.001f)
        assertEquals(-30f, crate.velocity.y, 0.001f)
    }

    @Test
    fun testThrowCrateLaunchesAirborneWithGravity() {
        val hero = PixieObject(id = "hero", position = Vec2(100f, 80f))
        val crate = ThrowableCrate().apply { pickUp(hero) }
        val context = GameContext()

        var thrownVelocity: Vec2? = null
        crate.onThrown = { vel -> thrownVelocity = vel }

        // Throw forward with arc velocity (210f, -70f)
        crate.throwCrate(Vec2(210f, -70f))

        assertEquals(CrateState.THROWN_AIRBORNE, crate.state)
        assertNull("Carrier should be cleared upon throwing", crate.carrier)
        assertNotNull(thrownVelocity)
        assertEquals(210f, thrownVelocity!!.x, 0.001f)
        assertEquals(-70f, thrownVelocity!!.y, 0.001f)
        assertFalse(crate.isSolid)

        val initialY = crate.position.y
        val initialX = crate.position.x

        // Advance simulation by 0.1s: horizontal position advances, gravity accelerates Vy
        crate.update(0.1f, context)

        // vy = -70 + 650 * 0.1 = -5f
        assertEquals(210f, crate.velocity.x, 0.001f)
        assertEquals(-5f, crate.velocity.y, 0.001f)
        assertTrue("X position moved forward", crate.position.x > initialX)

        // Advance another 0.1s: vy = -5 + 650 * 0.1 = +60f (downward arc)
        crate.update(0.1f, context)
        assertEquals(60f, crate.velocity.y, 0.001f)
    }

    @Test
    fun testShatterDestructionLifecycle() {
        val crate = ThrowableCrate(position = Vec2(50f, 50f))
        val context = GameContext()

        var shatteredFired = false
        crate.onShattered = { shatteredFired = true }

        crate.shatter()

        assertEquals(CrateState.SHATTERED, crate.state)
        assertTrue(shatteredFired)
        assertFalse("Shattered crate is no longer alive", crate.isAlive)
        assertFalse("Shattered crate is not solid", crate.isSolid)
        assertEquals(Vec2.ZERO, crate.velocity)
        assertEquals(ThrowableCrate.SHATTERED_SPRITE, crate.getCurrentSprite())

        // Still visible during shatter animation
        assertTrue(crate.isVisible)

        // Advance past shatter duration (0.25s) -> crate becomes invisible
        crate.update(0.26f, context)
        assertFalse("Crate should be invisible after shatter duration completes", crate.isVisible)
    }

    @Test
    fun testPlaceOnGroundRestoresPlatformSolidity() {
        val hero = PixieObject(id = "hero", position = Vec2(40f, 60f))
        val crate = ThrowableCrate().apply { pickUp(hero) }

        assertFalse(crate.isSolid)

        crate.placeOnGround(Vec2(40f, 80f))

        assertEquals(CrateState.PLACED_ON_GROUND, crate.state)
        assertTrue(crate.isSolid)
        assertTrue(crate.canStandOn())
        assertNull(crate.carrier)
        assertEquals(40f, crate.position.x, 0.001f)
        assertEquals(80f, crate.position.y, 0.001f)
    }

    @Test
    fun testSpriteAndAnimationArt() {
        val crate = ThrowableCrate()

        assertEquals(16, ThrowableCrate.CRATE_SPRITE.width)
        assertEquals(16, ThrowableCrate.CRATE_SPRITE.height)
        assertEquals(ThrowableCrate.CRATE_SPRITE, crate.getCurrentSprite())

        assertEquals(16, ThrowableCrate.SHATTERED_SPRITE.width)
        assertEquals(16, ThrowableCrate.SHATTERED_SPRITE.height)
    }

    @Test
    fun testFluentBuildersAndSpawnFactories() {
        val custom = ThrowableCrate()
            .withPosition(Vec2(64f, 48f))
            .withGravity(500f)
            .withMaxFallSpeed(350f)
            .withCarryOffset(Vec2(0f, -16f))

        assertEquals(64f, custom.position.x, 0.001f)
        assertEquals(48f, custom.position.y, 0.001f)
        assertEquals(500f, custom.gravity, 0.001f)
        assertEquals(350f, custom.maxFallSpeed, 0.001f)
        assertEquals(Vec2(0f, -16f), custom.carryOffset)

        val spawned = ThrowableCrate.spawn(col = 5, row = 10, tileSize = 16f)
        assertEquals(80f, spawned.position.x, 0.001f)
        assertEquals(160f, spawned.position.y, 0.001f)
        assertEquals(CrateState.PLACED_ON_GROUND, spawned.state)

        val blockSpawned = ThrowableCrate.spawnFromBlock(col = 5, row = 10, tileSize = 16f)
        assertEquals(80f, blockSpawned.position.x, 0.001f)
        assertEquals(144f, blockSpawned.position.y, 0.001f) // (10 - 1) * 16 = 144f
    }

    @Test
    fun testFallingCrateLandsOnTilemapFloor() {
        val layer = TilemapChunkLayer(tileSize = 16f)
        layer.loadAscii(
            listOf(
                "...",
                "...",
                "...",
                "###" // Row 3 is solid ground at y = 48f
            )
        )

        val crate = ThrowableCrate(
            position = Vec2(16f, 0f),
            tilemap = layer
        )
        crate.isGrounded = false
        val context = GameContext()

        // Advance simulation -> crate falls under gravity until hitting row 3 ground at y = 48 - 16 = 32f
        val dt = 1f / 60f
        for (i in 0 until 40) {
            crate.update(dt, context)
        }

        assertTrue("Crate should be grounded after landing on tilemap", crate.isGrounded)
        assertEquals(32f, crate.position.y, 0.001f)
        assertEquals(0f, crate.velocity.y, 0.001f)
        assertTrue(crate.canStandOn())
    }

    @Test
    fun testThrownCrateCollidingWithEnemyDefeatsEnemyAndShattersCrate() {
        val particleLayer = ParticleLayer()
        val resolver = CrateImpactResolver().withParticleLayer(particleLayer)
        val context = GameContext(score = 100L)

        val crate = ThrowableCrate(position = Vec2(100f, 64f))
            .withState(CrateState.THROWN_AIRBORNE)
        crate.velocity = Vec2(210f, 0f)

        val goomba = GoombaEnemy(position = Vec2(108f, 64f))
        assertTrue(goomba.isAlive)
        assertEquals(PlatformerEnemyState.PATROL, goomba.state)

        var enemySmashedFired = false
        resolver.onEnemySmashed { smashedCrate, enemy, points ->
            enemySmashedFired = true
            assertEquals(crate, smashedCrate)
            assertEquals(goomba, enemy)
            assertEquals(200L, points)
        }

        val result = resolver.resolveCrateVsEnemy(crate, goomba, context)

        assertTrue(result is CrateImpactResult.EnemySmash)
        assertTrue(enemySmashedFired)
        assertEquals(CrateState.SHATTERED, crate.state)
        assertFalse(crate.isAlive)
        assertTrue("Goomba should be knocked into defeat fall", goomba.isDefeatedFall)
        assertEquals(300L, context.score) // 100 + 200 = 300 pts

        // Particle layer should have spawned spark hit and 4 wooden splinters
        assertTrue(particleLayer.activeParticles.isNotEmpty())
    }

    @Test
    fun testThrownCrateStrikingBrickBlockDestroysBrickAndShatters() {
        val particleLayer = ParticleLayer()
        val resolver = CrateImpactResolver().withParticleLayer(particleLayer)
        val context = GameContext(score = 0L)

        val tilemap = TilemapChunkLayer(tileSize = 16f).loadAscii(
            listOf(
                "...",
                ".B.", // Row 1, Col 1 is a BRICK tile
                "..."
            )
        )
        val brickTile = tilemap.getTile(1, 1)
        assertNotNull(brickTile)
        assertEquals(TileType.BRICK, brickTile!!.type)

        // Crate airborne moving into Brick tile at (16, 16)
        val crate = ThrowableCrate(position = Vec2(14f, 16f))
            .withState(CrateState.THROWN_AIRBORNE)
        crate.velocity = Vec2(210f, 0f)

        var brickSmashedFired = false
        resolver.onBrickSmashed { _, col, row, pts ->
            brickSmashedFired = true
            assertEquals(1, col)
            assertEquals(1, row)
            assertEquals(50L, pts)
        }

        val result = resolver.resolveCrateVsTilemap(crate, tilemap, context)

        assertTrue(result is CrateImpactResult.BrickSmash)
        assertTrue(brickSmashedFired)
        assertEquals(CrateState.SHATTERED, crate.state)
        assertNull("Brick tile should be cleared after smashing", tilemap.getTile(1, 1))
        assertEquals(50L, context.score)
    }

    @Test
    fun testThrownCrateStrikingSolidWallShattersWithSound() {
        val resolver = CrateImpactResolver()
        val tilemap = TilemapChunkLayer(tileSize = 16f).loadAscii(
            listOf(
                "...",
                ".#.", // Row 1, Col 1 is a solid wall
                "..."
            )
        )

        val crate = ThrowableCrate(position = Vec2(14f, 16f))
            .withState(CrateState.THROWN_AIRBORNE)

        var soundPlayed: String? = null
        resolver.onPlaySound { soundPlayed = it }

        var wallSmashedFired = false
        resolver.onWallSmashed { _, col, row ->
            wallSmashedFired = true
            assertEquals(1, col)
            assertEquals(1, row)
        }

        val result = resolver.resolveCrateVsTilemap(crate, tilemap)

        assertTrue(result is CrateImpactResult.WallSmash)
        assertTrue(wallSmashedFired)
        assertEquals(CrateState.SHATTERED, crate.state)
        assertEquals("crate_smash", soundPlayed)
    }

    @Test
    fun testDuckingWhileCarryingProvidesDefensiveCoverAgainstLateralEnemyContact() {
        val combatSystem = PlatformerCombatSystem()
        val player = PlatformerPlayer(position = Vec2(100f, 64f))
        val crate = ThrowableCrate(position = Vec2(100f, 64f))
        val goomba = GoombaEnemy(position = Vec2(108f, 64f), direction = -1f)

        // Player picks up crate
        player.pickUpCrate(crate)
        assertTrue(player.isCarrying)

        // Player ducks -> pulls crate over head/body
        player.isDucking = true
        player.update(0.016f, GameContext())

        // Crate is pulled down over player at (0f, 0f) offset
        assertEquals(player.position.x, crate.position.x, 0.001f)
        assertEquals(player.position.y, crate.position.y, 0.001f)
        assertTrue(player.isCrateShielded)

        var crateDefendedFired = false
        combatSystem.onCrateDefended { enemy ->
            crateDefendedFired = true
            assertEquals(goomba, enemy)
        }

        // Enemy attempts lateral contact with player
        val result = combatSystem.resolvePlayerEnemyCollision(player, goomba)

        // Player should be immune to lateral contact (returns None)
        assertEquals(CombatCollisionResult.None, result)
        assertTrue(crateDefendedFired)
        assertTrue("Player should still be alive and not hurt", player.isAlive)
        assertFalse(player.isDefeated)
        assertEquals(PlayerTier.SMALL, player.tier)

        // Enemy should bounce / reverse direction
        assertEquals(1f, goomba.direction, 0.001f)
    }
}
