# Raptor: Call of the Shadows — Vertical Arcade Scroller Roadmap

> **Target Version:** Pixie Engine v0.5.0  
> **Status:** Active Design & Phase 1 Execution  
> **Genre:** Vertical Top-Down Shoot 'Em Up (Shmup) / Combat Flight Arcade  
> **Engine Subsystems Validated:** Vertical Camera, Continuous Difficulty System, Zero-Allocation Bullet & Particle Pools, Self-Encapsulated Weapons, Ground Destructibles, Multi-Level Progression.

---

## 🎯 1. Vision & Architectural Philosophy

Raptor is the second Proof of Concept (PoC) for the **Pixie Engine**, taking the engine from horizontal endless runner physics (Dinorun) into a fast-paced, high-density vertical combat arcade experience inspired by genre titans (*Raptor: Call of the Shadows*, *Raiden*, *1942*, *Xenon 2*).

### Architectural Pillars:
1. **Zero-Allocation High-Throughput (GC-Friendly):**
   Arcade shmups feature 100+ active projectiles, dozens of flying enemies, and hundreds of exploding sparks simultaneously. Running at 60 FPS on Android without stutter requires strict zero-allocation in the main loop via object pooling (`PixiePool<T>`).
2. **Procedural Particle & Explosion Subsystem:**
   Instead of static pre-rendered GIF/spritesheet explosions, explosions are procedurally generated from configurable particle emitters (fireball cores, expanding shockwave rings, trailing smoke, high-velocity sparks, and metallic debris).
3. **Self-Encapsulated Weapons Architecture:**
   Every weapon—whether wielded by the player ship, an enemy ace, or a ground turret—is a complete, self-contained definition (`WeaponDefinition`) encapsulating:
   - Bullet Pixel Sprite & palette
   - Motion Blur Trail configuration
   - Trajectory calculation function (Linear, Spread, Sine Wave, Homing/Tracking, Spiral)
   - Procedural Explosion type & sound event
   - Hit power / damage formulas
   - Piercing & lifespan rules
4. **Continuous Difficulty & Context-Driven Dynamic Property Calculation:**
   The engine provides a continuous integer difficulty scale from `0` to `MAX_DIFFICULTY = 100`. Games map difficulty presets (e.g. `EASY = 10`, `MEDIUM = 25`, `DIFFICULT = 50`, `HEAVY_METAL = 100`).
   All game elements calculate runtime parameters (movement speed, rate of fire, damage, enemy health, bullet patterns, pickup drop rates) dynamically from `GameContext` based on:
   - `difficulty` (0..100)
   - `currentLevel` (1..N)
   - `livesLeft` (player survival pressure / rubber-banding)
   - `currentMovingSpeed`
   - `activePerks` (e.g. `RAPID_FIRE`, `OVERCLOCK`, `PLASMA_CORE`, `TITANIUM_HULL`, `MAGNET_BEAM`)
   - Custom stage modifiers.
5. **Multi-Level Campaign Support:**
   Modular mission system with distinct terrain biomes, environmental parallax layers, scripted wave timelines, ground installations, and multi-phase bosses.

---

## ⚙️ 2. Engine Subsystem Specifications

### 2.1 Continuous Difficulty & Context System (`com.pixie.engine.core.context`)
- **Difficulty Range:** `0` (Minimal/Sandbox) to `100` (`MAX_DIFFICULTY`).
- **Presets:**
  - `EASY = 10`: Forgiving enemy health, slower bullets, generous drop rates.
  - `MEDIUM = 25`: Standard arcade challenge, balanced bullet density.
  - `DIFFICULT = 50`: Fast projectiles, aggressive enemy formations, tactical shield management.
  - `HEAVY_METAL = 100`: Relentless bullet hell, ultra-dense waves, intense boss rage phases.
- **Dynamic Property Calculation Formula:**
  ```kotlin
  val speed = ctx.calculateDynamicProperty(
      baseValue = 120f,
      difficultyWeight = 0.5f, // scales with difficulty (0..100)
      levelWeight = 0.15f,     // scales with level (1..N)
      perkBonus = if (ctx.hasPerk("OVERCLOCK")) 1.25f else 1.0f,
      speedInfluence = ctx.currentMovingSpeed * 0.05f
  )
  ```

### 2.2 GC-Friendly Object Pool (`com.pixie.engine.core.pool.PixiePool<T>`)
- Array-backed reusable object storage with zero garbage collection overhead.
- Features `obtain(): T`, `recycle(item: T)`, `recycleAll(items: List<T>)`, capacity expansion alerts, and auto-reset.
- Applied across:
  - `ProjectilePool` (Player and Enemy projectiles)
  - `ParticlePool` (Sparks, smoke, fire, plasma)
  - `GroundDebrisPool` (Scorched earth, rubble)

