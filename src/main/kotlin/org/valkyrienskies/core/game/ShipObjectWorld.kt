package org.valkyrienskies.core.game

import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.core.util.names.NounListNameGenerator
import java.util.*
import kotlin.collections.HashMap

/**
 * Manages all the [ShipObject]s in a world.
 */
class ShipObjectWorld(val queryableShipData: QueryableShipData, val chunkAllocator: ChunkAllocator) {

    val uuidToShipObjectMap = HashMap<UUID, ShipObject>()

    fun tick() {

    }

    /**
     * Creates a new [ShipData] centered at the block at [blockPosInWorldCoordinates].
     *
     * If [createShipObjectImmediately] is true then a [ShipObject] will be created immediately.
     */
    fun createNewShipAtBlock(blockPosInWorldCoordinates: Vector3ic, createShipObjectImmediately: Boolean): ShipData {
        val chunkClaim = chunkAllocator.allocateNewChunkClaim()
        val shipName = NounListNameGenerator.generateName()
        val shipCenterInWorldCoordinates: Vector3dc = Vector3d(blockPosInWorldCoordinates.x() + .5, blockPosInWorldCoordinates.y() + .5, blockPosInWorldCoordinates.z() + .5)

        val blockPosInShipCoordinates: Vector3ic = chunkClaim.getCenterBlockCoordinates(Vector3i())

        val shipCenterInShipCoordinates: Vector3dc = Vector3d(blockPosInShipCoordinates.x() + .5, blockPosInShipCoordinates.y() + .5, blockPosInShipCoordinates.z() + .5)

        val newShipData = ShipData.newEmptyShipData(
            name = shipName,
            chunkClaim = chunkClaim,
            shipCenterInWorldCoordinates = shipCenterInWorldCoordinates,
            shipCenterInShipCoordinates = shipCenterInShipCoordinates
        )

        queryableShipData.addShipData(newShipData)

        if (createShipObjectImmediately) {
            TODO("Not implemented")
        }

        return newShipData
    }

    fun onSetBlock(posX: Int, posY: Int, posZ: Int, blockType: VSBlockType, oldBlockMass: Double, newBlockMass: Double) {
        // If there is a ShipData at this position, then tell it about the block update
        queryableShipData.getShipDataFromChunkPos(posX shr 4, posZ shr 4)?.onSetBlock(posX, posY, posZ, blockType, oldBlockMass, newBlockMass)

        // TODO: Update the physics voxel world here
        // voxelWorld.onSetBlock(posX, posY, posZ, blockType)
    }

}