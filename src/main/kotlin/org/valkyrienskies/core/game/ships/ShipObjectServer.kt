package org.valkyrienskies.core.game.ships

import org.valkyrienskies.core.chunk_tracking.IShipChunkTracker
import org.valkyrienskies.core.chunk_tracking.ShipChunkTracker

class ShipObjectServer(
    override val shipData: ShipData,
    // val rigidBody: RigidBodyReference TODO: Put something else here, we don't want references accidentally being used in the wrong thread
) : ShipObject(shipData) {

    internal val shipChunkTracker: IShipChunkTracker =
        ShipChunkTracker(shipData.shipActiveChunksSet, DEFAULT_CHUNK_WATCH_DISTANCE, DEFAULT_CHUNK_UNWATCH_DISTANCE)

    companion object {
        private const val DEFAULT_CHUNK_WATCH_DISTANCE = 128.0
        private const val DEFAULT_CHUNK_UNWATCH_DISTANCE = 192.0
    }
}