### 2.3 Particle System & Procedural Explosions (`com.pixie.engine.core.particle`)
- **Particle Model:** Position, velocity, acceleration/drag, lifespan, startColor, endColor, alpha fade, pixel size.
- **Explosion Archetypes:**
  - `SPARK_HIT`: Tiny directional sparks upon bullet impact against shields or armor.
  - `PLASMA_BURST`: Rapidly expanding cyan/neon-violet energy ring with exponential decay.
  - `AERIAL_EXPLOSION`: Circular fire core with radial shrapnel and dark smoke clouds for destroyed aircraft.
  - `GROUND_CRATER_EXPLOSION`: Heavy ground blast leaving scorched craters and rising smoke columns.
  - `CHAIN_REACTION_TITAN`: Cascading multi-point sequential explosions across large ships or buildings.

### 2.4 Self-Encapsulated Weapon Definition (`com.pixie.engine.core.weapon`)
```kotlin
data class WeaponDefinition(
    val id: String,
    val name: String,
    val bulletSprite: PixelSprite,
    val motionBlur: MotionBlurConfig,
    val trajectoryType: TrajectoryType,
    val explosionType: ExplosionType,
    val baseHitPower: Float,
    val fireRatePerSecond: Float,
    val bulletSpeed: Float,
    val pierceCount: Int = 1,
    val audioEvent: AudioEvent = AudioEvent.FIRE_PRIMARY,
    val hapticFeedback: HapticFeedbackType? = null
)
```
- **Trajectory Types:**
  - `LINEAR_FORWARD`: Direct vertical trajectory.
  - `ANGLED_SPREAD`: Multi-shot fan (e.g. 3-way, 5-way shotgun spread).
  - `SINE_WAVE`: High-frequency oscillating plasma bolts.
  - `HOMING_MISSILE`: Target-seeking missile with turn rate and acceleration.
  - `SPIRAL_VORTEX`: Swirling barrage used by bosses.

---

## 🚀 3. Raptor Game Architecture (`com.example.raptor`)

### 3.1 Player Ship — Raptor Interceptor
- **Handling:** Direct 2D touch drag or virtual floating thumbstick with responsive boundary clamping.
- **Arsenal Slots:**
  - **Primary:** Vulcan Autocannon -> Twin Plasma Spreader -> Quad Ion Gatling (upgradeable).
  - **Secondary:** Micro-Missile Pods (homing lock-on on nearby aerial and ground targets).
  - **Super Action:** EMP Mega-Bomb / Orbital Air Strike (clears all incoming enemy bullets and damages all screen targets).
- **Defense:**
  - Regenerative Plasma Shield with visual hit-ripple effects.
  - Hull Integrity (Health points).

### 3.2 Enemy Archetypes & Squadrons
1. **Drone Scout:** High-speed agile scouts flying in V-formations, firing quick single laser pulses.
2. **Assault Interceptor:** Sweeps down in sine waves, aligns with the player, and unleashes 3-way spreads.
3. **Heavy Gunship:** Armored mid-tier combat craft with twin plasma cannons and trailing engine fire.
4. **Stealth Bomber:** Drops slow cluster bombs that detonate into radial shrapnel.
5. **Ground Turrets & SAM Sites:** Mounted on buildings or islands, rotating to track the player.

### 3.3 Ground Targets & Destructibles
- Fully interactive terrain elements rendered on the ground gamefield layer:
  - **Gun Turrets & Flak Cannons:** Fire flak bursts at player elevation.
  - **Fuel Silos & Ammo Bunkers:** Explode with massive area-of-effect damage, destroying adjacent enemies.
  - **Radar Stations:** Destroying them disables local turret tracking.
  - **Supply Crates / Cargo Pods:** Drop weapon upgrade tokens, shield cells, and score gems.

### 3.4 Pickable Items & Power-ups
- **Weapon Powerup (P):** Advances primary weapon level (Vulcan -> Dual -> Tri-Plasma -> Ion Gatling).
- **Missile Pod (M):** Adds secondary homing missile salvoes.
- **Shield Cell (S):** Instantly restores 50% shield energy.
- **Repair Nanites (R):** Restores ship hull integrity.
- **Mega-Bomb (B):** Adds an orbital air strike charge.
- **Credit Pods / Medals:** Bonus score multipliers.
- **Perk Badges:** Unlocks passive perks in `GameContext.activePerks` (e.g. `RAPID_FIRE`, `MAGNET_BEAM`).

