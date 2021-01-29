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
    internal fun onSetBlock(posX: Int, posY: Int, posZ: Int, blockType: VSBlockType, oldBlockMass: Double, newBlockMass: Double) {
        // Update [blockPositionsSet]
        if (blockType != VSBlockType.AIR) {
            blockPositionSet.add(posX, posY, posZ)
        } else {
            blockPositionSet.remove(posX, posY, posZ)
        }

        // Update [inertiaData]
        inertiaData.onSetBlock(posX, posY, posZ, oldBlockMass, newBlockMass)

        // Update [forceBlockPositionsSet]
        // TODO: Add support for forceBlockPositionsSet eventually
    }
}