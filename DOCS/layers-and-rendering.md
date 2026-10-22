# Layers & Rendering System

Pixie Engine structures visuals into an ordered `LayerStack`. Layers are drawn back-to-front and can scroll at different speeds to produce rich depth and parallax effects.

---

## 🥞 The Layer Stack

```
Back
  │   1. SkyLayer                 (Celestial cycles, day/night gradients, sun/moon)
  │   2. CloudLayer               (Slow atmospheric parallax clouds)
  │   3. BackgroundParallaxLayer  (Distant mountain ranges, skylines, deep space biomes)
  │   4. GroundGamefieldLayer     (Terrain plane, road tiles, ground destructibles)
  │   5. GameItemsLayer           (Active entities: Player, Enemies, Obstacles, Pickups)
  │   6. ParticleLayer            (Sparks, smoke clouds, explosion shockwaves)
  ▼   7. ForegroundParallaxLayer  (Fast foreground foliage, dust particles, UI HUD)
Front
```

The standard layers are pre-registered in `engine.layerStack`, but you can customize, reorder, or add custom layers.

---

## 🎨 Creating a Custom Layer

To create a new visual layer, extend `PixieLayer`:

```kotlin
import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.layer.PixieLayer
import com.pixie.engine.core.ports.RenderBatchPort

class StarfieldLayer(
    override val id: String = "starfield",
    override var zOrder: Int = 10,
    override var isVisible: Boolean = true
) : PixieLayer {

    private val stars = List(40) { Star(x = Math.random().toFloat() * 320f, y = Math.random().toFloat() * 180f) }

    override fun update(dt: Float, context: GameContext) {
        // Move stars downward for a vertical space effect
        stars.forEach { star ->
            star.y += 20f * dt
            if (star.y > 180f) star.y = 0f
        }
    }

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        stars.forEach { star ->
            // Draw a single 1x1 pixel star
            batch.drawPixelRect(
                x = star.x,
                y = star.y,
                width = 1f,
                height = 1f,
                color = 0xFFFFFFFF.toInt(),
                alpha = star.brightness
            )
        }
    }
}
```

Register it with the engine:
```kotlin
engine.layerStack.addLayer(StarfieldLayer())
```

---

## 👾 Defining Pixel Sprites (`PixelSprite`)

Sprites in Pixie Engine are authored directly in code as clean pixel matrices, requiring no external image assets:

```kotlin
import com.pixie.engine.core.sprite.PixelSprite

// 8x8 Gem Sprite Definition
val GEM_SPRITE = PixelSprite.fromPattern(
    pattern = listOf(
        "  GGGG  ",
        " GYYYYG ",
        "GYYYYYYG",
        "GGYYYYGG",
        " GYYYYG ",
        "  GYYG  ",
        "   GG   ",
        "    G   "
    ),
    colorMap = mapOf(
        'G' to 0xFF00AA00.toInt(), // Dark Green Border
        'Y' to 0xFF55FF55.toInt()  // Bright Emerald Body
    )
)
```

### Drawing Sprites
Inside an entity's `render()` method:
```kotlin
override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
    val screenX = position.x - camera.position.x
    val screenY = position.y - camera.position.y
    
    batch.drawSprite(
        sprite = GEM_SPRITE,
        x = screenX,
        y = screenY,
        saturation = context.colorRecoveryProgress, // Real-time color recovery!
        alpha = 1.0f
    )
}
```

---

## 📺 Retro Pixel Gap Mode (`pixelsGap`)

Pixie Engine supports an authentic CRT-style pixel separation mode via the `pixelsGap` configuration:

- **`pixelsGap = 0` (Default)**: Modern solid pixel art. Pixels fill their entire virtual unit box.
- **`pixelsGap = 1`**: Retro arcade monitor style. Leaves a 1-pixel gap between virtual dots, producing a crisp CRT shadow-mask appearance without any performance penalty.

```kotlin
// Set solid style
engine.pixelsGap = 0

// Or enable CRT retro dot matrix style
engine.pixelsGap = 1
```

---

## 🔍 Layer Visibility & Runtime Inspector

You can toggle visibility or inspect individual layers dynamically (e.g. for debug screens or pause menus):

```kotlin
// Toggle specific layers
engine.layerStack.getLayer("cloud")?.isVisible = false
engine.layerStack.getLayer("sky")?.isVisible = true

// Query all active layers
val allLayers = engine.layerStack.layers
```
