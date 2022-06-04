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
    companion object {
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
