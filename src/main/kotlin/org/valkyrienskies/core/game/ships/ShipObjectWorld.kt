package org.valkyrienskies.core.game.ships

import org.valkyrienskies.core.game.DimensionId
import org.valkyrienskies.core.game.VSBlockType

/**
 * Manages all the [ShipObject]s in a world.
 */
abstract class ShipObjectWorld(
    open val queryableShipData: QueryableShipDataCommon,
) {

    abstract val shipObjects: Map<ShipId, ShipObject>

    open fun onSetBlock(
        posX: Int,
        posY: Int,
        posZ: Int,
        dimensionId: DimensionId,
        oldBlockType: VSBlockType,
        newBlockType: VSBlockType,
        oldBlockMass: Double,
        newBlockMass: Double
    ) {
        // If there is a ShipData at this position and dimension, then tell it about the block update
        queryableShipData.getShipDataFromChunkPos(posX shr 4, posZ shr 4, dimensionId)
            ?.onSetBlock(posX, posY, posZ, oldBlockType, newBlockType, oldBlockMass, newBlockMass)
    }

    abstract fun destroyWorld()
}
