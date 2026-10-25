# Project Audit Roadmap: 2026-10-24

Generated on: **October 24, 2026**  
Ruleset Reference: [`DOCS/audit/rules.md`](../rules.md)  
Audit Results Reference: [`DOCS/audit/2026-10-24/results.md`](results.md)  
Compliance Baseline: [`AGENTS.md`](/AGENTS.md)

---

## How to Advance This Roadmap

To advance the roadmap step-by-step in subsequent chat sessions, use the standardized prompt defined in [`DOCS/audit/prompt.md`](../prompt.md). Each session will automatically select the first pending task (`- [ ]`), implement the fix, run verification, and mark the item as completed (`- [x]`).

---

## Priority 1: Critical Fixes & AGENTS.md Violations

- [x] **Task 1.1: Extract Raptor Modals from `RaptorGameScreen.kt` to `RaptorModals.kt`**
  - **Target File(s)**: `app/src/main/java/com/example/raptor/ui/RaptorGameScreen.kt`, `app/src/main/java/com/example/raptor/ui/RaptorModals.kt`
  - **Action**: Extract `RaptorPauseDialog`, `RaptorGameOverDialog`, and `RaptorVictoryDialog` into a new dedicated file `RaptorModals.kt`. Break down the 300-line pause dialog function into smaller sub-composables (audio/haptic toggles, difficulty selector, level selector).
  - **Rationale**: `RaptorGameScreen.kt` is currently 1,276 lines (violating Rule 2: 300–500 line limit). These three dialogs account for ~520 lines. Also addresses Rule 3 (functions exceeding 100–150 lines).

- [x] **Task 1.2: Extract Raptor HUD & Status Gauges to `RaptorHudComponents.kt`**
  - **Target File(s)**: `app/src/main/java/com/example/raptor/ui/RaptorGameScreen.kt`, `app/src/main/java/com/example/raptor/ui/RaptorHudComponents.kt`
  - **Action**: Extract `RaptorTopHud` and `StatusGauge` composables into `RaptorHudComponents.kt`. Modularize top status row, score display, and shield/hull integrity bars into sub-composables under 50 lines.
  - **Rationale**: Reduces `RaptorGameScreen.kt` by ~170 lines, matching the modular structure of Arkanoid (`*HudComponents.kt`) mandated by Rule 2.

- [x] **Task 1.3: Extract Raptor Control Deck to `RaptorControls.kt`**
  - **Target File(s)**: `app/src/main/java/com/example/raptor/ui/RaptorGameScreen.kt`, `app/src/main/java/com/example/raptor/ui/RaptorControls.kt`
  - **Action**: Extract `RaptorBottomControls` into `RaptorControls.kt`. Extract sub-composables for the Auto-Fire toggle, Secondary Missile trigger, and Mega-Bomb detonator.
  - **Rationale**: Reduces `RaptorGameScreen.kt` by another ~130 lines, bringing the main screen file under 450 lines to fully satisfy Rule 2.

- [x] **Task 1.4: Decompose Monolithic `RaptorTerminalShopScreen.kt` (720 lines)**
  - **Target File(s)**: `app/src/main/java/com/example/raptor/ui/RaptorTerminalShopScreen.kt`, `app/src/main/java/com/example/raptor/ui/RaptorTerminalComponents.kt`
  - **Action**: Break down the monolithic 630-line composable into dedicated sub-composables: `TerminalTabHeader`, `TerminalWeaponCard`, `TerminalPerkCard`, `TerminalSupplyCard`, and `TerminalPilotStatusHeader`. Keep orchestration in `RaptorTerminalShopScreen.kt` (< 350 lines).
  - **Rationale**: Resolves severe Rule 3 violation (single function spanning 630 lines) and Rule 2 (720-line file).

- [x] **Task 1.5: Decompose Monolithic `RaptorHangarBaseScreen.kt` (566 lines)**
  - **Target File(s)**: `app/src/main/java/com/example/raptor/ui/RaptorHangarBaseScreen.kt`, `app/src/main/java/com/example/raptor/ui/RaptorHangarComponents.kt`
  - **Action**: Extract hangar ship silhouette preview, stat gauges, difficulty chips, and mission launch button into `RaptorHangarComponents.kt`.
  - **Rationale**: Resolves Rule 3 violation (single 436-line function) and Rule 2 violation (566-line file).

- [x] **Task 1.6: Extract Viewport Showcase Dialog from `PixieGameScaffold.kt` (593 lines)**
  - **Target File(s)**: `pixie-android/src/main/kotlin/com/pixie/android/ui/PixieGameScaffold.kt`, `pixie-android/src/main/kotlin/com/pixie/android/ui/PixieScaffoldShowcaseDialog.kt`
  - **Action**: Extract the inlined 170-line `Arcade & Viewport Setup` modal dialog (`showShowcaseDialog`) into a separate file.
  - **Rationale**: Resolves Rule 2 violation (593 lines) and brings `PixieGameScaffold.kt` down to ~420 lines.

- [x] **Task 1.7: Modularize `StandardLayers.kt` (513 lines) in `pixie-engine`**
  - **Target File(s)**: `pixie-engine/src/main/kotlin/com/pixie/engine/core/layer/StandardLayers.kt`, `SkyLayer.kt`, `LandscapeLayers.kt`
  - **Action**: Separate `SkyLayer`, celestial transit models, and parallax landscape layers (`FarLandscapeLayer`, `MidLandscapeLayer`, `ForeLandscapeLayer`) into individual focused files.
  - **Rationale**: Adheres to Rule 2 by keeping all engine layer files below 300 lines and improving reusability.

