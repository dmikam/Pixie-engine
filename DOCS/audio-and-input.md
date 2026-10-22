# Audio & Input Adapters

Pixie Engine provides integrated low-latency procedural audio synthesis and flexible multi-touch input adapters for Android.

---

## 🔊 Procedural Retro Audio Synthesizer

Rather than requiring large external `.mp3` or `.wav` sound assets, Pixie Engine features an algorithmic synthesizer (`RetroAudioSynthesizer`) that generates authentic 8-bit / 16-bit sound waveforms directly in memory:

- **Laser Chirps**: Exponential frequency sweeps (`800Hz -> 200Hz`).
- **Missile Launches**: Modulated low-frequency rockets with white noise hiss.
- **Explosions**: Decaying pink noise bursts for punchy bass impacts.
- **Shield Pings & Gem Chimes**: Ascending dual-tone sine wave arpeggios.

### Synthesizer & Hardware Playback

```kotlin
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.engine.core.ports.AudioEvent

// Initialized with Android Context
val audioAdapter = AndroidSoundPoolAudioAdapter(context)
engine.audio = audioAdapter

// Trigger sounds anywhere via the port:
engine.audio?.playSound(AudioEvent.FIRE_PRIMARY)
engine.audio?.playSound(AudioEvent.EXPLOSION_LARGE)
engine.audio?.playSound(AudioEvent.COLLECT)
```

The adapter:
- Pre-caches synthesized sound buffers into Android's native hardware `SoundPool`.
- Employs zero-lag playback with randomized micro-pitch modulation (`0.96x - 1.04x`) so repetitive shooting sounds natural.
- Supports global muting (`audioAdapter.isMuted = true`).

---

## 📳 Haptic Feedback (`AndroidHapticAdapter`)

Deliver tactile rumble on game actions using `HapticFeedbackType`:

```kotlin
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.engine.core.ports.HapticFeedbackType

val hapticAdapter = AndroidHapticAdapter(context)
engine.haptics = hapticAdapter

// Trigger haptic pulses
engine.haptics?.trigger(HapticFeedbackType.LIGHT)        // UI taps, collecting gems
engine.haptics?.trigger(HapticFeedbackType.MEDIUM)       // Secondary missile firing
engine.haptics?.trigger(HapticFeedbackType.HEAVY)        // Hull collision, taking damage
engine.haptics?.trigger(HapticFeedbackType.DOUBLE_PULSE) // Mega-bomb detonation
```

---

## 🕹️ Input Adapter (`DualModeInputAdapter`)

Pixie Engine supports dual control paradigms selectable by the player:

```kotlin
import com.pixie.android.input.ControlMode
import com.pixie.android.input.DualModeInputAdapter

val inputAdapter = DualModeInputAdapter()
engine.input = inputAdapter

// Switch modes dynamically
inputAdapter.setMode(ControlMode.VIRTUAL_BUTTONS) // On-screen buttons
inputAdapter.setMode(ControlMode.TOUCH_GESTURES)  // Touch drag & swipe gestures
```

### 1. Direct Touch Drag (Ideal for Shmups / Airplanes)
When the player drags a finger across the screen, the input adapter translates touch coordinates from physical pixels to virtual coordinates, properly accounting for aspect-ratio letterboxing:

```kotlin
// In your Composable touch handler or drag gesture:
inputAdapter.updateDirectPosition(virtualTargetX, virtualTargetY)
```

### 2. Virtual Buttons & D-Pad (Ideal for Platformers / Runners)
Buttons directly update the boolean properties of `InputPort`:

```kotlin
inputAdapter.pressJump()
inputAdapter.releaseJump()

inputAdapter.pressDuck()
inputAdapter.releaseDuck()

inputAdapter.setMoveAxes(x = -1.0f, y = 0.0f) // Move Left
```

In your entities, read normalized input via `context.input`:
```kotlin
val input = context.input ?: return
val moveX = input.moveX // -1.0 .. 1.0
val isJumping = input.isJumpPressed
```
