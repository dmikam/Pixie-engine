# 05 — Chip 'n Dale 2 Crate Pick-and-Throw Subsystem

> **Module Scope:** `:pixie-engine` / `:app` (`com.example.platformer.entities.crate.*`)  
> **Target Release:** v0.7.0 (Step 5)  
> **Status:** PENDING  
> **Compliance:** Pure Kotlin models, AGENTS.md compliance, files < 500 lines, functions < 50 lines.

---

## 📋 Objectives

Implement the iconic signature mechanic from **Chip 'n Dale Rescue Rangers 2**: discovering wooden crates, lifting them above the hero's head, walking/jumping while carrying, aiming and hurling crates as high-impact projectiles to smash enemies and break bricks, and ducking inside crates for defensive cover.

---

## 🗺️ Tasks & Checklist

- [ ] **Task 5.1: Implement `ThrowableCrate` Entity**
  - **Target File:** `app/src/main/java/com/example/platformer/entities/crate/ThrowableCrate.kt`
  - **Action:**
    - Create `ThrowableCrate(pos: Vec2)` extending `PixieObject`:
      - 16x16 pixel wooden crate sprite with cross-beam bracing.
      - State machine: `PLACED_ON_GROUND`, `CARRIED`, `THROWN_AIRBORNE`, `SHATTERED`.
      - While placed: acts as solid obstacle that can be stood upon as a platform.
      - Can also be spawned from hit blocks or scattered across stages.

- [ ] **Task 5.2: Implement Overhead Carry State in Player Controller**
  - **Target File:** `app/src/main/java/com/example/platformer/entities/PlatformerPlayer.kt`
  - **Action:**
    - When standing adjacent to a crate and pressing Action / Throw (<kbd>B</kbd>):
      - Attach crate to player: `carriedObject = crate`.
      - Crate state becomes `CARRIED`, updating its position to `(player.x, player.y - 14f)` (held above head).
      - Player enters `CARRY_IDLE`, `CARRY_WALK`, `CARRY_JUMP` animation states.
      - Jumping physics remain intact while carrying.

- [ ] **Task 5.3: Implement Aimed Projectile Throw Physics**
  - **Target File:** `app/src/main/java/com/example/platformer/entities/crate/CrateThrowController.kt`
  - **Action:**
    - When carrying and pressing Action / Throw (<kbd>B</kbd>):
      - Detach crate and transition to `THROWN_AIRBORNE`.
      - If pressing <kbd>UP</kbd>: launch upward arc (`velocity = Vec2(facingDirection * 40f, -280f)`).
      - If neutral or horizontal: throw straight forward with gentle parabolic arc (`velocity = Vec2(facingDirection * 210f, -70f)`).
      - Integrate gravity (`gravity = 650f`) while airborne.
      - Play `AudioEvent.CRATE_THROW` sound.

- [ ] **Task 5.4: Implement Crate Impact Smash & Crate-Ducking Defense**
  - **Target File:** `app/src/main/java/com/example/platformer/entities/crate/CrateImpactResolver.kt`
  - **Action:**
    - **Impact Collision:**
      - If thrown crate strikes an enemy: enemy is defeated, crate shatters into 4 spinning wooden splinters (`ProceduralExplosion.trigger(ExplosionType.SPARK_HIT)`), award 200 pts.
      - If thrown crate strikes a `BRICK` block: destroys the brick, awards points, and shatters.
      - If thrown crate strikes a solid wall: shatters with impact sound.
    - **Crate Ducking (Chip 'n Dale Homage):**
      - If player ducks (<kbd>DOWN</kbd>) while carrying a crate: player pulls crate down over themselves, becoming immune to lateral enemy contact!

---

## 🔍 Verification

1. Run unit tests in `app/src/test/java/com/example/platformer/ThrowableCrateTest.kt` verifying:
   - Pressing action next to crate picks it up and anchors position above player head.
   - Throwing crate gives forward/upward velocity and gravity.
   - Thrown crate colliding with enemy damages/defeats enemy and shatters crate.
   - Ducking while carrying provides defensive cover.
