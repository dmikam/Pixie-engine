package com.pixie.android.input

import com.pixie.engine.core.ports.InputPort

enum class ControlMode {
    VIRTUAL_BUTTONS,
    TOUCH_GESTURES
}

/**
 * Standardized input adapter supporting on-screen D-Pad/Buttons and touch gestures
 * with real-time toggle between the two modes.
 */
class DualModeInputAdapter : InputPort {

    var currentMode: ControlMode = ControlMode.VIRTUAL_BUTTONS

    override var moveX: Float = 0f
    override var moveY: Float = 0f
    override var isJumpPressed: Boolean = false
    override var isDuckPressed: Boolean = false
    override var isActionPressed: Boolean = false

    // Button states
    fun setButtonJump(pressed: Boolean) {
        if (currentMode == ControlMode.VIRTUAL_BUTTONS) {
            isJumpPressed = pressed
        }
    }

    fun setButtonDuck(pressed: Boolean) {
        if (currentMode == ControlMode.VIRTUAL_BUTTONS) {
            isDuckPressed = pressed
        }
    }

    fun setButtonMoveX(direction: Float) {
        if (currentMode == ControlMode.VIRTUAL_BUTTONS) {
            moveX = direction
        }
    }

    fun setButtonAction(pressed: Boolean) {
        if (currentMode == ControlMode.VIRTUAL_BUTTONS) {
            isActionPressed = pressed
        }
    }

    // Gesture triggers (called by touch/gesture detectors)
    fun triggerGestureJump() {
        if (currentMode == ControlMode.TOUCH_GESTURES) {
            isJumpPressed = true
        }
    }

    fun triggerGestureDuck(active: Boolean) {
        if (currentMode == ControlMode.TOUCH_GESTURES) {
            isDuckPressed = active
        }
    }

    fun releaseGestureJump() {
        if (currentMode == ControlMode.TOUCH_GESTURES) {
            isJumpPressed = false
        }
    }

    fun reset() {
        moveX = 0f
        moveY = 0f
        isJumpPressed = false
        isDuckPressed = false
        isActionPressed = false
    }
}
