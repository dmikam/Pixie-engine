# 🦖 Dinorun Engine-Core Migration Plan

> **Document Version:** 1.0.0  
> **Target Release:** v0.6.1  
> **Status:** PROPOSED / READY FOR EXECUTION  
> **Module Scope:** `:app` (`com.example.dinorun.*`) ➔ `:pixie-engine` / `:pixie-android`  
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


## 📋 Executive Summary

**Dinorun** was the foundational proof-of-concept (PoC v0.3) created to establish 2D parallax rendering, fixed-timestep looping, and virtual resolution scaling. During the subsequent development of **Raptor (v0.5)** and **Arkanoid (v0.6)**, numerous systems that were originally implemented ad-hoc or from scratch inside Dinorun were generalized, optimized for zero-allocation, and promoted into the core modules:
- `:pixie-engine` (JVM Pure Kotlin): `Trajectory`, `ProceduralExplosion`, `ParticleLayer`, `GameContext.difficulty`, `SkyLayer` celestial orbits.
- `:pixie-android` (Hardware Adapters): `ArcadeControlDeck`, `TactileArcadeButton`, `FlexArcadeButtonRow`, `FloatingAdjustableDeck`, `RetroAudioPresets`.

### Primary Objective
Refactor Dinorun using **localized, surgical edits** to consume engine-core subsystems instead of custom from-scratch implementations.

### Strict Non-Negotiable Constraints
1. **Preserve All Existing Features:** Gameplay feel, physics (jump arc, gravity, duck hitbox shrinking), 5-layer parallax, procedural chunk generation, 140px/150px safe clearance rules, color recovery pipeline, and world switching must remain 100% functionally identical.
2. **No Rewrites From Scratch:** Do not discard or rewrite working classes. Replace only internal mechanics with engine port/core equivalents.
3. **Zero Test Regressions:** All 11 tests in `DinoGameCoordinatorTest.kt` must remain fully passing.
4. **AGENTS.md Compliance:**
   - Keep all source files strictly under 300–500 lines.
   - Functions under 50 lines (max 100).
   - Maintain fluent builder patterns (`PixieGame`).
   - Extract control definitions into dedicated `DinoControls.kt` (reducing `DinoHud.kt` from 403 lines down to ~270 lines).

---

## 🗺️ Migration Roadmap & Checklist

```
┌────────────────────────────────────────────────────────────────────────┐
│                   DINORUN ENGINE-CORE MIGRATION                         │
├──────────────────────────┬─────────────────────────────────────────────┤
│ Phase 1: Controls        │ ArcadeButtonConfig, TactileArcadeButton     │
│ Phase 2: Trajectories    │ Trajectory.SineWave & Linear/Accelerating   │
│ Phase 3: Sky & Orbits    │ CelestialTransitMode, Orbit Centers         │
│ Phase 4: Particles & FX  │ ProceduralExplosion (SPARK_HIT, BLAST)     │
│ Phase 5: Audio Events    │ DUCK, EXPLOSION_LARGE, BUTTON_CLICK         │
│ Phase 6: Difficulty Sync │ Continuous context.difficulty mapping       │
│ Phase 7: Verification    │ Robolectric JVM tests & compilation         │
└──────────────────────────┴─────────────────────────────────────────────┘
```

---

### Phase 1: Arcade Controls & Input System Migration
*Current State:* `DinoHud.kt` manually implements `DinoVirtualControlDeck`, `DinoVirtualDuckButton`, and `DinoVirtualJumpButton` using raw Compose `Box` with `detectTapGestures` and `tryAwaitRelease()`.  
*Target State:* Modular control configuration via `com.example.dinorun.ui.DinoControls` using `:pixie-android`'s `ArcadeButtonConfig`, `TactileArcadeButton`, and flex/floating decks (mirroring `RaptorControls` and `ArkanoidControls`).

- [x] **1.1. Create `DinoControls.kt`** (`com.example.dinorun.ui.DinoControls`)
  - [x] Define `createDuckButton(inputAdapter: DualModeInputAdapter, audioAdapter: AndroidSoundPoolAudioAdapter)` returning `ArcadeButtonConfig` with `ArcadeButtonShape.CIRCLE`, `actionType = ArcadeActionType.DUCK`, and pressed/released callbacks.
  - [x] Define `createJumpButton(inputAdapter: DualModeInputAdapter, audioAdapter: AndroidSoundPoolAudioAdapter, player: DinoPlayer)` returning `ArcadeButtonConfig` with `ArcadeButtonShape.CIRCLE`, `actionType = ArcadeActionType.JUMP`.
  - [x] Define `createFloatingButtons(...)` to allow players to freely drag and position Jump & Duck buttons when in floating layout mode.
