package org.valkyrienskies.core.game.ships

class ShipObjectClient(shipData: ShipDataClient) : ShipObject(shipData) {
    val renderTransform get() = shipData.shipTransform
}
