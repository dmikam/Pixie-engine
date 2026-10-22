package com.pixie.engine.core.screen

import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.ports.RenderBatchPort

/**
 * Screen lifecycle manager for Pixie Engine games.
 *
 * Manages screen registration, scene transitions, active screen updates,
 * and rendering delegation.
 */
class ScreenManager {
    private val screens = mutableMapOf<String, PixieScreen>()

    var currentScreen: PixieScreen? = null
        private set

    var previousScreenId: String? = null
        private set

    var onScreenChanged: ((fromId: String?, toId: String) -> Unit)? = null

    fun registerScreen(screen: PixieScreen) {
        screens[screen.screenId] = screen
    }

    fun unregisterScreen(screenId: String) {
        if (currentScreen?.screenId == screenId) {
            currentScreen = null
        }
        screens.remove(screenId)
    }

    fun getScreen(screenId: String): PixieScreen? = screens[screenId]

    fun hasScreen(screenId: String): Boolean = screens.containsKey(screenId)

    fun setScreen(screenId: String, context: GameContext): Boolean {
        val nextScreen = screens[screenId] ?: return false
        if (currentScreen === nextScreen) return true

        val oldScreen = currentScreen
        previousScreenId = oldScreen?.screenId
        oldScreen?.onExit(context)

        currentScreen = nextScreen
        nextScreen.onEnter(context)
        onScreenChanged?.invoke(previousScreenId, screenId)
        return true
    }

    fun update(dt: Float, context: GameContext) {
        currentScreen?.update(dt, context)
    }

    fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        currentScreen?.render(batch, camera, context)
    }

    fun clear(context: GameContext) {
        currentScreen?.onExit(context)
        currentScreen = null
        previousScreenId = null
        screens.clear()
    }
}
