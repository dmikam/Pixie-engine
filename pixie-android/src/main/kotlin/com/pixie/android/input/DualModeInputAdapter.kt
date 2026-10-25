package com.pixie.android.input

import android.view.KeyEvent
import com.pixie.engine.core.ports.InputPort

enum class ControlMode {
    VIRTUAL_BUTTONS,
    TOUCH_GESTURES
}

enum class InputSourceType {
    TOUCH,
    GAMEPAD,
    KEYBOARD
}

/**
 * Standardized input adapter supporting on-screen D-Pad/Arcade Buttons,
 * touch gestures, and physical gamepads / keyboards.
 */
class DualModeInputAdapter : InputPort {

    var currentMode: ControlMode = ControlMode.VIRTUAL_BUTTONS
    var activeInputSource: InputSourceType = InputSourceType.TOUCH
        private set

    override var moveX: Float = 0f
    override var moveY: Float = 0f
    override var isJumpPressed: Boolean = false
    override var isDuckPressed: Boolean = false
    override var isActionPressed: Boolean = false
    override var isActionSecondaryPressed: Boolean = false

    // State tracking for physical keys to handle simultaneous press / release
    private var keyLeft = false
    private var keyRight = false
    private var keyUp = false
    private var keyDown = false

    // Virtual Button states (called by on-screen touch arcade buttons)
    fun setButtonJump(pressed: Boolean) {
        if (currentMode == ControlMode.VIRTUAL_BUTTONS) {
            activeInputSource = InputSourceType.TOUCH
            isJumpPressed = pressed
        }
    }

    fun setButtonDuck(pressed: Boolean) {
        if (currentMode == ControlMode.VIRTUAL_BUTTONS) {
            activeInputSource = InputSourceType.TOUCH
            isDuckPressed = pressed
        }
    }

    fun setButtonMoveX(direction: Float) {
        if (currentMode == ControlMode.VIRTUAL_BUTTONS) {
            activeInputSource = InputSourceType.TOUCH
            moveX = direction
        }
    }

    fun setButtonMoveY(direction: Float) {
        if (currentMode == ControlMode.VIRTUAL_BUTTONS) {
            activeInputSource = InputSourceType.TOUCH
            moveY = direction
        }
    }

    fun setButtonAction(pressed: Boolean) {
        if (currentMode == ControlMode.VIRTUAL_BUTTONS) {
            activeInputSource = InputSourceType.TOUCH
            isActionPressed = pressed
        }
    }

    fun setButtonActionSecondary(pressed: Boolean) {
        if (currentMode == ControlMode.VIRTUAL_BUTTONS) {
            activeInputSource = InputSourceType.TOUCH
            isActionSecondaryPressed = pressed
        }
    }

    // Gesture triggers (called by touch/gesture detectors)
    fun triggerGestureJump() {
        if (currentMode == ControlMode.TOUCH_GESTURES) {
            activeInputSource = InputSourceType.TOUCH
            isJumpPressed = true
        }
    }

    fun triggerGestureDuck(active: Boolean) {
        if (currentMode == ControlMode.TOUCH_GESTURES) {
            activeInputSource = InputSourceType.TOUCH
            isDuckPressed = active
        }
    }

    fun releaseGestureJump() {
        if (currentMode == ControlMode.TOUCH_GESTURES) {
            isJumpPressed = false
        }
    }

    /**
     * Intercepts physical key events from external Bluetooth/USB gamepads, arcade sticks,
     * or hardware keyboards.
     * Returns true if the key was consumed by game controls.
     */
    fun handlePhysicalKeyEvent(keyCode: Int, isDown: Boolean): Boolean {
        var consumed = false
        val isGamepadKey = when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_SELECT -> true
            else -> false
        }

        activeInputSource = if (isGamepadKey) InputSourceType.GAMEPAD else InputSourceType.KEYBOARD

        when (keyCode) {
            // Direction Left
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> {
                keyLeft = isDown
                moveX = if (keyLeft && !keyRight) -1f else if (keyRight) 1f else 0f
                consumed = true
            }
            // Direction Right
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
                keyRight = isDown
                moveX = if (keyRight && !keyLeft) 1f else if (keyLeft) -1f else 0f
                consumed = true
            }
            // Direction Up / Jump
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> {
                keyUp = isDown
                moveY = if (keyUp && !keyDown) -1f else if (keyDown) 1f else 0f
                isJumpPressed = isDown
                consumed = true
            }
            // Direction Down / Duck
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> {
                keyDown = isDown
                moveY = if (keyDown && !keyUp) 1f else if (keyUp) -1f else 0f
                isDuckPressed = isDown
                consumed = true
            }
            // Primary Action (Fire / Serve / Jump)
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_J -> {
                isActionPressed = isDown
                consumed = true
            }
            // Secondary Action (Bomb / Missile / Turbo / Super)
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT, KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_K -> {
                isActionSecondaryPressed = isDown
                consumed = true
            }
        }

        return consumed
    }

    fun reset() {
        moveX = 0f
        moveY = 0f
        isJumpPressed = false
        isDuckPressed = false
        isActionPressed = false
        isActionSecondaryPressed = false
        keyLeft = false
        keyRight = false
        keyUp = false
        keyDown = false
    }
}
