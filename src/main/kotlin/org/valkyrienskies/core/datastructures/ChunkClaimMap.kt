package org.valkyrienskies.core.datastructures

import ch.ethz.globis.phtree.PhTreeSolid
import org.valkyrienskies.core.game.ChunkClaim

/**
 * The [ChunkClaimMap] is a spatial map. We define [ChunkClaim]s as rectangles, and all points within that rectangle have
 * the value that was assigned to [ChunkClaim].
 *
 * This map DOES NOT ALLOW OVERLAPPING KEYS. This means if you try adding a ChunkClaim that overlaps with an existing
 * chunk claim you will get an [IllegalArgumentException].
 */
class ChunkClaimMap<T> {

    private val chunkClaimMap = PhTreeSolid.create<T>(2)

    fun addChunkClaim(chunkClaim: ChunkClaim, data: T) {
        val lowerBound = longArrayOf(chunkClaim.xStart.toLong(), chunkClaim.zStart.toLong())
        val upperBound = longArrayOf(chunkClaim.xEnd.toLong(), chunkClaim.zEnd.toLong())

        // Verify that this doesn't intersect with an existing claim
        val queryForIntersection = chunkClaimMap.queryIntersect(lowerBound, upperBound)
        if (queryForIntersection.hasNext()) {
            // There is an intersection, throw exception
            throw IllegalArgumentException("Old chunk claim of ${queryForIntersection.next()} intersects with new chunk claim of $chunkClaim")
        }

        chunkClaimMap.put(lowerBound, upperBound, data)
    }

    fun removeChunkClaim(chunkClaim: ChunkClaim) {
        val lowerBound = longArrayOf(chunkClaim.xStart.toLong(), chunkClaim.zStart.toLong())
        val upperBound = longArrayOf(chunkClaim.xEnd.toLong(), chunkClaim.zEnd.toLong())
        // Remove the data
        if (chunkClaimMap.remove(lowerBound, upperBound) == null) {
            // Throw exception if we didn't remove anything
            throw IllegalArgumentException("Tried to remove data at $chunkClaim, but that claim wasn't in the chunk claim map!")
        }
    }

    fun updateChunkClaim(oldChunkClaim: ChunkClaim, newChunkClaim: ChunkClaim) {
        val oldLowerBound = longArrayOf(oldChunkClaim.xStart.toLong(), oldChunkClaim.zStart.toLong())
        val oldUpperBound = longArrayOf(oldChunkClaim.xEnd.toLong(), oldChunkClaim.zEnd.toLong())

        val newLowerBound = longArrayOf(newChunkClaim.xStart.toLong(), newChunkClaim.zStart.toLong())
        val newUpperBound = longArrayOf(newChunkClaim.xEnd.toLong(), newChunkClaim.zEnd.toLong())

        // Verify that the new bounds don't collide with anything
        val queryForIntersection = chunkClaimMap.queryIntersect(newLowerBound, newUpperBound)
        if (queryForIntersection.hasNext()) {
            // There is an intersection, throw exception
            throw IllegalArgumentException("Tried to update data at $oldChunkClaim to be $newChunkClaim, but $newChunkClaim intersects with existing chunk claim at ${queryForIntersection.next()}!")
        }

        // Update the data
        if (chunkClaimMap.update(oldLowerBound, oldUpperBound, newLowerBound, newUpperBound) == null) {
            // Throw exception if we didn't remove anything
            throw IllegalArgumentException("Tried to update data at $oldChunkClaim to be $newChunkClaim, but there was no data at $oldChunkClaim!")
        }
    }

    fun getDataAtChunkPosition(chunkX: Int, chunkZ: Int): T? {
        val chunkPos = longArrayOf(chunkX.toLong(), chunkZ.toLong())
        val iterator = chunkClaimMap.queryIntersect(chunkPos, chunkPos)
        return if (iterator.hasNext()) {
            iterator.next()
        } else {
            null
        }
    }
}