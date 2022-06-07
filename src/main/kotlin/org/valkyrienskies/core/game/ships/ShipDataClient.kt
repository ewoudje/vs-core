package org.valkyrienskies.core.game.ships

import org.joml.primitives.AABBdc
import org.valkyrienskies.core.chunk_tracking.IShipActiveChunksSet
import org.valkyrienskies.core.game.ChunkClaim
import org.valkyrienskies.core.game.DimensionId
import java.util.UUID

class ShipDataClient(
    shipUUID: UUID,
    name: String,
    chunkClaim: ChunkClaim,
    chunkClaimDimension: DimensionId,
    physicsData: ShipPhysicsData,
    shipTransform: ShipTransform,
    prevTickShipTransform: ShipTransform,
    shipAABB: AABBdc,
    shipActiveChunksSet: IShipActiveChunksSet
) : ShipDataCommon(
    shipUUID, name, chunkClaim, chunkClaimDimension, physicsData, shipTransform, prevTickShipTransform,
    shipAABB, shipActiveChunksSet
) {
    companion object {
        fun createShipDataClientFromShipDataCommon(shipDataCommon: ShipDataCommon): ShipDataClient {
            return ShipDataClient(
                shipDataCommon.shipUUID,
                shipDataCommon.name,
                shipDataCommon.chunkClaim,
                shipDataCommon.chunkClaimDimension,
                shipDataCommon.physicsData,
                shipDataCommon.shipTransform,
                shipDataCommon.prevTickShipTransform,
                shipDataCommon.shipAABB,
                shipDataCommon.shipActiveChunksSet
            )
        }
    }
}
