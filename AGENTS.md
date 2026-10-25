# Coding Guidelines & Agent Preferences

## 1. Architectural & API Design: Fluent / Chainable Calls
- **Fluent Builders**: Strongly prefer chainable, fluent API methods (returning `this` / `apply { ... }`) for configuring engines, layouts, components, and controllers.
  - Example: `PixieGame(engine).input(...).haptic(...).onTap(...).onDragDelta(...).onFrameTick(...).portrait(...).landscape(...).Render(...)`
  - Example: `PixieScreenLayout().topStatusBar { ... }.bottomButtons(...)`
- **Declarative Simplicity**: Avoid low-level boilerplate in UI screens. Common patterns like canvas gesture detection (`detectTapGestures`, `detectDragGestures`), frame loops (`withFrameNanos`), and letterboxed resolution scaling belong encapsulated inside engine DSLs rather than copied across game screens.

## 2. File Size Limits (Soft Limit: 300–500 lines)
- **Soft Maximum**: Strive to keep source files within 300 to 500 lines.
- **Proactive Modularization**:
  - Split large composables and monolithic screens into dedicated, focused files.
  - Typical breakdown for game screens:
    - Main screen / coordinator orchestration (`*GameScreen.kt`)
    - HUD and status components (`*HudComponents.kt`)
    - Dialogs, overlays, and modal alerts (`*Modals.kt`)
    - Control deck and button configurations (`*Controls.kt`)
  - Keep domain logic, coordinators, and renderers cleanly separated.

## 3. Function Size Limits (Soft Limit: 100–150 lines, Target: < 50 lines)
- **Maximum Threshold**: Functions must not exceed 100–150 lines.
- **Ideal Target**: Under 50 lines wherever possible without introducing unnecessary indirection or boilerplate.
- **Sub-Composable Extraction**:
  - Break down complex Compose hierarchies into smaller, single-purpose composables (e.g., status pill, lives indicator, score column, button group).
  - Use clear parameter lists or UI state dataclasses to feed sub-composables.
