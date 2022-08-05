package org.valkyrienskies.core.game.ships

import org.valkyrienskies.core.game.ships.networking.ShipObjectNetworkManagerClient

class ShipObjectClientWorld(
    override val queryableShipData: MutableQueryableShipDataCommon
) : ShipObjectWorld<ShipObjectClient>() {

    private val shipObjectMap = HashMap<ShipId, ShipObjectClient>()
    override val shipObjects: Map<ShipId, ShipObjectClient> = shipObjectMap
    val networkManager = ShipObjectNetworkManagerClient(this)

    init {
        networkManager.registerPacketListeners()
    }

    fun addShip(ship: ShipDataCommon) {
        queryableShipData.addShipData(ship)
        shipObjectMap[ship.id] = ShipObjectClient(ship)
    }

    fun removeShip(shipId: ShipId) {
        queryableShipData.removeShipData(shipId)
        shipObjectMap.remove(shipId)
    }

    public override fun preTick() {
        super.preTick()

        shipObjects.forEach { (_, shipObjectClient) ->
            shipObjectClient.tickUpdateShipTransform()
        }
    }

    override fun destroyWorld() {
        networkManager.onDestroy()
    }
}
