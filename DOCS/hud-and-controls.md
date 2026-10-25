# 🎮 HUD, Viewports & Arcade Controls Guide

Pixie Engine provides an engine-level **Safe Viewport, Responsive Layout & Arcade Control Deck** architecture in `:pixie-android`. It guarantees that retro games run seamlessly across both portrait and landscape orientations without restarting, protects in-game elements from being obscured by status or navigation bars, eliminates vertical space waste in landscape by absorbing HUD elements into lateral wings, and provides customizable on-screen arcade buttons with native physical gamepad and keyboard support.

---

## 🏗️ Architecture & Component Model

The control and viewport subsystem decouples game logic from Android device form factors using a **Chainable Fluent Builder (`PixieGame`)** and **Orientation/Breakpoint Layouts (`PixieScreenLayout`)**:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PixieGame(engine)                               │
│  - WindowInsets & safeDrawingPadding coordination                     │
│  - Orientation preservation (seamless rotation without Activity reset) │
│  - Real-time Hardware KeyEvent & Gamepad interception                 │
│  - Multiple Breakpoints: .portrait(maxWidth) and .landscape(maxHeight) │
├──────────────────────────────────┬─────────────────────────────────────┤
│ 1. Docked Top Status Bar Slot    │ Non-overlapping isolated HUD surface│
│    or Top Overlay Status Bar     │ (docked for puzzle/brick, overlay   │
│                                  │  for runners like Dino/Raptor)      │
├──────────────────────────────────┼─────────────────────────────────────┤
│ 2. Center Safe Viewport Slot     │ Letterboxed to virtual resolution,  │
│    (Aspect-ratio preserved)      │ 100% vertical height in landscape   │
├──────────────────────────────────┼─────────────────────────────────────┤
│ 3. Lateral Control Wings         │ Left & Right Pillars (width 120dp)  │
│    (Landscape 0px vertical loss) │ Absorbs HUD: Score, Lives, Controls │
├──────────────────────────────────┼─────────────────────────────────────┤
│ 4. Docked Bottom Action Deck     │ Proportional Flex Distribution      │
│    (FlexArcadeButtonRow)         │ (weight, minWidthDp, maxWidthDp)    │
└──────────────────────────────────┴─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       DualModeInputAdapter                             │
│  - Unified InputPort implementation for touch, gamepads, and keyboards │
│  - Zero-latency awaitPointerEventScope instantaneous down events      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 🎨 Fluent Builder DSL (`PixieGame`)

Games are structured cleanly using the chainable `PixieGame` builder:

```kotlin
PixieGame(coordinator.engine)
    .input(inputAdapter)
    .haptic(hapticAdapter)
    // Parameterized Canvas Gestures & Tick
    .onTap { coordinator.onActionTriggered() }
    .onDragDelta { dx, dy -> coordinator.movePaddleByDelta(dx * 0.45f) }
    .onFrameTick { dt -> coordinator.update(dt) }
    .canvasOverlay {
        // In-game overlays like Serve hints or game notifications
        if (state.isBallDocked) ServeHint()
    }
    // Small to medium portrait (phones)
    .portrait(maxWidth = 600.dp) {
        topStatusBar {
            MyPortraitHud(state)
        }
        bottomButtons(
            steerLeftBtn.copy(weight = 1f),
            actionBtn.copy(weight = 1.3f),
            steerRightBtn.copy(weight = 1f),
            pauseBtn.copy(widthDp = 48.dp)
        )
        floatingButtons(floatingBtns)
    }
    // Wide portrait (tablets & foldables)
    .portrait(maxWidth = 1200.dp) {
        topStatusBar { MyTabletHeader(state) }
        bottomButtons(...)
    }
    // Landscape (0px vertical space wasted by HUD)
    .landscape(maxHeight = 600.dp) {
        leftWing {
            ArcadeWingPillar(width = 124.dp) {
                // Exit button, Level badge, Score, Hi-Score, Steer Left & Right
            }
        }
        rightWing {
            ArcadeWingPillar(width = 124.dp) {
                // Lives, Pause button, Combo multiplier, Action button
            }
        }
        floatingButtons(floatingBtns)
    }
    // Modals and dialog overlays
    .overlays {
        if (state.isPaused) PauseDialog()
        if (state.isGameOver) GameOverDialog()
    }
    // Zero-boilerplate Canvas & Layout render
    .Render(modifier = modifier)
```

---

## 📏 Responsive Layout Capabilities (`PixieScreenLayout`)

Each screen layout exposes both **docked** and **overlay** slots:

