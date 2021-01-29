package org.valkyrienskies.core.game

import java.util.*
import kotlin.collections.HashMap

class ShipObjectWorld(val queryableShipData: QueryableShipData) {

    val uuidToShipObjectMap = HashMap<UUID, ShipObject>()

    fun onSetBlock(posX: Int, posY: Int, posZ: Int, blockType: VSBlockType, oldBlockMass: Double, newBlockMass: Double) {
        // If there is a ShipData at this position, then tell it about the block update
        queryableShipData.getShipDataFromChunkPos(posX shr 4, posZ shr 4)?.onSetBlock(posX, posY, posZ, blockType, oldBlockMass, newBlockMass)

        // TODO: Update the physics voxel world here
        // voxelWorld.onSetBlock(posX, posY, posZ, blockType)
    }

}