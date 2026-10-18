# Standalone 2D Pixel-Art Game Engine & Proof-of-Concept Client

Extract and architect a standalone, reusable 2D pixel-art game engine library across a multi-module Gradle architecture (`:core-engine`, `:core-ui`, and `:app`), featuring a unified fixed-step game loop, 5-layer strictly ordered rendering pipeline with shared horizon line, ColorMatrix grayscale-to-color restoration, and an Android client featuring the Dinorun endless runner with dual-mode controls.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions have been confirmed and incorporated into the design:

- **Confirmed Decision 1 (Sample Game)**: The proof-of-concept application in `:app` will showcase **Dinorun endless runner with color recovery**, testing horizontal scrolling, dynamic physics, asteroid/pterodactyl hazards, and the grayscale-to-color transformation.
- **Confirmed Decision 2 (Module Architecture)**: Strict multi-module separation:
  - `:core-engine`: Pure Kotlin module (100% platform-independent, zero Android/Compose dependencies).
  - `:core-ui`: Android library module containing hardware/platform adapters (Canvas nearest-neighbor renderer, global Android ColorMatrix grayscale-to-color filter, SoundPool audio adapter, Vibrator haptics, and touch/gesture input handlers).
  - `:app`: Android client application using Jetpack Compose to host the game, HUD, dialogs, and game engine controller.
- **Confirmed Decision 3 (Input Scheme)**: Dual-mode input with real-time toggle between on-screen virtual buttons (D-pad / Jump / Duck) and swipe/tap touch gestures.

---

## 1. Overview & Core Concept

- **What It Does**: Provides a lightweight, high-performance 2D pixel-art game engine capable of powering horizontal scrollers, vertical scrollers, and fixed-screen games. Decouples game logic, physics, and state updates into a pure Kotlin library while packaging standardized Android rendering, audio, haptics, and inputs into reusable adapters.
- **Target Audience**: Mobile game developers and players wanting responsive, retro pixel-art games with 60 FPS deterministic gameplay.
- **Key Value**: Clean separation of concerns. Game simulation can run headlessly or be unit-tested without Android mocks, while the visual presentation features a rich 5-layer parallax atmosphere with synchronized celestial horizon and dynamic color grading.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Launch & Menu Screen**: The player enters the prehistoric world. The world is initially drained of color (ash-grey prehistoric atmosphere).
2. **Gameplay**:
   - Fixed-step 60 FPS runner simulation.
   - Dino runs, jumps over boulders and bones, and ducks under flying Pterodactyls.
   - Day/Night cycle transits across the sky, dynamically coloring stars, sun, and clouds.
   - Collecting primal sparks/energy restores color to the world via the `ColorMatrix` grayscale-to-color recovery pipeline.
3. **Controls**:
   - Player can toggle between **On-Screen Virtual Buttons** (Jump, Duck, Action) and **Direct Touch Gestures** (Tap to jump, Swipe down to duck) via an on-screen HUD pill.
4. **Game Over & Restart**: Clean score summary, distance run, color recovery percentage achieved, and instant restart with state reset.

### Visual Identity & Theme
- **Aesthetic Direction**: 90s retro arcade pixel-art with modern fluid parallax depth and post-processing color grading.
- **Palette & Atmosphere**:
  - *Desaturated / Ash State*: Neutral grays, charcoal stone, pale slate sky.
  - *Full Color Recovery*: Rich jurassic greens (`#388E3C`), volcanic magma orange/red (`#D32F2F`, `#FF5722`), amber sun (`#FFB300`), deep twilight indigo (`#1A237E`).
- **Typography & HUD**: Crisp retro pixel typography, minimal modern M3 HUD chips for score, distance, multiplier, and color recovery progress.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Pure Kotlin `:core-engine` (No Android / Compose imports)
- *Chosen Approach*: Put all math (`Vec2`, `RectF`), physics (AABB collision detection), game loop, entity state machines, chunk generation, and view modes in `:core-engine` with zero platform dependencies.
- *Why*: Allows the engine to be reused across Android, Desktop JVM, and iOS (Kotlin Multiplatform ready), and enables lightning-fast local unit tests without Robolectric or Android runtime overhead.
- *Alternative Considered*: Combining everything into a single Android library module. Rejected because it couples game physics to Android OS lifecycles.

### Decision 2: 5-Layer Stack with Shared Coordinate Horizon
- *Chosen Approach*: Strictly ordered layers with a unified `horizonY` reference coordinate:
  1. **Layer 1 (Sky)**: Celestial body (sun/moon) orbiting around a celestial origin, dynamic day/night ambient color shift.
  2. **Layer 2 (Clouds)**: Atmospheric drift independent of ground movement.
  3. **Layer 3 (Background)**: Volcanoes, mountain peaks, and forests with depth-based parallax factor `parallax = z / (z + 1)`. Non-collidable.
  4. **Layer 4 (Ground / Gamefield)**: Main collision plane with height contours and pit intervals.
  5. **Layer 5 (Game Items)**: Interactive entities (Dino, hazards, collectibles, particle effects).
- *Why*: Eliminates visual disconnects where background mountains or sun appear below or disconnected from the ground plane.

