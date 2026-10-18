package com.pixie.engine.core.loop

/**
 * Fixed-step game loop runner providing deterministic physics updates and rendering interpolation.
 *
 * Decouples game logic updates (run at exact [targetUpdatesPerSecond], e.g. 60 Hz)
 * from arbitrary display refresh rates (60Hz, 90Hz, 120Hz).
 */
class PixieGameLoop(
    val targetUpdatesPerSecond: Int = 60,
    private val maxFrameSkip: Int = 5,
    private val onFixedUpdate: (dt: Float) -> Unit,
    private val onRender: (alpha: Float) -> Unit
) {
    val fixedDeltaTime: Float = 1.0f / targetUpdatesPerSecond
    private var accumulator: Float = 0f
    private var isRunning: Boolean = false
    private var isPaused: Boolean = false

    var currentFps: Float = 0f
        private set
    var totalUpdates: Long = 0L
        private set

    private var frameCount: Int = 0
    private var fpsTimer: Float = 0f

    fun start() {
        isRunning = true
        isPaused = false
        accumulator = 0f
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun stop() {
        isRunning = false
    }

    /**
     * Called once per display frame with the elapsed real time [rawDeltaSeconds].
     */
    fun tick(rawDeltaSeconds: Float) {
        if (!isRunning) return

        // Update FPS diagnostics
        fpsTimer += rawDeltaSeconds
        frameCount++
        if (fpsTimer >= 1.0f) {
            currentFps = frameCount / fpsTimer
            frameCount = 0
            fpsTimer = 0f
        }

        if (isPaused) {
            onRender(1.0f)
            return
        }

        // Clamp raw delta time to avoid accumulator overflow on stalls/hitches
        val clampedDelta = rawDeltaSeconds.coerceIn(0f, fixedDeltaTime * maxFrameSkip)
        accumulator += clampedDelta

        while (accumulator >= fixedDeltaTime) {
            onFixedUpdate(fixedDeltaTime)
            accumulator -= fixedDeltaTime
            totalUpdates++
        }

        // Alpha interpolation factor [0..1] for smooth rendering between physics states
        val alpha = if (fixedDeltaTime > 0f) (accumulator / fixedDeltaTime).coerceIn(0f, 1f) else 1f
        onRender(alpha)
    }

    fun reset() {
        accumulator = 0f
        totalUpdates = 0L
        frameCount = 0
        fpsTimer = 0f
    }
}
