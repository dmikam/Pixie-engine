package com.example.dinorun.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dinorun.entities.DinoPlayer
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.input.ArcadeActionType
import com.pixie.android.input.ArcadeButtonConfig
import com.pixie.android.input.ArcadeButtonShape
import com.pixie.android.input.DualModeInputAdapter
import com.pixie.engine.core.ports.AudioEvent

/**
 * Arcade control configuration factories for Dinorun.
 * Provides modular, reusable button configurations adhering to the Pixie arcade controls standard.
 */
object DinoControls {

    /**
     * Creates the circular Duck arcade button configuration.
     */
    fun createDuckButton(
        inputAdapter: DualModeInputAdapter,
        audioAdapter: AndroidSoundPoolAudioAdapter? = null
    ): ArcadeButtonConfig {
        return ArcadeButtonConfig(
            id = "duck_btn",
            label = "DUCK\n▼",
            shape = ArcadeButtonShape.CIRCLE,
            sizeDp = 64.dp,
            baseColor = Color(0x99212121),
            pressedColor = Color(0xFF424242),
            rimColor = Color(0xCC616161),
            actionType = ArcadeActionType.DUCK,
            onPressDown = {
                inputAdapter.setButtonDuck(true)
                audioAdapter?.playSound(AudioEvent.DUCK)
            },
            onPressUp = {
                inputAdapter.setButtonDuck(false)
            }
        )
    }

    /**
     * Creates the circular Jump arcade button configuration.
     */
    fun createJumpButton(
        inputAdapter: DualModeInputAdapter,
        audioAdapter: AndroidSoundPoolAudioAdapter? = null,
        player: DinoPlayer? = null
    ): ArcadeButtonConfig {
        return ArcadeButtonConfig(
            id = "jump_btn",
            label = "JUMP\n▲",
            shape = ArcadeButtonShape.CIRCLE,
            sizeDp = 76.dp,
            baseColor = Color(0xCC2E7D32),
            pressedColor = Color(0xFF43A047),
            rimColor = Color(0xFF81C784),
            actionType = ArcadeActionType.JUMP,
            onPressDown = {
                val wasGrounded = player?.isGrounded ?: true
                inputAdapter.setButtonJump(true)
                player?.jump()
                if (wasGrounded && (player == null || !player.isGrounded)) {
                    audioAdapter?.playSound(AudioEvent.JUMP)
                }
            },
            onPressUp = {
                inputAdapter.setButtonJump(false)
            }
        )
    }

    /**
     * Creates floating positioned button variants for adjustable layout positioning.
     */
    fun createFloatingButtons(
        jumpBtn: ArcadeButtonConfig,
        duckBtn: ArcadeButtonConfig
    ): List<ArcadeButtonConfig> {
        return listOf(
            duckBtn.copy(
                id = "fl_duck",
                floatingOffset = Offset(24f, 440f)
            ),
            jumpBtn.copy(
                id = "fl_jump",
                floatingOffset = Offset(260f, 430f)
            )
        )
    }
}