| Method | Behavior | Best Used For |
| :--- | :--- | :--- |
| `topStatusBar { ... }` | Docked above the game canvas. Never overlaps top game elements (e.g. bricks, barriers). | Arkanoid, Breakout, Space Invaders |
| `topOverlayStatusBar { ... }` | Floats directly over top of the game canvas. | Scrollers (Dino Run, Raptor) where sky extends edge-to-edge |
| `bottomDeck { ... }` | Docked below canvas. Never covers player, paddle, or ground. | Portrait action games |
| `bottomButtons(vararg ...)` | Docked bottom shelf with **proportional flex weights** (`weight`, `minWidthDp`). | Standard arcade controls |
| `bottomOverlayDeck { ... }` | Floats over bottom edge of canvas. | Minimalist transparent HUDs |
| `leftWing { ... }` | Left lateral control & HUD wing in landscape. | Directional controls, Level, Score |
| `rightWing { ... }` | Right lateral control & HUD wing in landscape. | Action buttons, Lives, Power-ups |
| `wings(left, right)` | Convenience helper configuring both lateral pillars. | Standard split arcade wings |
| `floatingButtons(vararg ...)` | On-screen draggable buttons with edit/lock mode. | Player-customizable placement |

---

## 🔘 Proportional Flex Distribution (No Vanishing Buttons)

Previous implementations used fixed pixel or dp sizes, causing the rightmost button (e.g. Pause) to get pushed off-screen and disappear on narrow devices.

Pixie Engine solves this via `FlexArcadeButtonRow`:

```kotlin
val steerLeft = ArcadeButtonConfig(
    id = "left",
    iconVector = Icons.Default.ChevronLeft,
    shape = ArcadeButtonShape.PILL,
    weight = 1.0f,          // Flex fraction (similar to CSS flex-grow)
    minWidthDp = 54.dp,     // Guaranteed minimum touch target
    heightDp = 48.dp
)

val action = ArcadeButtonConfig(
    id = "action",
    label = "SERVE",
    weight = 1.3f,          // Slightly larger flex fraction
    minWidthDp = 68.dp,
    heightDp = 48.dp
)

val pause = ArcadeButtonConfig(
    id = "pause",
    iconVector = Icons.Default.Pause,
    shape = ArcadeButtonShape.CIRCLE,
    widthDp = 48.dp,        // Fixed width for compact square/circle buttons
    minWidthDp = 48.dp
)

// When laid out together:
bottomButtons(steerLeft, action, steerRight, pause)
```
- Weighted buttons share available row width proportionally.
- Fixed buttons (like 48dp Pause) maintain their size.
- Every button is guaranteed to remain visible without clipping on any screen width (320dp to 1200dp).

---

## 🌄 Landscape 0px Vertical Waste Pattern

In landscape mode on phones, vertical height is precious (often only 360–420dp). Placing a 50dp status bar at the top reduces the game canvas height by ~15%, squishing gameplay.

**The Lateral Wing Solution:**
- Move all HUD elements (Score, High Score, Level badge, Lives, Combo, Power-ups) into the left and right wing pillars.
- `topStatusBar` is omitted in the landscape layout.
- The game canvas receives **100% of the screen height**, letterboxed horizontally between the wings.

---

## 🕹️ Draggable Floating Controls (`FloatingAdjustableDeck`)

When using `floatingButtons`:
- Players can toggle between **Lock Mode** (active gameplay) and **Move Mode** (edit mode with drag handles).
- In Move Mode, buttons can be freely dragged anywhere on the screen without triggering in-game actions.
- In Lock Mode, buttons deliver zero-latency instantaneous microswitch touches.

---

## 🎮 Dual Controller & Physical Hardware Support

Pixie Engine automatically binds physical hardware inputs alongside on-screen touch:

### Supported Input Methods
1. **On-Screen Touch**: Tactile arcade buttons and touch gestures.
2. **Physical Gamepads**: Bluetooth / USB gamepads (Xbox, PlayStation, 8BitDo, arcade fightsticks):
   - **D-Pad / Analog Sticks**: Directional movement (`moveX`, `moveY`).
   - **Button A / R1**: Primary Action (Serve / Jump / Fire).
   - **Button B / X**: Secondary Action (Bomb / Missile / Turbo).
3. **Hardware Keyboards**:
   - **Arrow Keys or WASD**: Directional movement.
   - **Spacebar / Enter / Z / J**: Primary Action.
   - **Shift / X / K**: Secondary Action.

### Real-Time Input Status Badge
The scaffold features a non-intrusive indicator chip displaying the active controller source:
- `TOUCH`: Touchscreen buttons actively being pressed.
- `GAMEPAD`: Physical gamepad controller active.
- `KEYS`: Hardware keyboard active.
