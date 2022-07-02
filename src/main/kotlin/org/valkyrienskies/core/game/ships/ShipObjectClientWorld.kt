package org.valkyrienskies.core.game.ships

import org.valkyrienskies.core.game.ships.networking.ShipObjectNetworkManagerClient

class ShipObjectClientWorld(
    override val queryableShipData: MutableQueryableShipDataCommon
) : ShipObjectWorld(queryableShipData) {

    private val shipObjectMap = HashMap<ShipId, ShipObjectClient>()
    override val shipObjects: Map<ShipId, ShipObjectClient> = shipObjectMap
    private val networkManager = ShipObjectNetworkManagerClient(this)

    init {
        networkManager.registerPacketListeners()
    }

    fun addShip(ship: ShipDataCommon) {
        queryableShipData.addShipData(ship)
        shipObjectMap[ship.id] = ShipObjectClient(ship)
    }

    public override fun tickShips() {
        super.tickShips()

        shipObjects.forEach { (_, shipObjectClient) ->
            shipObjectClient.tickUpdateShipTransform()
        }
    }

    override fun destroyWorld() {
        networkManager.onDestroy()
    }
}
