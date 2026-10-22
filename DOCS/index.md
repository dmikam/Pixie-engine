# 🧚 Pixie Engine Documentation

Welcome to the **Pixie Engine** documentation. Pixie Engine is a lightweight, zero-allocation 2D pixel-art game engine written in pure Kotlin with native Android Jetpack Compose hardware adapters.

Whether you are a junior developer writing your first 2D mobile game or a third-party developer building a custom title, this documentation provides clear, modular guides and copy-pasteable recipes.

---

## 🚀 Quick Architecture Overview

Pixie Engine is split into two cleanly decoupled Gradle modules following the **Ports & Adapters (Hexagonal)** architecture:

```
┌────────────────────────────────────────────────────────┐
│                   YOUR GAME (:app)                     │
│       Entities • Levels • Weapons • Game Screens       │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│                 :pixie-engine (Pure Kotlin)            │
│  Fixed Loop • Camera • LayerStack • Physics • Pools    │
│  Particles • Weapon Trajectories • GameContext         │
└──────────────────────────┬─────────────────────────────┘
                           │ Ports (Pure Interfaces)
┌──────────────────────────▼─────────────────────────────┐
│                :pixie-android (Platform)               │
│  PixieGameCanvas • Canvas Renderer • SoundPool Audio   │
│  Vibrator Haptics • Touch Drag & Gesture Inputs        │
└────────────────────────────────────────────────────────┘
```

1. **`:pixie-engine`** *(Pure Kotlin JVM)*: Contains game loop, entity component models, AABB physics, layer management, continuous difficulty, zero-allocation projectile/particle pools, and trajectory math. Zero Android dependencies—runs in standard JVM unit tests!
2. **`:pixie-android`** *(Android / Jetpack Compose)*: Hardware adapters implementing engine ports—Canvas rendering, hardware-accelerated `SoundPool` audio synthesis, device vibrator haptics, and multi-touch input.

---

## 📖 Documentation Index

Explore the topic guides below to start building your game:

| Document | Description |
| :--- | :--- |
| **[Getting Started](getting-started.md)** | Engine setup, virtual resolution, Jetpack Compose canvas embedding, and game lifecycle control. |
| **[Architecture & Design](architecture.md)** | Core architectural principles, Ports & Adapters pattern, zero-allocation philosophy, and headless testing. |
| **[Game Loop & Camera](game-loop-and-camera.md)** | 60 FPS deterministic fixed-timestep loop, `PixieCamera`, and view modes (`HorizontalScroller`, `VerticalScroller`, `FixedScreen`). |
| **[Layers & Rendering](layers-and-rendering.md)** | `LayerStack`, 5-layer parallax scrolling, `PixelSprite` design, and solid vs retro pixel-gap styles. |
| **[Entities & Physics](entities-and-physics.md)** | `PixieObject` lifecycle (`onSpawn`, `update`, `render`, `onCollision`), players, enemies, collectibles, and AABB collision resolution. |
| **[Weapons & Projectiles](weapons-and-projectiles.md)** | Self-encapsulated `WeaponDefinition`, zero-allocation `ProjectilePool`, and trajectories (`Linear`, `Spread`, `SineWave`, `Homing`). |
| **[Particles & Visual Effects](particles-and-effects.md)** | Procedural explosion archetypes, zero-allocation `ParticlePool`, screen shake, and real-time `ColorMatrix` saturation recovery. |
| **[Context & Difficulty](context-and-difficulty.md)** | `GameContext`, continuous difficulty scaling (`0..100`), presets (`EASY`, `MEDIUM`, `DIFFICULT`, `HEAVY_METAL`), and dynamic property formulas. |
| **[Audio & Input](audio-and-input.md)** | Procedural 16-bit sound synthesis, low-latency `SoundPool` integration, haptic feedback, and dual-mode touch controls. |
| **[Cookbook: First 2D Game](cookbook.md)** | Step-by-step complete tutorial: Build "Gem Catcher", a working 2D arcade game from scratch in under 80 lines. |

---

## 🎮 Reference Proof-of-Concepts (PoCs)

The repository includes two fully completed reference games demonstrating the engine across two distinct paradigms:
- **[Dinorun](roadmap.md#1--dinorun-horizontal-endless-scroller--closed)**: Horizontal endless runner featuring 5-layer parallax, procedural chunk generation, and real-time color recovery.
- **[Raptor](raptor-roadmap.md)**: Vertical top-down combat shmup featuring high-density bullet pools, homing missiles, procedural particle explosions, ground destructibles, and multi-stage boss encounters.

---

## 🛠️ Contributing & Standards

- **Kotlin Idiomatic**: Pure data classes, extension functions, sealed hierarchies, and coroutine-friendly primitives.
- **GC-Friendly**: Do not allocate objects (`new`, `List` creation) inside `update()` or `render()` ticks. Always use `PixiePool<T>`.
- **Decoupled**: Engine code inside `:pixie-engine` must never import Android SDK classes (`android.*`).
