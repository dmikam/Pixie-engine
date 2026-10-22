package com.example.arkanoid.world

import com.pixie.engine.core.matrix.BrickCell
import com.pixie.engine.core.matrix.BrickType
import com.pixie.engine.core.matrix.PowerUpType

data class ArkanoidLevel(
    val levelNumber: Int,
    val name: String,
    val rows: Int,
    val cols: Int,
    val bricks: List<BrickCell?>
)

object ArkanoidLevelCatalog {

    fun getLevel(levelNumber: Int): ArkanoidLevel {
        return when (levelNumber) {
            1 -> createLevel1()
            2 -> createLevel2()
            3 -> createLevel3()
            else -> createLevel1()
        }
    }

    private fun createLevel1(): ArkanoidLevel {
        val rows = 5
        val cols = 12
        val list = mutableListOf<BrickCell?>()

        val rowColors = listOf(
            0xFFFF1744.toInt(), // Red
            0xFFFF9100.toInt(), // Orange
            0xFFFFEA00.toInt(), // Yellow
            0xFF00E676.toInt(), // Green
            0xFF00E5FF.toInt()  // Cyan
        )

        for (r in 0 until rows) {
            val color = rowColors[r]
            for (c in 0 until cols) {
                val isArmored = (r == 4 && (c == 0 || c == 1 || c == 5 || c == 6 || c == 10 || c == 11))
                val powerUp = when {
                    r == 0 && c == 5 -> PowerUpType.LASER
                    r == 1 && c == 2 -> PowerUpType.WIDE_PADDLE
                    r == 2 && c == 9 -> PowerUpType.MULTI_BALL
                    r == 3 && c == 6 -> PowerUpType.SLOW_BALL
                    r == 4 && c == 6 -> PowerUpType.SAFETY_BARRIER
                    else -> null
                }

                if (isArmored) {
                    list.add(
                        BrickCell(
                            row = r,
                            col = c,
                            type = BrickType.ARMORED,
                            hp = 2,
                            maxHp = 2,
                            color = 0xFFB0BEC5.toInt(),
                            score = 250,
                            powerUp = powerUp
                        )
                    )
                } else {
                    list.add(
                        BrickCell(
                            row = r,
                            col = c,
                            type = BrickType.STANDARD,
                            hp = 1,
                            maxHp = 1,
                            color = color,
                            score = (rows - r) * 30 + 50,
                            powerUp = powerUp
                        )
                    )
                }
            }
        }

        return ArkanoidLevel(1, "Neon Perimeter", rows, cols, list)
    }

    private fun createLevel2(): ArkanoidLevel {
        val rows = 6
        val cols = 12
        val list = mutableListOf<BrickCell?>()

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                // Pyramid / diamond stepped mask
                val distFromCenter = kotlin.math.abs(c - 5.5f)
                val isInsidePyramid = distFromCenter <= (r + 1.2f)

                if (!isInsidePyramid) {
                    list.add(null)
                    continue
                }

                val isExplosive = (r == 3 && (c == 5 || c == 6))
                val isArmored = (r == 5 && (c == 2 || c == 9))
                val powerUp = when {
                    r == 1 && c == 5 -> PowerUpType.PLASMA_BALL
                    r == 2 && c == 4 -> PowerUpType.WIDE_PADDLE
                    r == 4 && c == 7 -> PowerUpType.LASER
                    r == 4 && c == 4 -> PowerUpType.MULTI_BALL
                    else -> null
                }

                val cell = when {
                    isExplosive -> BrickCell(
                        row = r,
                        col = c,
                        type = BrickType.EXPLOSIVE,
                        hp = 1,
                        maxHp = 1,
                        color = 0xFFFF3D00.toInt(),
                        score = 400,
                        powerUp = powerUp
                    )
                    isArmored -> BrickCell(
                        row = r,
                        col = c,
                        type = BrickType.ARMORED,
                        hp = 2,
                        maxHp = 2,
                        color = 0xFFB0BEC5.toInt(),
                        score = 300,
                        powerUp = powerUp
                    )
                    else -> BrickCell(
                        row = r,
                        col = c,
                        type = BrickType.STANDARD,
                        hp = 1,
                        maxHp = 1,
                        color = if (r % 2 == 0) 0xFFD500F9.toInt() else 0xFF651FFF.toInt(),
                        score = 120,
                        powerUp = powerUp
                    )
                }
                list.add(cell)
            }
        }

        return ArkanoidLevel(2, "Pyramid Fortress", rows, cols, list)
    }

    private fun createLevel3(): ArkanoidLevel {
        val rows = 7
        val cols = 12
        val list = mutableListOf<BrickCell?>()

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val isGoldObstacle = (r == 2 && (c == 3 || c == 8)) || (r == 4 && (c == 5 || c == 6))
                val isExplosive = (r == 1 && (c == 2 || c == 9)) || (r == 5 && (c == 3 || c == 8))
                val isArmored = (r == 0) || (r == 3 && (c == 0 || c == 11))

                val powerUp = when {
                    r == 0 && c == 5 -> PowerUpType.SAFETY_BARRIER
                    r == 3 && c == 5 -> PowerUpType.PLASMA_BALL
                    r == 5 && c == 2 -> PowerUpType.MULTI_BALL
                    r == 6 && c == 6 -> PowerUpType.LASER
                    else -> null
                }

                val cell = when {
                    isGoldObstacle -> BrickCell(
                        row = r,
                        col = c,
                        type = BrickType.UNBREAKABLE,
                        hp = 999,
                        maxHp = 999,
                        color = 0xFFFFD700.toInt(),
                        score = 10
                    )
                    isExplosive -> BrickCell(
                        row = r,
                        col = c,
                        type = BrickType.EXPLOSIVE,
                        hp = 1,
                        maxHp = 1,
                        color = 0xFFFF3D00.toInt(),
                        score = 500,
                        powerUp = powerUp
                    )
                    isArmored -> BrickCell(
                        row = r,
                        col = c,
                        type = BrickType.ARMORED,
                        hp = 3,
                        maxHp = 3,
                        color = 0xFF78909C.toInt(),
                        score = 350,
                        powerUp = powerUp
                    )
                    else -> BrickCell(
                        row = r,
                        col = c,
                        type = BrickType.STANDARD,
                        hp = 1,
                        maxHp = 1,
                        color = when (r % 3) {
                            0 -> 0xFF00E5FF.toInt()
                            1 -> 0xFFFF80AB.toInt()
                            else -> 0xFF76FF03.toInt()
                        },
                        score = 150,
                        powerUp = powerUp
                    )
                }
                list.add(cell)
            }
        }

        return ArkanoidLevel(3, "Cyber Citadel", rows, cols, list)
    }
}
