# Safe Viewport Architecture & Customizable Arcade Deck

Implement seamless horizontal orientation handling, engine-level non-overlapping viewport calculation, 3 switchable landscape presentation modes (Centered Side Wings, Retro Cabinet Bezel, Adaptive Docked Fit), and a fully customizable Arcade / Console Button framework supporting both on-screen touch and physical controllers (gamepad / keyboard).

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user-confirmed choices from Phase 1 have been incorporated into this plan:
> - **Landscape Viewport Modes**: Implement all three viewport modes with a live showcase switch (Centered Aspect Ratio with Side Wings, Retro Cabinet Bezel, and Adaptive Dynamic Fit).
> - **Arcade Button Placements**: Implement all three placements with a live showcase switch (Split Side Wings, Docked Bottom Console Strip, and Floating Adjustable Controls).
> - **Input Support**: Provide first-class support for both physical controllers (gamepads, Bluetooth arcade sticks, hardware keyboards) and on-screen touch controls, with an auto-detect / toggle switch.

---

## 1. Overview & Core Concept

When switching between portrait and landscape orientations, retro games need to preserve uninterrupted gameplay while dynamically adapting their layout. Currently:
1. Android recreates the Activity on rotation, destroying engine state and restarting the active game.
2. The Top Status Bar and Bottom Actions Deck float directly over the fullscreen canvas; in landscape, the top bar covers the bricks and the bottom bar occludes the paddle/carriage.

This update establishes an engine-level **Safe Viewport & Arcade Deck Architecture** in `pixie-android` and `pixie-engine`, ensuring that:
- Game worlds render into an un-occluded, aspect-ratio-correct safe zone where neither bricks nor paddle are ever obstructed.
- Orientation changes are handled natively without Activity destruction or state loss.
- Players can toggle between three landscape layouts and three arcade button placements on the fly.
- Physical gamepads and keyboards work out of the box with zero configuration alongside tactile, low-latency on-screen arcade buttons.

---

## 2. User Experience & Visual Design

### Landscape Viewport Presentation Modes
1. **Mode A: Centered Aspect Ratio with Side Control Wings**
   - The game canvas is centered with its authentic vertical aspect ratio preserved.
   - Left and right flanks become dedicated tactile control decks (left D-pad/steerer, right arcade buttons) with no overlap over the game field.
2. **Mode B: Retro Arcade Cabinet Bezel**
   - The playfield is embedded inside an arcade cabinet frame featuring CRT scanlines/phosphor glow styling, metallic edge molding, and marquee banner badges.
   - Arcade buttons are styled as inset microswitch buttons embedded directly into the lower bezel wings.
3. **Mode C: Adaptive Dynamic Fit (Docked Bars)**
   - The game canvas dynamically claims the exact remaining height between the top status bar and bottom docked action strip, recalculating its scale and offset so elements (e.g. carriage at y=580) are 100% visible and un-clipped.

### Customizable Arcade / Console Deck
- **Tactile Shapes**: Choose from classic Circular arcade caps, Octagonal competition buttons, Rounded Bevel slabs, or Pill buttons.
- **Button Visual Types**: Customizable via Text/Letter (A, B, X, Y, TURBO), Vector Material Icons, or Custom Retro `PixelSprite` icons.
- **Microswitch Haptics & Response**: Zero-lag immediate pointer-down trigger with release safety and tactile haptic vibration pulses.
- **Customizable Layouts**:
  - *Split Wings*: Left hand handles directional navigation (D-Pad / Steerer); right hand fires actions.
  - *Docked Bottom Strip*: Traditional horizontal arcade shelf.
  - *Floating / Draggable*: Floating buttons that can be repositioned and toggled by the user.

### Seamless Orientation & Input Switching
- Smooth rotation without audio stutter, canvas flicker, or game restart.
- An accessible **Arcade Quick Settings Panel** (floating micro-gear or options chip) accessible during gameplay to switch presentation modes, arcade button arrangements, and input methods in real time.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Activity Configuration Changes vs State Serialization**
  - *Chosen Approach*: Add `android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboard|keyboardHidden"` to `MainActivity` in `AndroidManifest.xml`.
  - *Why*: Eliminates Activity destruction on rotation, preventing game loop resets, re-allocations, and audio drops, while letting Jetpack Compose recompose smoothly to the new window constraints.
  - *Alternatives Considered*: Retaining state in ViewModels / Saveable. Still causes Activity teardown, SoundPool re-creation, and dropped audio threads.

- **Decision 2: Slot-Based `PixieGameScaffold` vs Game-Specific Layouts**
  - *Chosen Approach*: Build a reusable `PixieGameScaffold` in `pixie-android` that encapsulates top status bar, bottom/side action decks, cabinet bezels, safe viewport math, and orientation handling.
  - *Why*: Solves the overlap issue universally across Arkanoid, Raptor, Dinorun, and future engine games rather than duplicating ad-hoc layout math in every screen.

