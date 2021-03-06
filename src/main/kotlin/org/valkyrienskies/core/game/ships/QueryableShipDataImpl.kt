package org.valkyrienskies.core.game.ships

import org.joml.primitives.AABBd
import org.joml.primitives.AABBdc
import org.valkyrienskies.core.datastructures.ChunkClaimMap
import java.util.UUID

typealias QueryableShipDataServer = QueryableShipData<ShipData>
typealias QueryableShipDataClient = QueryableShipData<ShipDataClient>
typealias MutableQueryableShipDataServer = MutableQueryableShipData<ShipData>
typealias MutableQueryableShipDataClient = MutableQueryableShipData<ShipDataClient>

interface QueryableShipData<out ShipDataType : ShipDataClient> : Iterable<ShipDataType> {
    val uuidToShipData: Map<UUID, ShipDataType>
    override fun iterator(): Iterator<ShipDataType>
    fun getShipDataFromUUID(uuid: UUID): ShipDataType?
    fun getShipDataFromChunkPos(chunkX: Int, chunkZ: Int): ShipDataType?
    fun getShipDataIntersecting(aabb: AABBdc): Iterator<ShipDataType>
}

interface MutableQueryableShipData<ShipDataType : ShipDataClient> : QueryableShipData<ShipDataType> {
    fun addShipData(shipData: ShipDataType)
    fun removeShipData(shipData: ShipDataType)
}

open class QueryableShipDataImpl<ShipDataType : ShipDataClient>(
    data: Iterable<ShipDataType> = emptyList()
) : MutableQueryableShipData<ShipDataType> {

    val _uuidToShipData: HashMap<UUID, ShipDataType> = HashMap()
    override val uuidToShipData: Map<UUID, ShipDataType> = _uuidToShipData
    val chunkClaimToShipData: ChunkClaimMap<ShipDataType> = ChunkClaimMap()

    init {
        data.forEach(::addShipData)
    }

    override fun iterator(): Iterator<ShipDataType> {
        return _uuidToShipData.values.iterator()
    }

    override fun getShipDataFromUUID(uuid: UUID): ShipDataType? {
        return _uuidToShipData[uuid]
    }

    override fun getShipDataFromChunkPos(chunkX: Int, chunkZ: Int): ShipDataType? {
        return chunkClaimToShipData.get(chunkX, chunkZ)
    }

    override fun addShipData(shipData: ShipDataType) {
        if (getShipDataFromUUID(shipData.shipUUID) != null) {
            throw IllegalArgumentException("Adding shipData $shipData failed because of duplicated UUID.")
        }
        _uuidToShipData[shipData.shipUUID] = shipData
        chunkClaimToShipData.set(shipData.chunkClaim, shipData)
    }

    override fun removeShipData(shipData: ShipDataType) {
        if (getShipDataFromUUID(shipData.shipUUID) == null) {
            throw IllegalArgumentException("Removing $shipData failed because it wasn't in the UUID map.")
        }
        _uuidToShipData.remove(shipData.shipUUID)
        chunkClaimToShipData.remove(shipData.chunkClaim)
    }

    override fun getShipDataIntersecting(aabb: AABBdc): Iterator<ShipDataType> {
        // TODO("Use https://github.com/tzaeschke/phtree")
        return _uuidToShipData.values
            .filter { it.shipAABB.intersectsAABB(aabb as AABBd) }
            .iterator()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QueryableShipDataImpl<*>

        if (uuidToShipData != other.uuidToShipData) return false

        return true
    }

    override fun hashCode(): Int {
        return uuidToShipData.hashCode()
    }
}
