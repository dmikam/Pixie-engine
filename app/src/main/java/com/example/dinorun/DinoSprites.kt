package com.example.dinorun

import com.pixie.engine.core.sprite.AnimationFrame
import com.pixie.engine.core.sprite.AnimationSequence
import com.pixie.engine.core.sprite.PixelSprite

/**
 * Pixel-art sprite definitions for the Dinorun proof-of-concept client.
 */
object DinoSprites {

    // Palette mapping
    private val P = mapOf(
        '.' to 0x00000000,
        'D' to 0xFF2E7D32.toInt(), // Dino forest green
        'd' to 0xFF1B5E20.toInt(), // Dino dark shade
        'E' to 0xFFFFFFFF.toInt(), // Eye white
        'B' to 0xFF000000.toInt(), // Eye pupil / black
        'C' to 0xFF43A047.toInt(), // Cactus bright green
        'c' to 0xFF2E7D32.toInt(), // Cactus outline
        'P' to 0xFFD84315.toInt(), // Pterodactyl wing red
        'p' to 0xFFBF360C.toInt(), // Pterodactyl body
        'G' to 0xFFFFD700.toInt(), // Gold crystal
        'g' to 0xFFFF8F00.toInt(), // Gold shade
        'M' to 0xFFFF5722.toInt(), // Meteor flame
        'm' to 0xFFD32F2F.toInt(), // Meteor core
        'S' to 0xFF00E5FF.toInt(), // Cyan primal spark
        's' to 0xFF00B0FF.toInt()  // Cyan spark shade
    )

    // Dino Run Frame 1 (16x16)
    val DINO_RUN_1 = PixelSprite.fromAscii(
        listOf(
            ".......DDDD.....",
            "......DDDDDD....",
            "......DDEBBD....",
            "......DDDDDD....",
            "......DDDD......",
            ".....DDDDDD.....",
            "..D.DDDDDD......",
            "..DDDDDDDDDD....",
            "..DDDDDDD.......",
            "...DDDDDD.......",
            "....DDDDD.......",
            ".....DDD........",
            ".....D.D........",
            ".....D..DD......",
            "....DD..........",
            "................"
        ),
        P
    )

    // Dino Run Frame 2 (16x16)
    val DINO_RUN_2 = PixelSprite.fromAscii(
        listOf(
            ".......DDDD.....",
            "......DDDDDD....",
            "......DDEBBD....",
            "......DDDDDD....",
            "......DDDD......",
            ".....DDDDDD.....",
            "..D.DDDDDD......",
            "..DDDDDDDDDD....",
            "..DDDDDDD.......",
            "...DDDDDD.......",
            "....DDDDD.......",
            ".....DDD........",
            ".....D.D........",
            "....DD..D.......",
            "........DD......",
            "................"
        ),
        P
    )

    // Dino Jump Frame
    val DINO_JUMP = PixelSprite.fromAscii(
        listOf(
            ".......DDDD.....",
            "......DDDDDD....",
            "......DDEBBD....",
            "......DDDDDD....",
            "......DDDD......",
            ".....DDDDDD.....",
            "..D.DDDDDD......",
            "..DDDDDDDDDD....",
            "..DDDDDDD.......",
            "...DDDDDD.......",
            "....DDDDD.......",
            ".....DDD........",
            "....D...D.......",
            "...DD...DD......",
            "................",
            "................"
        ),
        P
    )

    // Dino Duck Frame 1 (20x12)
    val DINO_DUCK_1 = PixelSprite.fromAscii(
        listOf(
            "....................",
            ".........DDDDDD.....",
            "........DDDEBBD.....",
            "..DD...DDDDDDDD.....",
            ".DDDDDDDDDDDD.......",
            "..DDDDDDDDDD........",
            "...DDDDDDDDD........",
            ".....DDDDDDD........",
            "......DD..DD........",
            ".....DD....DD.......",
            "....................",
            "...................."
        ),
        P
    )

    // Dino Duck Frame 2 (20x12)
    val DINO_DUCK_2 = PixelSprite.fromAscii(
        listOf(
            "....................",
            ".........DDDDDD.....",
            "........DDDEBBD.....",
            "..DD...DDDDDDDD.....",
            ".DDDDDDDDDDDD.......",
            "..DDDDDDDDDD........",
            "...DDDDDDDDD........",
            ".....DDDDDDD........",
            "......D...D.........",
            ".....DD....D........",
            "............DD......",
            "...................."
        ),
        P
    )

    // Dino Hit / Stumble
    val DINO_HIT = PixelSprite.fromAscii(
        listOf(
            ".......DDDD.....",
            "......DDDDDD....",
            "......DDBBBD....",
            "......DDDDDD....",
            "......DDDD......",
            ".....DDDDDD.....",
            "..D.DDDDDD......",
            "..DDDDDDDDDD....",
            "..DDDDDDD.......",
            "...DDDDDD.......",
            "....DDDDD.......",
            "....D..D........",
            "...D....D.......",
            "................",
            "................",
            "................"
        ),
        P
    )

