package org.valkyrienskies.core.game.ships

class ShipObjectClient(val shipDataClient: ShipDataClient) : ShipObject(shipDataClient) {
    val renderTransform get() = shipDataClient.renderShipTransform
}
