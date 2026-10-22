# Weapons & Projectiles System

Pixie Engine features a high-throughput, self-encapsulated weapon and projectile engine designed to handle hundreds of active bullets at 60 FPS with **zero runtime GC allocations**.

---

## 🔫 Self-Encapsulated Weapon Definition (`WeaponDefinition`)

Rather than scattering weapon logic across multiple classes, every weapon in Pixie Engine is a self-contained data contract:

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

Everything needed to spawn, move, render, detonate, and play audio for a bullet is defined in one place.

---

## 🎯 Trajectory Math Types

Bullets can follow varied trajectory paths defined by `TrajectoryType`:

| Trajectory | Description | Example Weapon |
| :--- | :--- | :--- |
| **`LINEAR_FORWARD`** | Direct straight line in the firing direction. | Vulcan Autocannon, Sniper Laser |
| **`ANGLED_SPREAD`** | Multi-bullet fan with angular offsets. | Shotgun, Plasma Spreader |
| **`SINE_WAVE`** | High-frequency oscillating sinusoidal wave. | Alien Wave Cannon, Ion Beam |
| **`HOMING_MISSILE`** | Smoothly steers toward nearest enemy target. | Micro-Missile Pod |
| **`ACCELERATING`** | Starts slow and ramps up velocity over time. | Heavy Rocket, Torpedo |

---

## ⚡ High-Throughput Projectile Pool (`ProjectilePool`)

Spawning bullets with `new Projectile()` causes severe GC stuttering in intense shoot 'em up games. Pixie Engine uses `ProjectilePool` to reuse bullets from a pre-allocated array.

```kotlin
// Spawning a bullet from the pool:
val bullet = engine.projectilePool.obtain()
bullet.init(
    weapon = StandardWeapons.PLASMA_CANNON,
    startX = player.position.x + 8f,
    startY = player.position.y - 4f,
    directionX = 0f,
    directionY = -1f, // Upward
    isPlayerOwned = true
)

// In update loop:
engine.projectilePool.update(dt, context)

// Projectiles are automatically recycled back into the pool upon:
// 1. Leaving the screen boundaries
// 2. Exceeding their lifespan
// 3. Reaching maximum hit pierce count
```

---

## 📦 Pre-Configured Standard Weapons

Pixie Engine provides ready-to-use weapons in `StandardWeapons`:

- **`StandardWeapons.VULCAN_CANNON`**: Rapid single-shot kinetic stream.
- **`StandardWeapons.TWIN_PLASMA`**: Dual parallel plasma bolts.
- **`StandardWeapons.SPREAD_SHOT`**: 3-way or 5-way spread pattern.
- **`StandardWeapons.HOMING_MISSILE`**: Target-seeking explosive projectiles.
- **`StandardWeapons.ENEMY_CRIMSON_LASER`**: Directed enemy pulse laser.

---

## 🛠️ Firing with `WeaponSystem`

Attach a `WeaponSystem` to your player or enemy craft to manage cooldowns and fire rates automatically:

```kotlin
class CombatShip : PixieObject() {
    private val weaponSystem = WeaponSystem(
        weapon = StandardWeapons.TWIN_PLASMA,
        projectilePool = engine.projectilePool
    )

    override fun update(dt: Float, context: GameContext) {
        weaponSystem.update(dt)

        // Fire when action button is held down
        if (context.input?.isActionPressed == true) {
            weaponSystem.tryFire(
                origin = position,
                direction = Vec2(0f, -1f),
                isPlayerOwned = true,
                context = context
            )
        }
    }
}
```
