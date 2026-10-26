# Pixie Engine PoC Roadmap & Planification

> **Version:** 0.6.0  
> **Status:** PoC 1 (Dinorun), PoC 2 (Raptor), & PoC 3 (Arkanoid) CLOSED / Validated  
> **Documentation:** [DOCS/index.md](index.md)  

Pixie Engine is an open-source, lightweight 2D pixel-art game engine engineered in pure Kotlin (`:pixie-engine`) with Jetpack Compose Android hardware adapters (`:pixie-android`) and a showcase client (`:app`).

To validate the engine across diverse 2D game paradigms, we designed 4 distinct Proof-of-Concept (PoC) games:
1. **Dinorun** — Horizontal Endless Scroller *(CLOSED / Fully Playable)*
2. **Raptor** — Vertical Arcade Space/Jet Scroller *(CLOSED / Fully Playable)*
3. **Arkanoid** — Fixed-Screen Brick Breaker *(CLOSED / Fully Playable)*
4. **Chiki** — Classic Side-Scrolling Platformer *(Next In Queue - v0.7)*

---

## 🗺️ Master Roadmap & Status Overview

| Game / Component | Paradigm | Engine Focus Area | Status | Target Version |
| :--- | :--- | :--- | :--- | :--- |
| **🎮 Game Hub / Menu** | Multi-Game Launcher | Dynamic game lifecycle & navigation | **CLOSED (Complete)** | v0.4 |
| **🦖 Dinorun** | Horizontal Scroller | 5-layer parallax, ColorMatrix recovery, chunk recycling | **CLOSED (Complete)** | v0.3 |
| **🚀 Raptor** | Vertical Scroller | High-speed vertical camera, bullet pool, procedural particles, synthesized audio | **CLOSED (Complete)** | v0.5 |
| **🧱 Arkanoid** | Fixed Screen | Static bounds, reflection angles, continuous collision detection (CCD) | **CLOSED (Complete)** | v0.6 |
| **⚔️ Chiki** | Classic Platformer | Tilemap collisions, gravity arcs, climbing, one-way ledges | **NEXT UP** | v0.7 |

---

## 1. 🦖 Dinorun (Horizontal Endless Scroller) — CLOSED
- **Genre:** Horizontal Endless Runner / Survival
- **Status:** `CLOSED (Completed & Validated in v0.3)`
- **Key Mechanics:**
  - Running, variable-height jumping, ducking under airborne hazards.
  - Procedural terrain generation with variable obstacles (Cactus clusters, low/high Pterodactyls, Volcanic Meteors).
  - Primal Color Spark pickups that progressively restore world saturation.
  - Day/Night celestial cycle with dynamic sun/moon trajectory and ambient shifts.
- **Engine Subsystems Validated & Promoted to Engine:**
  - `HorizontalScrollerViewMode`: X-axis tracking camera with dynamic horizon reference (`horizonY`).
  - `5-Layer Parallax Stack`: Sky, Cloud, Mountain Background, Ground collision plane, Entity layer.
  - `ChunkManager`: Dynamic spawning, culling, and pooling of terrain chunks.
  - `ColorMatrix Pipeline`: Real-time post-processing saturation recovery (0% grayscale to 100% vibrant color).
  - `DualModeInputAdapter`: Dynamic switching between on-screen virtual buttons and touch swipe/tap gestures.
  - `Layer Inspector`: In-game runtime toggling of individual layers and pixel grid/solid modes.

---

## 2. 🚀 Raptor (Vertical Arcade Scroller) — CLOSED
- **Genre:** Vertical Top-Down Arcade Shoot 'em Up (Shmup)
- **Status:** `CLOSED (Completed & Validated in v0.5)`
- **Key Mechanics:**
  - Continuous vertical upward flight over changing planetary surfaces and cloud covers.
  - Smooth 2D touch drag / virtual analog stick ship steering with screen boundary clamping.
  - Fast-firing laser cannons and homing secondary missiles.
  - Coordinated enemy squadrons attacking in spline and sine wave flight patterns.
  - Destructible ground turrets, aerial bosses with phase transitions, and shield power-ups.
- **Engine Subsystems Validated & Promoted to Engine:**
  - `VerticalScrollerViewMode`: High-speed Y-axis camera movement with background layer velocity multipliers.
  - `High-Density Projectile Pool`: Zero-allocation reuse of 100+ simultaneous player and enemy bullets (`PixiePool<Projectile>`).
  - `Self-Encapsulated Weapon System`: `WeaponDefinition` data contracts with customizable trajectories (`Linear`, `Spread`, `SineWave`, `Homing`).
  - `Procedural Particle & Explosion Subsystem`: Zero-allocation particle pool with configurable explosion archetypes (`SPARK_HIT`, `PLASMA_BURST`, `AERIAL_EXPLOSION`, `GROUND_BLAST`).
  - `Continuous Difficulty & Dynamic Property Scaler`: Continuous `0..100` difficulty scaling based on dynamic context (`GameContext`).
  - `Native Retro Audio Synthesizer`: Offline algorithmic WAV synthesis with low-latency Android `SoundPool` hardware playback.

