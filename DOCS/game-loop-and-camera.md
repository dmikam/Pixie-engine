# Game Loop & Camera System

Pixie Engine utilizes a deterministic fixed-timestep game loop paired with a flexible 2D camera system supporting multiple game paradigms.

---

## ⏱️ Fixed-Timestep Game Loop (`PixieGameLoop`)

Mobile screens can refresh at variable frequencies: 60Hz, 90Hz, 120Hz, or drop frames when battery saver engages. Physics and gameplay logic must run deterministically regardless of device refresh rate.

```
Hardware VSync (withFrameNanos)
       │
       ▼
   engine.tick(rawDeltaSeconds)
       │
       ├── Accumulate time: accumulator += rawDeltaSeconds
       │
       ├── while (accumulator >= 1/60s) {
       │       updateFixed(1/60s)  <-- Deterministic Physics & Game Logic!
       │       accumulator -= 1/60s
       │   }
       │
       └── render(batch)           <-- Interpolated Frame Draw
```

### Loop Configuration
```kotlin
val loop = PixieGameLoop(
    targetUpdatesPerSecond = 60, // 60 updates/sec = 16.6ms per tick
    maxSubSteps = 5,             // Prevents spiral-of-death on heavy frame drops
    onFixedUpdate = { dt -> updateFixed(dt) },
    onRender = { alpha -> /* Ready for sub-frame interpolation */ }
)
```

- **Deterministic Physics**: Movement, gravity, and collision logic always calculate using fixed `dt = 0.0166f`, eliminating inconsistent jumps or tunneling on high-refresh-rate displays.
- **Spiral-of-Death Protection**: If a frame takes unusually long (e.g., coming back from background), `maxSubSteps` bounds update execution to avoid freeze cycles.

---

## 📷 Camera System (`PixieCamera`)

The `PixieCamera` translates world coordinates into the screen viewport according to the selected `GameViewMode`.

```kotlin
val camera = PixieCamera(
    viewport = VirtualResolution(320, 180),
    viewMode = GameViewMode.HorizontalScroller()
)
```

### Supported View Modes

| View Mode | Used In | Tracking Behavior |
| :--- | :--- | :--- |
| **`GameViewMode.HorizontalScroller`** | *Dinorun* | Follows target along the X-axis. Y-axis remains locked to the world horizon. Supports forward lookahead offset. |
| **`GameViewMode.VerticalScroller`** | *Raptor* | Continuous upward scrolling along the Y-axis. X-axis remains centered or follows player with soft damping. |
| **`GameViewMode.FixedScreen`** | *Arkanoid* | Camera position is fixed at `(0, 0)`. The entire play arena is locked to the virtual resolution rectangle. |

### Camera Positioning & Smoothing

```kotlin
// Track an entity smoothly with linear interpolation (lerp)
camera.target = player
camera.smoothing = 0.1f // 0.0 = instant snap, 0.1 = smooth trailing

// Or snap immediately without interpolation
camera.setPositionImmediate(Vec2(0f, 0f))

// Screen shake (e.g. for explosions or impacts)
camera.shake(intensity = 6f, durationSeconds = 0.25f)
```

---

## 📐 Coordinate Systems & Safe Letterboxing

Pixie Engine uses a virtual pixel coordinate space where `(0, 0)` is the top-left corner:

```
(0, 0) ────────────────────────► +X (Virtual Width, e.g. 320)
  │
  │     World / Viewport Space
  │
  ▼
 +Y (Virtual Height, e.g. 180)
```

When rendered to an Android device screen:
1. `AndroidCanvasRenderer` calculates the maximum uniform scale factor `min(screenWidth / virtualWidth, screenHeight / virtualHeight)`.
2. The virtual canvas is centered on the physical screen.
3. Black letterbox/pillarbox margins are automatically handled, preserving exact pixel aspect ratios without distortion.
4. Input coordinates from `DualModeInputAdapter` are automatically inverse-mapped to virtual pixel coordinates.
