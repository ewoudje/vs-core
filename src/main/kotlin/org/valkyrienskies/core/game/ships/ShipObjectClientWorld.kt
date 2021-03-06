package org.valkyrienskies.core.game.ships

import java.util.UUID

class ShipObjectClientWorld(
    override val queryableShipData: MutableQueryableShipDataClient
) : ShipObjectWorld(queryableShipData) {

    private val shipObjectMap = HashMap<UUID, ShipObjectClient>()
    override val shipObjects: Map<UUID, ShipObjectClient> = shipObjectMap

    fun tickShips() {
        // For now, just make a [ShipObject] for every [ShipData]
        for (shipData in queryableShipData) {
            val shipID = shipData.shipUUID
            shipObjectMap.computeIfAbsent(shipID) { ShipObjectClient(shipData) }
        }
    }
}
