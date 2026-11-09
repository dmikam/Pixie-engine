# 08 — UI, NES Arcade Controls & PixieGame DSL Screen

> **Module Scope:** `:app` (`com.example.platformer.ui.*`)  
> **Target Release:** v0.7.0 (Step 8)  
> **Status:** PENDING  
> **Compliance:** `PixieGame` fluent builder DSL, AGENTS.md compliance, files < 500 lines, functions < 50 lines.

---

## 📋 Objectives

Implement the user interface and input layout adhering to the `PixieGame` fluent DSL: an authentic NES gamepad layout (D-Pad + A Jump + B Run/Throw), classic 8-bit status HUD, pause overlay, game over dialog, and stage clear modal.

---

## 🗺️ Tasks & Checklist

- [ ] **Task 8.1: Implement NES D-Pad & Action Buttons in `SuperPixieControls.kt`**
  - **Target File:** `app/src/main/java/com/example/platformer/ui/SuperPixieControls.kt`
  - **Action:**
    - Provide factory functions returning `ArcadeButtonConfig`:
      - `createDpadLeftButton(inputAdapter)`: Left directional tap/hold (`Modifier.testTag("dpad_left")`).
      - `createDpadRightButton(inputAdapter)`: Right directional tap/hold (`Modifier.testTag("dpad_right")`).
      - `createDpadDuckButton(inputAdapter)`: Down directional duck/hide in crate (`Modifier.testTag("dpad_duck")`).
      - `createJumpButton(inputAdapter)`: Button <kbd>A</kbd> circle arcade cap (`Modifier.testTag("button_jump")`).
      - `createActionButton(inputAdapter)`: Button <kbd>B</kbd> circle arcade cap (`Modifier.testTag("button_action")`).
      - `createPauseButton(coordinator)`: Micro top pause button (`Modifier.testTag("button_pause")`).
      - `createFloatingButtons(...)`: Floating customizable layout variant.

- [ ] **Task 8.2: Implement Retro 8-Bit HUD in `SuperPixieHudComponents.kt`**
  - **Target File:** `app/src/main/java/com/example/platformer/ui/SuperPixieHudComponents.kt`
  - **Action:**
    - Replicate the iconic NES top status bar:
      - Column 1: `PIXIE` / Score (`024500`).
      - Column 2: Coin icon + `x04`.
      - Column 3: `WORLD` / `1-1`.
      - Column 4: `TIME` / countdown (`342`).
      - Column 5: Lives count (`x3`).
    - Responsive layout for portrait and landscape split-wings mode.
    - All sub-composables under 40 lines.

- [ ] **Task 8.3: Implement Modals in `SuperPixieModals.kt`**
  - **Target File:** `app/src/main/java/com/example/platformer/ui/SuperPixieModals.kt`
  - **Action:**
    - Extract dialog components into dedicated composables:
      - `SuperPixiePauseDialog`: Resume, Restart, Audio/Haptic toggles, Return to Arcade Hub.
      - `SuperPixieGameOverDialog`: Score summary, restart prompt, high-score display.
      - `SuperPixieVictoryDialog`: Stage clear time bonus calculation, total score, and next level/hub buttons.
    - Keep coordinator file under 120 lines.

- [ ] **Task 8.4: Assemble `SuperPixieGameScreen.kt` with Fluent `PixieGame` DSL**
  - **Target File:** `app/src/main/java/com/example/platformer/ui/SuperPixieGameScreen.kt`
  - **Action:**
    - Chain fluent configuration:
      ```kotlin
      PixieGame(coordinator.engine)
          .input(inputAdapter)
          .haptic(hapticAdapter)
          .onFrameTick { dt -> coordinator.update(dt) }
          .portrait(maxWidth = 600.dp) {
              arcadeCabinet(
                  topBar = { SuperPixiePortraitStatusBar(state, onExitToMenu, onPause) },
                  buttons = listOf(dpadLeft, dpadRight, dpadDuck, actionBtn, jumpBtn, pauseBtn),
                  floatingButtons = floatingBtns
              )
          }
          .landscape(maxHeight = 600.dp) {
              splitWings(
                  leftWing = { SuperPixieLandscapeLeftWing(dpadLeft, dpadRight, dpadDuck) },
                  rightWing = { SuperPixieLandscapeRightWing(actionBtn, jumpBtn, onPause) },
                  floatingButtons = floatingBtns
              )
          }
          .overlays { SuperPixieModals(state, coordinator, onExitToMenu) }
          .Render(modifier = modifier)
      ```
    - Keep file size strictly under 150 lines.

---

## 🔍 Verification

1. Verify portrait and landscape orientations render without UI overlapping the gameplay area.
2. Confirm touch inputs and hardware keyboard keys dispatch correctly to `inputAdapter`.
3. Check all `Modifier.testTag` attributes are present and accessible.