### 3.5 Multi-Level Campaign Structure
- **Level 1: Archipelago Infiltration**
  - Biome: Deep blue ocean, sandy atolls, offshore oil rigs, naval gunboats.
  - Boss: *Sea Titan Super-Cruiser* (Destructible deck turrets, missile silo, bridge core).
- **Level 2: Industrial Canyon & Refinery**
  - Biome: Red rock canyons, metal bridges, smokestacks, automated rail defense.
  - Boss: *Colossus Siege Tank* (Treads, main railgun cannon, defense shield generators).
- **Level 3: Orbital Command Citadel**
  - Biome: Low planetary orbit, solar array fields, high-tech fortress towers.
  - Boss: *Apex Mothership Dreadnought* (Multi-phase transformation, bullet hell spiral arrays, core meltdown).

---

## 📋 4. Roadmap Phasing & Detailed Todos List

```
[Phase 1] ───► [Phase 2] ───► [Phase 3] ───► [Phase 4] ───► [Phase 5] ───► [Phase 6] ───► [Phase 7]
Engine Core     Weapons &     Vertical      Player Ship   Enemy Waves   Ground &       Campaign &
& Difficulty    Projectiles   Scroller      & Controls    & Combat AI   Destructibles  Boss Battles
```

### 🟩 Phase 1: Engine Core Foundation (Difficulty, Pool & Particles) — *[COMPLETED]*
- [x] Create comprehensive roadmap in `DOCS/raptor-roadmap.md`.
- [x] Implement `DifficultyPreset` and enhance `GameContext`:
  - Support continuous integer difficulty `0..MAX_DIFFICULTY` (100).
  - Add standard presets: `EASY = 10`, `MEDIUM = 25`, `DIFFICULT = 50`, `HEAVY_METAL = 100`.
  - Add `activePerks`, `currentMovingSpeed`, and property calculation helper `calculateDynamicProperty(...)`.
  - Maintain backward compatibility with Dinorun (`difficultyLevel` float mapping).
- [x] Implement GC-friendly zero-allocation `PixiePool<T>` in `:pixie-engine`.
- [x] Implement Particle System in `:pixie-engine`:
  - `Particle` model with lifecycle, color interpolation, size, velocity, and drag.
  - `ParticlePool` with zero-allocation recycling.
  - `ProceduralExplosion` factory for `SPARK_HIT`, `PLASMA_BURST`, `AERIAL_EXPLOSION`, `GROUND_BLAST`, `SHOCKWAVE`, `TITAN_CHAIN`.
  - `ParticleLayer` integration in `LayerStack`.
- [x] Add unit tests in `pixie-engine` verifying difficulty scaling, pooling, and particle physics.

### 🟩 Phase 2: Encapsulated Weapon & Projectile Engine — *[COMPLETED]*
- [x] Create `WeaponDefinition` data contract (bullet sprite, motion blur, trajectory, explosion, hit power).
- [x] Implement `Projectile` entity with `ProjectilePool` management.
- [x] Implement trajectory math calculators:
  - `LinearTrajectory`
  - `SpreadTrajectory`
  - `SineWaveTrajectory`
  - `HomingTrajectory` (with angular velocity clamp).
  - `Accelerating` trajectory.
- [x] Implement `WeaponSystem` managing cooldowns, multi-shot patterns, and sound/haptics.
- [x] Add unit tests for trajectory accuracy, multi-shot patterns, and bullet lifecycle recycling.

### 🟩 Phase 3: Vertical Camera & Terrain Layering — *[COMPLETED]*
- [x] Configure `GameViewMode.VerticalScroller` for smooth vertical camera tracking.
- [x] Build multi-layer campaign surface biome scrolling:
  - Deep biome background color (deep oceanic navy, canyon basalt, space void).
  - Cloud and atmospheric mist layers.
  - Interactive ground layer for destructible buildings and turrets.

### 🟩 Phase 4: Raptor Player Ship & Weapon Arsenal — *[COMPLETED]*
- [x] Create pixel-art sprite sets for Raptor Interceptor (neutral, banking left, banking right, thruster plume).
- [x] Implement touch drag input steering with screen boundary clamping.
- [x] Implement primary weapon firing system (single vulcan, twin cannons, plasma spreader).
- [x] Implement secondary homing missile launcher.
- [x] Implement plasma shield with dynamic damage absorption and recharge delay.
- [x] Implement mega-bomb screen wipe with shockwave and particle clear.

### 🟩 Phase 5: Enemy Squadron Spawner & AI Formations — *[COMPLETED]*
- [x] Build timeline wave spawner supporting scripted and procedural encounters.
- [x] Implement Enemy AI flight formations (Scouts, Interceptors, Heavy Gunships, Stealth Bombers).
- [x] Implement enemy weapons (crimson lasers, plasma fans) utilizing `WeaponDefinition`.
- [x] High-frequency collision resolution between player bullets and enemy craft.

