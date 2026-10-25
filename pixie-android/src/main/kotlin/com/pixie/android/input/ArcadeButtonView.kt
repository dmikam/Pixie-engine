package com.pixie.android.input

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixie.engine.core.sprite.PixelSprite

/**
 * Geometric Octagonal shape for classic Japanese fightsticks / arcade buttons.
 */
class OctagonShape(private val cornerCutRatio: Float = 0.28f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val cutX = w * cornerCutRatio
        val cutY = h * cornerCutRatio

        val path = Path().apply {
            moveTo(cutX, 0f)
            lineTo(w - cutX, 0f)
            lineTo(w, cutY)
            lineTo(w, h - cutY)
            lineTo(w - cutX, h)
            lineTo(cutX, h)
            lineTo(0f, h - cutY)
            lineTo(0f, cutY)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Resolves button shape according to ArcadeButtonShape configuration.
 */
fun resolveButtonShape(shape: ArcadeButtonShape): Shape = when (shape) {
    ArcadeButtonShape.CIRCLE -> CircleShape
    ArcadeButtonShape.OCTAGON -> OctagonShape()
    ArcadeButtonShape.ROUNDED_RECT -> RoundedCornerShape(12.dp)
    ArcadeButtonShape.PILL -> RoundedCornerShape(24.dp)
}

/**
 * Resolves width and height sizing modifiers based on configuration.
 */
fun resolveButtonSizeModifier(config: ArcadeButtonConfig): Modifier = when {
    config.weight != null -> Modifier.height(config.heightDp ?: config.sizeDp)
    config.widthDp != null -> Modifier.width(config.widthDp).height(config.heightDp ?: config.sizeDp)
    else -> Modifier.width(config.sizeDp).height(config.heightDp ?: config.sizeDp)
}

/**
 * Internal 3D lighting highlight rim for tactile arcade button.
 */
@Composable
fun ArcadeButtonHighlight(
    shape: Shape,
    isPressed: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(3.dp)
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.10f else 0.25f),
                        Color.Transparent
                    )
                )
            )
    )
}

/**
 * Centered button content: retro PixelSprite, Material vector icon, or arcade label.
 */
@Composable
fun ArcadeButtonContent(
    config: ArcadeButtonConfig,
    isPressed: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            config.sprite != null && config.sprite != PixelSprite.EMPTY -> {
                PixelSpriteView(sprite = config.sprite, modifier = Modifier.size(28.dp))
            }
            config.iconVector != null -> {
                Icon(
                    imageVector = config.iconVector,
                    contentDescription = config.label ?: config.id,
                    tint = if (isPressed) Color.White else Color(0xFFE0F7FA),
                    modifier = Modifier.size(24.dp)
                )
            }
            !config.label.isNullOrEmpty() -> {
                Text(
                    text = config.label,
                    color = if (isPressed) Color.White else Color(0xFFE0F7FA),
                    fontSize = if (config.label.length <= 2) 18.sp else 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * High-responsiveness tactile arcade button supporting custom shapes, icons, letters,
 * flex weights, and retro PixelSprite graphics.
 */
@Composable
fun TactileArcadeButton(
    config: ArcadeButtonConfig,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }
    val shape = remember(config.shape) { resolveButtonShape(config.shape) }

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        label = "button_scale"
    )

    val currentBackground = if (isPressed) config.pressedColor else config.baseColor

    Box(
        modifier = modifier
            .then(resolveButtonSizeModifier(config))
            .scale(animatedScale)
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = shape,
                spotColor = config.pressedColor
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        currentBackground.copy(alpha = 0.95f),
                        currentBackground.copy(alpha = 0.70f)
                    )
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        config.rimColor,
                        config.rimColor.copy(alpha = 0.4f)
                    )
                ),
                shape = shape
            )
            .then(
                if (isInteractive) {
                    Modifier.pointerInput(config.id) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                isPressed = true
                                config.onPressDown()
                                val pointerId = down.id
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId }
                                    if (change == null || !change.pressed) {
                                        isPressed = false
                                        config.onPressUp()
                                        break
                                    }
                                    change.consume()
                                }
                            }
                        }
                    }
                } else Modifier
            )
            .testTag("arcade_btn_${config.id}"),
        contentAlignment = Alignment.Center
    ) {
        ArcadeButtonHighlight(shape = shape, isPressed = isPressed)
        ArcadeButtonContent(config = config, isPressed = isPressed)
    }
}

/**
 * Draws an authentic PixelSprite in Compose.
 */
@Composable
fun PixelSpriteView(
    sprite: PixelSprite,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (sprite.width == 0 || sprite.height == 0) return@Canvas
        val pixelW = size.width / sprite.width
        val pixelH = size.height / sprite.height

        drawIntoCanvas { composeCanvas ->
            val nativeCanvas = composeCanvas.nativeCanvas
            val paint = android.graphics.Paint().apply {
                isAntiAlias = false
                isFilterBitmap = false
                style = android.graphics.Paint.Style.FILL
            }

            for (py in 0 until sprite.height) {
                for (px in 0 until sprite.width) {
                    val color = sprite.getPixel(px, py)
                    if ((color ushr 24) != 0) {
                        paint.color = color
                        val left = px * pixelW
                        val top = py * pixelH
                        nativeCanvas.drawRect(left, top, left + pixelW + 0.3f, top + pixelH + 0.3f, paint)
                    }
                }
            }
        }
    }
}
