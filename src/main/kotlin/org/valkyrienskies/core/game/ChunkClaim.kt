package org.valkyrienskies.core.game

import java.lang.Math.floorDiv
import kotlin.math.abs

data class ChunkClaim(
    /**
     * CHUNK center X
     */
    val x: Int,
    /**
     * CHUNK center Z
     */
    val z: Int
) {

    companion object {
        // Every ship is given 625x625 chunks (10k blocks)
        // Hard-coded
        const val RADIUS: Int = 312

        /**
         * Get the claim for a specific chunk
         */
        fun getClaim(chunkX: Int, chunkZ: Int) =
            ChunkClaim(
                floorDiv(chunkX, RADIUS) * RADIUS,
                floorDiv(chunkZ, RADIUS) * RADIUS
            )
    }
    /**
     * x start (inclusive)
     */
    val xStart = x - RADIUS

    /**
     * x end (inclusive)
     */
    val xEnd = x + RADIUS

    /**
     * z start (inclusive)
     */
    val zStart = z - RADIUS

    /**
     * z end (inclusive)
     */
    val zEnd = z + RADIUS

    val size = RADIUS * RADIUS * 4

    fun contains(x: Int, z: Int) =
        abs(this.x - x) <= RADIUS && abs(this.z - z) <= RADIUS;

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