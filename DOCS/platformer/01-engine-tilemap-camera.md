# 01 — Engine Tilemap & Follow Camera Subsystems

> **Module Scope:** `:pixie-engine` (`com.pixie.engine.core.tilemap.*`, `com.pixie.engine.core.camera.*`)  
> **Target Release:** v0.7.0 (Step 1)  
> **Status:** COMPLETE  
> **Compliance:** Pure Kotlin JVM (zero Android dependencies), files < 500 lines, functions < 50 lines.

---

## 📋 Objectives

Establish the 2D tilemap data structures, grid rendering layer, AABB-to-grid collision queries, and horizontal tracking follow camera required for NES-style platforming.

---

## 🗺️ Tasks & Checklist

- [x] **Task 1.1: Define `TileType` and `TileData` in `:pixie-engine`**
  - **Target File:** `pixie-engine/src/main/kotlin/com/pixie/engine/core/tilemap/TileType.kt`
  - **Action:**
    - Define enum `TileType` with properties:
      - `EMPTY` (walk-through)
      - `GROUND` (solid rock/earth)
      - `QUESTION_BLOCK` (bumpable from below, spawns coin/powerup, turns into `USED_BLOCK`)
      - `BRICK` (bumpable from below, breakable by large hero)
      - `USED_BLOCK` (solid inert brown block)
      - `PIPE_TOP_LEFT`, `PIPE_TOP_RIGHT`, `PIPE_BODY_LEFT`, `PIPE_BODY_RIGHT` (solid green warp pipe segments)
      - `ONE_WAY_LEDGE` (passable from below, landable from above)
      - `SPIKE_HAZARD` (damages player on contact)
      - `FLAG_POLE`, `FLAG_TOP`, `CASTLE_BRICK` (level exit markers)
    - Add collision flag helpers: `isSolid`, `isBumpable`, `isDestructible`, `isOneWay`, `isHazard`.
    - Define lightweight data class `TileData(val type: TileType, var bumpOffsetY: Float = 0f, var bumpTimer: Float = 0f)`.

- [x] **Task 1.2: Implement `TilemapChunkLayer` in `:pixie-engine`**
  - **Target File:** `pixie-engine/src/main/kotlin/com/pixie/engine/core/tilemap/TilemapChunkLayer.kt`
  - **Action:**
    - Create `TilemapChunkLayer(val tileSize: Float = 16f, zIndex: Int = 200)` inheriting from `PixieLayer`.
    - Support loading level grids as `Array<IntArray>` or ASCII strings (e.g. `'#'` = Ground, `'?'` = Question, `'B'` = Brick, `'P'` = Pipe).
    - Implement zero-allocation viewport culling: only iterate through tile column/row indices visible inside `camera.viewportRect`.
    - Render pixel sprites with 3D beveled retro shading or retro color fills via `CanvasPort`.

- [x] **Task 1.3: Implement Fast AABB Grid Collision Query**
  - **Target File:** `pixie-engine/src/main/kotlin/com/pixie/engine/core/tilemap/TilemapCollisionQuery.kt`
  - **Action:**
    - Create utility converting entity bounding box `(x, y, width, height)` to tile grid coordinates:
      - `minCol = (left / tileSize).toInt()`, `maxCol = (right / tileSize).toInt()`
      - `minRow = (top / tileSize).toInt()`, `maxRow = (bottom / tileSize).toInt()`
    - Return list of colliding tile cells with contact sides: `CEILING` (player head bonk), `GROUND` (player feet landing), `LEFT_WALL`, `RIGHT_WALL`.
    - Support one-way platform resolution (ignore collision if player velocity is upward or feet were previously below the ledge).

- [x] **Task 1.4: Implement `PlatformerFollowViewMode` Camera**
  - **Target File:** `pixie-engine/src/main/kotlin/com/pixie/engine/core/camera/PlatformerFollowViewMode.kt`
  - **Action:**
    - Implement `GameViewMode`:
      - `targetPosition: () -> Vec2` tracking the player.
      - Configurable horizontal deadzone (`deadzoneWidth = 48f`): camera remains static while player moves inside the deadzone.
      - Lookahead lead: smoothly biases camera center in the player's facing direction (`leadOffset = 32f`).
      - Stage boundaries: clamp `camera.x` between `0f` and `stageWidth - viewportWidth`.
      - Vertical smoothing with fixed baseline horizon lock to prevent nauseating jump jitter.

---

## 🔍 Verification

1. Run `PixieEngineTest.kt` to ensure zero compilation or regression errors.
2. Add unit tests in `pixie-engine/src/test/kotlin/com/pixie/engine/tilemap/TilemapCollisionTest.kt` verifying:
   - Entity landing detects `GROUND` collision and clamps position.
   - Upward jumping into a tile detects `CEILING` head bonk.
   - Entity walking through `ONE_WAY_LEDGE` from below is not blocked.
   - Camera follows target with deadzone smoothing.
