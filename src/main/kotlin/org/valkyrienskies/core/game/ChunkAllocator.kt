package org.valkyrienskies.core.game

import org.joml.Vector2i
import org.joml.Vector3i

/**
 * Allocates chunks in a straight line across the Z axis
 */
class ChunkAllocator(
    private var lastChunkZ: Int = CHUNK_Z_START
) {

    companion object {
        const val MAX_CHUNK_LENGTH = 3200
        const val MAX_CHUNK_RADIUS = MAX_CHUNK_LENGTH / 2 - 1
        const val CHUNK_X = 320000
        const val CHUNK_Z_START = 0
    }

    fun isChunkInShipyard(chunkX: Int, chunkZ: Int): Boolean {
        return chunkX >= CHUNK_X - MAX_CHUNK_RADIUS && chunkZ >= CHUNK_Z_START - MAX_CHUNK_RADIUS
    }

    /**
     * Determines whether or not a chunk is in the shipyard
     * @param pos The position of the chunk
     * @return True if the chunk is in the shipyard
     */
    fun isChunkInShipyard(pos: Vector2i): Boolean {
        return isChunkInShipyard(pos.x, pos.y)
    }

    /**
     * Determines whether or not a block is in the shipyard
     * @param pos The position of the block
     * @return True if the block is in the shipyard
     */
    fun isBlockInShipyard(pos: Vector3i): Boolean {
        return isChunkInShipyard(pos.x shr 4, pos.z shr 4)
    }

    /**
     * This finds the next empty chunkSet for use, currently only increases the xPos to get new
     * positions
     */
    fun allocateNextChunkClaim(): ChunkClaim {
        lastChunkZ += MAX_CHUNK_LENGTH
        return ChunkClaim(CHUNK_X, lastChunkZ, MAX_CHUNK_RADIUS)
    }

}