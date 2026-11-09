# 🍄 Super Pixie (NES Platformer) — Current State Tracker

> **Last Updated:** 2026-11-09  
> **Target Release:** v0.7.0  
> **Status:** PLANNING COMPLETE / READY FOR IMPLEMENTATION  
> **Reference Guidelines:** [`AGENTS.md`](/AGENTS.md), [`DOCS/roadmap.md`](/DOCS/roadmap.md)

---

## 📌 Snapshot Overview

Super Pixie is the 4th reference proof-of-concept for the Pixie Engine, validating the **Classic NES 2D Platformer** paradigm (combining **Super Mario Bros** running/stomping/block-bonking with **Chip 'n Dale 2** crate lifting and throwing).

- **Current Active File:** [`04-enemies-stomp-physics.md`](04-enemies-stomp-physics.md)
- **Last Completed Task:** **Task 4.1** — Implement `PlatformerEnemy` Base Entity (`PlatformerEnemy.kt`) in `:pixie-engine`
- **Next Pending Task:** **Task 4.2** — Implement Goomba Mushroom Walker (`GoombaEnemy.kt`) in `:app`
- **Engine Compliance:** Pure JVM in `:pixie-engine`, Android Compose adapters in `:pixie-android`, UI & game logic in `:app`.
- **Target Metrics:** All files < 500 lines, functions < 50 lines, fluent `PixieGame` builder DSL, 100% automated test coverage.

---

## 🗺️ Master Plan Index & Progress Tracker

| File | Module / Focus | Description | Status | Progress |
| :--- | :--- | :--- | :--- | :---: |
| **[`01-engine-tilemap-camera.md`](01-engine-tilemap-camera.md)** | `:pixie-engine` | Tile definitions, tilemap layer, swept collision query, follow camera | ✅ **COMPLETE** | 4/4 |
| **[`02-player-physics-movement.md`](02-player-physics-movement.md)** | `:pixie-engine` | NES momentum, run acceleration, skid, variable jump cut, coyote time | ✅ **COMPLETE** | 4/4 |
| **[`03-block-interactions-items.md`](03-block-interactions-items.md)** | `:pixie-engine` | Head-bonk recoil, '?' blocks, coins, mushrooms, breakable brick shards | ✅ **COMPLETE** | 4/4 |
| **[`04-enemies-stomp-physics.md`](04-enemies-stomp-physics.md)** | `:app` / `:pixie-engine` | Goombas, Koopas, patrol AI, enemy stomp bounce, shell kick | 🔄 **IN PROGRESS** | 1/4 |
| **[`05-chip-and-dale-crate-system.md`](05-chip-and-dale-crate-system.md)** | `:app` / `:pixie-engine` | Wooden crate pickup, overhead carry, projectile toss & smash physics | ⏳ PENDING | 0/4 |
| **[`06-audio-presets-haptics.md`](06-audio-presets-haptics.md)** | `:pixie-android` | 8-bit NES sound synthesis presets & tactile micro-haptics | ⏳ PENDING | 0/4 |
| **[`07-stage-catalog-level-design.md`](07-stage-catalog-level-design.md)** | `:app` | World 1-1 level design, pipes, coin caches, flagpole goal sequence | ⏳ PENDING | 0/4 |
| **[`08-ui-controls-game-screen.md`](08-ui-controls-game-screen.md)** | `:app` | `PixieGame` DSL screen, NES D-Pad + A/B buttons, retro HUD & modals | ⏳ PENDING | 0/4 |
| **[`09-launcher-integration-testing.md`](09-launcher-integration-testing.md)** | `:app` | Game selector wiring, navigation routing, automated Robolectric tests | ⏳ PENDING | 0/4 |

---

## 🔄 How to Advance

To execute the next pending step in an automated chat session, use the runner prompt in [`DOCS/platformer/prompt.md`](prompt.md). Each session automatically picks up the first unchecked checkbox (`- [ ]`), implements the code adhering to `AGENTS.md`, runs compilation and tests, marks the item complete (`- [x]`), and updates this tracker.