### 🟩 Phase 6: Ground Targets, Destructibles & Pickups — *[COMPLETED]*
- [x] Implement Ground Target entities (Gun Turrets, Fuel Silos, Radar Towers, Supply Bunkers).
- [x] Implement chained destructible explosion reactions (fuel silo blast damages surrounding enemies).
- [x] Implement item drop tables and collectible pickups (Weapon Upgrades, Missile Salvoes, Shield Cells, Repair Kits, Mega-Bombs, Score Gems).
- [x] Implement active perks logic (`OVERCLOCK`, `RAPID_FIRE`, `MAGNET_BEAM`, `TITANIUM_HULL`).

### 🟩 Phase 7: Multi-Level Campaign, Boss Battles & Launcher Integration — *[COMPLETED]*
- [x] Implement 3 campaign levels in `RaptorLevelCatalog`:
  - Level 1: "Archipelago Infiltration" + Boss: *Sea Titan Cruiser*.
  - Level 2: "Sulfur Canyon & Refinery".
  - Level 3: "Orbital Command Citadel".
- [x] Implement multi-segment boss battle with Left Cannon, Right Cannon, Core Reactor, and Phase 2 Rage Mode.
- [x] Build Raptor HUD: Hull health bar, Shield gauge, Weapon tier indicator, Bomb count, Missile count, Score, Active perks chips.
- [x] Add Raptor Game Screen into Launcher (`GameSelectorScreen`), allowing players to select difficulty (Easy, Medium, Difficult, Heavy Metal) and launch Raptor directly!

### 🟩 Phase 8: Sound Synthesis, Speed Calibration & Ground Destructibles Overhaul — *[COMPLETED]*
- [x] **Procedural Retro Sound Synthesizer (`RetroAudioSynthesizer.kt` & `AndroidSoundPoolAudioAdapter.kt`):**
  - Generated offline 16-bit 22050Hz PCM WAV audio files for all 14 `AudioEvent` cues (laser chirps, missile launches, small/large explosions, shield deflections, ascending arpeggios, UI clicks, jump/duck slides).
  - Loaded into native `SoundPool` for zero-lag hardware-accelerated concurrent sound playback with pitch variation.
  - Connected sound events across both DinoRun and Raptor (Jump, Duck, Fire Primary, Missiles, Explosions, Collect, Milestones, Game Over, UI clicks).
- [x] **Pacing & Speed Calibration:**
  - Diagnosed and resolved the 3x-4x speed bug caused by redundant update loops between `PixieGameCanvas` vsync driver and the coordinator.
  - Rebalanced aircraft flight speeds (Scout 52f, Interceptor 44f, Gunship 28f, Bomber 22f) and wave trajectories (smooth broad sine arcs).
  - Rebalanced bullet velocities (Vulcan 230f, Plasma 200f, Missiles 160f, Enemy lasers 100f, Enemy plasma 85f) for authentic tactical dodging.
  - Synchronized terrain and ground drift speeds to 36 virtual px/s.
- [x] **Ground Destructibles & Surface Targets Expansion:**
  - Expanded ground catalog: Coastal Gun Batteries, Volatile Fuel Silos, Radar Installations, SAM Missile Silos, Reinforced Supply Bunkers, Naval Patrol Gunboats, and Crude Oil Tanks.
  - Added visual hit-flashing (monochrome flash on impact), mini health meters, and scorched rubble craters remaining on the scrolling terrain.
  - Tuned ground spawn frequency (every 1.5 - 2.5s) with multi-target AoE chain explosion reactions.
- [x] **Letterbox-Safe Relative Touch Controls:**
  - Aspect-ratio-corrected coordinate transformations eliminating border sticking and enabling 1-to-1 finger steering across all device screen aspect ratios.

---

## 🎯 5. What We Should Implement First

To build this systematically without technical debt or rework, the execution sequence is:

1. **First Implementation Step (Now):**  
   - Update `GameContext` and add `DifficultyPreset` with continuous `0..MAX_DIFFICULTY` integer support, `activePerks`, `currentMovingSpeed`, and dynamic scaling calculation methods.
   - Implement `PixiePool<T>` (zero-allocation object pool).
   - Implement `Particle`, `ParticlePool`, `ProceduralExplosion`, and `ParticleLayer` in `pixie-engine`.
   - Validate with unit tests in `PixieEngineTest`.
2. **Next Step:**  
   - Implement the self-encapsulated `WeaponDefinition`, `Projectile`, and trajectory systems in `pixie-engine`.
3. **Third Step:**  
   - Build the Raptor player ship, vertical terrain, enemy waves, ground targets, and UI screen in `:app`.
