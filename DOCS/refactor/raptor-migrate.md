# 🚀 Raptor Engine-Core Migration Plan

> **Document Version:** 1.0.0  
> **Target Release:** v0.6.2  
> **Status:** COMPLETED  
> **Module Scope:** `:app` (`com.example.raptor.*`) ➔ `:pixie-engine` / `:pixie-android`  
> **Reference Guidelines:** `AGENTS.md` (Fluent builders, file limits < 500 lines, function limits < 50 lines)

---

Follow these execution steps:
Read AGENTS.md.
Identify the FIRST unchecked task (- [ ]) in this file.md.
Implement the required refactoring, fix, or improvement for that single task, adhering strictly to the guidelines in AGENTS.md:
Fluent / chainable builders and low-level boilerplate encapsulation.
File size limits (keep files modular, targeted at 300–500 lines maximum).
Function size limits (target < 50 lines, maximum 100–150 lines, extract sub-composables).
Compile and verify the project to confirm that no compilation or runtime errors were introduced.
Update this file by checking off that completed task (- [x]).
Provide a concise summary of:
The task that was executed and the problem it addressed.
The files modified or created.
The next pending task remaining in this file.

---

## 📋 Executive Summary

**Raptor** was developed in v0.5 as the vertical arcade shmup reference game to stress-test high-density projectile pools, procedural particle explosions, and continuous difficulty scaling. Following the subsequent additions in **Arkanoid (v0.6)** and the **Dinorun (v0.6.1)** migration, several reusable systems are now standardized in `:pixie-engine` and `:pixie-android`:
- `:pixie-android`: `ArcadeButtonConfig`, `TactileArcadeButton`, `FlexArcadeButtonRow`, `DockedBottomConsoleDeck`, `SplitWingsArcadeDeck` (specifically designed for landscape shmups), `FloatingAdjustableDeck`, `DualModeInputAdapter`.
- `:pixie-engine`: Trajectory strategies (`Trajectory.SineWave`, `Trajectory.Linear`, `Trajectory.Accelerating`), `StandardWeapons`, `ProceduralExplosion` archetypes, and modular screen management.

### Primary Objective
Refactor Raptor using **localized, surgical edits** to consume engine-core subsystems instead of custom from-scratch implementations, bringing Raptor to parity with the engine's latest architecture.

### Strict Non-Negotiable Constraints
1. **Preserve All Existing Gameplay Mechanics:** Flight controls (smooth 2D touch drag with screen bounding box clamping), auto-fire pacing, homing missile deployment, mega-bomb screen clearance, multi-stage boss encounters, terrain destruction, terminal shop weapon purchases, and briefing desk difficulty selection must remain 100% functionally identical.
2. **No Rewrites From Scratch:** Do not rebuild working entity, collision, or spawner logic. Only integrate engine-core components into the existing structure.
3. **Zero Test Regressions:** All tests in `RaptorGameCoordinatorTest.kt` must remain fully passing.
4. **AGENTS.md Compliance:**
   - Keep all source files strictly under 300–500 lines (modularize `RaptorGameCoordinator.kt` by extracting `RaptorDropCatalog.kt`).
   - Keep functions under 50 lines (max 100).
   - Use fluent builder pattern (`PixieGame`).
   - Use `Modifier.testTag` on all primary interactive elements (`auto_fire_toggle`, `missile_fire_button`, `mega_bomb_button`).

---

## 🗺️ Migration Roadmap & Checklist

```
┌────────────────────────────────────────────────────────────────────────┐
│                    RAPTOR ENGINE-CORE MIGRATION                         │
├──────────────────────────┬─────────────────────────────────────────────┤
│ Phase 1: Arcade Controls │ ArcadeButtonConfig, SplitWings, DockedDeck  │
│ Phase 2: Trajectories    │ Trajectory.SineWave & Linear Flight Curves  │
│ Phase 3: Input Adapter   │ DualModeInputAdapter & Virtual Axis Steer   │
│ Phase 4: Drop Catalog    │ Modularize Drop Table to RaptorDropCatalog  │
│ Phase 5: Parallax/View   │ LayerStack & Velocity Scaling               │
│ Phase 6: FX Harmonize    │ ProceduralExplosion & Audio presets         │
│ Phase 7: Verification    │ Robolectric JVM tests & compilation         │
└──────────────────────────┴─────────────────────────────────────────────┘
```

