package org.valkyrienskies.core.game.ships

import dagger.Component
import org.valkyrienskies.core.game.ships.networking.ShipObjectNetworkManagerClient
import org.valkyrienskies.core.networking.VSNetworking.NetworkingModule
import javax.inject.Inject

class ShipObjectClientWorld @Inject constructor(
    networkManagerFactory: ShipObjectNetworkManagerClient.Factory
) : ShipObjectWorld<ShipObjectClient>() {

    @Component(modules = [NetworkingModule::class])
    interface Factory {
        fun make(): ShipObjectClientWorld
    }

    override val queryableShipData: MutableQueryableShipDataCommon = QueryableShipDataImpl()

    private val shipObjectMap = HashMap<ShipId, ShipObjectClient>()
    override val shipObjects: Map<ShipId, ShipObjectClient> = shipObjectMap

    val networkManager: ShipObjectNetworkManagerClient = networkManagerFactory.make(this)

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
