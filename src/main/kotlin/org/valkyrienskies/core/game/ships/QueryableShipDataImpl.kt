package org.valkyrienskies.core.game.ships

import org.joml.primitives.AABBd
import org.joml.primitives.AABBdc
import org.valkyrienskies.core.datastructures.ChunkClaimMap

typealias QueryableShipDataServer = QueryableShipData<ShipData>
typealias QueryableShipDataCommon = QueryableShipData<ShipDataCommon>
typealias MutableQueryableShipDataServer = MutableQueryableShipData<ShipData>
typealias MutableQueryableShipDataCommon = MutableQueryableShipData<ShipDataCommon>

interface QueryableShipData<out ShipDataType : ShipDataCommon> : Iterable<ShipDataType> {
    val idToShipData: Map<ShipId, ShipDataType>
    override fun iterator(): Iterator<ShipDataType>
    fun getById(uuid: ShipId): ShipDataType?
    fun getShipDataFromChunkPos(chunkX: Int, chunkZ: Int): ShipDataType?
    fun getShipDataIntersecting(aabb: AABBdc): Iterator<ShipDataType>
}

interface MutableQueryableShipData<ShipDataType : ShipDataCommon> : QueryableShipData<ShipDataType> {
    fun addShipData(shipData: ShipDataType)
    fun removeShipData(shipData: ShipDataType)
}

open class QueryableShipDataImpl<ShipDataType : ShipDataCommon>(
    data: Iterable<ShipDataType> = emptyList()
) : MutableQueryableShipData<ShipDataType> {

    val _idToShipData: HashMap<ShipId, ShipDataType> = HashMap()
    override val idToShipData: Map<ShipId, ShipDataType> = _idToShipData
    val chunkClaimToShipData: ChunkClaimMap<ShipDataType> = ChunkClaimMap()

    init {
        data.forEach(::addShipData)
    }

    override fun iterator(): Iterator<ShipDataType> {
        return _idToShipData.values.iterator()
    }

    override fun getById(uuid: ShipId): ShipDataType? {
        return _idToShipData[uuid]
    }

    override fun getShipDataFromChunkPos(chunkX: Int, chunkZ: Int): ShipDataType? {
        return chunkClaimToShipData.get(chunkX, chunkZ)
    }

    override fun addShipData(shipData: ShipDataType) {
        if (getById(shipData.id) != null) {
            throw IllegalArgumentException("Adding shipData $shipData failed because of duplicated UUID.")
        }
        _idToShipData[shipData.id] = shipData
        chunkClaimToShipData.set(shipData.chunkClaim, shipData)
    }

    override fun removeShipData(shipData: ShipDataType) {
        if (getById(shipData.id) == null) {
            throw IllegalArgumentException("Removing $shipData failed because it wasn't in the UUID map.")
        }
        _idToShipData.remove(shipData.id)
        chunkClaimToShipData.remove(shipData.chunkClaim)
    }

    override fun getShipDataIntersecting(aabb: AABBdc): Iterator<ShipDataType> {
        // TODO("Use https://github.com/tzaeschke/phtree")
        return _idToShipData.values
            .filter { it.shipAABB.intersectsAABB(aabb as AABBd) }
            .iterator()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QueryableShipDataImpl<*>

        if (idToShipData != other.idToShipData) return false

        return true
    }

    override fun hashCode(): Int {
        return idToShipData.hashCode()
    }
}
