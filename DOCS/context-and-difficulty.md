# Game Context & Continuous Difficulty

Pixie Engine provides a unified runtime state hub (`GameContext`) paired with a continuous difficulty system (`0..100`) that dynamically scales game properties at runtime.

---

## 🌐 The Game Context (`GameContext`)

`GameContext` is the central blackboard passed to all entity updates, layer rendering calls, and physics checks:

```kotlin
class GameContext {
    var score: Long = 0L
    var distance: Float = 0f
    var timeElapsedSeconds: Float = 0f
    var livesLeft: Int = 3
    var isGameOver: Boolean = false
    var isVictory: Boolean = false
    var colorRecoveryProgress: Float = 1.0f

    // Continuous difficulty scale from 0 to 100
    var difficulty: Int = DifficultyPreset.MEDIUM
    var currentLevel: Int = 1
    var currentMovingSpeed: Float = 60f

    // Active perk tags unlocked by player
    val activePerks = mutableSetOf<String>()

    fun hasPerk(perkId: String): Boolean = activePerks.contains(perkId)
}
```

---

## 📈 Continuous Difficulty System

Unlike engines limited to fixed "Easy / Normal / Hard" enums, Pixie Engine represents difficulty as a continuous integer scale from `0` to `100` (`MAX_DIFFICULTY = 100`).

### Standard Presets (`DifficultyPreset`)

| Preset | Value | Description |
| :--- | :---: | :--- |
| **`EASY`** | `10` | Forgiving enemy health, slower projectiles, generous item drop rates. |
| **`MEDIUM`** | `25` | Standard balanced arcade challenge. |
| **`DIFFICULT`** | `50` | Fast projectiles, aggressive enemy formations, tactical shield management. |
| **`HEAVY_METAL`**| `100` | Relentless bullet hell density, fast boss rage phases. |

You can also set custom arbitrary values (e.g. `difficulty = 37`) to implement rubber-banding or adaptive difficulty that scales with player performance.

---

## ⚖️ Dynamic Property Calculation (`calculateDynamicProperty`)

Instead of hardcoding speeds and health pools for each difficulty, use `calculateDynamicProperty()` to scale values automatically based on context:

```kotlin
// Example: Calculate enemy speed dynamically
val enemySpeed = context.calculateDynamicProperty(
    baseValue = 40f,         // Default speed at difficulty 0
    difficultyWeight = 0.5f,  // +0.5 units per difficulty point (0..100)
    levelWeight = 0.15f,      // +15% per campaign level (1..N)
    perkBonus = if (context.hasPerk("OVERCLOCK")) 1.25f else 1.0f,
    speedInfluence = context.currentMovingSpeed * 0.05f
)
```

### Formula Breakdown
```
Result = (baseValue + (difficulty * difficultyWeight)) * (1.0 + (level - 1) * levelWeight) * perkBonus + speedInfluence
```

This single formula unifies balancing across:
- **Enemy Movement Speed & Fire Rates**
- **Boss Health & Phase Durations**
- **Collectible Drop Probabilities**
- **Obstacle Spawn Intervals**

---

## 🏅 The Active Perks System

Reward players with perks stored in `context.activePerks`:

```kotlin
// Grant a perk upon picking up a badge or completing a milestone
context.activePerks.add("RAPID_FIRE")
context.activePerks.add("TITANIUM_HULL")

// Query in weapons or entities
if (context.hasPerk("RAPID_FIRE")) {
    fireInterval *= 0.75f // 25% faster shooting
}
```