---

### Phase 1: Arcade Controls & Dual Layout Migration
*Current State:* `RaptorControls.kt` manually implements `RaptorBottomControls` using standard Material 3 `Button` components. In landscape mode, `RaptorGameScreen.kt` stretches bottom buttons across the screen instead of using side wings.  
*Target State:* Modular control configuration via `com.example.raptor.ui.RaptorControls` using `:pixie-android`'s `ArcadeButtonConfig`, `TactileArcadeButton`, `FlexArcadeButtonRow`, `DockedBottomConsoleDeck`, and `SplitWingsArcadeDeck`.

- [x] **1.1. Refactor `RaptorControls.kt` to use `ArcadeButtonConfig`**
  - [x] Define `createAutoFireButton(isAutoFire: Boolean, weaponTier: Int, onToggle: () -> Unit)` returning `ArcadeButtonConfig` with `ArcadeButtonShape.PILL`, `ArcadeActionType.ACTION_PRIMARY`, and active glow styling.
  - [x] Define `createMissileButton(missilesLeft: Int, onFire: () -> Unit)` returning `ArcadeButtonConfig` with `ArcadeButtonShape.PILL`, `ArcadeActionType.ACTION_SECONDARY`, and missile count badge.
  - [x] Define `createMegaBombButton(bombsLeft: Int, onTrigger: () -> Unit)` returning `ArcadeButtonConfig` with `ArcadeButtonShape.OCTAGON`, `ArcadeActionType.ACTION_SPECIAL`, and warning border styling.
  - [x] Preserve all existing `Modifier.testTag` identifiers (`"auto_fire_toggle"`, `"missile_fire_button"`, `"mega_bomb_button"`).
- [x] **1.2. Implement `DockedBottomConsoleDeck` for Portrait Mode**
  - [x] In `RaptorGameScreen.kt` portrait layout: Replace manual `Row` button layout with `FlexArcadeButtonRow` inside `DockedBottomConsoleDeck`, distributing widths proportionally via button weights so buttons never squish or overflow on narrow screens.
  - [x] Retain active perks row (`RaptorActivePerksBadges`) docked neatly above the console deck.
- [x] **1.3. Implement `SplitWingsArcadeDeck` for Landscape Mode**
  - [x] In `RaptorGameScreen.kt` landscape layout: Deploy `SplitWingsArcadeDeck`:
    - **Left Wing Pillar:** Houses HUD integrity metrics (Shield & Hull gauges) and Auto-Fire toggle button.
    - **Center Area:** Clean letterboxed 240x320 combat arena with unobstructed view.
    - **Right Wing Pillar:** Houses tactical action triggers (Secondary Missiles, Mega-Bomb Detonator, Pause button).
- [x] **1.4. Support Floating Adjustable Controls**
  - [x] Add `createFloatingButtons(...)` in `RaptorControls.kt` and integrate `FloatingAdjustableDeck` in `RaptorGameScreen.kt` for customizable on-screen control placement.

---

### Phase 2: Enemy Flight Trajectory System Migration
*Current State:* `ScoutEnemyShip.kt`, `InterceptorEnemyShip.kt`, and `GunshipEnemyShip.kt` calculate sinusoidal flight curves via manual `kotlin.math.sin` calls in `calculateFlightVelocity()`. `StealthBomberEnemyShip.kt` hardcodes `Vec2(0f, speed)`.  
*Target State:* Adopt engine `com.pixie.engine.core.weapon.Trajectory` strategies for deterministic motion calculation.

