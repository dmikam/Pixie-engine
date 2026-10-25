
Please advance the project by completing exactly one pending task from the audit roadmap.

Follow these execution steps:
1. Locate the latest audit folder under `DOCS/audit/` (or the one matching the most recent date `Y-m-d`).
2. Read `DOCS/audit/rules.md`, `AGENTS.md`, and the active `DOCS/audit/Y-m-d/roadmap.md` (and reference `results.md` for context on the issue).
3. Identify the FIRST unchecked task (`- [ ]`) in `roadmap.md`.
4. Implement the required refactoring, fix, or improvement for that single task, adhering strictly to the guidelines in `AGENTS.md`:
   - Fluent / chainable builders and low-level boilerplate encapsulation.
   - File size limits (keep files modular, targeted at 300–500 lines maximum).
   - Function size limits (target < 50 lines, maximum 100–150 lines, extract sub-composables).
5. Compile and verify the project to confirm that no compilation or runtime errors were introduced.
6. Update `DOCS/audit/Y-m-d/roadmap.md` by checking off that completed task (`- [x]`).
7. Provide a concise summary of:
   - The task that was executed and the problem it addressed.
   - The files modified or created.
   - The next pending task remaining on the roadmap.