### Decision 3: ColorMatrix Grayscale-to-Color Post-Processing
- *Chosen Approach*: The engine calculates `colorRecoveryProgress: Float` (0.0 to 1.0). The `:core-ui` adapter constructs an Android `ColorMatrix` that smoothly scales saturation from 0% (full monochrome) to 100% (vibrant full color), plus a subtle warm tint as progress nears completion.
- *Why*: Highly efficient GPU-accelerated post-process that applies uniformly to all layers and sprites without requiring dual color/gray sprite assets.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                               :app                                     │
│  - MainActivity & Compose Navigation                                   │
│  - DinorunGameScreen (Compose UI, HUD, Dialogs)                        │
│  - DinoGameViewModel (Orchestrates Loop, State, Persistence)           │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ depends on
                   ┌─────────────────┴─────────────────┐
                   ▼                                   ▼
┌──────────────────────────────────────┐  ┌──────────────────────────────┐
│              :core-ui                │  │         :core-engine         │
│  (Android Library)                   │  │  (Pure Kotlin Library)       │
│                                      │  │                              │
│  - CanvasRenderer (Nearest-Neighbor) │  │  - GameLoop (Fixed Timestep) │
│  - ColorMatrix Recovery Pipeline     │  │  - GameViewMode Strategies   │
│  - SoundPoolAudioAdapter             │  │  - 5-Layer Rendering Stack   │
│  - AndroidHapticAdapter              │  │  - AABB Physics & Collisions │
│  - VirtualController & Gestures      │  │  - Entity State Machine      │
│  - PixelSpriteSheet Cache            │  │  - ChunkManager & Generation │
│                                      │  │  - Ports & Interfaces        │
└──────────────────────────────────────┘  └──────────────────────────────┘
                   │                                   ▲
                   └───────────────────────────────────┘
                       implements Ports & Interfaces
```

### Module Specifications

#### 1. `:core-engine`
- `com.engine.core.loop.GameLoop`: Fixed time step accumulator (e.g. 60 updates/sec, `dt = 0.0166s`), alpha interpolation factor for smooth rendering.
- `com.engine.core.math.Vec2` & `com.engine.core.math.BoundingBox`: Pure Kotlin 2D vector math and AABB collision queries.
- `com.engine.core.viewmode.GameViewMode`:
  - `HorizontalScrollerViewMode`: Tracks player horizontal progress, dynamically updates parallax offsets.
  - `VerticalScrollerViewMode`: Vertical camera scrolling with bounds.
  - `FixedScreenViewMode`: Static arena camera (Arkanoid mode).
- `com.engine.core.layers`:
  - `SkyLayer`: Celestial angle, sun/moon trajectory, star twinkle, day/night ambient color.
  - `CloudLayer`: Independent drift velocity, procedural cloud positions.
  - `ParallaxLayer`: Background mountain/volcano silhouettes with strict z-index depth scaling.
  - `GroundLayer`: Continuous ground segments, scalar height changes, pit generation.
  - `EntitiesLayer`: Player, obstacles, hazards, particles, items.
  - `HorizonConstraint`: Centralized `horizonY` coordinate ensuring perfect horizon alignment between Sky, Background, and Ground.
- `com.engine.core.color.ColorRecoveryTracker`: Progress float `[0..1]`, thresholds, and color unlock events.
- `com.engine.core.ports`:
  - `RenderCommandPort`: Decoupled draw commands (`DrawRect`, `DrawSprite`, `DrawText`, `FillColor`).
  - `AudioPort`: Sound effects trigger interface.
  - `HapticPort`: Haptic feedback interface.
  - `InputPort`: Normalized input state query.

#### 2. `:core-ui`
- `com.engine.ui.render.PixelArtCanvas`: Android Compose/Canvas adapter scaling low-resolution pixel art (e.g. 320x180 virtual resolution) to physical device screens using nearest-neighbor filtering.
- `com.engine.ui.color.ColorMatrixFilterAdapter`: Builds Android `ColorMatrixColorFilter` dynamically from color recovery progress.
- `com.engine.ui.audio.SoundPoolAudioAdapter`: Manages synthesized or sound asset playback for jump, hit, milestone, score, and ambient loops.
- `com.engine.ui.input.DualInputController`: Manages on-screen virtual buttons (D-pad, jump, duck) and gesture detection (touch swipes/taps) with runtime switching.

#### 3. `:app`
- Dinorun client wiring:
  - Game entity setup: Dino runner, Pterodactyls, Cactus clusters, volcanic meteors, primal sparks.
  - Dynamic score, high score tracking, color recovery level indicator.
  - Game Over and Pause dialogs.
  - Settings bar for switching controls and viewing engine metrics (FPS, fixed update count, active entity count).

---

## 5. Implementation Sequence
1. Update `settings.gradle.kts` to register `:core-engine` and `:core-ui`.
2. Configure `libs.versions.toml` and root `build.gradle.kts` for Android library and Kotlin JVM modules.
3. Implement `:core-engine` with pure Kotlin game loop, math, 5-layer stack, view modes, physics, and chunk manager.
4. Implement `:core-ui` with Canvas nearest-neighbor renderer, ColorMatrix filter, SoundPool adapter, and input controls.
5. Implement `:app` client with the Dinorun minigame, Compose screen, HUD, and dual-mode controls.
6. Verify compilation with `compile_applet` and test with Robolectric unit tests.
