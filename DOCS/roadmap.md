# Pixie Engine PoC Roadmap & Planification

> **Version:** 0.4.0  
> **Last Updated:** 2026-10-18  
> **Status:** Active Multi-Genre Engine Validation  

Pixie Engine is a standalone, lightweight 2D pixel-art game engine engineered in pure Kotlin (`:pixie-engine`) with Android hardware adapters (`:pixie-android`) and a showcase client (`:app`).

To validate the engine across diverse 2D game paradigms, we are developing 4 distinct Proof-of-Concept (PoC) games gradually:
1. **Dinorun** — Horizontal Endless Scroller *(Complete / Playable)*
2. **Raptor** — Vertical Arcade Space/Jet Scroller *(In Queue / Next Up)*
3. **Arkanoid** — Fixed-Screen Brick Breaker *(Planned)*
4. **Chiki** — Classic Side-Scrolling Platformer *(Planned)*

---

## 🗺️ Master Roadmap & Status Overview

| Game | Paradigm | Engine Focus Area | Status | Target Version |
| :--- | :--- | :--- | :--- | :--- |
| **🎮 Game Hub / Menu** | Multi-Game Launcher | Dynamic game lifecycle & navigation | **DONE** | v0.4 |
| **🦖 Dinorun** | Horizontal Scroller | 5-layer parallax, ColorMatrix recovery, chunk recycling | **DONE (Playable)** | v0.3 |
| **🚀 Raptor** | Vertical Scroller | High-speed vertical camera, bullet pool, wave spawner | **IN QUEUE** | v0.5 |
| **🧱 Arkanoid** | Fixed Screen | Static bounds, reflection angles, destructible grid matrix | **PLANNED** | v0.6 |
| **⚔️ Chiki** | Classic Platformer | Tilemap collisions, gravity arcs, climbing, one-way ledges | **PLANNED** | v0.7 |

---

## 1. 🦖 Dinorun (Horizontal Endless Scroller)
- **Genre:** Horizontal Endless Runner / Survival
- **Current Status:** `COMPLETED / PLAYABLE (v0.3)`
- **Key Mechanics:**
  - Running, variable-height jumping, ducking under airborne hazards.
  - Procedural terrain generation with variable obstacles (Cactus clusters, low/high Pterodactyls, Volcanic Meteors).
  - Primal Color Spark pickups that progressively restore world saturation.
  - Day/Night celestial cycle with dynamic sun/moon trajectory and ambient shifts.
- **Engine Subsystems Validated:**
  - `HorizontalScrollerViewMode`: X-axis tracking camera with dynamic horizon reference (`horizonY`).
  - `5-Layer Parallax Stack`: Sky, Cloud, Mountain Background, Ground collision plane, Entity layer.
  - `ChunkManager`: Dynamic spawning, culling, and pooling of terrain chunks.
  - `ColorMatrix Pipeline`: Real-time post-processing saturation recovery (0% grayscale to 100% vibrant color).
  - `DualModeInputAdapter`: Dynamic switching between on-screen virtual buttons and touch swipe/tap gestures.
  - `Layer Inspector`: In-game runtime toggling of individual layers and pixel grid/solid modes.

---

## 2. 🚀 Raptor (Vertical Arcade Scroller)
- **Genre:** Vertical Top-Down Arcade Shoot 'em Up (Shmup)
- **Current Status:** `IN QUEUE - NEXT UP`
- **Key Mechanics:**
  - Continuous vertical upward flight over changing planetary surfaces and cloud covers.
  - Smooth 2D touch drag / virtual analog stick ship steering with screen boundary clamping.
  - Fast-firing laser cannons and homing secondary missiles.
  - Coordinated enemy squadrons attacking in spline and sine wave flight patterns.
  - Destructible ground turrets, aerial bosses with phase transitions, and shield power-ups.
- **Engine Subsystems to Validate:**
  - `VerticalScrollerViewMode`: High-speed Y-axis camera movement with background layer velocity multipliers.
  - `High-Density Projectile Pool`: Zero-allocation reuse of 100+ simultaneous player and enemy bullets.
  - `Wave Spawner & Pathing Engine`: Timeline-based enemy wave triggers and parametric motion paths.
  - `Rotated Sprite & Directional Rendering`: Rendering directional crafts with 8-way or angular heading.
  - `Radial & Multi-AABB Collisions`: Fast broad-phase spatial partition for projectile-dense combat.

---

## 3. 🧱 Arkanoid (Fixed-Screen Brick Breaker)
- **Genre:** Fixed-Screen Precision Arcade Action
- **Current Status:** `PLANNED`
- **Key Mechanics:**
  - Horizontal paddle movement along the bottom screen boundary.
  - High-velocity energy ball bouncing off arena walls, paddle segments, and bricks.
  - Variable bounce mechanics based on paddle impact offset (angle modulation).
  - Multi-tier destructible brick grid (silver armored bricks, explosive bricks, item drop bricks).
  - Power-ups: Laser paddle, wide paddle, triple ball, slow speed field.
- **Engine Subsystems to Validate:**
  - `FixedScreenViewMode`: Static arena camera with strict coordinate letterboxing / aspect ratio locking.
  - `Continuous Collision Detection (CCD)`: Sub-stepping physics loop to prevent high-speed ball tunneling.
  - `Grid-Based Destruction Matrix`: 2D block tile state manager with instant dirty-rect updates.
  - `Per-Brick Color Recovery`: Clearing bricks selectively restores saturation to corresponding palette bands.

---

## 4. ⚔️ Chiki (Classic 2D Platformer)
- **Genre:** Classic Side-Scrolling Platformer / Action Adventure
- **Current Status:** `PLANNED`
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

| Subsystem / Feature | Dinorun | Raptor | Arkanoid | Chiki |
| :--- | :---: | :---: | :---: | :---: |
| **Parallax Scrolling** | Horizontal | Vertical | Static | Bi-directional |
| **Camera ViewMode** | `HorizontalScroller` | `VerticalScroller` | `FixedScreen` | `PlatformerFollow` |
| **Collision Query** | AABB (Obstacle) | High-Freq Pools | Sub-step CCD | Tilemap + Ledges |
| **Entity Volume** | 5 - 15 entities | 50 - 150 entities | 30 - 80 bricks | 20 - 40 entities |
| **Input Scheme** | Pad / Gestures | Direct Touch / Virtual Stick | Touch Drag / Buttons | D-Pad + Jump + Action |
| **Color Recovery** | Global ColorMatrix | Shields / Plasma Bloom | Per-Layer Palette | Environmental Relics |

---

## 📝 Update & Change Log

- **v0.4.0 (Current):**
  - Created central **PoC Starting Menu** allowing players to pick games from the retro arcade hub.
  - Added game preview cards, engine capability breakdown, and specs preview modal for upcoming games.
  - Implemented in-app **Roadmap Viewer** directly in the launcher.
  - Added clean exit/back navigation from gameplay back to the game selection hub.
  - Created `DOCS/roadmap.md` master specification.

- **v0.3.0:**
  - Released Dinorun endless runner with 5-layer parallax stack.
  - Implemented dynamic ColorMatrix grayscale-to-color recovery pipeline.
  - Added dual-mode controls (Virtual buttons + Swipe gestures) and pause layer inspector.
