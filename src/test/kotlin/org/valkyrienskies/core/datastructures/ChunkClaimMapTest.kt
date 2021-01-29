package org.valkyrienskies.core.datastructures

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.valkyrienskies.core.VSRandomUtils

internal class ChunkClaimMapTest {

    @Test
    fun addChunkClaim() {
        val chunkClaimMap = ChunkClaimMap<Int>()
        val chunkClaim = VSRandomUtils.randomChunkClaim()
        val value = VSRandomUtils.randomIntegerNotCloseToLimit()
        chunkClaimMap.addChunkClaim(chunkClaim, value)
        assertEquals(value, chunkClaimMap.getDataAtChunkPosition(chunkClaim.xStart, chunkClaim.zStart))
        assertThrows<IllegalArgumentException> { chunkClaimMap.addChunkClaim(chunkClaim, value) }
    }

    @Test
    fun removeChunkClaim() {
        val chunkClaimMap = ChunkClaimMap<Int>()
        val chunkClaim = VSRandomUtils.randomChunkClaim()
        val value = VSRandomUtils.randomIntegerNotCloseToLimit()
        chunkClaimMap.addChunkClaim(chunkClaim, value)
        assertEquals(value, chunkClaimMap.getDataAtChunkPosition(chunkClaim.xStart, chunkClaim.zStart))
        chunkClaimMap.removeChunkClaim(chunkClaim)
        assertEquals(null, chunkClaimMap.getDataAtChunkPosition(chunkClaim.xStart, chunkClaim.zStart))
        assertThrows<IllegalArgumentException> { chunkClaimMap.removeChunkClaim(chunkClaim) }
    }

    @Test
    fun getDataAtChunkPosition() {
        val chunkClaimMap = ChunkClaimMap<Int>()
        val chunkClaim = VSRandomUtils.randomChunkClaim()
        val value = VSRandomUtils.randomIntegerNotCloseToLimit()
        chunkClaimMap.addChunkClaim(chunkClaim, value)
        assertEquals(value, chunkClaimMap.getDataAtChunkPosition(chunkClaim.xStart, chunkClaim.zStart))
    }
}