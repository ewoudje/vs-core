package org.valkyrienskies.core.game.ships

class ShipObjectClientWorld(
    override val queryableShipData: MutableQueryableShipDataCommon
) : ShipObjectWorld<ShipObjectClient>(queryableShipData) {

    private val shipObjectMap = HashMap<ShipId, ShipObjectClient>()
    override val shipObjects: Map<ShipId, ShipObjectClient> = shipObjectMap

    fun tickShips() {
        // For now, just make a [ShipObject] for every [ShipData]
        for (shipData in queryableShipData) {
            val shipID = shipData.id
            shipObjectMap.computeIfAbsent(shipID) { ShipObjectClient(shipData) }
        }

        shipObjects.forEach { (_, shipObjectClient) ->
            shipObjectClient.tickUpdateShipTransform()
        }
    }

    override fun destroyWorld() {
        // Do nothing for now
    }
}
