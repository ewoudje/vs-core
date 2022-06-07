package org.valkyrienskies.core.game.ships

import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.primitives.AABBd
import org.joml.primitives.AABBdc
import org.valkyrienskies.core.chunk_tracking.IShipActiveChunksSet
import org.valkyrienskies.core.chunk_tracking.ShipActiveChunksSet
import org.valkyrienskies.core.datastructures.IBlockPosSet
import org.valkyrienskies.core.game.ChunkClaim
import org.valkyrienskies.core.game.DimensionId
import org.valkyrienskies.core.game.VSBlockType
import org.valkyrienskies.core.util.serialization.VSPacketIgnore
import java.util.UUID

open class ShipDataCommon(
    val shipUUID: UUID,
    var name: String,
    val chunkClaim: ChunkClaim,
    val chunkClaimDimension: DimensionId,
    val physicsData: ShipPhysicsData,
    @VSPacketIgnore
    var shipTransform: ShipTransform,
    @VSPacketIgnore
    var prevTickShipTransform: ShipTransform,
    @VSPacketIgnore
    var shipAABB: AABBdc,
    val shipActiveChunksSet: IShipActiveChunksSet
) {
    /**
     * Updates the [IBlockPosSet] and [ShipInertiaData] for this [ShipData]
     */
    internal open fun onSetBlock(
        posX: Int,
        posY: Int,
        posZ: Int,
        oldBlockType: VSBlockType,
        newBlockType: VSBlockType,
        oldBlockMass: Double,
        newBlockMass: Double
    ) {
        // Sanity check
        require(
            chunkClaim.contains(
                posX shr 4,
                posZ shr 4
            )
        ) { "Block at <$posX, $posY, $posZ> is not in the chunk claim belonging to $this" }

        // Add the chunk to the active chunk set
        shipActiveChunksSet.addChunkPos(posX shr 4, posZ shr 4)
        // Add the neighbors too (Required for rendering code in MC 1.16, chunks without neighbors won't render)
        // TODO: Make a separate set for keeping track of neighbors
        shipActiveChunksSet.addChunkPos((posX shr 4) - 1, (posZ shr 4))
        shipActiveChunksSet.addChunkPos((posX shr 4) + 1, (posZ shr 4))
        shipActiveChunksSet.addChunkPos((posX shr 4), (posZ shr 4) - 1)
        shipActiveChunksSet.addChunkPos((posX shr 4), (posZ shr 4) + 1)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShipDataCommon

        if (shipUUID != other.shipUUID) return false
        if (name != other.name) return false
        if (chunkClaim != other.chunkClaim) return false
        if (physicsData != other.physicsData) return false
        if (shipTransform != other.shipTransform) return false
        if (prevTickShipTransform != other.prevTickShipTransform) return false
        if (shipAABB != other.shipAABB) return false
        if (shipActiveChunksSet != other.shipActiveChunksSet) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shipUUID.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + chunkClaim.hashCode()
        result = 31 * result + physicsData.hashCode()
        result = 31 * result + shipTransform.hashCode()
        result = 31 * result + prevTickShipTransform.hashCode()
        result = 31 * result + shipAABB.hashCode()
        result = 31 * result + shipActiveChunksSet.hashCode()
        return result
    }

    companion object {
        /**
         * Creates a new [ShipData] from the given name and coordinates. The resulting [ShipData] is completely empty,
         * so it must be filled with blocks by other code.
         */
        internal fun createEmpty(
            name: String,
            chunkClaim: ChunkClaim,
            chunkClaimDimension: DimensionId,
            shipCenterInWorldCoordinates: Vector3dc,
            shipCenterInShipCoordinates: Vector3dc
        ): ShipDataCommon {
            val shipTransform = ShipTransform.createFromCoordinatesAndRotationAndScaling(
                shipCenterInWorldCoordinates,
                shipCenterInShipCoordinates,
                Quaterniond().fromAxisAngleDeg(0.0, 1.0, 0.0, 45.0),
                Vector3d(.5, .5, .5)
            )

            return ShipDataCommon(
                shipUUID = UUID.randomUUID(),
                name = name,
                chunkClaim = chunkClaim,
                chunkClaimDimension = chunkClaimDimension,
                physicsData = ShipPhysicsData.createEmpty(),
                shipTransform = shipTransform,
                prevTickShipTransform = shipTransform,
                shipAABB = AABBd(),
                shipActiveChunksSet = ShipActiveChunksSet.create()
            )
        }
    }
}
