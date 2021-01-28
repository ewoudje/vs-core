package org.valkyrienskies.core.game

import org.joml.primitives.AABBd
import org.joml.primitives.AABBdc
import org.valkyrienskies.core.datastructures.ChunkClaimMap
import java.util.*

class QueryableShipData {

    private val uuidToShipData: MutableMap<UUID, ShipData>
    // This maps chunk positions to the ShipData that has claimed it.
    private val chunkClaimMap: ChunkClaimMap<ShipData>

    init {
        uuidToShipData = HashMap<UUID, ShipData>()
        chunkClaimMap = ChunkClaimMap()
    }

    fun getAllShipData(): Iterator<ShipData> {
        return uuidToShipData.values.iterator()
    }

    fun getShipDataFromUUID(uuid: UUID): ShipData? {
        return uuidToShipData[uuid]
    }

    fun getShipDataFromChunkPos(chunkX: Int, chunkZ: Int): ShipData? {
        return chunkClaimMap.getDataAtChunkPosition(chunkX, chunkZ)
    }

    fun addShipData(shipData: ShipData) {
        if (getShipDataFromUUID(shipData.shipUUID) != null) {
            throw IllegalArgumentException("Adding shipData $shipData failed because of duplicated UUID.")
        }
        uuidToShipData[shipData.shipUUID] = shipData
        chunkClaimMap.addChunkClaim(shipData.chunkClaim, shipData)
    }

    fun removeShipData(shipData: ShipData) {
        if (getShipDataFromUUID(shipData.shipUUID) == null) {
            throw IllegalArgumentException("Removing $shipData failed because it wasn't in the UUID map.")
        }
        uuidToShipData.remove(shipData.shipUUID)
        chunkClaimMap.removeChunkClaim(shipData.chunkClaim)
    }

    fun getShipDataIntersecting(aabb: AABBdc): Iterator<ShipData> {
        // TODO("Use https://github.com/tzaeschke/phtree")
        return uuidToShipData.values.filter { shipData: ShipData -> shipData.shipAABB.intersectsAABB(aabb as AABBd) }.iterator()
    }
}