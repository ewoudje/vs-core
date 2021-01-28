package org.valkyrienskies.core.game

import java.util.*

class QueryableShipData {

    private val uuidToShipData: MutableMap<UUID, ShipData>

    init {
        uuidToShipData = HashMap<UUID, ShipData>()
    }

    fun getAllShipData(): Iterator<ShipData> {
        return uuidToShipData.values.iterator()
    }

    fun getShipDataFromUUID(uuid: UUID): ShipData? {
        return uuidToShipData[uuid]
    }

    fun removeShipData(uuid: UUID) {
        uuidToShipData.remove(uuid)
    }

    /*
    fun getShipDataIntersecting(aabb: AABBdc): Iterator<ShipData> {
        TODO("Use https://github.com/tzaeschke/phtree")
    }
     */
}