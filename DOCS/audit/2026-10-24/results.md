# Project Codebase Audit Report: 2026-10-24

Conducted on: **October 24, 2026**  
Ruleset Reference: [`DOCS/audit/rules.md`](../rules.md)  
Compliance Baseline: [`AGENTS.md`](/AGENTS.md)

---

## 1. Executive Summary

This comprehensive audit evaluated the Pixie Engine architecture across all three modules:
1. **`pixie-engine`**: Pure Kotlin multiplatform core (state, math, physics, particles, layers, weapons, loops, chunks).
2. **`pixie-android`**: Android Jetpack Compose runtime adapters, rendering pipeline, audio synthesis, and the `PixieGame` fluent builder DSL.
3. **`app`**: The game showcases and menu launcher, comprising **Arkanoid** (Breakout clone), **DinoRun** (Endless runner), **Raptor** (Vertical arcade shmup), and the **Menu/Launcher** system.

### Key Strengths
- **Exemplary Arkanoid Implementation**: `app/arkanoid` demonstrates ideal adherence to `AGENTS.md`. It uses the `PixieGame` fluent builder DSL, encapsulates all low-level gesture detection and letterbox scaling, and is cleanly decomposed into `ArkanoidGameScreen.kt` (134 lines), `ArkanoidHudComponents.kt` (395 lines), `ArkanoidModals.kt` (324 lines), and `ArkanoidControls.kt` (104 lines).
- **Rock-Solid Core Engine**: `pixie-engine` maintains high reusability (10/10), decoupled ports, deterministic collision math, and efficient entity/particle pooling.
- **Rich Retro Graphics & Audio**: The canvas renderer, ColorRecovery pipeline, and procedural audio synthesizer deliver an authentic retro gaming experience.

### Critical Concerns & AGENTS.md Violations
- **Severe Monolith in Raptor UI**: `RaptorGameScreen.kt` has ballooned to **1,276 lines**, containing the main screen, HUD gauges, control decks, pause modal (300 lines in a single function), game over dialog, victory dialog, and raw gesture handling.
- **Massive Function Bodies**: `RaptorTerminalShopScreen.kt` contains a single `@Composable` function spanning **630 lines** (Rule 3 violation). `RaptorHangarBaseScreen.kt` contains a single `@Composable` function spanning **436 lines**.
- **Boilerplate Duplication in DinoRun & Raptor**: Both Raptor and DinoRun bypass the `PixieGame` fluent builder DSL, manually duplicating raw gesture detection (`detectDragGestures`), hardware key preview listeners, and viewport letterboxing calculations in UI composables (Rule 1 violation).
- **Monolithic Engine & Library Files**: `PixieGameScaffold.kt` (593 lines), `ArcadeControlDeck.kt` (530 lines), `StandardLayers.kt` (513 lines), and `GameSelectorScreen.kt` (590 lines) all exceed the soft maximum of 300–500 lines (Rule 2 violation).

---

## 2. Scorecard Matrix (0–10)

| Subsystem / Module | Modularity | Reusability | Extensibility | Readability | Easy to Understand | Easy to Use | Overall Quality |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **`pixie-engine` Core** | 8 | 10 | 9 | 9 | 8 | 9 | **8.8** |
| **`pixie-android` Adapters** | 7 | 9 | 8 | 8 | 7 | 8 | **7.8** |
| **`app/arkanoid` Showcase** | 9 | 8 | 9 | 9 | 9 | 10 | **9.0** |
| **`app/dinorun` Showcase** | 8 | 7 | 8 | 8 | 8 | 6 | **7.5** |
| **`app/raptor` Showcase** | 3 | 5 | 5 | 4 | 4 | 3 | **4.1** |
| **`app/menu` Launcher** | 6 | 7 | 7 | 7 | 8 | 8 | **7.1** |
| **Project Weighted Average** | **6.8** | **7.7** | **7.7** | **7.5** | **7.3** | **7.3** | **7.4** |

---

## 3. Section-by-Section Analysis

