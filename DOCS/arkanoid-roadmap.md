# Arkanoid: Neon Matrix — Fixed-Screen Brick Breaker Roadmap

> **Target Version:** Pixie Engine v0.6.0  
> **Status:** IMPLEMENTING / VALIDATING  
> **Genre:** Fixed-Screen Precision Arcade Action / Brick Breaker  
> **Engine Subsystems Validated:** `FixedScreenViewMode`, Continuous Collision Detection (CCD) Sub-stepping, 2D Destructible Matrix Layer (`DestructibleMatrixLayer`), Dynamic Impact Angle Modulation, Capsule Power-up Dropper, Synthesized Sound Effects.

---

## 🎯 1. Vision & Architectural Philosophy

Arkanoid is the third Proof of Concept (PoC) for the **Pixie Engine**, shifting focus from continuous camera scrolling (Dinorun horizontal runner, Raptor vertical shmup) to a **bounded, fixed-arena precision arcade system**.

### Architectural Pillars:
1. **FixedScreenViewMode & Strict Arena Coordinate Clamping:**
   - The playfield is completely enclosed by static reflective borders (Left, Top, Right) and a lethal bottom hazard plane.
   - Guarantees letterbox-safe rendering on any device aspect ratio with pixel-perfect coordinate scaling.
2. **Sub-Step Continuous Collision Detection (CCD):**
   - High-velocity projectiles moving 300+ pixels/sec can easily tunnel through 6-8px thin bricks or paddle edges in standard discrete time-step physics.
   - The engine implements continuous ray-swept circle-to-box and sub-stepped trajectory integration, guaranteeing zero tunneling even at maximum ball velocity escalation.
3. **2D Destructible Matrix Layer (`DestructibleMatrixLayer`):**
   - Instead of instantiating 100 individual game entities for bricks, the playfield uses an O(1) indexed grid matrix.
   - Enables instant collision index determination `(x, y) -> (row, col)`, atomic health decrements, explosive chain reactions, and single-pass batch rendering with beveled 3D retro arcade styling.
4. **Dynamic Paddle Impact Angle Modulation:**
   - Rebound trajectory is not merely a mirror reflection. The outgoing angle is dynamically modulated by the distance from the paddle's center of mass:
     - Center hit: Near-vertical bounce (~85°-90°).
     - Edge hit: Steep aggressive angle (~25°-40°).
     - Paddle velocity adds horizontal momentum bias.
5. **Capsule Power-Up System:**
   - Dislodging specific bricks releases falling power capsules:
     - **[L] Laser Paddle:** Grants twin laser cannons to shoot bricks directly.
     - **[W] Wide Paddle:** Expands paddle width by 50% for easier defense.
     - **[M] Multi-Ball:** Splits the energy sphere into 3 active balls.
     - **[P] Plasma / Fireball:** Energy sphere pierces through bricks without bouncing.
     - **[S] Slow Ball:** Resets ball velocity escalation to base speed.
     - **[B] Safety Barrier:** Generates a 1-hit energy forcefield at the bottom boundary.

---

## ⚙️ 2. Detailed Technical Specifications

### 2.1 Continuous Collision Detection (CCD) (`com.pixie.engine.core.physics`)
- **Swept Circle Against AABB:**
  Given a sphere at $P_0$ moving to $P_1 = P_0 + V \cdot dt$ with radius $r$:
  - Expands the target rectangle by $r$ in all 4 directions (Minkowski sum).
  - Raycasts from $P_0$ to $P_1$ against the expanded box.
  - Determines precise entry time $t \in [0, 1]$ and collision normal vector $\vec{N} \in \{(-1, 0), (1, 0), (0, -1), (0, 1)\}$.
  - Reflects velocity vector: $\vec{V}' = \vec{V} - 2(\vec{V} \cdot \vec{N})\vec{N}$.

### 2.2 Destructible Matrix Layer (`com.pixie.engine.core.matrix`)
```kotlin
data class BrickCell(
    val row: Int,
    val col: Int,
    var type: BrickType,
    var currentHp: Int,
    val maxHp: Int,
    val baseColor: Int,
    val scoreValue: Int,
    val powerUpDrop: PowerUpType? = null
)
```
- **Brick Archetypes:**
  - `STANDARD`: 1-hit colorful bricks (Red, Yellow, Green, Cyan, Magenta, Amber).
  - `ARMORED_SILVER`: Requires 2-3 hits; emits metallic clink sparks when struck.
  - `EXPLOSIVE_TNT`: Detonates on impact, dealing radial damage to adjacent 8 neighbors.
  - `UNBREAKABLE_GOLD`: Indestructible obstacle bricks forcing tactical bank shots.
  - `BONUS_CAPSULE`: Guarantees a power-up drop when destroyed.

### 2.3 Audio Integration
- Leverages `RetroAudioSynthesizer` for arcade sounds:
  - Paddle boing reflection (`PADDLE_BOUNCE`)
  - Crisp brick break (`BRICK_BREAK`)
  - Armored brick deflection (`SHIELD_HIT`)
  - Power capsule chime (`COLLECT` / `POWERUP_PICKUP`)
  - Laser cannon fire (`FIRE_PRIMARY`)
  - Ball loss drain (`HIT` / `DUCK`)
  - Victory stage clear fanfare (`SCORE_MILESTONE`)

---

## 📋 3. Phased Implementation Roadmap

- [x] Create `DOCS/arkanoid-roadmap.md`.
- [x] Implement `ContinuousCollision` (CCD circle/AABB ray-sweep) in `:pixie-engine`.
- [x] Implement `DestructibleMatrixLayer` in `:pixie-engine`.
- [x] Add unit tests in `PixieEngineTest` verifying zero-tunneling and grid damage.
- [x] Implement Arkanoid Entities in `:app`:
  - `ArkanoidPaddle` (steering, width modulation, laser cannons)
  - `EnergyBall` (CCD physics, trail particles, angle modulation)
  - `PowerCapsule` & `ArkanoidLaser`
- [x] Implement `ArkanoidLevelCatalog` (3 unique challenging stages).
- [x] Implement `ArkanoidGameCoordinator` (ball launch docking, lives, combos, stage progression).
- [x] Build `ArkanoidGameScreen` with M3 HUD and touch steering.
- [x] Integrate into `MainActivity` and `GameSelectorScreen`.
- [x] Update `DOCS/roadmap.md` and release history.
