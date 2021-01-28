package org.valkyrienskies.core.game

import kotlin.math.max
import kotlin.math.min

/**
 * Claims all chunks within [xStart] .. [xEnd] and [zStart] .. [zEnd] inclusive.
 *
 * Avoid calling this constructor, when possible use [ChunkClaim.createChunkClaimSafe] instead.
 */
data class ChunkClaim(
    val xStart: Int,
    val xEnd: Int,
    val zStart: Int,
    val zEnd: Int
) {

    companion object {
        /**
         * Create a ChunkClaim that guarantees xStart <= xEnd and zStart <= zEnd.
         */
        fun createChunkClaimSafe(xStart: Int, xEnd: Int, zStart: Int, zEnd: Int): ChunkClaim {
            return ChunkClaim(min(xStart, xEnd), max(xStart, xEnd), min(zStart, zEnd), max(zStart, zEnd))
        }
    }

    val size = (xEnd - xStart + 1) * (zEnd - zStart + 1)

    fun contains(x: Int, z: Int) =
        x in xStart .. xEnd && z in zStart .. zEnd

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