# Project Audit Roadmap: 2026-10-26

Generated on: **October 26, 2026**  
Ruleset Reference: [`DOCS/audit/rules.md`](../rules.md)  
Audit Results Reference: [`DOCS/audit/2026-10-26/results.md`](results.md)  
Compliance Baseline: [`AGENTS.md`](/AGENTS.md)

---

## How to Advance This Roadmap

To advance the roadmap step-by-step in subsequent chat sessions, use the standardized prompt defined in [`DOCS/audit/prompt.md`](../prompt.md). Each session will automatically select the first pending task (`- [ ]`), implement the fix, run verification, and mark the item as completed (`- [x]`).

---

## Priority 1: Critical Fixes & AGENTS.md Violations

- [x] **Task 1.1: Modularize `RaptorModals.kt` (679 lines) into Dedicated Dialog Files**
  - **Target File(s)**: `app/src/main/java/com/example/raptor/ui/RaptorModals.kt`, `app/src/main/java/com/example/raptor/ui/RaptorPauseDialog.kt`, `app/src/main/java/com/example/raptor/ui/RaptorGameOverDialog.kt`, `app/src/main/java/com/example/raptor/ui/RaptorVictoryDialog.kt`
  - **Action**: Extract `RaptorPauseDialog` (~340 lines), `RaptorGameOverDialog` (~130 lines), and `RaptorVictoryDialog` (~140 lines) into individual, dedicated files, mirroring the clean pattern of `dinorun` (`DinoPauseDialog.kt`, `DinoGameOverDialog.kt`, `DinoModals.kt`). Retain `RaptorModals.kt` as the lightweight overlay coordinator (< 100 lines).
  - **Rationale**: Resolves the Rule 2 violation in `RaptorModals.kt` (679 lines exceeds the 300–500 line soft limit).

- [x] **Task 1.2: Prune Orphaned / Redundant Hangar Screen Files**
  - **Target File(s)**: `app/src/main/java/com/example/raptor/ui/RaptorHangarBaseScreen.kt` (141 lines), `app/src/main/java/com/example/raptor/ui/RaptorHangarComponents.kt` (528 lines)
  - **Action**: Remove or retire the unreferenced legacy hangar files since active game navigation routes through `RaptorBriefingScreen.kt` and `RaptorTerminalShopScreen.kt`. Verify that no broken imports or unresolved references remain in test or production source sets.
  - **Rationale**: Eliminates the Rule 2 violation in `RaptorHangarComponents.kt` (528 lines) and removes dead code that creates developer cognitive overload.

---

## Priority 2: Architecture & Modularity Improvements

- [x] **Task 2.1: Modularize `RoadmapScreen.kt` (489 lines)**
  - **Target File(s)**: `app/src/main/java/com/example/menu/ui/RoadmapScreen.kt`, `app/src/main/java/com/example/menu/ui/RoadmapComponents.kt`
  - **Action**: Extract `GamesRoadmapTab`, `CapabilitiesMatrixTab`, and `LogAndDocsTab` into `RoadmapComponents.kt`, keeping `RoadmapScreen.kt` focused on scaffold orchestration (< 150 lines).
  - **Rationale**: Preemptively prevents `RoadmapScreen.kt` from breaching the 500-line Rule 2 ceiling as new capabilities and audit logs are added.

- [x] **Task 2.2: Extract Audio Preset Catalog from `RetroAudioSynthesizer.kt` (487 lines)**
  - **Target File(s)**: `pixie-android/src/main/kotlin/com/pixie/android/audio/RetroAudioSynthesizer.kt`, `pixie-android/src/main/kotlin/com/pixie/android/audio/RetroAudioPresets.kt`
  - **Action**: Separate audio frequency waveforms, envelope decay constants, and `AudioEvent` sound generation lookup tables from the low-level PCM audio synthesis engine.
  - **Rationale**: Brings `RetroAudioSynthesizer.kt` well under 350 lines, cleanly separating DSP audio synthesis from sound effect definitions.

- [x] **Task 2.3: Decompose `DinoPauseDialog.kt` Settings Blocks (477 lines)**
  - **Target File(s)**: `app/src/main/java/com/example/dinorun/ui/DinoPauseDialog.kt`, `app/src/main/java/com/example/dinorun/ui/DinoPauseComponents.kt`
  - **Action**: Extract the inlined pixel-gap selector, audio/haptic toggles, and world theme selector into focused sub-composables under 40 lines.
  - **Rationale**: Adheres to Rule 3 (sub-composable extraction, functions < 50 lines) and brings the file under 350 lines.

---

## Priority 3: API Ergonomics & Reusability Enhancements

- [x] **Task 3.1: Streamline `RaptorShip.kt` Physics & Firing Subsystems (439 lines)**
  - **Target File(s)**: `app/src/main/java/com/example/raptor/entities/RaptorShip.kt`, `app/src/main/java/com/example/raptor/entities/RaptorWeaponController.kt`, `app/src/main/java/com/example/raptor/entities/RaptorShipSprites.kt`
  - **Action**: Delegate projectile salvo math and missile angle calculations to `RaptorWeaponController` and sprite matrices to `RaptorShipSprites`, keeping `RaptorShip` focused on ship state and rendering (< 250 lines).
  - **Rationale**: Enforces separation of concerns and reduces cognitive load in entity models.

- [x] **Task 3.2: Extract Arkanoid Powerup Catalog from `ArkanoidGameCoordinator.kt` (470 lines)**
  - **Target File(s)**: `app/src/main/java/com/example/arkanoid/ArkanoidGameCoordinator.kt`, `app/src/main/java/com/example/arkanoid/world/ArkanoidPowerupCatalog.kt`
  - **Action**: Extract capsule drop probability tables, effect duration timers, and powerup assignment logic into `ArkanoidPowerupCatalog.kt`.
  - **Rationale**: Prevents `ArkanoidGameCoordinator.kt` from crossing the 500-line limit and enhances drop table configurability.

---

## Priority 4: Quality & Polish Items

- [ ] **Task 4.1: Add Automated Robolectric Tests for Arkanoid Gameplay**
  - **Target File(s)**: `app/src/test/java/com/example/arkanoid/ArkanoidGameCoordinatorTest.kt`
  - **Action**: Implement unit and Robolectric tests validating paddle collision angle deflection, powerup activation, laser projectile firing, and level progression.
  - **Rationale**: Establishes comprehensive regression test coverage matching `RaptorGameCoordinatorTest.kt`.

- [ ] **Task 4.2: Add Automated Robolectric Tests for DinoRun Gameplay**
  - **Target File(s)**: `app/src/test/java/com/example/dinorun/DinoGameCoordinatorTest.kt`
  - **Action**: Implement unit and Robolectric tests validating jump physics, ground detection, color recovery pipeline integration, and obstacle collision.
  - **Rationale**: Ensures test parity across all three showcase games.
