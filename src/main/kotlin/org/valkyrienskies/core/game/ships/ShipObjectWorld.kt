package org.valkyrienskies.core.game.ships

import org.valkyrienskies.core.game.VSBlockType
import java.util.UUID

/**
 * Manages all the [ShipObject]s in a world.
 */
abstract class ShipObjectWorld(
    open val queryableShipData: QueryableShipDataClient,
) {

    abstract val shipObjects: Map<UUID, ShipObject>

    fun onSetBlock(
        posX: Int,
        posY: Int,
        posZ: Int,
        blockType: VSBlockType,
        oldBlockMass: Double,
        newBlockMass: Double
    ) {
        // If there is a ShipData at this position, then tell it about the block update
        queryableShipData.getShipDataFromChunkPos(posX shr 4, posZ shr 4)
            ?.onSetBlock(posX, posY, posZ, blockType, oldBlockMass, newBlockMass)

        // TODO: Update the physics voxel world here
        // voxelWorld.onSetBlock(posX, posY, posZ, blockType)
    }
}