- [x] **Task 1.8: Modularize `GameSelectorScreen.kt` (590 lines)**
  - **Target File(s)**: `app/src/main/java/com/example/menu/ui/GameSelectorScreen.kt`, `app/src/main/java/com/example/menu/ui/PocGameCard.kt`, `app/src/main/java/com/example/menu/ui/GameSelectorHeader.kt`
  - **Action**: Extract the featured game showcase card, game catalog card, and header banner into separate composables.
  - **Rationale**: Resolves Rule 2 violation (590 lines) and Rule 3 (functions over 190 lines).

---

## Priority 2: Architecture & Modularity Improvements

- [x] **Task 2.1: Decompose `RaptorGameCoordinator.kt` (570 lines)**
  - **Target File(s)**: `app/src/main/java/com/example/raptor/RaptorGameCoordinator.kt`, `app/src/main/java/com/example/raptor/world/RaptorWaveSpawner.kt`, `app/src/main/java/com/example/raptor/world/RaptorCollisionSystem.kt`
  - **Action**: Extract procedural squadron wave spawning and ground target spawning into `RaptorWaveSpawner.kt`. Extract combat collision handling into `RaptorCollisionSystem.kt`.
  - **Rationale**: Separates domain coordination from low-level collision math and wave timelines; brings coordinator well under 350 lines.

- [x] **Task 2.2: Modularize `ArcadeControlDeck.kt` (530 lines)**
  - **Target File(s)**: `pixie-android/src/main/kotlin/com/pixie/android/input/ArcadeControlDeck.kt`, `FloatingArcadeDeck.kt`, `ArcadeButtonView.kt`
  - **Action**: Extract floating draggable button deck and custom button graphic painters into separate files.
  - **Rationale**: Brings control deck under 350 lines and decouples floating gesture state from standard dock layouts.

- [x] **Task 2.3: Modularize `DinoPauseDialog.kt` (407 lines) and `DinoHud.kt` (322 lines)**
  - **Target File(s)**: `app/src/main/java/com/example/dinorun/ui/DinoPauseDialog.kt`, `app/src/main/java/com/example/dinorun/ui/DinoHud.kt`
  - **Action**: Break down dialog settings rows and HUD telemetry pills into focused sub-composables under 40 lines.
  - **Rationale**: Composes UI with small, single-purpose functions according to Rule 3.

---

## Priority 3: API Ergonomics & Reusability Enhancements

- [x] **Task 3.1: Adopt `PixieGame` Fluent Builder in `DinoGameScreen.kt`**
  - **Target File(s)**: `app/src/main/java/com/example/dinorun/ui/DinoGameScreen.kt`
  - **Action**: Refactor `DinoGameScreen` to use the chainable `PixieGame(coordinator.engine).input(...).haptic(...).onTap(...).portrait(...).landscape(...).Render()` DSL, eliminating manual `focusRequester`, `onPreviewKeyEvent`, and `PixieGameCanvas` wiring.
  - **Rationale**: Enforces Rule 1 (Fluent / Chainable API & Declarative Simplicity) across all showcase games.

- [x] **Task 3.2: Encapsulate Raptor Touch Drag Gestures via `PixieGame` DSL**
  - **Target File(s)**: `app/src/main/java/com/example/raptor/ui/RaptorGameScreen.kt`
  - **Action**: Replace manual `detectDragGestures` with letterbox scaling math in `RaptorGameScreen` by registering drag listeners through the `PixieGame` DSL or coordinator adapter.
  - **Rationale**: Adheres to Rule 1: canvas gesture detection and letterboxed resolution scaling belong encapsulated inside engine DSLs rather than copied across game screens.

- [x] **Task 3.3: Fluent Screen Layout Presets for Arcade Form Factors**
  - **Target File(s)**: `pixie-android/src/main/kotlin/com/pixie/android/ui/PixieScreenLayout.kt`
  - **Action**: Add fluent convenience builder methods (e.g., `.arcadeCabinet()`, `.splitWings()`, `.fullCanvas()`) to streamline layout definitions across all games.
  - **Rationale**: Improves developer ergonomics and code reusability across future game prototypes.

---

## Priority 4: Quality & Polish Items

- [x] **Task 4.1: Add Automated Robolectric Tests for Raptor Game Flow**
  - **Target File(s)**: `app/src/test/java/com/example/raptor/RaptorGameCoordinatorTest.kt`
  - **Action**: Implement unit and coordinator tests verifying level progression, boss spawning, difficulty multipliers, and score/credits accumulation.
  - **Rationale**: Protects against regressions during coordinator refactoring.

- [x] **Task 4.2: Add Automated Tests for `PixieGame` Fluent Builder Breakpoints**
  - **Target File(s)**: `pixie-android/src/test/kotlin/com/pixie/android/ui/PixieGameTest.kt`
  - **Action**: Test responsive breakpoint matching and slot resolution in portrait and landscape.
  - **Rationale**: Ensures robustness of engine UI DSL across varied screen form factors.

- [x] **Task 4.3: Retro CRT & Audio Polish**
  - **Target File(s)**: `pixie-android/src/main/kotlin/com/pixie/android/audio/RetroAudioSynthesizer.kt`
  - **Action**: Optimize procedural sound generation buffer allocation to eliminate transient audio stutter on lower-end devices.
  - **Rationale**: Enhances craftsmanship and retro audio quality.
