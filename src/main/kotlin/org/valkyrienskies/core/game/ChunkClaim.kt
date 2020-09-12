package org.valkyrienskies.core.game

data class ChunkClaim(
    /**
     * center X
     */
    val x: Int,
    /**
     * center Z
     */
    val z: Int,
    val radius: Int
) {
    /**
     * x start (inclusive)
     */
    val xStart = x - radius

    /**
     * x end (inclusive)
     */
    val xEnd = x + radius

    /**
     * z start (inclusive)
     */
    val zStart = z - radius

    /**
     * z end (inclusive)
     */
    val zEnd = z + radius

    val size = radius * radius * 4

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