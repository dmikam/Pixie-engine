package com.pixie.engine.core.chunk

import com.pixie.engine.core.context.GameContext
import com.pixie.engine.core.layer.GameItemsLayer
import com.pixie.engine.core.entity.PixieObject

/**
 * Represents a terrain chunk spanning a specific X or Y range in world space.
 */
data class TerrainChunk(
    val id: String,
    val startX: Float,
    val width: Float,
    val objects: MutableList<PixieObject> = mutableListOf()
) {
    val endX: Float get() = startX + width
}

/**
 * Manages procedural generation, caching, and disposal of terrain chunks ahead of the camera.
 */
class ChunkManager(
    val chunkWidth: Float = 320f,
    private val spawnAheadDistance: Float = 480f,
    private val despawnBehindDistance: Float = 240f
) {
    private val activeChunks = mutableListOf<TerrainChunk>()
    private var nextChunkX: Float = 0f
    private var chunkCounter: Int = 0

    var chunkSpawner: ((chunkIndex: Int, startX: Float, width: Float, context: GameContext) -> List<PixieObject>)? = null

    fun reset(initialWorldX: Float = 0f) {
        activeChunks.clear()
        nextChunkX = initialWorldX
        chunkCounter = 0
    }

    fun update(cameraX: Float, itemsLayer: GameItemsLayer, context: GameContext) {
        // Despawn chunks behind camera
        val despawnThreshold = cameraX - despawnBehindDistance
        val iterator = activeChunks.iterator()
        while (iterator.hasNext()) {
            val chunk = iterator.next()
            if (chunk.endX < despawnThreshold) {
                // Remove chunk's objects from items layer
                for (obj in chunk.objects) {
                    obj.isAlive = false
                    itemsLayer.removeObject(obj)
                }
                iterator.remove()
            }
        }

        // Spawn chunks ahead of camera
        val spawnThreshold = cameraX + spawnAheadDistance
        while (nextChunkX < spawnThreshold) {
            val chunkId = "chunk_${chunkCounter++}"
            val newObjects = chunkSpawner?.invoke(chunkCounter, nextChunkX, chunkWidth, context) ?: emptyList()

            // Tag each object with this chunk group
            for (obj in newObjects) {
                obj.addGroup(chunkId)
                itemsLayer.addObject(obj)
            }

            val chunk = TerrainChunk(
                id = chunkId,
                startX = nextChunkX,
                width = chunkWidth,
                objects = newObjects.toMutableList()
            )
            activeChunks.add(chunk)
            nextChunkX += chunkWidth
        }
    }

    fun getActiveChunks(): List<TerrainChunk> = activeChunks.toList()
}
