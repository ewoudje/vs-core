package org.valkyrienskies.core.game.ships

import org.joml.primitives.AABBdc
import org.valkyrienskies.core.chunk_tracking.IShipActiveChunksSet
import org.valkyrienskies.core.game.ChunkClaim
import java.util.UUID

class ShipDataClient(
    shipUUID: UUID,
    name: String,
    chunkClaim: ChunkClaim,
    physicsData: ShipPhysicsData,
    shipTransform: ShipTransform,
    prevTickShipTransform: ShipTransform,
    shipAABB: AABBdc,
    shipActiveChunksSet: IShipActiveChunksSet
) : ShipDataCommon(
    shipUUID, name, chunkClaim, physicsData, shipTransform, prevTickShipTransform,
    shipAABB, shipActiveChunksSet
) {
    // The last ship transform sent by the sever
    private var nextShipTransform: ShipTransform
    // The transform used when rendering the ship
    var renderShipTransform: ShipTransform
        private set

    init {
        nextShipTransform = shipTransform
        renderShipTransform = shipTransform
    }

    fun updateNextShipTransform(nextShipTransform: ShipTransform) {
        this.nextShipTransform = nextShipTransform
    }

    fun tickUpdateShipTransform() {
        prevTickShipTransform = shipTransform
        shipTransform = ShipTransform.createFromSlerp(shipTransform, nextShipTransform, EMA_ALPHA)
    }

    fun updateRenderShipTransform(partialTicks: Double) {
        renderShipTransform = ShipTransform.createFromSlerp(prevTickShipTransform, shipTransform, partialTicks)
    }

    companion object {
        // Higher values will converge to the transforms sent by the server faster, but lower values make movement
        // smoother. Ideally this should be configurable, but leave it constant for now.
        private const val EMA_ALPHA = 0.7
        fun createShipDataClientFromShipDataCommon(shipDataCommon: ShipDataCommon): ShipDataClient {
            return ShipDataClient(
                shipDataCommon.shipUUID,
                shipDataCommon.name,
                shipDataCommon.chunkClaim,
                shipDataCommon.physicsData,
                shipDataCommon.shipTransform,
                shipDataCommon.prevTickShipTransform,
                shipDataCommon.shipAABB,
                shipDataCommon.shipActiveChunksSet
            )
        }
    }
}
