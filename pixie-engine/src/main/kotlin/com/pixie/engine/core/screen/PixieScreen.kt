package com.pixie.engine.core.screen

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.ports.RenderBatchPort

/**
 * Standard screen IDs for retro arcade and multi-scene titles.
 */
object StandardScreenIds {
    const val TITLE = "title"
    const val HANGAR_SHOP = "hangar_shop"
    const val GAMEPLAY = "gameplay"
    const val DEBRIEFING = "debriefing"
    const val GAME_OVER = "game_over"
}

/**
 * Screen contract for multi-scene games in Pixie Engine.
 *
 * Models distinct game flow states such as:
 * - Title / Intro screen
 * - Intermission / Base Hangar / Weapon Shop
 * - Active Gameplay / Combat screen
 * - Debriefing / Mission Victory screen
 * - Game Over screen
 */
interface PixieScreen {
    val screenId: String

    /**
     * Called when this screen becomes the active screen.
     */
    fun onEnter(context: GameContext) {}

    /**
     * Updates screen state per frame tick.
     */
    fun update(dt: Float, context: GameContext) {}

    /**
     * Renders screen visuals through the [RenderBatchPort].
     */
    fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {}

    /**
     * Called when this screen is about to be replaced by another screen.
     */
    fun onExit(context: GameContext) {}
}
