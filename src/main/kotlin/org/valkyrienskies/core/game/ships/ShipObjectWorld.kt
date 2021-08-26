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

    open fun onSetBlock(
        posX: Int,
        posY: Int,
        posZ: Int,
        oldBlockType: VSBlockType,
        newBlockType: VSBlockType,
        oldBlockMass: Double,
        newBlockMass: Double
    ) {
        // If there is a ShipData at this position, then tell it about the block update
        queryableShipData.getShipDataFromChunkPos(posX shr 4, posZ shr 4)
            ?.onSetBlock(posX, posY, posZ, oldBlockType, newBlockType, oldBlockMass, newBlockMass)
    }

    abstract fun destroyWorld()
}
