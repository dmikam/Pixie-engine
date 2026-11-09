# 03 — Head-Bonk Block Interactions & Collectible Items

> **Module Scope:** `:pixie-engine` (`com.pixie.engine.core.platformer.*`, `com.pixie.engine.core.tilemap.*`)  
> **Target Release:** v0.7.0 (Step 3)  
> **Status:** COMPLETE (4/4 Tasks Complete)  
> **Compliance:** Pure Kotlin JVM, files < 500 lines, functions < 50 lines.

---

## 📋 Objectives

Implement the signature Super Mario Bros block-bumping mechanics: head-bonking blocks from underneath to trigger a physical spring recoil, popping coins and power-up mushrooms out of question blocks, and shattering bricks into 4 spinning corner fragments.

---

## 🗺️ Tasks & Checklist

- [x] **Task 3.1: Implement Head-Bonk Spring Recoil Animation**
  - **Target File:** `pixie-engine/src/main/kotlin/com/pixie/engine/core/tilemap/TilemapChunkLayer.kt`
  - **Action:**
    - In `TileData`, maintain `bumpOffsetY: Float` and `bumpVelocityY: Float`.
    - When `triggerBlockBonk(col, row)` is invoked:
      - Set `bumpVelocityY = -90f` to launch tile upward.
      - In layer update tick, apply spring-damper or gravity return until `bumpOffsetY == 0f`.
      - Offset the rendered sprite by `bumpOffsetY` (recoils 5–6 pixels upward and snaps back in ~0.15s).
      - Any entity standing directly on top of the bonked tile receives an upward knock impulse (`velocityY = -120f`).

- [x] **Task 3.2: Implement Question Mark `?` Block & Coin Pop**
  - **Target File:** `pixie-engine/src/main/kotlin/com/pixie/engine/core/platformer/InteractiveBlockController.kt`
  - **Action:**
    - When a `QUESTION_BLOCK` is hit from underneath:
      - Trigger tile recoil animation.
      - Permanently mutate tile type to `USED_BLOCK`.
      - If containing coin: spawn animated popping coin that arches upward 24 pixels, spins, awards 200 pts and 1 coin, and despawns.
      - If containing mushroom: spawn `SuperMushroom` rising smoothly out of the top of the block.

- [x] **Task 3.3: Implement Breakable Brick Shattering & Debris Particles**
  - **Target File:** `pixie-engine/src/main/kotlin/com/pixie/engine/core/platformer/InteractiveBlockController.kt`
  - **Action:**
    - When a `BRICK` is hit:
      - If player is `SMALL`: recoil tile with bounce sound without breaking.
      - If player is `SUPER` (powered-up) or struck by a thrown crate:
        - Erase brick tile to `EMPTY`.
        - Spawn 4 spinning brick shards into `ParticleLayer` launching diagonally outward (top-left, top-right, bottom-left, bottom-right) with gravity.
        - Trigger `ProceduralExplosion.trigger(ExplosionType.SPARK_HIT, ...)`.

- [x] **Task 3.4: Implement Super Mushroom Entity & Player Powerup Tier**
  - **Target File:** `pixie-engine/src/main/kotlin/com/pixie/engine/core/platformer/SuperMushroom.kt`
  - **Action:**
    - Spawns out of `QUESTION_BLOCK` moving vertically upward until cleared.
    - Moves horizontally along ground (`speed = 45f`), falls off ledges with gravity, and reverses direction upon striking solid tiles.
    - When collected by player: transforms player from `SMALL` (16x16) to `SUPER` (16x24), awards 1000 pts, and triggers powerup fanfare.

---

## 🔍 Verification

1. Run unit tests in `pixie-engine/src/test/kotlin/com/pixie/engine/platformer/InteractiveBlockTest.kt` verifying:
   - Hitting `QUESTION_BLOCK` converts it to `USED_BLOCK` and spawns pop coin.
   - Hitting `BRICK` as `SMALL` recoils without destroying tile.
   - Hitting `BRICK` as `SUPER` destroys tile and emits 4 debris particles.
   - Super Mushroom emerges, crawls horizontally, and upgrades player size.