    // Small Cactus (12x18)
    val CACTUS_SMALL = PixelSprite.fromAscii(
        listOf(
            "....CCCC....",
            "....CCCC....",
            "..C.CCCC....",
            "..C.CCCC.C..",
            "..C.CCCC.C..",
            "..CCCCCCCc..",
            "..CCCCCCCc..",
            "....CCCC....",
            "....CCCC....",
            "....CCCC....",
            "....CCCC....",
            "....CCCC....",
            "....CCCC....",
            "....CCCC....",
            "....CCCC....",
            "....CCCC....",
            "....CCCC....",
            "...CCCCCC..."
        ),
        P
    )

    // Double Cactus (18x18)
    val CACTUS_DOUBLE = PixelSprite.fromAscii(
        listOf(
            "..CCCC....CCCC....",
            "..CCCC....CCCC....",
            "..CCCC..C.CCCC....",
            "..CCCC..C.CCCC.C..",
            "C.CCCC..C.CCCC.C..",
            "C.CCCC..CCCCCCCc..",
            "CCCCCC....CCCC....",
            "..CCCC....CCCC....",
            "..CCCC....CCCC....",
            "..CCCC....CCCC....",
            "..CCCC....CCCC....",
            "..CCCC....CCCC....",
            "..CCCC....CCCC....",
            "..CCCC....CCCC....",
            "..CCCC....CCCC....",
            "..CCCC....CCCC....",
            "..CCCC....CCCC....",
            ".CCCCCC..CCCCCC..."
        ),
        P
    )

    // Pterodactyl Wing Up (18x14)
    val PTERODACTYL_UP = PixelSprite.fromAscii(
        listOf(
            "...PPP............",
            "..PPPPP...........",
            ".PPPPPPP..........",
            "PPPPPPPPP.........",
            "...PPPPPPP...ppp..",
            "....PPPPPPPPppppp.",
            ".....PPPPPPPpEBpp.",
            "......PPPPPPppppp.",
            ".......PPPP.......",
            "........PP........",
            "..................",
            "..................",
            "..................",
            ".................."
        ),
        P
    )

    // Pterodactyl Wing Down (18x14)
    val PTERODACTYL_DOWN = PixelSprite.fromAscii(
        listOf(
            "..................",
            "..................",
            "..................",
            "..................",
            ".......PP....ppp..",
            "......PPPP..ppppp.",
            ".....PPPPPPPpEBpp.",
            "....PPPPPPPPppppp.",
            "...PPPPPPP........",
            "PPPPPPPPP.........",
            ".PPPPPPP..........",
            "..PPPPP...........",
            "...PPP............",
            ".................."
        ),
        P
    )

    // Primal Color Spark / Energy Crystal (10x10)
    val COLOR_SPARK = PixelSprite.fromAscii(
        listOf(
            "....SS....",
            "...SSSS...",
            "..SSSSSS..",
            ".SSSSSSSS.",
            "SSSSSSSSSS",
            "SSSSSSSSSS",
            ".SSSSSSSS.",
            "..SSSSSS..",
            "...SSSS...",
            "....SS...."
        ),
        P
    )

    // Volcanic Meteor (12x12)
    val METEOR = PixelSprite.fromAscii(
        listOf(
            "....MMMM....",
            "...MMMMMM...",
            "..MMmmmmMM..",
            ".MMmmmmmmMM.",
            "MMmmmmmmmmMM",
            "MMmmmmmmmmMM",
            "MMmmmmmmmmMM",
            "MMmmmmmmmmMM",
            ".MMmmmmmmMM.",
            "..MMmmmmMM..",
            "...MMMMMM...",
            "....MMMM...."
        ),
        P
    )

    val dinoRunAnim = AnimationSequence(
        name = "run",
        frames = listOf(
            AnimationFrame(DINO_RUN_1, 0.12f),
            AnimationFrame(DINO_RUN_2, 0.12f)
        )
    )

    val dinoDuckAnim = AnimationSequence(
        name = "duck",
        frames = listOf(
            AnimationFrame(DINO_DUCK_1, 0.12f),
            AnimationFrame(DINO_DUCK_2, 0.12f)
        )
    )

    val dinoJumpAnim = AnimationSequence(
        name = "jump",
        frames = listOf(
            AnimationFrame(DINO_JUMP, 1.0f)
        )
    )

    val pterodactylAnim = AnimationSequence(
        name = "fly",
        frames = listOf(
            AnimationFrame(PTERODACTYL_UP, 0.18f),
            AnimationFrame(PTERODACTYL_DOWN, 0.18f)
        )
    )
}
