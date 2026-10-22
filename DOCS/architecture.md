# Pixie Engine Architecture & Design

Pixie Engine is designed for high performance, portability, and zero-allocation execution on mobile devices. This document outlines the architectural patterns and conventions.

---

## 🏛️ Ports & Adapters (Hexagonal Architecture)

The core engine is isolated from any operating system or graphics library:

```
┌─────────────────────────────────────────────────────────────┐
│                    :pixie-engine (Domain)                   │
│                                                             │
│  Entities • Physics • Camera • Layers • Pools • Math       │
│                                                             │
│   [RenderBatchPort]   [AudioPort]  [HapticPort] [InputPort] │
└───────────▲────────────────▲─────────────▲───────────▲──────┘
            │                │             │           │
┌───────────┴────────────────┴─────────────┴───────────┴──────┐
│                    :pixie-android (Adapters)                │
│                                                             │
│   AndroidCanvasRenderer     AndroidSoundPoolAudioAdapter    │
│   AndroidHapticAdapter      DualModeInputAdapter            │
└─────────────────────────────────────────────────────────────┘
```

### Pure Interfaces (Ports in `:pixie-engine`)

- **`RenderBatchPort`**: Generic 2D batch renderer for drawing pixel rectangles, sprites, motion blur trails, and fonts.
- **`AudioPort`**: Dispatches high-level semantic sound cues (`AudioEvent.JUMP`, `AudioEvent.FIRE_PRIMARY`, etc.).
- **`HapticPort`**: Dispatches haptic vibrations (`HapticFeedbackType.LIGHT`, `MEDIUM`, `HEAVY`, `DOUBLE_PULSE`).
- **`InputPort`**: Normalized input axes (`moveX`, `moveY`) and action states (`isJumpPressed`, `isDuckPressed`, `isActionPressed`).

### Platform Implementations (Adapters in `:pixie-android`)

- **`AndroidCanvasRenderer`**: Translates virtual coordinates to Android's hardware-accelerated `Canvas`, supporting integer aspect ratio scaling and letterboxing.
- **`AndroidSoundPoolAudioAdapter`**: Low-latency playback via Android's native `SoundPool`.
- **`AndroidHapticAdapter`**: Android `Vibrator` / `VibratorManager` execution with API level backward-compatibility.
- **`DualModeInputAdapter`**: Bridges Compose touch events (virtual buttons or directional gestures) to normalized engine inputs.

---

## ♻️ Zero-Allocation (GC-Friendly) Philosophy

Mobile games that allocate temporary objects (like vectors, bullet instances, or collision lists) inside the 60 FPS update or render loop suffer from **Garbage Collection (GC) pauses**, causing micro-stuttering.

Pixie Engine solves this with strict allocation discipline:

1. **`PixiePool<T>`**: Array-backed reusable object storage.
   ```kotlin
   val pool = PixiePool(factory = { Projectile() }, initialCapacity = 64)
   
   // In update loop:
   val bullet = pool.obtain() // O(1), no GC allocation
   bullet.spawn(x, y, vx, vy)
   
   // When bullet expires or hits:
   pool.recycle(bullet) // Reclaims instance without GC
   ```
2. **Pre-allocated Subsystems**:
   - `ProjectilePool` manages up to 200+ simultaneous bullets.
   - `ParticlePool` manages up to 300+ sparks, flames, and smoke puffs.
   - Fixed-size lists for spatial collision queries.
3. **Reusable Data Types**:
   - `Vec2` supports in-place math (`set()`, `add()`, `scale()`) alongside immutable convenience methods.

---

## 🧪 Headless Testing & Portability

Because `:pixie-engine` has **zero dependencies on Android SDK classes** (`android.*`), your entire game logic can be unit-tested on the JVM in milliseconds without needing an emulator or Robolectric.

Example pure JVM test:

```kotlin
@Test
fun testPlayerMovementAndCollision() {
    val engine = PixieEngine(VirtualResolution(320, 180))
    val player = TestPlayer(x = 10f, y = 50f)
    val enemy = TestEnemy(x = 12f, y = 50f)
    
    engine.itemsLayer.add(player)
    engine.itemsLayer.add(enemy)
    
    // Simulate 1 fixed physics frame (1/60th second)
    engine.tick(1f / 60f)
    
    assertTrue(player.wasDamaged)
}
```

---

## 📂 Module Layout

```
├── pixie-engine/
│   └── src/main/kotlin/com/pixie/engine/core/
│       ├── camera/       # PixieCamera & ViewModes
│       ├── chunk/        # ChunkManager terrain recycling
│       ├── color/        # ColorRecoveryPipeline
│       ├── context/      # GameContext & DifficultyPreset
│       ├── entity/       # PixieObject, Player, Enemy, Obstacle
│       ├── layer/        # LayerStack & built-in layers
│       ├── loop/         # Fixed-timestep loop
│       ├── math/         # Vec2 & AABB
│       ├── particle/     # Particle, ParticlePool, Explosions
│       ├── pool/         # PixiePool<T>
│       ├── ports/        # RenderBatchPort, AudioPort, InputPort
│       ├── screen/       # ScreenManager & PixieScreen
│       ├── sprite/       # PixelSprite & Color palettes
│       └── weapon/       # WeaponDefinition, Trajectories, Projectiles
│
└── pixie-android/
    └── src/main/kotlin/com/pixie/android/
        ├── audio/        # SoundPool & RetroAudioSynthesizer
        ├── color/        # Android ColorMatrixAdapter
        ├── haptic/       # Android Vibrator adapter
        ├── input/        # DualModeInputAdapter
        ├── render/       # AndroidCanvasRenderer
        └── ui/           # PixieGameCanvas Jetpack Compose component
```
