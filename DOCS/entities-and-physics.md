# Entities & Physics System

All dynamic elements in Pixie Engine—players, enemies, hazards, and pickups—extend the base `PixieObject` class.

---

## 🧬 Entity Lifecycle (`PixieObject`)

Every entity implements a standardized lifecycle managed by `PixieEngine`:

```
                 ┌───────────────┐
                 │    Instantiate│
                 └───────┬───────┘
                         │
                         ▼
                 ┌───────────────┐
                 │   onSpawn()   │
                 └───────┬───────┘
                         │
        ┌────────────────┼────────────────┐
        │                ▼                │
        │         ┌─────────────┐         │
        │         │ update(dt)  │         │
        │         └──────┬──────┘         │ Fixed 60 FPS Loop
        │                ▼                │
        │         ┌─────────────┐         │
        │         │  render()   │         │
        │         └──────┬──────┘         │
        │                ▼                │
        │     ┌───────────────────────┐   │
        │     │ onCollision(other,...)│   │ (If solid & intersects)
        │     └───────────────────────┘   │
        └────────────────┬────────────────┘
                         │ isAlive = false
                         ▼
                 ┌───────────────┐
                 │   destroy()   │
                 └───────────────┘
```

### Essential Properties of `PixieObject`

```kotlin
open class PixieObject(
    var position: Vec2 = Vec2.ZERO,
    var velocity: Vec2 = Vec2.ZERO,
    var width: Float = 16f,
    var height: Float = 16f,
    var isSolid: Boolean = true, // Solid objects participate in AABB collision checks
    var isAlive: Boolean = true  // Set false to remove or recycle
) {
    val boundingBox: AABB
        get() = AABB(position.x, position.y, position.x + width, position.y + height)

    open fun onSpawn(context: GameContext) {}
    open fun update(dt: Float, context: GameContext) {}
    open fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {}
    open fun onCollision(other: PixieObject, context: GameContext) {}
    open fun destroy(context: GameContext) {}
}
```

---

## 📦 Base Entity Classes

Pixie Engine provides convenient archetypes with pre-configured behaviors:

### 1. `PixiePlayer`
Manages player movement, input polling from `engine.input`, jump/duck states, invulnerability frames, and health.
```kotlin
class HeroPlayer : PixiePlayer() {
    override fun update(dt: Float, context: GameContext) {
        val input = context.input ?: return
        
        // Horizontal movement
        velocity.x = input.moveX * 120f
        
        // Jump when grounded
        if (input.isJumpPressed && isGrounded) {
            velocity.y = -220f
            isGrounded = false
            context.audio?.playSound(AudioEvent.JUMP)
        }
        
        super.update(dt, context)
    }
}
```

### 2. `PixieEnemy`
Base class for airborne or ground-based adversaries. Supports flight patterns, health pools, and weapon firing cycles.

### 3. `PixieObstacle`
Static or moving environmental hazards (e.g. Cacti, Pterodactyls, Spikes). Typically inflicts damage on the player upon collision.

### 4. `PixieCollectible`
Pickups such as coins, gems, repair kits, or power-up tokens.
```kotlin
class GemPickup(x: Float, y: Float) : PixieCollectible(Vec2(x, y)) {
    override fun onCollected(player: PixiePlayer, context: GameContext) {
        context.score += 500
        context.audio?.playSound(AudioEvent.COLLECT)
        context.haptics?.trigger(HapticFeedbackType.LIGHT)
        isAlive = false // Remove from game
    }
}
```

---

## 💥 Collision Detection (AABB)

Pixie Engine runs broad-phase and narrow-phase **Axis-Aligned Bounding Box (AABB)** collision checks every fixed update for all active entities in `engine.itemsLayer`:

```kotlin
// Two boxes intersect if their min/max extents overlap on both X and Y:
fun intersects(other: AABB): Boolean {
    return this.minX < other.maxX && this.maxX > other.minX &&
           this.minY < other.maxY && this.maxY > other.minY
}
```

When a collision is detected:
1. `a.onCollision(b, context)` is invoked.
2. `b.onCollision(a, context)` is invoked.

### Adding and Managing Entities

```kotlin
// Add entities to active game items layer
engine.itemsLayer.add(heroPlayer)
engine.itemsLayer.add(GemPickup(x = 100f, y = 50f))

// Clear all active entities
engine.itemsLayer.clear()

// Remove dead entities (automatically called at end of frame)
engine.itemsLayer.cleanup()
```