- [x] **2.1. Integrate `Trajectory.SineWave` into `ScoutEnemyShip.kt`**
  - [x] Encapsulate flight motion with `Trajectory.SineWave(baseAngleRad = 1.5707963f, frequency = 1.8f, amplitude = 18f)`.
  - [x] Calculate velocity via `trajectory.computeVelocity(...)` preserving gentle reconnaissance drift.
  - [x] Preserve health (18f), speed (52f), sprite, single red laser salvo, and score award (150 pts).
- [x] **2.2. Integrate `Trajectory.SineWave` into `InterceptorEnemyShip.kt`**
  - [x] Encapsulate flight motion with `Trajectory.SineWave(baseAngleRad = 1.5707963f, frequency = 2.2f, amplitude = 42f)`.
  - [x] Preserve aggressive sweeping intercept maneuvers, health (38f), speed (44f), sprite, and score award (350 pts).
- [x] **2.3. Integrate `Trajectory.SineWave` into `GunshipEnemyShip.kt`**
  - [x] Encapsulate flight motion with `Trajectory.SineWave(baseAngleRad = 1.5707963f, frequency = 1.2f, amplitude = 14f)` with vertical hover deceleration past Y = 85f.
  - [x] Preserve 3-way plasma fan barrages, heavy armor (80f), and score award (800 pts).
- [x] **2.4. Integrate `Trajectory.Linear` into `StealthBomberEnemyShip.kt`**
  - [x] Encapsulate downward flight using `Trajectory.Linear(angleRad = 1.5707963f)`.
  - [x] Preserve massive hull (120f), slow advance (22f), plasma fan, and score award (1500 pts).
- [x] **2.5. Verify Wave Pacing and Formations**
  - [x] Confirm wave spawner delays (`waveDelay = 1.2f`, `groundDelay = 0.4f`) and offscreen cleanup (Y > 350f) remain untouched.

---

### Phase 3: Input Scheme Unification (`DualModeInputAdapter`)
*Current State:* `RaptorGameCoordinator.kt` directly handles touch dragging via `movePlayer(dx, dy)` and `handleDragStart(x, y)` without accepting an `InputPort`.  
*Target State:* Integrate `DualModeInputAdapter` into `RaptorGameCoordinator` to unify touch gestures, virtual buttons, and gamepad inputs.

- [x] **3.1. Add `inputAdapter: DualModeInputAdapter` to `RaptorGameCoordinator`**
  - [x] Wire `input = inputAdapter` into `PixieEngine(...)`.
  - [x] Support both touch-drag steering (via `onVirtualDragDelta` / `dragDelta`) and virtual D-pad / joystick steering via `inputAdapter.moveX` and `inputAdapter.moveY`.
- [x] **3.2. Unify Input Chaining in `RaptorGameScreen.kt`**
  - [x] Chain `.input(inputAdapter)` in `PixieGame(coordinator.engine)` DSL.
  - [x] Retain high-precision virtual touch drag while enabling arcade button input compatibility.

---

### Phase 4: Modularize Drop & Pickup Catalog
*Current State:* `RaptorGameCoordinator.kt` lines 340–357 implement randomized roll tables inside `spawnDrop()` with inline percentages, inflating coordinator size to 422 lines.  
*Target State:* Extract drop generation logic into `com.example.raptor.world.RaptorDropCatalog`.

- [x] **4.1. Create `RaptorDropCatalog.kt`** (`com.example.raptor.world.RaptorDropCatalog`)
  - [x] Define `rollDropType(rng: Random = Random): PickupType` encapsulating weapon upgrade (20%), homing missiles (16%), shield cell (18%), repair kit (16%), mega bomb (10%), score gem (12%), and perk chip (8%) probabilities.
  - [x] Define `spawnPickup(pos: Vec2, rng: Random = Random): RaptorPickup`.
- [x] **4.2. Delegate in `RaptorGameCoordinator.kt`**
  - [x] Update `spawnDrop(pos: Vec2)` to call `RaptorDropCatalog.spawnPickup(pos)`.
  - [x] Reduce `RaptorGameCoordinator.kt` file length comfortably below 350 lines.

---

