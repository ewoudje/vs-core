package org.valkyrienskies.core.game

import org.joml.primitives.AABBdc
import org.valkyrienskies.core.datastructures.IBlockPosSet
import org.valkyrienskies.core.datastructures.IBlockPosSetAABB
import java.util.*

data class ShipData(
    val shipUUID: UUID,
    var name: String,
    val chunkClaim: ChunkClaim,
    val physicsData: ShipPhysicsData,
    val inertiaData: ShipInertiaData,
    var shipTransform: ShipTransform,
    var prevTickShipTransform: ShipTransform,
    var shipAABB: AABBdc,
    val blockPositionSet: IBlockPosSetAABB,
    val forceBlockPositionsSet: IBlockPosSet
) {

}