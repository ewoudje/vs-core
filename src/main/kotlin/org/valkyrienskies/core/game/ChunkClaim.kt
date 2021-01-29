package org.valkyrienskies.core.game

import org.valkyrienskies.core.game.ChunkClaim.Companion.DIAMETER
import java.lang.Math.floorDiv

/**
 * Each ChunkClaim claims all chunks between the coordinates
 * ([xIndex] * [DIAMETER], [zIndex] * [DIAMETER]) and ([xIndex] * [DIAMETER] + [DIAMETER] - 1, [zIndex] * [DIAMETER] + [DIAMETER] - 1)
 *
 * So for example, if [xIndex] is 5, [zIndex] is 10, and [DIAMETER] is 50, then this claim contains all chunks between
 * (250, 500) and (299, 549)
 */
data class ChunkClaim(val xIndex: Int, val zIndex: Int) {

    companion object {
        /**
         * Every ship is given [DIAMETER] x [DIAMETER] chunks, hard-coded.
         */
        const val DIAMETER: Int = 256

        private const val BOTTOM_32_BITS_MASK: Long = 0xFFFFFFFFL

        /**
         * Get the claim for a specific chunk
         */
        fun getClaim(chunkX: Int, chunkZ: Int) =
            ChunkClaim(floorDiv(chunkX, DIAMETER), floorDiv(chunkZ, DIAMETER))

        private fun claimToLong(claimXIndex: Int, claimZIndex: Int): Long {
            return ((claimXIndex.toLong() shl 32) or (claimZIndex.toLong() and BOTTOM_32_BITS_MASK))
        }

        fun getClaimThenToLong(chunkX: Int, chunkZ: Int): Long {
            // Compute the coordinates of the claim this chunk is in (not the same as chunk coordinates)
            val claimXIndex = floorDiv(chunkX, DIAMETER)
            val claimZIndex = floorDiv(chunkZ, DIAMETER)
            // Then convert
            return claimToLong(claimXIndex, claimZIndex)
        }
    }

    /**
     * x start (inclusive)
     */
    val xStart = xIndex * DIAMETER

    /**
     * x end (inclusive)
     */
    val xEnd = (xIndex * DIAMETER) + DIAMETER - 1

    /**
     * z start (inclusive)
     */
    val zStart = zIndex * DIAMETER

    /**
     * z end (inclusive)
     */
    val zEnd = (zIndex * DIAMETER) + DIAMETER - 1

    /**
     * The number of chunks owned by this claim
     */
    val size = (xEnd - xStart + 1) * (zEnd - zStart + 1)

    fun toLong(): Long {
        return claimToLong(xIndex, zIndex)
    }

    fun contains(x: Int, z: Int) =
        (x in xStart .. xEnd) and (z in zStart .. zEnd)

    inline fun <T> mapChunks(func: (x: Int, y: Int) -> T): MutableList<T> {
        val list = ArrayList<T>(size)
        for (chunkX in xStart..xEnd)
            for (chunkY in zStart..zEnd)
                list += func(chunkX, chunkY)
        return list
    }

    inline fun iterateChunks(func: (x: Int, y: Int) -> Unit) {
        for (chunkX in xStart..xEnd)
            for (chunkY in zStart..zEnd)
                func(chunkX, chunkY)
    }

    inline fun iterateBlocks(func: (x: Int, y: Int, z: Int) -> Unit) {
        iterateChunks { chunkX, chunkZ ->
            for (x in chunkX until chunkX + 16)
                for (z in chunkZ until chunkZ + 16)
                    for (y in 0 until 256)
                        func(x, y, z)
        }
    }

    fun chunkIterator(): Iterable<Pair<Int, Int>> {
        return Iterable {
            object : Iterator<Pair<Int, Int>> {
                var curX = xStart
                var curZ = zStart

                override fun hasNext(): Boolean {
                    return curZ <= zEnd
                }

                override fun next(): Pair<Int, Int> {
                    if (!hasNext()) throw NoSuchElementException()

                    val toReturn = Pair(curX, curZ)

                    if (curX == xEnd) {
                        curX = 0
                        curZ++
                    } else {
                        curX++
                    }

                    return toReturn
                }

            }
        }
    }

}