- **Decision 3: Unified Input Pipeline (Touch + Physical Controller)**
  - *Chosen Approach*: Extend `DualModeInputAdapter` with a `GamepadInputHandler` that consumes `KeyEvent` (DPAD_LEFT, DPAD_RIGHT, DPAD_UP, DPAD_DOWN, BUTTON_A, BUTTON_B, KEYCODE_SPACE, Arrow Keys) and `MotionEvent` (analog sticks), mapping directly into `InputPort`.
  - *Why*: Works identically across touch screens, Bluetooth gamepads (Xbox, PlayStation, 8BitDo), Android TV / Chromebooks, and PC keyboards in emulator environments.

---

## 4. Technical Architecture & Component Hierarchy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          MainActivity                                  │
│  (configChanges: orientation, screenSize, keyboard, screenLayout)       │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       PixieGameScaffold                                │
│  - Window Size / Orientation Detector (Portrait vs Landscape)          │
│  - Mode Switcher State: ViewportMode, ControlPlacement, InputSource    │
│  - Key Event Listener (Gamepad / Keyboard interceptor)                 │
├──────────────────────────────────┬─────────────────────────────────────┤
│ 1. Engine Top Status Bar Slot    │ Non-overlapping status surface      │
├──────────────────────────────────┼─────────────────────────────────────┤
│ 2. Center Safe Viewport Slot     │ [Mode A: Centered with Side Wings]  │
│    (PixieGameCanvas inside       │ [Mode B: Retro Cabinet Bezel]       │
│     aspect-constrained frame)    │ [Mode C: Adaptive Docked Fit]       │
├──────────────────────────────────┼─────────────────────────────────────┤
│ 3. Arcade Deck Slot              │ [Split Side Wings / Bottom / Float] │
│    (ArcadeControlDeck)           │ - Customizable Buttons & D-Pad      │
│                                  │ - Sprites, Shapes, Icons & Labels   │
└──────────────────────────────────┴─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Engine Ports & Adapters                         │
│  - DualModeInputAdapter: Touch + Gamepad/Keyboard Key Event dispatch   │
│  - AndroidCanvasRenderer: Safe letterbox transformation                │
│  - PixieEngine: Deterministic game physics & rendering                 │
└────────────────────────────────────────────────────────────────────────┘
```

### Component Details
1. **`AndroidManifest.xml`**:
   - Update `MainActivity` declaration with `android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboard|keyboardHidden"`.
2. **`pixie-android/src/main/kotlin/com/pixie/android/ui/PixieGameScaffold.kt`**:
   - The universal container providing:
     - Viewport Mode switcher (`CENTERED_WINGS`, `CABINET_BEZEL`, `ADAPTIVE_FIT`).
     - Arcade Placement switcher (`SPLIT_WINGS`, `BOTTOM_STRIP`, `FLOATING`).
     - Input Method switcher (`HYBRID_AUTO`, `TOUCH_ONLY`, `PHYSICAL_ONLY`).
     - Precise bounds calculation ensuring the canvas never occupies space reserved by the status bar or action bar.
3. **`pixie-android/src/main/kotlin/com/pixie/android/input/ArcadeControlDeck.kt` & `ArcadeButtonConfig.kt`**:
   - Reusable, customizable arcade button model:
     - Shape (`CIRCLE`, `ROUNDED_RECT`, `OCTAGON`, `PILL`)
     - Visual content (Text, Icon, or `PixelSprite`)
     - Size & Color styling
     - Target action binding (`MOVE_LEFT`, `MOVE_RIGHT`, `MOVE_UP`, `MOVE_DOWN`, `ACTION_PRIMARY`, `ACTION_SECONDARY`, `PAUSE`)
4. **`pixie-android/src/main/kotlin/com/pixie/android/input/PhysicalControllerHandler.kt`**:
   - Intercepts key presses and joystick motions from physical gamepads, keyboards, and arcade sticks, forwarding them directly to `DualModeInputAdapter`.
5. **`ArkanoidGameScreen.kt` & Coordinator**:
   - Refactor to use `PixieGameScaffold` and the new `ArcadeControlDeck`.
   - Ensure paddle / carriage movement drag gestures calibrate precisely against the letterboxed viewport.
   - Expose the Quick Settings showcase drawer/chip to toggle all 3 viewport modes and 3 button placements in real time.

---

## 5. Verification Plan

1. **Compilation Verification**:
   - Run `compile_applet` to ensure zero compilation or dependency errors.
2. **Orientation Handling Test**:
   - Verify that rotating between portrait and landscape maintains the game loop, keeps music/audio playing, and preserves score/lives/bricks without resetting.
3. **Safe Viewport & Non-Overlap Test**:
   - Test in landscape orientation to verify that top bricks are completely clear of the status bar and the carriage/paddle at the bottom is completely clear of the buttons bar.
4. **Showcase Switcher Verification**:
   - Cycle between the 3 viewport modes (Centered Side Wings, Retro Cabinet Bezel, Adaptive Fit) and 3 arcade button placements (Split Wings, Bottom Strip, Floating) in real time.
5. **Input Verification**:
   - Test on-screen arcade buttons (immediate mousedown, continuous hold).
   - Test hardware keyboard keys (Arrow keys, A/D, Space, Enter) and gamepad buttons.
