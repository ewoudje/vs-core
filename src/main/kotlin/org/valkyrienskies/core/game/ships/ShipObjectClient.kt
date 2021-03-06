package org.valkyrienskies.core.game.ships

class ShipObjectClient(shipData: ShipDataCommon) : ShipObject(shipData) {
    val renderTransform get() = shipData.shipTransform
}
