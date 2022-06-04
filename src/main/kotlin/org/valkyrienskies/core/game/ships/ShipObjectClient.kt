package org.valkyrienskies.core.game.ships

class ShipObjectClient(shipData: ShipDataCommon) : ShipObject(shipData) {
    // The last ship transform sent by the sever
    private var nextShipTransform: ShipTransform

    // The transform used when rendering the ship
    var renderTransform: ShipTransform
        private set

    init {
        nextShipTransform = shipData.shipTransform
        renderTransform = shipData.shipTransform
    }

    fun updateNextShipTransform(nextShipTransform: ShipTransform) {
        this.nextShipTransform = nextShipTransform
    }

    fun tickUpdateShipTransform() {
        shipData.prevTickShipTransform = shipData.shipTransform
        shipData.shipTransform = ShipTransform.createFromSlerp(shipData.shipTransform, nextShipTransform, EMA_ALPHA)
    }

    fun updateRenderShipTransform(partialTicks: Double) {
        renderTransform =
            ShipTransform.createFromSlerp(shipData.prevTickShipTransform, shipData.shipTransform, partialTicks)
    }

    companion object {
        // Higher values will converge to the transforms sent by the server faster, but lower values make movement
        // smoother. Ideally this should be configurable, but leave it constant for now.
        private const val EMA_ALPHA = 0.7
    }
}
