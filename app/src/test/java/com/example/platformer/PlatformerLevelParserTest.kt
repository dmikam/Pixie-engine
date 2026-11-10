package com.example.platformer

import com.example.platformer.entities.GoombaEnemy
import com.example.platformer.entities.KoopaEnemy
import com.example.platformer.entities.crate.CrateState
import com.example.platformer.entities.crate.ThrowableCrate
import com.example.platformer.world.PlatformerLevelParser
import com.example.platformer.world.SpawnType
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.platformer.BlockContent
import com.pixie.engine.core.tilemap.TileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformerLevelParserTest {

    @Test
    fun testParseBasicTilesAndGround() {
        val ascii = """
            ..........
            ..?.M.B...
            .[].||....
            ##########
        """.trimIndent()

        val stage = PlatformerLevelParser.parse(ascii, tileSize = 16f)

        assertEquals(10, stage.cols)
        assertEquals(4, stage.rows)
        assertEquals(160f, stage.stageWidthPx, 0.001f)
        assertEquals(64f, stage.stageHeightPx, 0.001f)

        // Check ground row 3
        for (c in 0 until 10) {
            assertEquals(TileType.GROUND, stage.tilemap.getTile(c, 3)?.type)
        }

        // Row 1: ? (coin), M (mushroom), B (brick)
        assertEquals(TileType.QUESTION_BLOCK, stage.tilemap.getTile(2, 1)?.type)
        assertEquals(BlockContent.Coin, stage.blockController.getBlockContent(2, 1))

        assertEquals(TileType.QUESTION_BLOCK, stage.tilemap.getTile(4, 1)?.type)
        assertEquals(BlockContent.SuperMushroom, stage.blockController.getBlockContent(4, 1))

        assertEquals(TileType.BRICK, stage.tilemap.getTile(6, 1)?.type)

        // Row 2: pipe top and pipe body
        assertEquals(TileType.PIPE_TOP_LEFT, stage.tilemap.getTile(1, 2)?.type)
        assertEquals(TileType.PIPE_TOP_RIGHT, stage.tilemap.getTile(2, 2)?.type)
        assertEquals(TileType.PIPE_BODY_LEFT, stage.tilemap.getTile(4, 2)?.type)
    }

    @Test
    fun testParseEntitiesSpawning() {
        val ascii = """
            ..........
            ....C.....
            .G....K...
            ##########
        """.trimIndent()

        val stage = PlatformerLevelParser.parse(ascii, tileSize = 16f)

        assertEquals(1, stage.goombas.size)
        assertEquals(1, stage.koopas.size)
        assertEquals(1, stage.crates.size)
        assertEquals(3, stage.allEntities.size)

        // Goomba at col 1, row 2
        val goomba = stage.goombas.first()
        assertEquals(16f, goomba.position.x, 0.001f)
        assertEquals(32f, goomba.position.y, 0.001f)
        assertTrue(goomba.isAlive)

        // Koopa at col 6, row 2 (foot aligns with row 3 ground)
        val koopa = stage.koopas.first()
        assertEquals(96f, koopa.position.x, 0.001f)
        assertEquals(24f, koopa.position.y, 0.001f) // 2 * 16 - (24 - 16) = 24f
        assertTrue(koopa.isAlive)

        // Crate at col 4, row 1
        val crate = stage.crates.first()
        assertEquals(64f, crate.position.x, 0.001f)
        assertEquals(16f, crate.position.y, 0.001f)
        assertEquals(CrateState.PLACED_ON_GROUND, crate.state)
        assertTrue(crate.isAlive)
    }

    @Test
    fun testPlayerSpawnParsingAndFallback() {
        val withPlayer = """
            .P........
            ##########
        """.trimIndent()

        val stage1 = PlatformerLevelParser.parse(withPlayer, tileSize = 16f)
        assertEquals(Vec2(16f, 0f), stage1.playerSpawnPosition)

        val withoutPlayer = """
            ..........
            ##########
        """.trimIndent()

        val stage2 = PlatformerLevelParser.parse(withoutPlayer, tileSize = 16f)
        assertEquals(PlatformerLevelParser.DEFAULT_PLAYER_SPAWN, stage2.playerSpawnPosition)
    }

    @Test
    fun testFlagpoleParsingAndScoreCalculation() {
        val ascii = """
            ..^..
            ..F..
            ..F..
            ..F..
            #####
        """.trimIndent()

        val stage = PlatformerLevelParser.parse(ascii, tileSize = 16f)
        val flagpole = stage.flagpole
        assertNotNull(flagpole)
        flagpole!!

        assertEquals(2, flagpole.col)
        assertEquals(0, flagpole.topRow)
        assertEquals(3, flagpole.bottomRow)
        assertEquals(0f, flagpole.topBallY, 0.001f)
        assertEquals(64f, flagpole.baseGroundY, 0.001f)

        // Test height-based score calculations
        // High touch (near top ball, y=5)
        assertEquals(5000L, flagpole.calculateScore(5f))
        // Mid-high touch (y=20)
        assertEquals(2000L, flagpole.calculateScore(20f))
        // Middle touch (y=34)
        assertEquals(1000L, flagpole.calculateScore(34f))
        // Low touch (y=45)
        assertEquals(400L, flagpole.calculateScore(45f))
        // Bottom touch (y=60)
        assertEquals(100L, flagpole.calculateScore(60f))
    }

    @Test
    fun testCastleFortressParsing() {
        val ascii = """
            ...***...
            ...***...
            ...***...
            #########
        """.trimIndent()

        val stage = PlatformerLevelParser.parse(ascii, tileSize = 16f)
        val castle = stage.castle
        assertNotNull(castle)
        castle!!

        assertEquals(3, castle.minCol)
        assertEquals(5, castle.maxCol)
        assertEquals(0, castle.minRow)
        assertEquals(2, castle.maxRow)

        assertEquals(48f, castle.bounds.left, 0.001f)
        assertEquals(0f, castle.bounds.top, 0.001f)
        assertEquals(48f, castle.bounds.width, 0.001f)
        assertEquals(48f, castle.bounds.height, 0.001f)
    }

    @Test
    fun testRespawnEntitiesReproducibility() {
        val ascii = """
            ..........
            ....C.....
            .G....K...
            ##########
        """.trimIndent()

        val stage = PlatformerLevelParser.parse(ascii, tileSize = 16f)
        assertEquals(3, stage.spawnPoints.size)

        // Respawn a fresh bundle of entities
        val freshBundle = stage.respawnEntities()
        assertEquals(1, freshBundle.goombas.size)
        assertEquals(1, freshBundle.koopas.size)
        assertEquals(1, freshBundle.crates.size)
        assertEquals(3, freshBundle.allEntities.size)

        // Ensure newly instantiated objects have identical positions
        assertEquals(stage.goombas[0].position, freshBundle.goombas[0].position)
        assertEquals(stage.koopas[0].position, freshBundle.koopas[0].position)
        assertEquals(stage.crates[0].position, freshBundle.crates[0].position)
    }

    @Test
    fun testFluentBuilderConfiguration() {
        val parser = PlatformerLevelParser()
            .withTileSize(32f)
            .withDefaultPlayerSpawn(Vec2(64f, 128f))
            .withGoombaPatrolSpeed(45f)
            .withKoopaPatrolSpeed(40f)
            .withKoopaShellSpeed(200f)

        val stage = parser.parse(listOf("G", "#"))
        assertEquals(32f, stage.tileSize, 0.001f)
        assertEquals(45f, stage.goombas[0].patrolSpeed, 0.001f)
    }
}
