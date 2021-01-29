package org.valkyrienskies.core.game

import org.joml.primitives.AABBd
import org.joml.primitives.AABBdc
import java.util.*

class QueryableShipData {

    private val uuidToShipData: MutableMap<UUID, ShipData>
    private val chunkClaimToShipData: MutableMap<Long, ShipData>

    init {
        uuidToShipData = HashMap<UUID, ShipData>()
        chunkClaimToShipData = HashMap<Long, ShipData>()
    }

    fun getAllShipData(): Iterator<ShipData> {
        return uuidToShipData.values.iterator()
    }

    fun getShipDataFromUUID(uuid: UUID): ShipData? {
        return uuidToShipData[uuid]
    }

    fun getShipDataFromChunkPos(chunkX: Int, chunkZ: Int): ShipData? {
        val chunkClaimAsLong = ChunkClaim.getClaimThenToLong(chunkX, chunkZ)
        return chunkClaimToShipData[chunkClaimAsLong]
    }

    fun addShipData(shipData: ShipData) {
        if (getShipDataFromUUID(shipData.shipUUID) != null) {
            throw IllegalArgumentException("Adding shipData $shipData failed because of duplicated UUID.")
        }
        if (getShipDataFromChunkPos(shipData.chunkClaim.xIndex, shipData.chunkClaim.zIndex) != null) {
            throw IllegalArgumentException("Adding shipData $shipData failed because of duplicated chunk claim.")
        }
        uuidToShipData[shipData.shipUUID] = shipData
        chunkClaimToShipData[shipData.chunkClaim.toLong()] = shipData
    }

    fun removeShipData(shipData: ShipData) {
        if (getShipDataFromUUID(shipData.shipUUID) == null) {
            throw IllegalArgumentException("Removing $shipData failed because it wasn't in the UUID map.")
        }
        if (getShipDataFromChunkPos(shipData.chunkClaim.xIndex, shipData.chunkClaim.zIndex) == null) {
            throw IllegalArgumentException("Removing shipData $shipData failed because it wasn't in the Chunk Claim map.")
        }
        uuidToShipData.remove(shipData.shipUUID)
        chunkClaimToShipData.remove(shipData.chunkClaim.toLong())
    }

    fun getShipDataIntersecting(aabb: AABBdc): Iterator<ShipData> {
        // TODO("Use https://github.com/tzaeschke/phtree")
        return uuidToShipData.values.filter { shipData: ShipData -> shipData.shipAABB.intersectsAABB(aabb as AABBd) }.iterator()
    }
}