# Project Audit Rules & Guidelines

This document outlines the standard auditing process, criteria, and reporting conventions for codebases within the project. Every audit must adhere to the instructions and scoring methodologies defined below.

---

## 1. Audit Process Overview

When conducting an audit:
1. **Analyze Each Part of the Project**: Systematically inspect all modules, packages, core engines, UI components, data structures, and game implementations (`pixie-engine`, `pixie-android`, and game-specific implementations like DinoRun, Arkanoid, Raptor, and menu systems).
2. **Score Against Categories (0–10)**: Grade each component across six core dimensions plus an aggregate Overall Quality score.
3. **Check Compliance with `AGENTS.md`**: Verify adherence to architectural rules, file size limits, and function size limits.
4. **Document Findings in Dated Reports**:
   - Detailed evaluation and scorecards must be saved to:  
     `DOCS/audit/Y-m-d/results.md` (e.g., `DOCS/audit/2026-10-24/results.md`).
   - An actionable roadmap of recommended fixes and refactorings formatted as a checklist must be saved to:  
     `DOCS/audit/Y-m-d/roadmap.md` (e.g., `DOCS/audit/2026-10-24/roadmap.md`).

---

## 2. Evaluation Criteria & Scoring Rubric (0 to 10)

Each audited section must be evaluated on an integer scale from 0 to 10 in the following categories:

| Category | Description | Scoring Guidance (0–10) |
| :--- | :--- | :--- |
| **Modularity** | Degree to which components are separated into independent, interchangeable, and focused units. | **10**: Pristine separation of concerns; zero leaky abstractions; isolated dependencies.<br>**5**: Partial modularity; some monolithic files or blended concerns.<br>**0**: High coupling; monolithic spaghetti code. |
| **Reusability** | Capability of code, engines, DSLs, and utility modules to be reused across different games or screens without modification. | **10**: Highly generic, parametric components easily leveraged in multiple contexts.<br>**5**: Reusable in theory but requires copy-pasting or heavy refactoring.<br>**0**: Hardcoded one-off solutions tied strictly to a single instance. |
| **Extensibility** | Ease of adding new features, entities, screens, or behaviors without modifying existing core code (Open/Closed Principle). | **10**: Clean plug-and-play architecture, interfaces, and open registries.<br>**5**: Extensions require modifying multiple conditional branches or existing classes.<br>**0**: Rigid architecture where new additions require rewrite of core logic. |
| **Readability** | Clarity of formatting, naming conventions, consistency, and clean code presentation. | **10**: Crystal-clear naming, self-documenting logic, clean declarative Kotlin/Compose style.<br>**5**: Moderate readability; occasional cryptic names or cluttered blocks.<br>**0**: Unreadable, obfuscated, or convoluted formatting. |
| **Easy to Understand** | Cognitive load required for a developer to comprehend the control flow, lifecycle, and mental model. | **10**: Intuitive mental model; straightforward control flow and clear state management.<br>**5**: Non-obvious side effects or complex state interactions requiring deep tracing.<br>**0**: Opaque, confusing flow with hidden side effects and tangled state. |
| **Easy to Use** | Developer ergonomics of APIs, builders, Compose components, and engines. | **10**: Fluent, ergonomic builders; discoverable DSLs; safe defaults; minimal boilerplate.<br>**5**: Usable but awkward API with excessive required parameters or setup steps.<br>**0**: Fragile API prone to misconfiguration or runtime errors. |
| **Overall Quality** | Holistic synthesis of stability, design elegance, robustness, and craftsmanship. | **10**: Production-ready, exemplary architecture.<br>**5**: Functional but carries noticeable technical debt.<br>**0**: Unstable, brittle, or fundamentally flawed. |

---

## 3. Compliance with `AGENTS.md` Rules

Auditors must explicitly audit compliance with the rules defined in `AGENTS.md`:

### Rule 1: Architectural & API Design — Fluent / Chainable Calls
- **Fluent Builders**: Check whether engines, layouts, components, and controllers offer chainable, fluent API methods (returning `this` or using `apply { ... }`).
  - *Example target pattern*: `PixieGame(engine).input(...).haptic(...).onTap(...).onDragDelta(...).onFrameTick(...).portrait(...).landscape(...).Render(...)`
  - *Example target pattern*: `PixieScreenLayout().topStatusBar { ... }.bottomButtons(...)`
- **Declarative Simplicity**: Verify that UI screens are free from low-level boilerplate. Check whether canvas gesture detection (`detectTapGestures`, `detectDragGestures`), frame loops (`withFrameNanos`), and letterboxed resolution scaling are encapsulated inside engine/DSL wrappers rather than copied directly into game composables.

### Rule 2: File Size Limits (Soft Limit: 300–500 Lines)
- **Soft Maximum**: Verify that source files do not exceed 300 to 500 lines.
- **Proactive Modularization**: Inspect whether large composables and monolithic screens are split into dedicated, focused files:
  - Main screen / coordinator orchestration (`*GameScreen.kt`)
  - HUD and status components (`*HudComponents.kt`)
  - Dialogs, overlays, and modal alerts (`*Modals.kt`)
  - Control deck and button configurations (`*Controls.kt`)
- Ensure domain logic, coordinators, and renderers remain cleanly separated.

### Rule 3: Function Size Limits (Soft Limit: 100–150 Lines, Target: < 50 Lines)
- **Maximum Threshold**: Confirm that no function exceeds 100–150 lines.
- **Ideal Target**: Target under 50 lines per function.
- **Sub-Composable Extraction**: Confirm that complex Compose hierarchies are broken down into smaller, single-purpose composables (e.g., status pill, lives indicator, score column, button group) with clean state/parameter models.

---

## 4. Output Artifacts & Directory Structure

Audit reports must be saved under the `DOCS/audit/Y-m-d/` directory, where `Y-m-d` is the current date in ISO format (e.g., `DOCS/audit/2026-10-24/`):

```
DOCS/
└── audit/
    ├── rules.md
    ├── prompt.md
    └── Y-m-d/
        ├── results.md
        └── roadmap.md
```

> **Note on Roadmap Execution**: To advance the roadmap step-by-step in subsequent chats, use the ready-to-use prompt defined in [`DOCS/audit/prompt.md`](prompt.md).

### 4.1. Results Format (`DOCS/audit/Y-m-d/results.md`)
The audit results file must contain:
1. **Executive Summary**: High-level overview of codebase health and key findings.
2. **Scorecard Matrix**: A table listing each evaluated component with grades (0–10) across all 7 categories.
3. **Section-by-Section Analysis**:
   - Component / Module Name
   - Category Scores (0–10)
   - `AGENTS.md` Compliance Checklist:
     - [x] / [ ] Rule 1: Fluent / Chainable API & Boilerplate Encapsulation
     - [x] / [ ] Rule 2: File Size Limits (300–500 lines) & Modularization
     - [x] / [ ] Rule 3: Function Size Limits (< 50 lines target, max 100–150 lines)
   - Strengths
   - Areas for Improvement & Violations Found

### 4.2. Roadmap Format (`DOCS/audit/Y-m-d/roadmap.md`)
The roadmap file must contain an actionable, prioritized task list using markdown checkboxes (`- [ ]`), categorized by impact or subsystem:
- **Priority 1: Critical Fixes & AGENTS.md Violations**
- **Priority 2: Architecture & Modularity Improvements**
- **Priority 3: API Ergonomics & Reusability Enhancements**
- **Priority 4: Quality & Polish Items**

Each task should describe:
- The target file(s) or module.
- The specific refactoring or action needed.
- The rationale or violated rule being addressed.