---

## 3. 🧱 Arkanoid (Fixed-Screen Brick Breaker) — CLOSED
- **Genre:** Fixed-Screen Precision Arcade Action
- **Status:** `CLOSED (Completed & Validated in v0.6)`
- **Key Mechanics:**
  - Horizontal paddle movement along the bottom screen boundary with touch-drag & dual-mode buttons.
  - High-velocity energy ball bouncing off arena walls, paddle segments, and bricks.
  - Dynamic bounce angle modulation based on paddle impact offset.
  - Multi-tier destructible brick grid: Standard colored bricks, 2-3 hit silver armored bricks, explosive TNT blocks with chained AOE detonation, and gold unbreakable obstacles.
  - Falling power capsules: Laser cannons, Wide paddle, Multi-ball (3x), Plasma ball (piercing), Slow ball, and Bottom safety barrier.
  - Escalating combo multiplier system for consecutive brick rebounds without hitting the paddle.
- **Engine Subsystems Validated & Promoted to Engine:**
  - `FixedScreenViewMode`: Static arena camera with strict coordinate letterboxing / aspect ratio locking.
  - `Continuous Collision Detection (CCD)`: Swept circle-to-AABB raycasting and sub-stepped trajectory integration eliminating tunneling at 250+ px/sec.
  - `2D Destructible Matrix Layer` (`DestructibleMatrixLayer`): O(1) indexed block grid with dirty-rect updates, chained explosive reactions, and 3D beveled retro pixel rendering.
  - Vector Math Extensions: Added `dot(other: Vec2)` and `reflect(normal: Vec2)` to `Vec2` for deterministic reflection physics.

---

## 4. ⚔️ Chiki (Classic 2D Platformer) — NEXT UP
- **Genre:** Classic Side-Scrolling Platformer / Action Adventure
- **Status:** `NEXT UP (v0.7)`
- **Key Mechanics:**
  - Hero character with physics-driven run, acceleration, jump apex, and ground slide.
  - Melee attack / magic projectile attack against roaming monsters.
  - Interactive stage structures: Ladders/climbing ropes, one-way pass-through ledges, spikes, moving platforms.
  - Collectible treasures (gems, fruit, keys) and stage exit doors.
- **Engine Subsystems to Validate:**
  - `Bidirectional Follow Camera`: Horizontal and vertical camera tracking with configurable deadzone box and lookahead.
  - `Tilemap Chunk Engine`: Static and animated tilemaps loaded from matrix definitions with tile collision flags (Solid, OneWay, Hazard, Ladder).
  - `Platformer Physics Controller`: Jump buffer, coyote time, variable jump cut, and slope handling.
  - `Character Animation State Machine`: Frame-based sprite strip sequencer (Idle, Run, Jump, Fall, Attack, Hurt, Climb).

---

## 📊 Engine Capabilities Cross-Matrix

| Subsystem / Feature | Dinorun (Closed) | Raptor (Closed) | Arkanoid (Closed) | Chiki (Planned) |
| :--- | :---: | :---: | :---: | :---: |
| **Parallax Scrolling** | Horizontal (5 layers) | Vertical (Multi-biome) | Static Arena | Bi-directional |
| **Camera ViewMode** | `HorizontalScroller` | `VerticalScroller` | `FixedScreen` | `PlatformerFollow` |
| **Collision Query** | AABB (Obstacle) | High-Freq Pools | Sub-step CCD | Tilemap + Ledges |
| **Entity Volume** | 5 - 15 entities | 50 - 150 entities | 30 - 80 bricks | 20 - 40 entities |
| **Input Scheme** | Pad / Gestures | Direct Touch / Virtual Stick | Touch Drag / Buttons | D-Pad + Jump + Action |
| **Color Recovery** | Global ColorMatrix | Shields / Plasma Bloom | Per-Layer Palette | Environmental Relics |
| **Object Pooling** | Chunk recycling | Bullets & Particles | Particle & Ball pool | Tile & Entity pools |

---

## 📚 Documentation & Developer Guides

Complete documentation for developing games on Pixie Engine is available in the [`DOCS/`](index.md) directory:
- [Overview & Table of Contents](index.md)
- [Getting Started Guide](getting-started.md)
- [Architecture & Design Principles](architecture.md)
- [Game Loop & Camera System](game-loop-and-camera.md)
- [Layers & Rendering System](layers-and-rendering.md)
- [Entities & Physics](entities-and-physics.md)
- [Weapons & Projectiles](weapons-and-projectiles.md)
- [Particles & Visual Effects](particles-and-effects.md)
- [Game Context & Continuous Difficulty](context-and-difficulty.md)
- [Audio & Input Adapters](audio-and-input.md)
- [Cookbook: Build a 2D Game in 15 Minutes](cookbook.md)
