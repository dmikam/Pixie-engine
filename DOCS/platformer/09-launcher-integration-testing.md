# 09 — Launcher Integration, End-to-End Testing & Validation

> **Module Scope:** `:app` (`com.example.*`, `com.example.platformer.*`)  
> **Target Release:** v0.7.0 (Step 9)  
> **Status:** PENDING  
> **Compliance:** Robolectric local JVM testing, zero emulator requirement, AGENTS.md compliance.

---

## 📋 Objectives

Wire Super Pixie into the main launcher arcade menu, connect `MainActivity` navigation routes, implement comprehensive automated Robolectric test coverage validating critical gameplay journeys, and perform full compilation and verification.

---

## 🗺️ Tasks & Checklist

- [ ] **Task 9.1: Register Navigation in `MainActivity.kt`**
  - **Target File:** `app/src/main/java/com/example/MainActivity.kt`
  - **Action:**
    - Add `AppScreen.SUPER_PIXIE` to `AppScreen` enum.
    - Wire `SUPER_PIXIE` branch in `Crossfade(targetState = currentScreen)`:
      ```kotlin
      AppScreen.SUPER_PIXIE -> {
          SuperPixieGameScreen(
              onExitToMenu = { currentScreen = AppScreen.MENU }
          )
      }
      ```
    - Pass `onLaunchSuperPixie = { currentScreen = AppScreen.SUPER_PIXIE }` into `GameSelectorScreen`.

- [ ] **Task 9.2: Connect Launcher Card & Detail Launch in `GameSelectorScreen.kt`**
  - **Target File:** `app/src/main/java/com/example/menu/ui/GameSelectorScreen.kt`
  - **Action:**
    - Update `launchGameById`:
      ```kotlin
      "superpixie" -> onLaunchSuperPixie()
      ```
    - Update `PocGameInfo` status for `superpixie` from `GameStatus.NEXT_UP` to `GameStatus.PLAYABLE`.
    - Ensure tapping "PLAY" on the Super Pixie card immediately launches the game.

- [ ] **Task 9.3: Add Automated Robolectric Tests in `SuperPixieGameCoordinatorTest.kt`**
  - **Target File:** `app/src/test/java/com/example/platformer/SuperPixieGameCoordinatorTest.kt`
  - **Action:**
    - Implement Robolectric test suite validating:
      - `initial setup configures world 1-1, grounded player, 3 lives and default metrics`
      - `right directional input accelerates player forward with momentum`
      - `jump input triggers vertical velocity and airborne state`
      - `jumping into question block triggers head bonk recoil, converts to used block and awards coin`
      - `falling onto enemy top triggers stomp, squashes enemy, and bounces player upward`
      - `lateral contact with enemy damages player and triggers invulnerability or life loss`
      - `action button adjacent to crate picks up crate and attaches above player head`
      - `throwing carried crate creates airborne projectile and smashes targets`
      - `touching flagpole stops timer, slides player down, and triggers stage victory`

- [ ] **Task 9.4: Final Compilation & Documentation Sync**
  - **Action:**
    - Run `compile_applet` to confirm clean compilation.
    - Run `gradle :app:testDebugUnitTest` to confirm 100% pass rate across all tests.
    - Update `DOCS/roadmap.md`, `DOCS/platformer/current_state.md`, and `DOCS/index.md` marking PoC 4 (Super Pixie) as **CLOSED / Complete (v0.7.0)**.

---

## 🔍 Verification

1. All automated tests in `:pixie-engine`, `:pixie-android`, and `:app` execute cleanly and pass.
2. Android launcher transitions smoothly between menu and Super Pixie.
3. Returning to menu via back button or pause modal functions without memory leaks.