- [x] **1.2. Refactor `DinoVirtualControlDeck` in `DinoHud.kt`**
  - [x] Replace custom raw `Box` pointer gestures with `TactileArcadeButton` or `DockedBottomConsoleDeck` / `FlexArcadeButtonRow`.
  - [x] Retain existing `testTag("jump_button")` and `testTag("duck_button")` on the buttons for automated testing compatibility.
  - [x] Shrink `DinoHud.kt` from 403 lines to under 300 lines by removing redundant button drawing logic.
- [x] **1.3. Wire UI Click Audio**
  - [x] Add `audioAdapter.playSound(AudioEvent.BUTTON_CLICK)` to `DinoPixelGapPill` and `DinoControlModePill` in `DinoHud.kt`.
- [x] **1.4. Preserve Touch Gestures**
  - [x] Confirm swipe/tap gestures in `PixieGame.onTap` and `DualModeInputAdapter` continue to function when `ControlMode.TOUCH_GESTURES` is active.

---

### Phase 2: Enemy & Hazard Trajectory System Migration
*Current State:* `PterodactylHazard.kt` calculates sinusoidal hovering via raw `kotlin.math.sin` dt integration in `update()`. `VolcanicMeteor.kt` manually steps velocity `Vec2(-40f, 130f)`.  
*Target State:* Adopt engine `com.pixie.engine.core.weapon.Trajectory` strategies for deterministic motion calculation.

- [ ] **2.1. Integrate `Trajectory.SineWave` into `PterodactylHazard.kt`**
  - [ ] Replace manual wave calculation with `Trajectory.SineWave(baseAngleRad = Math.PI.toFloat(), frequency = 6f, amplitude = 28f)`.
  - [ ] Delegate velocity update per frame via `trajectory.computeVelocity(position, velocity, flightTime, speed, dt)`.
  - [ ] Preserve flap animation sequence (`WING_UP_SPRITE`, `WING_DOWN_SPRITE`), bounding box (18x14), health (15f), and score (250 pts).
- [ ] **2.2. Integrate `Trajectory.Linear` into `VolcanicMeteor.kt`**
  - [ ] Replace hardcoded vector mutation with `Trajectory.Linear(angleRad = 1.87f)` (or `Trajectory.Accelerating`).
  - [ ] Keep proximity trigger distance (`targetGroundX - playerX <= triggerDistance`) and ground clamping surface logic (`groundY - size.y`).
  - [ ] Preserve meteor and scorch impact animation sequences.
- [ ] **2.3. Verify Hazard Separation & Playability**
  - [ ] Ensure Pterodactyls still spawn strictly after 500m and never overlap cacti (140px safe clearance).
  - [ ] Ensure Meteors still spawn strictly after 2000m with 150px clearance from ground and aerial hazards.

---

### Phase 3: Sky & Celestial Orbit Configuration Migration
*Current State:* `DinoGameCoordinator.kt` only wires `dayNightCycleSeconds` and `windVelocity`. `PrehistoricWorldDefinition` and `VolcanicCraterWorldDefinition` do not parameterize celestial transit or star dynamics.  
*Target State:* Expose and configure engine `SkyLayer` features in `WorldDefinition`.

- [ ] **3.1. Extend `WorldDefinition` / Dinorun Worlds with Sky Properties**
  - [ ] Add optional `transitMode: CelestialTransitMode = CelestialTransitMode.CIRCULAR` to `WorldDefinition`.
  - [ ] Add `celestialCenter: Vec2`, `celestialRadiusX: Float`, `celestialRadiusY: Float` to allow worlds to customize the sun/moon arc.
  - [ ] Configure `PrehistoricWorldDefinition` for majestic high-arch circular orbit with 60s day/night cycle and full rotating star field.
  - [ ] Configure `VolcanicCraterWorldDefinition` for aggressive, low-horizon orbit with 40s cycle and reddish twilight.
- [ ] **3.2. Apply Celestial Properties in `DinoGameCoordinator.kt`**
  - [ ] In `applyWorldConfiguration(world: WorldDefinition)`:
    ```kotlin
    engine.skyLayer.transitMode = world.transitMode
    engine.skyLayer.celestialCenter = world.celestialCenter
    engine.skyLayer.celestialRadiusX = world.celestialRadiusX
    engine.skyLayer.celestialRadiusY = world.celestialRadiusY
    ```
- [ ] **3.3. Celestial Shift Audio Cues**
  - [ ] Detect day-to-night and night-to-day transition thresholds in `DinoGameCoordinator` or `SkyLayer` and trigger `AudioEvent.DAY_NIGHT_SHIFT`.

---

### Phase 4: Procedural Particle & Visual FX Integration
*Current State:* Dinorun does not emit any particles. Picking up a spark only changes a numeric progress bar; meteor ground impacts only swap to a static sprite.  
*Target State:* Trigger engine `com.pixie.engine.core.particle.ProceduralExplosion` archetypes via `engine.particleLayer`.

