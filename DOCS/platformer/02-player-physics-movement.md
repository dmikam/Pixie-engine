# 02 — Player Physics & NES Momentum Movement

> **Module Scope:** `:pixie-engine` (`com.pixie.engine.core.platformer.*`)  
> **Target Release:** v0.7.0 (Step 2)  
> **Status:** COMPLETE  
> **Compliance:** Pure Kotlin JVM, files < 500 lines, functions < 50 lines.

---

## 📋 Objectives

Implement the physics controller delivering authentic Super Mario Bros NES movement feel: smooth momentum acceleration, skidding when reversing direction at high speeds, responsive variable jump heights, coyote time, and robust tilemap collision resolution.

---

## 🗺️ Tasks & Checklist

- [x] **Task 2.1: Define `PlatformerPhysicsConfig` & Player States**
  - **Target File:** `pixie-engine/src/main/kotlin/com/pixie/engine/core/platformer/PlatformerPhysicsConfig.kt`
  - **Action:**
    - Define parameter data class `PlatformerPhysicsConfig`:
      - `walkAcceleration: Float = 420f`
      - `runAcceleration: Float = 750f`
      - `maxWalkSpeed: Float = 110f`
      - `maxRunSpeed: Float = 185f`
      - `groundFriction: Float = 550f`
      - `skidDeceleration: Float = 950f`
      - `gravity: Float = 920f`
      - `jumpImpulse: Float = -270f`
      - `jumpCutGravityMultiplier: Float = 2.4f` (rapid drop when releasing button early)
      - `coyoteTimeDuration: Float = 0.12f` (~7 frames)
      - `jumpBufferDuration: Float = 0.10f` (~6 frames)
    - Define enum `PlayerAnimationState`: `IDLE`, `WALK`, `RUN`, `SKID`, `JUMP_ASCENT`, `FALL`, `DUCK`, `CARRY`, `THROW`, `HURT`, `VICTORY_FLAG`.

- [x] **Task 2.2: Implement Momentum & Skidding Physics**
  - **Target File:** `pixie-engine/src/main/kotlin/com/pixie/engine/core/platformer/PlatformerPhysicsController.kt`
  - **Action:**
    - Process horizontal input `moveX: Float (-1f..1f)`:
      - Accelerate toward target direction up to max speed (`maxWalkSpeed` or `maxRunSpeed` when Dash/Run is held).
      - If moving fast in one direction and input opposes motion, enter `SKID` state with elevated deceleration (`skidDeceleration`) until velocity reaches zero.
      - Apply `groundFriction` when no input is provided to decelerate smoothly to a stop.

- [x] **Task 2.3: Implement Variable Jump Cut, Coyote Time & Jump Buffering**
  - **Target File:** `pixie-engine/src/main/kotlin/com/pixie/engine/core/platformer/PlatformerPhysicsController.kt`
  - **Action:**
    - **Coyote Time:** Track `timeSinceGrounded`. Allow jump activation if `timeSinceGrounded <= coyoteTimeDuration` even if player just slipped off a ledge.
    - **Jump Buffering:** When jump button is pressed while airborne, store `jumpBufferedTimer = jumpBufferDuration`. If player touches ground while timer > 0, trigger jump immediately.
    - **Variable Jump Cut:** If jump button is released while `velocityY < 0` (moving upward), multiply gravity by `jumpCutGravityMultiplier` to gracefully truncate the jump apex.

- [x] **Task 2.4: Integrate 2-Axis Sub-Stepped Tilemap Collision Resolution**
  - **Target File:** `pixie-engine/src/main/kotlin/com/pixie/engine/core/platformer/PlatformerCollisionResolver.kt`
  - **Action:**
    - Decouple X and Y collision queries:
      - Step 1: Update X position -> check wall collisions against `TilemapChunkLayer` -> clamp X to tile boundary and zero `velocityX` if blocked.
      - Step 2: Update Y position -> check floor/ceiling collisions.
        - If landing on floor: zero `velocityY`, set `isGrounded = true`, reset coyote timer.
        - If bonking ceiling: zero `velocityY`, trigger `onHeadBonk(tileX, tileY)` event on colliding tile!

---

## 🔍 Verification

1. Run unit tests in `pixie-engine/src/test/kotlin/com/pixie/engine/platformer/PlatformerPhysicsTest.kt` verifying:
   - Reversing direction while sprinting enters skid mode.
   - Tapping jump produces a lower jump apex than holding jump.
   - Slipped ledge allows jump within coyote window.
   - Hitting ceiling zeroes vertical velocity and triggers head bonk hook.
