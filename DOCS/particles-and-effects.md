# Particles & Visual Effects

Pixie Engine features a procedural particle engine and dynamic color recovery pipeline designed to deliver tactile visual feedback without static sprite sheets.

---

## ✨ Zero-Allocation Particle System (`ParticlePool`)

Particles (sparks, smoke, flames, debris) are managed by `ParticlePool`, recycling lightweight `Particle` instances with zero garbage collection overhead.

```kotlin
// Spawn custom sparks at impact point
engine.particleLayer.spawnSparks(
    x = impactX,
    y = impactY,
    count = 12,
    color = 0xFFFFCC00.toInt(),
    speed = 80f
)
```

Each particle features:
- Physics: Position, velocity, acceleration, and air drag.
- Life & Decay: Lifespan countdown with alpha fade.
- Color Interpolation: Smooth transition from `startColor` (e.g. bright yellow) to `endColor` (e.g. dark smoke red).

---

## 💥 Procedural Explosion Archetypes (`ProceduralExplosion`)

Instead of fixed sprite animations, explosions are synthesized procedurally via `ProceduralExplosion.create()`:

```kotlin
ProceduralExplosion.trigger(
    type = ExplosionType.AERIAL_EXPLOSION,
    x = enemy.position.x,
    y = enemy.position.y,
    particleLayer = engine.particleLayer,
    camera = engine.camera,
    audio = engine.audio
)
```

### Built-in Explosion Types

| Explosion Type | Visual Effect | Typical Use |
| :--- | :--- | :--- |
| **`SPARK_HIT`** | 4–6 fast directional sparks. | Bullet glancing off shield or armor. |
| **`PLASMA_BURST`** | Expanding neon cyan/violet ring. | Plasma projectile detonating. |
| **`AERIAL_EXPLOSION`** | Fireball core, radial shrapnel, and trailing smoke. | Destroyed drones, scouts, or fighters. |
| **`GROUND_BLAST`** | Heavy outward burst leaving a scorched crater. | Bombs, fuel silos, or ground installations. |
| **`SHOCKWAVE`** | Large expanding shockwave ring with screen shake. | Mega-bombs or screen wipes. |
| **`TITAN_CHAIN`** | Multi-point cascading sequential explosions. | Boss defeat sequences. |

---

## 📳 Screen Shake

Add physical weight to impacts and explosions by triggering camera shake:

```kotlin
// Subtle shudder on player hit
engine.camera.shake(intensity = 4f, durationSeconds = 0.15f)

// Massive earthquake on boss explosion
engine.camera.shake(intensity = 12f, durationSeconds = 0.6f)
```

---

## 🌈 Real-Time Color Recovery Pipeline (`ColorRecoveryPipeline`)

A signature mechanic of Pixie Engine is the ability to dynamically transition the world from monochrome grayscale to vibrant RGB color:

```kotlin
// Progress goes from 0.0f (100% grayscale) to 1.0f (full vibrant color)
engine.colorRecovery.addProgress(0.05f) // e.g. when collecting a color spark

// Set target saturation smoothly
engine.colorRecovery.targetSaturation = 1.0f
```

The `AndroidCanvasRenderer` uses `AndroidColorMatrixAdapter` to apply saturation grading across all rendered layers in real time with hardware acceleration.
