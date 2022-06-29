package org.valkyrienskies.core.game.ships

class ShipObjectServer(
    override val shipData: ShipData
) : ShipObject(shipData) {

    companion object {
        private const val DEFAULT_CHUNK_WATCH_DISTANCE = 128.0
        private const val DEFAULT_CHUNK_UNWATCH_DISTANCE = 192.0
    }
}
