# 04 — Enemies, Patrol AI & Stomp Physics

> **Module Scope:** `:pixie-engine` / `:app` (`com.example.platformer.entities.*`)  
> **Target Release:** v0.7.0 (Step 4)  
> **Status:** PENDING  
> **Compliance:** Pure Kotlin models, AGENTS.md compliance, files < 500 lines, functions < 50 lines.

---

## 📋 Objectives

Implement classic NES platformer enemies: Goomba-style mushroom walkers and Koopa-style turtles with autonomous patrol AI, lateral player damage, and authentic stomp physics that launch the hero into the air upon landing on their heads.

---

## 🗺️ Tasks & Checklist

- [x] **Task 4.1: Implement `PlatformerEnemy` Base Entity**
  - **Target File:** `pixie-engine/src/main/kotlin/com/pixie/engine/core/platformer/PlatformerEnemy.kt`
  - **Action:**
    - Extend `PixieEnemy`:
      - Autonomous horizontal patrol velocity (`direction = -1f or 1f`).
      - Gravity and floor snapping via `TilemapChunkLayer`.
      - Reverse direction upon colliding with solid walls or reaching stage borders.
      - State machine: `PATROL`, `STOMPED`, `SHELL_STATIONARY`, `SHELL_SLIDING`, `DEFEATED_FALL`.

- [ ] **Task 4.2: Implement Goomba Mushroom Walker**
  - **Target File:** `app/src/main/java/com/example/platformer/entities/GoombaEnemy.kt`
  - **Action:**
    - 2-frame walking wobble animation (`16x16` pixel sprite).
    - Patrol speed `35f` px/sec.
    - When stomped from above:
      - Switch to squashed flat sprite (`16x8`).
      - Halt movement and remain squashed for `0.4f` seconds before despawning.
      - Award 100 points.

- [ ] **Task 4.3: Implement Koopa Shell Turtle & Kick Physics**
  - **Target File:** `app/src/main/java/com/example/platformer/entities/KoopaEnemy.kt`
  - **Action:**
    - 2-frame walking turtle animation with shell.
    - First stomp: retracts into stationary shell (`SHELL_STATIONARY`).
    - When touched by player while stationary: kicked into high-speed sliding shell (`SHELL_SLIDING`, speed = `180f`).
    - Sliding shell rebounds off solid walls, breaks `BRICK` blocks, and defeats all enemies in its path.
    - Second stomp stops sliding shell.

- [ ] **Task 4.4: Stomp vs Lateral Hit Collision Resolver**
  - **Target File:** `app/src/main/java/com/example/platformer/world/PlatformerCombatSystem.kt`
  - **Action:**
    - Detect relative collision geometry:
      - **Stomp (Top Hit):** If `player.velocityY > 0` and `player.bounds.bottom <= enemy.bounds.top + 8f`:
        - Enemy receives `onStomped()` callback.
        - Player receives upward rebound impulse (`velocityY = -210f` or `-280f` if jump button is held down!).
        - Trigger `AudioEvent.STOMP` and spawn hit spark particles.
      - **Lateral Hit (Damage):** If player touches sides or bottom of enemy:
        - If player has active invulnerability frames, ignore.
        - If `SUPER`, revert player to `SMALL` and trigger 1.5s invulnerability flash.
        - If `SMALL`, trigger player defeat and death hop.

---

## 🔍 Verification

1. Run unit tests in `app/src/test/java/com/example/platformer/PlatformerCombatTest.kt` verifying:
   - Falling onto enemy top triggers stomp, squashes enemy, and sets player upward velocity.
   - Walking into enemy side damages player and triggers invulnerability.
   - Kicking stationary shell sets sliding velocity and wall rebounds.