### 3.1. `pixie-engine` (Core Engine)
- **Modularity**: 8/10
- **Reusability**: 10/10
- **Extensibility**: 9/10
- **Readability**: 9/10
- **Easy to Understand**: 8/10
- **Easy to Use**: 9/10
- **Overall Quality**: 8.8/10

#### AGENTS.md Compliance Checklist
- [x] Rule 1: Fluent / Chainable API & Boilerplate Encapsulation
- [ ] Rule 2: File Size Limits (300–500 lines) & Modularization *(Failed: `StandardLayers.kt` is 513 lines)*
- [x] Rule 3: Function Size Limits (< 50 lines target, max 100–150 lines)

#### Strengths
- Decoupled `EnginePorts` (Render, Audio, Haptic, Input) allow pure JVM unit testing with zero Android dependencies.
- Vector math (`Vec2`), continuous sweep collision detection, and pooling architecture (`PixiePool`, `ParticlePool`, `ProjectilePool`) achieve zero garbage generation during active game loops.
- Clean screen state machine (`ScreenManager`, `PixieScreen`).

#### Areas for Improvement & Violations
- **`StandardLayers.kt` File Size (513 lines)**: Consolidates `SkyLayer`, `FarLandscapeLayer`, `MidLandscapeLayer`, `ForeLandscapeLayer`, and `ParticleWrapperLayer` in a single file. Should be modularized into individual layer files under `com.pixie.engine.core.layer`.

---

### 3.2. `pixie-android` (Compose Runtime & DSL)
- **Modularity**: 7/10
- **Reusability**: 9/10
- **Extensibility**: 8/10
- **Readability**: 8/10
- **Easy to Understand**: 7/10
- **Easy to Use**: 8/10
- **Overall Quality**: 7.8/10

#### AGENTS.md Compliance Checklist
- [x] Rule 1: Fluent / Chainable API & Boilerplate Encapsulation *(Provided via `PixieGame` builder)*
- [ ] Rule 2: File Size Limits (300–500 lines) & Modularization *(Failed: `PixieGameScaffold.kt` is 593 lines; `ArcadeControlDeck.kt` is 530 lines)*
- [ ] Rule 3: Function Size Limits (< 50 lines target, max 100–150 lines) *(Failed: `PixieGameScaffold` inlines a 170-line modal dialog)*

#### Strengths
- `PixieGame` DSL provides fluent, chainable method chaining (`input()`, `haptic()`, `onTap()`, `onDragDelta()`, `portrait()`, `landscape()`, `Render()`).
- `RetroAudioSynthesizer` generates rich 8-bit procedural sound effects on-device without bundled audio assets.
- `DualModeInputAdapter` harmonizes physical controllers, keyboard inputs, and on-screen arcade buttons.

#### Areas for Improvement & Violations
- **`PixieGameScaffold.kt` (593 lines)**: Inlines the entire `Arcade & Viewport Setup` dialog (~170 lines) and complex placement logic. The dialog should be extracted to `PixieScaffoldShowcaseDialog.kt`.
- **`ArcadeControlDeck.kt` (530 lines)**: Bundles split-wings layouts, horizontal strips, floating draggable buttons, and individual button painters. The floating deck and individual button painters should be extracted into separate files.

---

### 3.3. `app/arkanoid` (Breakout Showcase)
- **Modularity**: 9/10
- **Reusability**: 8/10
- **Extensibility**: 9/10
- **Readability**: 9/10
- **Easy to Understand**: 9/10
- **Easy to Use**: 10/10
- **Overall Quality**: 9.0/10

#### AGENTS.md Compliance Checklist
- [x] Rule 1: Fluent / Chainable API & Boilerplate Encapsulation *(Exemplary implementation)*
- [x] Rule 2: File Size Limits (300–500 lines) & Modularization *(All files under 470 lines)*
- [x] Rule 3: Function Size Limits (< 50 lines target, max 100–150 lines)

#### Strengths
- Reference architecture for the entire project. Cleanly split into:
  - `ArkanoidGameScreen.kt` (134 lines)
  - `ArkanoidHudComponents.kt` (395 lines)
  - `ArkanoidControls.kt` (104 lines)
  - `ArkanoidModals.kt` (324 lines)
  - `ArkanoidGameCoordinator.kt` (470 lines)
  - `ArkanoidLevelCatalog.kt` (220 lines)
