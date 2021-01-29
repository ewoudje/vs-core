package org.valkyrienskies.core.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.valkyrienskies.core.VSRandomUtils

class TestQueryableShipData {

    /**
     * Tests getting [ShipData] from [java.util.UUID].
     */
    @Test
    fun testGetShipFromUUID() {
        val queryableShipData = QueryableShipData()
        val shipData = VSRandomUtils.randomShipData()
        queryableShipData.addShipData(shipData)
        assertEquals(shipData, queryableShipData.getShipDataFromUUID(shipData.shipUUID))
    }

    /**
     * Tests getting [ShipData] from [ChunkClaim].
     */
    @Test
    fun testGetShipFromChunkClaim() {
        val queryableShipData = QueryableShipData()
        val shipData = VSRandomUtils.randomShipData()
        queryableShipData.addShipData(shipData)
        val shipChunkClaim = shipData.chunkClaim
        assertEquals(shipData, queryableShipData.getShipDataFromChunkPos(shipChunkClaim.xIndex, shipChunkClaim.zIndex))
    }

    /**
     * Test adding duplicate [ShipData].
     */
    @Test
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
    @Test
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
    @Test
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