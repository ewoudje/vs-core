package org.valkyrienskies.core.game.ships

import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.core.chunk_tracking.ChunkUnwatchTask
import org.valkyrienskies.core.chunk_tracking.ChunkWatchTask
import org.valkyrienskies.core.game.ChunkAllocator
import org.valkyrienskies.core.game.IPlayer
import org.valkyrienskies.core.game.VSBlockType
import org.valkyrienskies.core.pipelines.VSPipeline
import org.valkyrienskies.core.util.names.NounListNameGenerator
import org.valkyrienskies.physics_api.voxel_updates.*
import java.util.*

class ShipObjectServerWorld(
    override val queryableShipData: MutableQueryableShipDataServer,
    val chunkAllocator: ChunkAllocator,
    val dimension: Int
) : ShipObjectWorld(queryableShipData) {

    private var lastPlayersSet: Set<IPlayer> = setOf()
    private val shipObjectMap = HashMap<UUID, ShipObjectServer>()
    override val shipObjects: Map<UUID, ShipObjectServer> = shipObjectMap
    val groundBodyUUID: UUID = UUID.randomUUID() // The UUID used when sending voxel updates for the ground shape

    /**
     * A map of voxel updates pending to be applied to ships.
     *
     * These updates will be sent to the physics engine, however they are not applied immediately. The physics engine
     * has full control of when the updates are applied.
     */
    private val shipToVoxelUpdates: MutableMap<ShipData?, MutableMap<Vector3ic, IVoxelShapeUpdate>> = HashMap()

    /**
     * Add the update to [shipToVoxelUpdates].
     */
    override fun onSetBlock(
        posX: Int, posY: Int, posZ: Int, oldBlockType: VSBlockType, newBlockType: VSBlockType, oldBlockMass: Double,
        newBlockMass: Double
    ) {
        super.onSetBlock(posX, posY, posZ, oldBlockType, newBlockType, oldBlockMass, newBlockMass)

        if (oldBlockType != newBlockType) {
            val chunkPos: Vector3ic = Vector3i(posX shr 4, posY shr 4, posZ shr 4)

            val shipData: ShipData? = queryableShipData.getShipDataFromChunkPos(chunkPos.x(), chunkPos.z())

            val voxelUpdates = shipToVoxelUpdates.getOrPut(shipData) { HashMap() }

            val voxelShapeUpdate =
                voxelUpdates.getOrPut(chunkPos) { SparseVoxelShapeUpdate.createSparseVoxelShapeUpdate(chunkPos) }

            val voxelType: Byte = when(newBlockType) {
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

    fun tickShips(newLoadedChunks: List<IVoxelShapeUpdate>) {
        // For now, just make a [ShipObject] for every [ShipData]
        for (shipData in queryableShipData) {
            val shipID = shipData.shipUUID
            shipObjectMap.computeIfAbsent(shipID) { ShipObjectServer(shipData) }
        }

        // region Add voxel shape updates for chunks that loaded this tick
        for (newLoadedChunk in newLoadedChunks) {
            val chunkPos: Vector3ic = Vector3i(newLoadedChunk.regionX, newLoadedChunk.regionY, newLoadedChunk.regionZ)
            val shipData: ShipData? = queryableShipData.getShipDataFromChunkPos(chunkPos.x(), chunkPos.z())
            val voxelUpdates = shipToVoxelUpdates.getOrPut(shipData) { HashMap() }
            voxelUpdates[chunkPos] = newLoadedChunk
        }
        // endregion
    }

    /**
     * If the chunk at [chunkX], [chunkZ] is a ship chunk, then this returns the [IPlayer]s that are watching that ship chunk.
     *
     * If the chunk at [chunkX], [chunkZ] is not a ship chunk, then this returns nothing.
     */
    fun getIPlayersWatchingShipChunk(chunkX: Int, chunkZ: Int): Iterator<IPlayer> {
        // Check if this chunk potentially belongs to a ship
        if (ChunkAllocator.isChunkInShipyard(chunkX, chunkZ)) {
            // Then look for the shipData that owns this chunk
            val shipDataManagingPos = queryableShipData.getShipDataFromChunkPos(chunkX, chunkZ)
            if (shipDataManagingPos != null) {
                // Then check if there exists a ShipObject for this ShipData
                val shipObjectManagingPos = shipObjects[shipDataManagingPos.shipUUID]
                if (shipObjectManagingPos != null) {
                    return shipObjectManagingPos.shipChunkTracker.getPlayersWatchingChunk(chunkX, chunkZ)
                }
            }
        }
        return Collections.emptyIterator()
    }

    /**
     * Determines which ship chunks should be watched/unwatched by the players.
     *
     * It only returns the tasks, it is up to the caller to execute the tasks; however they do not have to execute all of them.
     * It is up to the caller to decide which tasks to execute, and which ones to skip.
     */
    fun tickShipChunkLoading(
        currentPlayers: Iterable<IPlayer>
    ): Pair<Spliterator<ChunkWatchTask>, Spliterator<ChunkUnwatchTask>> {
        val removedPlayers = lastPlayersSet - currentPlayers
        lastPlayersSet = currentPlayers.toHashSet()

        val chunkWatchTasksSorted = TreeSet<ChunkWatchTask>()
        val chunkUnwatchTasksSorted = TreeSet<ChunkUnwatchTask>()

        for (shipObject in shipObjects.values) {
            shipObject.shipChunkTracker.tick(
                players = currentPlayers,
                removedPlayers = removedPlayers,
                shipTransform = shipObject.shipData.shipTransform
            )

            val chunkWatchTasks = shipObject.shipChunkTracker.getChunkWatchTasks()
            val chunkUnwatchTasks = shipObject.shipChunkTracker.getChunkUnwatchTasks()

            chunkWatchTasks.forEach { chunkWatchTasksSorted.add(it) }
            chunkUnwatchTasks.forEach { chunkUnwatchTasksSorted.add(it) }
        }

        return Pair(chunkWatchTasksSorted.spliterator(), chunkUnwatchTasksSorted.spliterator())
    }

    /**
     * Creates a new [ShipData] centered at the block at [blockPosInWorldCoordinates].
     *
     * If [createShipObjectImmediately] is true then a [ShipObject] will be created immediately.
     */
    fun createNewShipAtBlock(
        blockPosInWorldCoordinates: Vector3ic, createShipObjectImmediately: Boolean, scaling: Double = 1.0
    ): ShipData {
        val chunkClaim = chunkAllocator.allocateNewChunkClaim()
        val shipName = NounListNameGenerator.generateName()

        val shipCenterInWorldCoordinates: Vector3dc = Vector3d(blockPosInWorldCoordinates).add(0.5, 0.5, 0.5)
        val blockPosInShipCoordinates: Vector3ic = chunkClaim.getCenterBlockCoordinates(Vector3i())
        val shipCenterInShipCoordinates: Vector3dc = Vector3d(blockPosInShipCoordinates).add(0.5, 0.5, 0.5)

        val newShipData = ShipData.createEmpty(
            name = shipName,
            chunkClaim = chunkClaim,
            shipCenterInWorldCoordinates = shipCenterInWorldCoordinates,
            shipCenterInShipCoordinates = shipCenterInShipCoordinates,
            scaling = scaling
        )

        queryableShipData.addShipData(newShipData)

        if (createShipObjectImmediately) {
            TODO("Not implemented")
        }

        return newShipData
    }

    override fun destroyWorld() {
        VSPipeline.getVSPipeline().removeShipWorld(this)
    }
}