- [ ] **4.1. Color Spark Collection Burst**
  - [ ] In `DinoGameCoordinator.checkCustomCollisions()`: when `obj is PixieCollectible`, trigger `ProceduralExplosion.trigger(ExplosionType.SPARK_HIT, obj.position, engine.particleLayer.pool, engine.particleLayer.activeParticles, scale = 1.2f)`.
  - [ ] Zero allocation: reuses pre-allocated `engine.particleLayer.pool`.
- [ ] **4.2. Volcanic Meteor Ground Impact Shockwave & Blast**
  - [ ] When `VolcanicMeteor` impacts the ground surface, trigger `ProceduralExplosion.trigger(ExplosionType.GROUND_BLAST, position, engine.particleLayer.pool, engine.particleLayer.activeParticles, scale = 1.0f)`.
  - [ ] Combines with the existing scorch animation for authentic arcade impact punch.
- [ ] **4.3. Player Damage & Hazard Impact Sparks**
  - [ ] When player strikes a cactus, pterodactyl, or meteor, trigger `ProceduralExplosion.trigger(ExplosionType.SPARK_HIT, player.position, engine.particleLayer.pool, engine.particleLayer.activeParticles, scale = 1.5f)`.

---

### Phase 5: Audio Synthesizer Integration
*Current State:* Dinorun only calls `JUMP`, `HIT`, `COLLECT`, `GAME_OVER`, and `SCORE_MILESTONE`. Sound presets already generated in `RetroAudioPresets` (`DUCK`, `EXPLOSION_LARGE`, `BUTTON_CLICK`, `DAY_NIGHT_SHIFT`) are unused.  
*Target State:* Complete audio mapping across all player actions and environmental events.

- [ ] **5.1. Duck Audio Feedback**
  - [ ] In `DinoGameCoordinator.updateInputs()` or `DinoControls.kt`: Play `AudioEvent.DUCK` on downward duck transition.
- [ ] **5.2. Meteor Impact Explosion Audio**
  - [ ] When `VolcanicMeteor` strikes ground or damages player, play `AudioEvent.EXPLOSION_LARGE` or `AudioEvent.EXPLOSION_SMALL`.
- [ ] **5.3. Milestone & Victory Fanfare**
  - [ ] Verify `AudioEvent.SCORE_MILESTONE` continues to trigger every 100m distance.

---

### Phase 6: Continuous Difficulty & GameContext Synchronization
*Current State:* `DinoPlayer.parameterCalculator` manually evaluates `calculateDifficultyMultiplier(1.0f, 0.08f)`. `engine.context.difficulty` (`0..100`) is not actively synchronized.  
*Target State:* Synchronize `engine.context.difficulty` dynamically from distance traveled.

- [ ] **6.1. Distance-to-Difficulty Mapping**
  - [ ] In `DinoGameCoordinator.checkCustomCollisions()`: update `engine.context.difficulty = (engine.context.distance / 25f).toInt().coerceIn(0, 100)`.
- [ ] **6.2. Harmonize Dynamic Scaling**
  - [ ] Keep `DinoPlayer` run speed scaling formula compatible with `engine.context.difficulty`.
  - [ ] Ensure difficulty progression remains smooth without sudden speed jumps.

---

### Phase 7: Validation, Unit Testing & Compliance
*Current State:* 11 passing tests in `DinoGameCoordinatorTest.kt`.  
*Target State:* 100% test pass rate with new tests verifying trajectory integration and particle emission.

- [ ] **7.1. Code Architecture & Limits Check**
  - [ ] Ensure `DinoHud.kt` is reduced below 300 lines.
  - [ ] Ensure `DinoControls.kt` is under 150 lines.
  - [ ] Ensure `DinoGameCoordinator.kt` remains under 200 lines.
  - [ ] Verify all Composable functions are under 50 lines.
- [ ] **7.2. Run JVM Unit & Robolectric Tests**
  - [ ] Execute `gradle :app:testDebugUnitTest`.
  - [ ] Verify all existing test cases pass:
    - `initialization sets up prehistoric world, grounded player and default metrics`
    - `jump input triggers vertical leap, airborne state and jump animation`
    - `airborne jump physics integrates gravity and lands smoothly back on ground`
    - `ducking input shrinks bounding box and suppresses jumping`
    - `collectible spark recovers color progression and awards score`
    - `obstacle collision inflicts lethal damage, deactivates obstacle and triggers game over`
    - `pterodactyl hazard collision inflicts damage and deactivates hazard`
    - `invulnerable player survives collision and decrements lives without ending game`
    - `distance progression updates score and tracks milestones`
    - `world switching and restart resets game state and updates world configuration`
    - `dynamic difficulty scales player run speed as difficulty rises`
- [ ] **7.3. Add Trajectory & Particle Verification Tests**
  - [ ] Add test verifying `PterodactylHazard` updates position along sine wave trajectory.
  - [ ] Add test verifying `VolcanicMeteor` ground impact spawns particles into `engine.particleLayer`.
- [ ] **7.4. Compile Applet Check**
  - [ ] Run `compile_applet` to ensure pristine Android build without warnings or missing symbols.
