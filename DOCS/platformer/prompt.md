Please advance the Super Pixie NES Platformer implementation by completing exactly one pending task from the plan.

Follow these execution steps:
1. Read `AGENTS.md` and `DOCS/platformer/current_state.md`.
2. Inspect the plan files in `DOCS/platformer/` in chronological order (`01-engine-tilemap-camera.md`, `02-player-physics-movement.md`, etc.).
3. Identify the FIRST plan file that contains an unchecked task (`- [ ]`), and select that first pending task.
4. Implement the required feature, refactoring, or improvement for that single task, strictly adhering to the guidelines in `AGENTS.md`:
   - Pure Kotlin in `:pixie-engine` (zero Android SDK dependencies).
   - Fluent / chainable builders and low-level boilerplate encapsulation.
   - File size limits (keep files modular, targeted at 300–500 lines maximum).
   - Function size limits (target < 50 lines, maximum 100–150 lines, extract sub-composables).
   - Ensure all interactive elements include unique `Modifier.testTag` identifiers.
5. Compile and verify the project (`compile_applet` and `gradle :app:testDebugUnitTest`) to confirm no errors or regressions were introduced.
6. Check off the completed task (`- [x]`) in that plan file.
7. Update `DOCS/platformer/current_state.md` to reflect the latest completed task, current file progress, and the next pending task.
8. Provide a concise summary of:
   - The task executed and problem addressed.
   - The files created or modified.
   - The next pending task remaining in the queue.
