package org.valkyrienskies.core.game.ships

import org.joml.primitives.AABBdc
import org.valkyrienskies.core.game.DimensionId
import org.valkyrienskies.core.game.VSBlockType
import org.valkyrienskies.core.util.intersectsAABBImmutable

/**
 * Manages all the [ShipObject]s in a world.
 */
abstract class ShipObjectWorld<ShipObjectType : ShipObject>(
    open val queryableShipData: QueryableShipDataCommon,
) {

    abstract val shipObjects: Map<ShipId, ShipObjectType>

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

    open fun getShipObjectsIntersecting(aabb: AABBdc): List<ShipObjectType> =
        shipObjects.values.filter { it.shipData.shipAABB.intersectsAABBImmutable(aabb) }.toCollection(ArrayList())

    abstract fun destroyWorld()
}
