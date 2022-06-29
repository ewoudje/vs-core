package org.valkyrienskies.core.game.ships

import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.core.chunk_tracking.ChunkUnwatchTask
import org.valkyrienskies.core.chunk_tracking.ChunkWatchTask
import org.valkyrienskies.core.chunk_tracking.ShipObjectServerWorldChunkTracker
import org.valkyrienskies.core.game.ChunkAllocator
import org.valkyrienskies.core.game.DimensionId
import org.valkyrienskies.core.game.IPlayer
import org.valkyrienskies.core.game.VSBlockType
import org.valkyrienskies.core.util.names.NounListNameGenerator
import org.valkyrienskies.physics_api.voxel_updates.DenseVoxelShapeUpdate
import org.valkyrienskies.physics_api.voxel_updates.EmptyVoxelShapeUpdate
import org.valkyrienskies.physics_api.voxel_updates.IVoxelShapeUpdate
import org.valkyrienskies.physics_api.voxel_updates.KrunchVoxelStates
import org.valkyrienskies.physics_api.voxel_updates.SparseVoxelShapeUpdate
import java.util.Collections
import java.util.Spliterator

class ShipObjectServerWorld(
    override val queryableShipData: MutableQueryableShipDataServer,
    val chunkAllocator: ChunkAllocator,
) : ShipObjectWorld(queryableShipData) {

    var lastPlayersSet: Set<IPlayer> = setOf()
        private set

    var players: Set<IPlayer> = setOf()

    private val shipObjectMap = HashMap<ShipId, ShipObjectServer>()

    // Explicitly make [shipObjects] a MutableMap so that we can use Iterator::remove()
    override val shipObjects: MutableMap<ShipId, ShipObjectServer> = shipObjectMap

    private val dimensionToGroundBodyId: MutableMap<DimensionId, ShipId> = HashMap()

    // An immutable view of [dimensionToGroundBodyId]
    val dimensionToGroundBodyIdImmutable: Map<DimensionId, ShipId>
        get() = dimensionToGroundBodyId

    private val dimensionsAddedThisTick = ArrayList<DimensionId>()
    private val dimensionsRemovedThisTick = ArrayList<DimensionId>()

    private val newLoadedChunksList = ArrayList<Pair<DimensionId, List<IVoxelShapeUpdate>>>()

    // These fields are used to generate [VSGameFrame]
    private val newShipObjects: MutableList<ShipObjectServer> = ArrayList()
    private val updatedShipObjects: MutableList<ShipObjectServer> = ArrayList()
    private val deletedShipObjects: MutableList<ShipId> = ArrayList()

    /**
     * A map of voxel updates pending to be applied to ships.
     *
     * These updates will be sent to the physics engine, however they are not applied immediately. The physics engine
     * has full control of when the updates are applied.
     */
    private val shipToVoxelUpdates: MutableMap<ShipId, MutableMap<Vector3ic, IVoxelShapeUpdate>> = HashMap()

    private val chunkTracker =
        ShipObjectServerWorldChunkTracker(this, DEFAULT_CHUNK_WATCH_DISTANCE, DEFAULT_CHUNK_UNWATCH_DISTANCE)

    companion object {
        private const val DEFAULT_CHUNK_WATCH_DISTANCE = 12.0 // 128.0
        private const val DEFAULT_CHUNK_UNWATCH_DISTANCE = 24.0 // 192.0
    }

    /**
     * Add the update to [shipToVoxelUpdates].
     */
    override fun onSetBlock(
        posX: Int,
        posY: Int,
        posZ: Int,
        dimensionId: DimensionId,
        oldBlockType: VSBlockType,
        newBlockType: VSBlockType,
        oldBlockMass: Double,
        newBlockMass: Double
    ) {
        super.onSetBlock(posX, posY, posZ, dimensionId, oldBlockType, newBlockType, oldBlockMass, newBlockMass)

        if (oldBlockType != newBlockType) {
            val chunkPos: Vector3ic = Vector3i(posX shr 4, posY shr 4, posZ shr 4)

            val shipData: ShipData? = queryableShipData.getShipDataFromChunkPos(chunkPos.x(), chunkPos.z(), dimensionId)

            val shipId: ShipId = shipData?.id ?: dimensionToGroundBodyId[dimensionId]!!
            val voxelUpdates = shipToVoxelUpdates.getOrPut(shipId) { HashMap() }

            val voxelShapeUpdate =
                voxelUpdates.getOrPut(chunkPos) { SparseVoxelShapeUpdate.createSparseVoxelShapeUpdate(chunkPos) }

            val voxelType: Byte = when (newBlockType) {
                VSBlockType.AIR -> KrunchVoxelStates.AIR_STATE
                VSBlockType.SOLID -> KrunchVoxelStates.SOLID_STATE
                VSBlockType.WATER -> KrunchVoxelStates.WATER_STATE
                VSBlockType.LAVA -> KrunchVoxelStates.LAVA_STATE
                else -> throw IllegalArgumentException("Unknown blockType $newBlockType")
            }

            when (voxelShapeUpdate) {
                is SparseVoxelShapeUpdate -> {
                    // Add the update to the sparse voxel update
                    voxelShapeUpdate.addUpdate(posX and 15, posY and 15, posZ and 15, voxelType)
                }
                is DenseVoxelShapeUpdate -> {
                    // Add the update to the dense voxel update
                    voxelShapeUpdate.setVoxel(posX and 15, posY and 15, posZ and 15, voxelType)
                }
                is EmptyVoxelShapeUpdate -> {
                    // Replace the empty voxel update with a sparse update
                    val newVoxelShapeUpdate = SparseVoxelShapeUpdate.createSparseVoxelShapeUpdate(chunkPos)
                    newVoxelShapeUpdate.addUpdate(posX and 15, posY and 15, posZ and 15, voxelType)
                    voxelUpdates[chunkPos] = newVoxelShapeUpdate
                }
            }
        }
    }

    fun addNewLoadedChunks(dimensionId: DimensionId, newLoadedChunks: List<IVoxelShapeUpdate>) {
        newLoadedChunksList.add(Pair(dimensionId, newLoadedChunks))
    }

    fun tickShips() {
        val it = shipObjects.iterator()
        while (it.hasNext()) {
            val shipObjectServer = it.next().value
            if (shipObjectServer.shipData.inertiaData.getShipMass() < 1e-8) {
                // Delete this ship
                deletedShipObjects.add(shipObjectServer.shipData.id)
                queryableShipData.removeShipData(shipObjectServer.shipData)
                shipToVoxelUpdates.remove(shipObjectServer.shipData.id)
                it.remove()
            }
        }

        // For now just update very ship object every tick
        shipObjects.forEach { (_, shipObjectServer) ->
            updatedShipObjects.add(shipObjectServer)
        }

        // For now, just make a [ShipObject] for every [ShipData]
        for (shipData in queryableShipData) {
            val shipID = shipData.id
            if (!shipObjectMap.containsKey(shipID)) {
                val newShipObject = ShipObjectServer(shipData)
                newShipObjects.add(newShipObject)
                shipObjectMap[shipID] = newShipObject
            }
        }

        // region Add voxel shape updates for chunks that loaded this tick
        for (newLoadedChunkAndDimension in newLoadedChunksList) {
            val (dimensionId, shapeUpdates) = newLoadedChunkAndDimension
            for (newLoadedChunk in shapeUpdates) {
                val chunkPos: Vector3ic =
                    Vector3i(newLoadedChunk.regionX, newLoadedChunk.regionY, newLoadedChunk.regionZ)
                val shipData: ShipData? =
                    queryableShipData.getShipDataFromChunkPos(chunkPos.x(), chunkPos.z(), dimensionId)

                val shipId: ShipId = shipData?.id ?: dimensionToGroundBodyId[dimensionId]!!

                val voxelUpdates = shipToVoxelUpdates.getOrPut(shipId) { HashMap() }
                voxelUpdates[chunkPos] = newLoadedChunk
            }
        }
        // endregion
    }

    /**
     * If the chunk at [chunkX], [chunkZ] is a ship chunk, then this returns the [IPlayer]s that are watching that ship chunk.
     *
     * If the chunk at [chunkX], [chunkZ] is not a ship chunk, then this returns nothing.
     */
    fun getIPlayersWatchingShipChunk(chunkX: Int, chunkZ: Int, dimensionId: DimensionId): Iterator<IPlayer> {
        // Check if this chunk potentially belongs to a ship
        if (ChunkAllocator.isChunkInShipyard(chunkX, chunkZ)) {
            return chunkTracker.getPlayersWatchingChunk(chunkX, chunkZ, dimensionId).iterator()
        }
        return Collections.emptyIterator()
    }

    /**
     * Determines which ship chunks should be watched/unwatched by the players.
     *
     * It only returns the tasks, it is up to the caller to execute the tasks; however they do not have to execute all of them.
     * It is up to the caller to decide which tasks to execute, and which ones to skip.
     */
    fun tickShipChunkLoading(): Pair<Spliterator<ChunkWatchTask>, Spliterator<ChunkUnwatchTask>> {
        chunkTracker.updateTracking(players)

        return Pair(chunkTracker.chunkWatchTasks.spliterator(), chunkTracker.chunkUnwatchTasks.spliterator())
    }

    /**
     * Creates a new [ShipData] centered at the block at [blockPosInWorldCoordinates].
     *
     * If [createShipObjectImmediately] is true then a [ShipObject] will be created immediately.
     */
    fun createNewShipAtBlock(
        blockPosInWorldCoordinates: Vector3ic, createShipObjectImmediately: Boolean, scaling: Double = 1.0,
        dimensionId: DimensionId
    ): ShipData {
        val chunkClaim = chunkAllocator.allocateNewChunkClaim()
        val shipName = NounListNameGenerator.generateName()

        val shipCenterInWorldCoordinates: Vector3dc = Vector3d(blockPosInWorldCoordinates).add(0.5, 0.5, 0.5)
        val blockPosInShipCoordinates: Vector3ic = chunkClaim.getCenterBlockCoordinates(Vector3i())
        val shipCenterInShipCoordinates: Vector3dc = Vector3d(blockPosInShipCoordinates).add(0.5, 0.5, 0.5)
        val newShipData = ShipData.createEmpty(
            name = shipName,
            chunkClaim = chunkClaim,
            shipId = chunkAllocator.allocateShipId(),
            shipCenterInWorldCoordinates = shipCenterInWorldCoordinates,
            shipCenterInShipCoordinates = shipCenterInShipCoordinates,
            scaling = scaling,
            chunkClaimDimension = dimensionId
        )

        queryableShipData.addShipData(newShipData)

        if (createShipObjectImmediately) {
            TODO("Not implemented")
        }

        return newShipData
    }

    override fun destroyWorld() {
    }

    fun getNewGroundRigidBodyObjects(): List<Pair<DimensionId, ShipId>> {
        val newDimensionsObjects = ArrayList<Pair<DimensionId, ShipId>>(dimensionsAddedThisTick.size)
        dimensionsAddedThisTick.forEach { dimensionId: DimensionId ->
            newDimensionsObjects.add(Pair(dimensionId, dimensionToGroundBodyId[dimensionId]!!))
        }
        return newDimensionsObjects
    }

    fun getNewShipObjects(): List<ShipObjectServer> {
        return newShipObjects
    }

    fun getUpdatedShipObjects(): List<ShipObjectServer> {
        return updatedShipObjects
    }

    fun getDeletedShipObjects(): List<ShipId> {
        val deletedGroundShips = ArrayList<ShipId>()
        dimensionsRemovedThisTick.forEach { dimensionRemovedThisTick: DimensionId ->
            deletedGroundShips.add(dimensionToGroundBodyId[dimensionRemovedThisTick]!!)
        }
        return deletedGroundShips + deletedShipObjects
    }

    fun getShipToVoxelUpdates(): Map<ShipId, Map<Vector3ic, IVoxelShapeUpdate>> {
        return shipToVoxelUpdates
    }

    fun clearNewUpdatedDeletedShipObjectsAndVoxelUpdates() {
        newShipObjects.clear()
        updatedShipObjects.clear()
        deletedShipObjects.clear()
        shipToVoxelUpdates.clear()
        newLoadedChunksList.clear()
        dimensionsAddedThisTick.clear()
        dimensionsRemovedThisTick.forEach { dimensionRemovedThisTick: DimensionId ->
            val removedSuccessfully = dimensionToGroundBodyId.remove(dimensionRemovedThisTick) != null
            assert(removedSuccessfully)
        }
        dimensionsRemovedThisTick.clear()
    }

    fun addDimension(dimensionId: DimensionId) {
        assert(!dimensionToGroundBodyId.contains(dimensionId))
        dimensionsAddedThisTick.add(dimensionId)
        dimensionToGroundBodyId[dimensionId] = chunkAllocator.allocateShipId()
    }

    fun removeDimension(dimensionId: DimensionId) {
        assert(dimensionToGroundBodyId.contains(dimensionId))
        dimensionsRemovedThisTick.add(dimensionId)
    }
}
