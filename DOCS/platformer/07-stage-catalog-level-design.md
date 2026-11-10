# 07 — Stage Catalog & NES World 1-1 Level Design

> **Module Scope:** `:app` (`com.example.platformer.world.*`)  
> **Target Release:** v0.7.0 (Step 7)  
> **Status:** PENDING  
> **Compliance:** AGENTS.md compliance, files < 500 lines, functions < 50 lines.

---

## 📋 Objectives

Design a complete, handcrafted NES World 1-1 level combining classic Super Mario elements (introductory blocks, pipes of varying heights, brick pyramids, coin caches, flagpole) and Chip 'n Dale elements (stacks of wooden crates and crate puzzles).

---

## 🗺️ Tasks & Checklist

- [x] **Task 7.1: Implement ASCII Stage Parser & Level Matrix Data**
  - **Target File:** `app/src/main/java/com/example/platformer/world/PlatformerLevelParser.kt`
  - **Action:**
    - Parse multi-line string matrices into `TilemapChunkLayer` and entity spawn lists:
      - `'#'` = Solid Ground / Stone
      - `'?'` = Question Block (Coin)
      - `'M'` = Question Block (Super Mushroom)
      - `'B'` = Breakable Brick
      - `'['`, `']'` = Pipe Top Left / Right
      - `'|'`, `'/'` = Pipe Body Left / Right
      - `'C'` = Spawn `ThrowableCrate`
      - `'G'` = Spawn `GoombaEnemy`
      - `'K'` = Spawn `KoopaEnemy`
      - `'F'` = Flagpole segment (`'^'` = Flagpole top ball)
      - `'*'` = Goal Castle
    - Zero allocations during gameplay (parse once into static layout at level load).

- [ ] **Task 7.2: Design Handcrafted World 1-1 Stage**
  - **Target File:** `app/src/main/java/com/example/platformer/world/World11LevelDefinition.kt`
  - **Action:**
    - Level length: 210 tiles wide (~3,360 virtual pixels), 15 tiles high (240 virtual pixels).
    - **Zone 1: Training Run (Tiles 0–40):** Flat ground, introductory single Goomba, first `?` block with coin, three floating blocks (`?`, Brick, `?` with Mushroom), first short 2-tile pipe.
    - **Zone 2: Pipes & Pits (Tiles 41–90):** 3-tile and 4-tile pipes, patrolling Koopa turtle, first ground pit gap requiring timed jump, stack of 3 wooden crates to pick up and throw.
    - **Zone 3: Brick Pyramids & Stacks (Tiles 91–150):** Stepped brick structures, hidden 1-Up block, flying crate targets, multi-enemy gauntlet.
    - **Zone 4: Flagpole & Castle (Tiles 151–210):** Stepped 8-block ascending staircase, grand flagpole, and brick castle gate.

- [ ] **Task 7.3: Implement 3-Layer NES Sky & Parallax Backdrop**
  - **Target File:** `app/src/main/java/com/example/platformer/world/PlatformerParallaxBackdrop.kt`
  - **Action:**
    - Layer 1: Authentic NES sky blue background (`#5C94FC`).
    - Layer 2: Fluffy white pixel clouds scrolling with subtle parallax (`scrollFactor = 0.25f`).
    - Layer 3: Distant rolling green hills with rounded tops and pixel bushes (`scrollFactor = 0.50f`).

- [ ] **Task 7.4: Implement Flagpole Slide & Victory Sequence**
  - **Target File:** `app/src/main/java/com/example/platformer/world/PlatformerVictorySequence.kt`
  - **Action:**
    - When player touches flagpole:
      - Freeze stage timer and lock player input.
      - Calculate bonus score based on touch height (100 to 5000 pts).
      - Animate player sliding down the pole to the ground.
      - Play `AudioEvent.STAGE_CLEAR` fanfare.
      - Player dismounts and walks automatically rightward into the castle entrance, triggering victory modal.

---

## 🔍 Verification

1. Verify level matrix parses correctly into tile layer and entity coordinates.
2. Confirm level bounds are correctly clamped at X = 0 and X = stageWidth.
3. Test flagpole contact initiates slide animation and victory state transition.