- Fully harnesses `PixieGame` DSL with responsive portrait/landscape breakpoints, zero vertical wasted space in landscape, and decoupled `StateFlow<ArkanoidUiState>`.

#### Areas for Improvement & Violations
- None critical. `ArkanoidGameCoordinator.kt` is 470 lines and near the 500-line ceiling, but remains cohesive.

---

### 3.4. `app/dinorun` (Endless Runner Showcase)
- **Modularity**: 8/10
- **Reusability**: 7/10
- **Extensibility**: 8/10
- **Readability**: 8/10
- **Easy to Understand**: 8/10
- **Easy to Use**: 6/10
- **Overall Quality**: 7.5/10

#### AGENTS.md Compliance Checklist
- [ ] Rule 1: Fluent / Chainable API & Boilerplate Encapsulation *(Failed: Bypasses `PixieGame` DSL; manually wires `Box`, `focusRequester`, `onPreviewKeyEvent`, and `PixieGameCanvas`)*
- [x] Rule 2: File Size Limits (300–500 lines) & Modularization *(All files under 410 lines)*
- [x] Rule 3: Function Size Limits (< 50 lines target, max 100–150 lines)

#### Strengths
- Already split into dedicated dialogs (`DinoPauseDialog.kt`, `DinoGameOverDialog.kt`) and HUD (`DinoHud.kt`).
- Lightweight coordinator (`DinoGameCoordinator.kt` at 153 lines) with well-separated world definitions.

#### Areas for Improvement & Violations
- **Bypasses `PixieGame` Builder**: `DinoGameScreen.kt` manually instantiates `PixieGameCanvas`, handles hardware key previews, and manages touch input, duplicating logic that `PixieGame` provides out of the box. Refactoring to `PixieGame` will reduce boilerplate and align with Arkanoid's pattern.

---

### 3.5. `app/raptor` (Arcade Shmup Showcase)
- **Modularity**: 3/10
- **Reusability**: 5/10
- **Extensibility**: 5/10
- **Readability**: 4/10
- **Easy to Understand**: 4/10
- **Easy to Use**: 3/10
- **Overall Quality**: 4.1/10

#### AGENTS.md Compliance Checklist
- [ ] Rule 1: Fluent / Chainable API & Boilerplate Encapsulation *(Failed: Bypasses `PixieGame` DSL; manually calculates letterbox aspect scaling in raw drag gestures)*
- [ ] Rule 2: File Size Limits (300–500 lines) & Modularization *(Failed: `RaptorGameScreen.kt` is 1,276 lines; `RaptorTerminalShopScreen.kt` is 720 lines; `RaptorGameCoordinator.kt` is 570 lines; `RaptorHangarBaseScreen.kt` is 566 lines)*
- [ ] Rule 3: Function Size Limits (< 50 lines target, max 100–150 lines) *(Failed: `RaptorTerminalShopScreen` has a 630-line function; `RaptorHangarBaseScreen` has a 436-line function; `RaptorPauseDialog` has a 300-line function)*

#### Strengths
- Gameplay design is rich: boss fights, weapon tiers, secondary homing missiles, mega-bombs, ground destructibles, radar towers, and CRT shop terminal.
- Visual layers (`RaptorTerrainLayer`, `RaptorCloudLayer`, `RaptorProjectilesLayer`) are clean and responsive.

#### Areas for Improvement & Violations
1. **Critical Monolith `RaptorGameScreen.kt` (1,276 lines)**:
   - Contains HUD gauges (`RaptorTopHud`, `StatusGauge`: 167 lines).
   - Contains control bar (`RaptorBottomControls`: 130 lines).
   - Contains 3 modals: `RaptorPauseDialog` (300 lines), `RaptorGameOverDialog` (109 lines), `RaptorVictoryDialog` (114 lines).
   - Must be modularized into:
     - `RaptorGameScreen.kt` (Orchestration only, ~250 lines)
     - `RaptorHudComponents.kt`
     - `RaptorControls.kt`
     - `RaptorModals.kt`
