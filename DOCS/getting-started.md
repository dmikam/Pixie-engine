# Getting Started with Pixie Engine

This guide walks you through setting up Pixie Engine and launching your first pixel-art game in Android using Jetpack Compose.

---

## 1. Project Dependencies

Pixie Engine is split into two modules. In your application's `app/build.gradle.kts`, add:

```kotlin
dependencies {
    implementation(project(":pixie-engine"))
    implementation(project(":pixie-android"))
}
```

- **`:pixie-engine`**: Pure Kotlin game engine core (game loop, math, camera, physics, entities, weapons, particles).
- **`:pixie-android`**: Jetpack Compose canvas renderer, touch input, audio synthesizer, and haptic adapters.

---

## 2. Instantiating the Engine

Create a `PixieEngine` instance within your ViewModel or Composable state coordinator.

```kotlin
import com.pixie.engine.core.PixieEngine
import com.pixie.engine.core.camera.GameViewMode
import com.pixie.engine.core.ports.VirtualResolution

val engine = PixieEngine(
    // 1. Virtual Resolution: All game coordinates work in this fixed pixel space
    virtualResolution = VirtualResolution(width = 320, height = 180),
    
    // 2. View Mode: Camera tracking paradigm
    viewMode = GameViewMode.HorizontalScroller(), // or VerticalScroller(), FixedScreen()
    
    // 3. Pixel Style: 0 = crisp solid retro pixels, 1 = simulated grid gap
    pixelsGap = 0
)
```

### Choosing Virtual Resolution
Pixie Engine operates on a fixed virtual resolution regardless of physical device screen size:
- **`320 x 180`** (16:9 Landscape): Ideal for horizontal runners (e.g., *Dinorun*) or side platformers.
- **`240 x 320`** (3:4 Portrait): Ideal for vertical arcade shmups (e.g., *Raptor*).
- **`256 x 224`** (SNES Classic): Ideal for retro-styled fixed arenas (e.g., *Arkanoid*).

The engine's renderer automatically scales and letterboxes the virtual coordinate system to fit any physical screen or foldable aspect ratio while preserving pixel-perfect alignment.

---

## 3. Embedding into Jetpack Compose

Use the provided `PixieGameCanvas` composable to host the engine in any Compose screen:

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pixie.android.ui.PixieGameCanvas
import com.pixie.engine.core.PixieEngine

@Composable
fun GameScreen(engine: PixieEngine) {
    PixieGameCanvas(
        engine = engine,
        modifier = Modifier.fillMaxSize()
    )
}
```

`PixieGameCanvas` handles:
- Synchronizing frame updates with Android's hardware `withFrameNanos` VSync loop.
- Calling `engine.tick(deltaSeconds)` at a deterministic 60 FPS.
- Scaling and centering the virtual canvas onto native Android hardware `Canvas`.

---

## 4. Connecting Audio, Haptics & Input

Plug in the native Android adapters to enable audio, vibration, and touch input:

```kotlin
import android.content.Context
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.android.input.DualModeInputAdapter

fun setupHardware(context: Context, engine: PixieEngine) {
    // Low-latency SoundPool with retro synthesized WAV audio
    val audioAdapter = AndroidSoundPoolAudioAdapter(context)
    engine.audio = audioAdapter

    // Android Vibrator haptic effects
    val hapticAdapter = AndroidHapticAdapter(context)
    engine.haptics = hapticAdapter

    // Dual-mode touch inputs (Buttons or Gestures)
    val inputAdapter = DualModeInputAdapter()
    engine.input = inputAdapter
}
```

---

## 5. Lifecycle Management

Control the engine loop according to Android lifecycle events (e.g., `DisposableEffect`, Activity/Fragment lifecycle, or pause dialogs):

```kotlin
// Start the game loop
engine.start()

// Pause when the game is suspended or a pause menu is shown
engine.pause()

// Resume gameplay
engine.resume()

// Stop the loop completely upon leaving the screen
engine.stop()

// Reset all game state, score, camera, and pools to begin anew
engine.resetGame()
```

---

## Next Steps
- Learn how the engine is structured in [Architecture & Design](architecture.md).
- Understand the camera and loop in [Game Loop & Camera](game-loop-and-camera.md).
- Or jump directly into code with [Cookbook: Build a 2D Game in 15 Minutes](cookbook.md).
