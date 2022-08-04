package org.valkyrienskies.core.game.ships.loading

import org.valkyrienskies.core.chunk_tracking.ShipObjectServerWorldChunkTracker
import org.valkyrienskies.core.game.IPlayer
import org.valkyrienskies.core.game.ships.ShipData
import org.valkyrienskies.core.game.ships.networking.ShipObjectNetworkManagerServer
import javax.inject.Inject

class ShipLoadManagerServer @Inject internal constructor(
    private val tracker: ShipObjectServerWorldChunkTracker,
    private val networkManager: ShipObjectNetworkManagerServer
) {

    fun tick(
        players: Set<IPlayer>,
        lastTickPlayer: Set<IPlayer>,
        ships: Iterable<ShipData>,
        deletedShips: Iterable<ShipData>
    ) {
        val trackingInfo = tracker.updateTracking(players, lastTickPlayer, ships, deletedShips)
        networkManager.tick(players, trackingInfo)

        // todo queue ship load/unloads
    }
}
