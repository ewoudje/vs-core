package org.valkyrienskies.core.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.RepeatedTest
import org.valkyrienskies.core.VSRandomUtils

class TestQueryableShipData {

    /**
     * Tests getting [ShipData] from [java.util.UUID].
     */
    @RepeatedTest(25)
    fun testGetShipFromUUID() {
        val queryableShipData = QueryableShipData()
        val shipData = VSRandomUtils.randomShipData()
        queryableShipData.addShipData(shipData)
        assertEquals(shipData, queryableShipData.getShipDataFromUUID(shipData.shipUUID))
    }

    /**
     * Tests getting [ShipData] from [ChunkClaim].
     */
    @RepeatedTest(25)
    fun testGetShipFromChunkClaim() {
        val queryableShipData = QueryableShipData()
        val shipData = VSRandomUtils.randomShipData()
        queryableShipData.addShipData(shipData)
        val shipChunkClaim = shipData.chunkClaim

        // Assert that querying the chunk claim corners returns shipData
        assertEquals(queryableShipData.getShipDataFromChunkPos(shipChunkClaim.xStart, shipChunkClaim.zStart), shipData)
        assertEquals(queryableShipData.getShipDataFromChunkPos(shipChunkClaim.xStart, shipChunkClaim.zEnd), shipData)
        assertEquals(queryableShipData.getShipDataFromChunkPos(shipChunkClaim.xEnd, shipChunkClaim.zStart), shipData)
        assertEquals(queryableShipData.getShipDataFromChunkPos(shipChunkClaim.xEnd, shipChunkClaim.zEnd), shipData)

        // Assert that querying the chunk claim center returns shipData
        assertEquals(queryableShipData.getShipDataFromChunkPos((shipChunkClaim.xStart + shipChunkClaim.xEnd) / 2, (shipChunkClaim.zStart + shipChunkClaim.zEnd) / 2), shipData)

        // Assert that querying outside the chunk claim returns nothing
        assertEquals(queryableShipData.getShipDataFromChunkPos(shipChunkClaim.xStart - 1, shipChunkClaim.zStart), null)
        assertEquals(queryableShipData.getShipDataFromChunkPos(shipChunkClaim.xStart - 1, shipChunkClaim.zEnd), null)
        assertEquals(queryableShipData.getShipDataFromChunkPos(shipChunkClaim.xEnd + 1, shipChunkClaim.zStart), null)
        assertEquals(queryableShipData.getShipDataFromChunkPos(shipChunkClaim.xEnd + 1, shipChunkClaim.zEnd), null)
        assertEquals(queryableShipData.getShipDataFromChunkPos(shipChunkClaim.xStart, shipChunkClaim.zStart - 1), null)
        assertEquals(queryableShipData.getShipDataFromChunkPos(shipChunkClaim.xStart, shipChunkClaim.zEnd + 1), null)
        assertEquals(queryableShipData.getShipDataFromChunkPos(shipChunkClaim.xEnd, shipChunkClaim.zStart - 1), null)
        assertEquals(queryableShipData.getShipDataFromChunkPos(shipChunkClaim.xEnd, shipChunkClaim.zEnd + 1), null)
    }

    /**
     * Test adding duplicate [ShipData].
     */
    @RepeatedTest(25)
    fun testAddDuplicateShip() {
        val queryableShipData = QueryableShipData()
        val shipData = VSRandomUtils.randomShipData()
        queryableShipData.addShipData(shipData)
        assertThrows(IllegalArgumentException::class.java) {
            queryableShipData.addShipData(shipData)
        }
    }

    /**
     * Test removing [ShipData] in [QueryableShipData].
     */
    @RepeatedTest(25)
    fun testRemovingShipNotInQueryableShipData() {
        val queryableShipData = QueryableShipData()
        val shipData = VSRandomUtils.randomShipData()
        val otherShipData = VSRandomUtils.randomShipData()
        queryableShipData.addShipData(shipData)
        assertThrows(IllegalArgumentException::class.java) {
            queryableShipData.removeShipData(otherShipData)
        }
    }

    /**
     * Test getting a [ShipData] by its [org.joml.primitives.AABBdc]
     */
    @RepeatedTest(25)
    fun testGettingShipByBoundingBox() {
        val queryableShipData = QueryableShipData()
        val shipData = VSRandomUtils.randomShipData()
        queryableShipData.addShipData(shipData)
        val shipsIntersectingBB = queryableShipData.getShipDataIntersecting(shipData.shipAABB)
        assertTrue(shipsIntersectingBB.hasNext())
        assertEquals(shipsIntersectingBB.next(), shipData)
        assertFalse(shipsIntersectingBB.hasNext())
    }
}