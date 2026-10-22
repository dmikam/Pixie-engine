# Cookbook: Build a 2D Game in 15 Minutes

This hands-on cookbook guides you through building a complete, playable 2D arcade game from scratch—**"Gem Catcher"**—using Pixie Engine and Jetpack Compose.

---

## 🎯 Game Concept: "Gem Catcher"
- **Objective**: Control a treasure basket at the bottom of the screen to catch falling gems.
- **Controls**: Slide your finger left/right to move the basket.
- **Engine Features Used**: `PixieEngine`, `FixedScreen` view mode, custom `PixelSprite`, `PixieObject` lifecycle, AABB collisions, procedural audio & haptics.

---

## Step 1: Define Pixel Sprites

In your Kotlin file, define sprites using simple text matrices:

```kotlin
import com.pixie.engine.core.sprite.PixelSprite

// 16x10 Basket Sprite
val BASKET_SPRITE = PixelSprite.fromPattern(
    pattern = listOf(
        "B              B",
        "B  BBBBBBBBBB  B",
        "BB BWWWWWWWW B BB",
        " B BWWWWWWWW B B ",
        " B BWWWWWWWW B B ",
        " BB BBBBBBBB BB  ",
        "  B          B   ",
        "  BBBBBBBBBBBB   ",
        "   BBBBBBBBBB    ",
        "    BBBBBBBB     "
    ),
    colorMap = mapOf(
        'B' to 0xFF8D6E63.toInt(), // Dark Wood
        'W' to 0xFFD7CCC8.toInt()  // Woven Straw
    )
)

// 8x8 Shining Gem Sprite
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
        'G' to 0xFF00AA00.toInt(), // Deep Green Outline
        'Y' to 0xFF69F0AE.toInt()  // Emerald Glow
    )
)
```

---

## Step 2: Implement Entities

### 1. Player Basket
```kotlin
import com.pixie.engine.core.camera.PixieCamera
import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.entity.PixieObject
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.ports.RenderBatchPort

class BasketPlayer(arenaWidth: Float, arenaHeight: Float) : PixieObject(
    position = Vec2(arenaWidth / 2f - 8f, arenaHeight - 20f),
    width = 16f,
    height = 10f,
    isSolid = true
) {
    override fun update(dt: Float, context: GameContext) {
        val input = context.input ?: return
        // Move with input axes (-1..+1)
        position.x += input.moveX * 160f * dt
        
        // Clamp inside arena boundaries (0 .. 240)
        position.x = position.x.coerceIn(0f, 240f - width)
    }

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        batch.drawSprite(BASKET_SPRITE, position.x, position.y)
    }
}
```

### 2. Falling Gem
```kotlin
import com.pixie.engine.core.ports.AudioEvent
import com.pixie.engine.core.ports.HapticFeedbackType
import kotlin.random.Random

class FallingGem(startX: Float, val fallSpeed: Float) : PixieObject(
    position = Vec2(startX, -10f),
    width = 8f,
    height = 8f,
    isSolid = true
) {
    override fun update(dt: Float, context: GameContext) {
        position.y += fallSpeed * dt
        
        // Missed gem — bottom boundary reached
        if (position.y > 320f) {
            isAlive = false
            context.livesLeft--
            context.audio?.playSound(AudioEvent.HIT)
            if (context.livesLeft <= 0) {
                context.isGameOver = true
                context.audio?.playSound(AudioEvent.GAME_OVER)
            }
        }
    }

    override fun render(batch: RenderBatchPort, camera: PixieCamera, context: GameContext) {
        batch.drawSprite(GEM_SPRITE, position.x, position.y)
    }

    override fun onCollision(other: PixieObject, context: GameContext) {
        if (other is BasketPlayer) {
            isAlive = false // Collected!
            context.score += 100
            context.audio?.playSound(AudioEvent.COLLECT)
            context.haptics?.trigger(HapticFeedbackType.LIGHT)
        }
    }
}
```

---

## Step 3: Put Everything Together in Compose

```kotlin
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.pixie.android.audio.AndroidSoundPoolAudioAdapter
import com.pixie.android.haptic.AndroidHapticAdapter
import com.pixie.android.input.DualModeInputAdapter
import com.pixie.android.ui.PixieGameCanvas
import com.pixie.engine.core.PixieEngine
import com.pixie.engine.core.camera.GameViewMode
import com.pixie.engine.core.ports.VirtualResolution
import kotlin.random.Random

@Composable
fun GemCatcherGameScreen() {
    val context = LocalContext.current
    
    // 1. Initialize Pixie Engine
    val engine = remember {
        PixieEngine(
            virtualResolution = VirtualResolution(width = 240, height = 320),
            viewMode = GameViewMode.FixedScreen()
        ).apply {
            audio = AndroidSoundPoolAudioAdapter(context)
            haptics = AndroidHapticAdapter(context)
            input = DualModeInputAdapter()
        }
    }

    // 2. Setup Entities and Spawner Loop
    val player = remember { BasketPlayer(240f, 320f) }
    var spawnTimer by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        engine.itemsLayer.clear()
        engine.itemsLayer.add(player)
        engine.start()
    }

    DisposableEffect(Unit) {
        onDispose { engine.stop() }
    }

    // 3. Render Canvas with Touch Drag Steering
    val inputAdapter = engine.input as DualModeInputAdapter

    Box(modifier = Modifier.fillMaxSize()) {
        PixieGameCanvas(
            engine = engine,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Move player left/right relative to drag
                            inputAdapter.setMoveAxes(
                                x = (dragAmount.x / 5f).coerceIn(-1f, 1f),
                                y = 0f
                            )
                        },
                        onDragEnd = { inputAdapter.setMoveAxes(0f, 0f) },
                        onDragCancel = { inputAdapter.setMoveAxes(0f, 0f) }
                    )
                },
            onFrameTick = { dt ->
                // Spawn new gems periodically
                if (!engine.context.isGameOver) {
                    spawnTimer += dt
                    if (spawnTimer >= 0.8f) {
                        spawnTimer = 0f
                        val randomX = Random.nextFloat() * (240f - 16f)
                        val speed = 80f + engine.context.score * 0.05f
                        engine.itemsLayer.add(FallingGem(randomX, speed))
                    }
                }
            }
        )
    }
}
```

That's it! You now have a complete, fully functional, hardware-accelerated 2D game running on Android with:
- Crisp retro pixel art.
- Deterministic 60 FPS physics.
- Responsive drag controls.
- Real-time collisions and score increments.
- Zero-lag audio synthesis and haptic vibrations.