### Phase 5: Background Parallax & ViewMode Alignment
*Current State:* `RaptorGameCoordinator` uses `GameViewMode.FixedScreen(Vec2.ZERO)` while manually managing terrain scroll offsets in `RaptorTerrainLayer` and `RaptorCloudLayer`.  
*Target State:* Ensure vertical scroller camera velocity multipliers harmonize with `engine.context.distance` and dynamic difficulty.

- [x] **5.1. Align Vertical Scroller Multipliers**
  - [x] Verify `RaptorTerrainLayer` and `RaptorCloudLayer` speed factors scale with forward velocity (`engine.context.currentMovingSpeed`).
  - [x] Ensure terrain biomes (Archipelago, Deep Canyon, Industrial Complex) switch cleanly on level transitions.
- [x] **5.2. Maintain Layer Stack Order**
  - [x] Ensure clean z-order: `terrainLayer` (150) -> `itemsLayer` (500) -> `projectilesLayer` (520) -> `particleLayer` (550) -> `cloudLayer` (600).

---

### Phase 6: Audio & Combat FX Harmonization
*Current State:* Some combat effects use raw particle manipulation alongside `ProceduralExplosion`.  
*Target State:* Route all explosions and hit sparks consistently through `ProceduralExplosion.trigger`.

- [x] **6.1. Route Combat Explosions Through `ProceduralExplosion`**
  - [x] Verify boss destruction sequence uses `ExplosionType.TITAN_CHAIN`.
  - [x] Verify mega-bomb detonation triggers `ExplosionType.SHOCKWAVE` and `ExplosionType.AERIAL_EXPLOSION`.
  - [x] Verify ground building destruction uses `ExplosionType.GROUND_BLAST`.
- [x] **6.2. Consistent Audio Presets**
  - [x] Ensure all audio events (`AudioEvent.FIRE_PRIMARY`, `FIRE_MISSILE`, `EXPLOSION_SMALL`, `EXPLOSION_LARGE`, `SHIELD_HIT`, `HIT`, `POWERUP_PICKUP`, `BUTTON_CLICK`, `SCORE_MILESTONE`) use hardware-synthesized presets in `RetroAudioPresets`.

---

### Phase 7: Validation, Unit Testing & Compliance
*Current State:* Comprehensive test suite passing in `RaptorGameCoordinatorTest.kt`.  
*Target State:* 100% test pass rate with new tests verifying arcade button configurations, trajectory math, and catalog decoupling.

- [x] **7.1. Code Architecture & Limits Check**
  - [x] Ensure `RaptorGameCoordinator.kt` is under 350 lines.
  - [x] Ensure `RaptorControls.kt` is under 200 lines.
  - [x] Ensure `RaptorDropCatalog.kt` is under 100 lines.
  - [x] Ensure all Composable functions are under 50 lines.
- [x] **7.2. Run JVM Unit & Robolectric Tests**
  - [x] Execute `gradle :app:testDebugUnitTest`.
  - [x] Verify existing test suite passes:
    - `initial setup configures level 1 full health and clean object lists`
    - `level progression updates catalog settings and resets flight distance`
    - `flight progress advances distance traveled and synchronizes engine context`
    - `difficulty presets configure engine difficulty context correctly`
    - `boss spawns automatically when flight distance reaches target distance`
    - `boss attack cycle spawns enemy projectiles and respects cooldown`
    - `combat collisions damage enemy and award score and credits`
    - `player weapon upgrade increases primary weapon tier`
    - `secondary missiles track and seek nearest active target`
    - `mega-bomb clears screen of enemy projectiles and damages visible craft`
    - `player craft destruction triggers game over callback`
- [x] **7.3. Add Trajectory & Drop Catalog Verification Tests**
  - [x] Add test verifying `RaptorDropCatalog` yields valid pickups within expected probability distributions.
  - [x] Add test verifying enemy ships calculate velocities along expected trajectory paths.
- [x] **7.4. Compile Applet Check**
  - [x] Run `compile_applet` to ensure pristine Android build without warnings or missing symbols.
