package com.pixie.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.pixie.android.input.ControlMode
import com.pixie.android.input.DualModeInputAdapter
import com.pixie.android.render.AndroidCanvasRenderer
import com.pixie.engine.core.PixieEngine
import kotlinx.coroutines.isActive

/**
 * Jetpack Compose host Composable for the Pixie Game Engine.
 *
 * Runs the display frame loop with frame delta calculation, delegates rendering to
 * [AndroidCanvasRenderer], and forces Compose Canvas invalidation on every frame.
 */
@Composable
fun PixieGameCanvas(
    engine: PixieEngine,
    renderer: AndroidCanvasRenderer,
    inputAdapter: DualModeInputAdapter,
    modifier: Modifier = Modifier,
    onFrameTick: ((deltaSeconds: Float) -> Unit)? = null
) {
    var lastFrameTimeNanos by remember { mutableLongStateOf(0L) }
    var frameTick by remember { mutableLongStateOf(0L) }

    // Display frame loop synchronized to screen vsync
    LaunchedEffect(engine) {
        lastFrameTimeNanos = 0L
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos != 0L) {
                    val deltaSeconds = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
                    // Drive engine fixed updates and callback
                    engine.tick(deltaSeconds)
                    onFrameTick?.invoke(deltaSeconds)
                    frameTick = frameTimeNanos // Invalidate canvas
                }
                lastFrameTimeNanos = frameTimeNanos
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(inputAdapter.currentMode) {
                    if (inputAdapter.currentMode == ControlMode.TOUCH_GESTURES) {
                        detectTapGestures(
                            onPress = {
                                inputAdapter.triggerGestureJump()
                                tryAwaitRelease()
                                inputAdapter.releaseGestureJump()
                            }
                        )
                    }
                }
                .pointerInput(inputAdapter.currentMode) {
                    if (inputAdapter.currentMode == ControlMode.TOUCH_GESTURES) {
                        detectDragGestures(
                            onDragEnd = {
                                inputAdapter.triggerGestureDuck(false)
                                inputAdapter.releaseGestureJump()
                            },
                            onDragCancel = {
                                inputAdapter.triggerGestureDuck(false)
                                inputAdapter.releaseGestureJump()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount.y > 10f) {
                                    // Swipe Down -> Duck
                                    inputAdapter.triggerGestureDuck(true)
                                } else if (dragAmount.y < -10f) {
                                    // Swipe Up -> Jump
                                    inputAdapter.triggerGestureJump()
                                }
                            }
                        )
                    }
                }
        ) {
            // Reading frameTick triggers Compose to execute this draw block on every frame
            val _tick = frameTick
            drawIntoCanvas { composeCanvas ->
                val nativeCanvas = composeCanvas.nativeCanvas
                renderer.setupViewport(nativeCanvas, screenWidth, screenHeight)
                engine.render(renderer)
            }
        }
    }
}