2. **Monolithic Function in `RaptorTerminalShopScreen.kt` (720 lines)**:
   - Single 630-line Composable function with tab selection, weapon upgrade list, perk list, item list, and scanline rendering.
   - Must extract sub-composables: `TerminalTabHeader`, `TerminalWeaponList`, `TerminalPerksList`, `TerminalConsumablesList`, `TerminalFooter`.
3. **Monolithic Function in `RaptorHangarBaseScreen.kt` (566 lines)**:
   - Single 436-line Composable function. Must extract hangar ship preview, stats meters, mission select carousel, and action button bar.
4. **Monolithic Coordinator `RaptorGameCoordinator.kt` (570 lines)**:
   - Coordinator combines collision handling (89 lines), wave spawning (45 lines), ground target spawning (60 lines), and drop tables.
   - Should extract wave spawner (`RaptorWaveSpawner`) and collision resolver (`RaptorCollisionSystem`).
5. **Direct Touch Calculation Boilerplate**:
   - `RaptorGameScreen.kt` computes raw letterbox aspect ratios inside `detectDragGestures` instead of using the engine DSL.

---

### 3.6. `app/menu` (Game Selector & Roadmap UI)
- **Modularity**: 6/10
- **Reusability**: 7/10
- **Extensibility**: 7/10
- **Readability**: 7/10
- **Easy to Understand**: 8/10
- **Easy to Use**: 8/10
- **Overall Quality**: 7.1/10

#### AGENTS.md Compliance Checklist
- [x] Rule 1: Fluent / Chainable API & Boilerplate Encapsulation
- [ ] Rule 2: File Size Limits (300–500 lines) & Modularization *(Failed: `GameSelectorScreen.kt` is 590 lines; `RoadmapScreen.kt` is 489 lines)*
- [ ] Rule 3: Function Size Limits (< 50 lines target, max 100–150 lines) *(Failed: `GameSelectorScreen` has functions over 190 lines)*

#### Strengths
- Engaging launcher UI with high-contrast arcade styling, quick launch buttons, audio effects, and modal inspect dialogs.
- `PocGameInfo.kt` model catalog is clean and informative.

#### Areas for Improvement & Violations
- **`GameSelectorScreen.kt` (590 lines)**: Inlines the header banner, category filter chips, featured banner, game card composable, and developer metrics. Sub-composables (`PocGameCard`, `SelectorHeader`, `QuickLaunchCarousel`) should be extracted.

---

## 4. Summary of AGENTS.md Rule Violations

| File | Lines | Violations Found |
| :--- | :---: | :--- |
| `app/.../raptor/ui/RaptorGameScreen.kt` | **1,276** | **Rule 2** (extreme file size), **Rule 1** (bypasses fluent builder; raw letterbox math), **Rule 3** (dialogs > 150 lines) |
| `app/.../raptor/ui/RaptorTerminalShopScreen.kt` | **720** | **Rule 2** (file size > 500), **Rule 3** (single Composable is 630 lines) |
| `pixie-android/.../ui/PixieGameScaffold.kt` | **593** | **Rule 2** (file size > 500), **Rule 3** (inlined 170-line modal dialog) |
| `app/.../menu/ui/GameSelectorScreen.kt` | **590** | **Rule 2** (file size > 500), **Rule 3** (functions > 190 lines) |
| `app/.../raptor/RaptorGameCoordinator.kt` | **570** | **Rule 2** (file size > 500), bloated domain logic |
| `app/.../raptor/ui/RaptorHangarBaseScreen.kt` | **566** | **Rule 2** (file size > 500), **Rule 3** (single Composable is 436 lines) |
| `pixie-android/.../input/ArcadeControlDeck.kt` | **530** | **Rule 2** (file size > 500), multiple control styles bundled |
| `pixie-engine/.../layer/StandardLayers.kt` | **513** | **Rule 2** (file size > 500), 5 distinct layers bundled in one file |
| `app/.../dinorun/ui/DinoGameScreen.kt` | 217 | **Rule 1** (bypasses `PixieGame` fluent builder; manual canvas & keys) |
