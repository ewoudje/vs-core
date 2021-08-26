package org.valkyrienskies.core.game.ships

import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.core.chunk_tracking.ChunkUnwatchTask
import org.valkyrienskies.core.chunk_tracking.ChunkWatchTask
import org.valkyrienskies.core.game.ChunkAllocator
import org.valkyrienskies.core.game.IPlayer
import org.valkyrienskies.core.game.VSBlockType
import org.valkyrienskies.core.game.VSBlockType.SOLID
import org.valkyrienskies.core.physics.VSPhysicsTask
import org.valkyrienskies.core.physics.VSPhysicsWorld
import org.valkyrienskies.core.util.names.NounListNameGenerator
import org.valkyrienskies.physics_api.RigidBody
import org.valkyrienskies.physics_api.VoxelRigidBody
import java.util.Collections
import java.util.Spliterator
import java.util.TreeSet
import java.util.UUID

class ShipObjectServerWorld(
    override val queryableShipData: MutableQueryableShipDataServer,
    val chunkAllocator: ChunkAllocator
) : ShipObjectWorld(queryableShipData) {

    private var lastPlayersSet: Set<IPlayer> = setOf()

    private val shipObjectMap = HashMap<UUID, ShipObjectServer>()
    override val shipObjects: Map<UUID, ShipObjectServer> = shipObjectMap

    private val vsPhysicsTask = VSPhysicsTask(VSPhysicsWorld())
    private val vsPhysicsThread = Thread(vsPhysicsTask)

    private val groundRigidBody: VoxelRigidBody = vsPhysicsTask.physicsWorld.createVoxelRigidBody()

    init {
        groundRigidBody.setRigidBodyTransform(Vector3d(.5, .5, .5), Quaterniond())
        groundRigidBody.isStatic = true

        for (x in -100..100) {
            for (z in -100..100) {
                groundRigidBody.collisionShape.addVoxel(x, 5, z)
            }
        }
        vsPhysicsTask.physicsWorld.addRigidBody(groundRigidBody)

        // Run [vsPhysicsTask] in [vsPhysicsThread]
        vsPhysicsThread.start()
    }

    override fun onSetBlock(
        posX: Int, posY: Int, posZ: Int, oldBlockType: VSBlockType, newBlockType: VSBlockType, oldBlockMass: Double,
        newBlockMass: Double
    ) {
        super.onSetBlock(posX, posY, posZ, oldBlockType, newBlockType, oldBlockMass, newBlockMass)

        if (oldBlockType != newBlockType) {
            if (!chunkAllocator.isBlockInShipyard(posX, posY, posZ)) {
                // Update the ground rigid body
                if (newBlockType == SOLID) {
                    vsPhysicsTask.queueTask {
                        groundRigidBody.collisionShape.addVoxel(posX, posY, posZ)
                    }
                } else {
                    vsPhysicsTask.queueTask {
                        groundRigidBody.collisionShape.removeVoxel(posX, posY, posZ)
                    }
                }
            }
        }
    }

    fun tickShips() {
        val newRigidBodies = ArrayList<RigidBody<*>>()
        // For now, just make a [ShipObject] for every [ShipData]
        for (shipData in queryableShipData) {
            val shipID = shipData.shipUUID
            shipObjectMap.computeIfAbsent(shipID) {
                val shipObjectServer = ShipObjectServer(shipData, vsPhysicsTask.physicsWorld.createVoxelRigidBody())
                shipObjectServer.rigidBody.collisionShape.setScaling(
                    shipData.shipTransform.shipCoordinatesToWorldCoordinatesScaling.x()
                )
                shipObjectServer.rigidBody.collisionShape.addVoxel(0, 0, 0)
                shipObjectServer.rigidBody.setRigidBodyTransform(
                    shipData.shipTransform.shipPositionInWorldCoordinates,
                    shipData.shipTransform.shipCoordinatesToWorldCoordinatesRotation
                )
                shipObjectServer.rigidBody.inertiaData.mass = shipData.inertiaData.getShipMass()
                shipObjectServer.rigidBody.inertiaData.momentOfInertia =
                    Vector3d(shipObjectServer.rigidBody.inertiaData.mass / 12.0)

                newRigidBodies.add(shipObjectServer.rigidBody)

                shipObjectServer
            }
        }
        vsPhysicsTask.queueTask {
            newRigidBodies.forEach { vsPhysicsTask.physicsWorld.addRigidBody(it) }
        }

        for (shipObject in shipObjectMap.values) {
            shipObject.shipData.prevTickShipTransform = shipObject.shipData.shipTransform

            val newTransform = voxelRigidBodyToShipTransform(shipObject)

            shipObject.shipData.shipTransform = newTransform
        }
    }

    private fun voxelRigidBodyToShipTransform(shipObjectServer: ShipObjectServer): ShipTransform {
        val scalingVector: Vector3dc =
            Vector3d(shipObjectServer.shipData.shipTransform.shipCoordinatesToWorldCoordinatesScaling)
        val shipPositionInWorld: Vector3dc = Vector3d(shipObjectServer.rigidBody.rigidBodyTransform.position)
        val shipRotationInWorld: Quaterniondc = Quaterniond(shipObjectServer.rigidBody.rigidBodyTransform.rotation)
        val shipPositionInShipCoordinates: Vector3dc =
            Vector3d(shipObjectServer.shipData.inertiaData.getCenterOfMassInShipSpace()).add(.5, .5, .5)
        return ShipTransform(
            shipPositionInWorldCoordinates = shipPositionInWorld,
            shipPositionInShipCoordinates = shipPositionInShipCoordinates,
            shipCoordinatesToWorldCoordinatesRotation = shipRotationInWorld,
            shipCoordinatesToWorldCoordinatesScaling = scalingVector
        )
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
    fun createNewShipAtBlock(blockPosInWorldCoordinates: Vector3ic, createShipObjectImmediately: Boolean): ShipData {
        val chunkClaim = chunkAllocator.allocateNewChunkClaim()
        val shipName = NounListNameGenerator.generateName()

        val shipCenterInWorldCoordinates: Vector3dc = Vector3d(blockPosInWorldCoordinates).add(0.5, 0.5, 0.5)
        val blockPosInShipCoordinates: Vector3ic = chunkClaim.getCenterBlockCoordinates(Vector3i())
        val shipCenterInShipCoordinates: Vector3dc = Vector3d(blockPosInShipCoordinates).add(0.5, 0.5, 0.5)

        val newShipData = ShipData.createEmpty(
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

    override fun destroyWorld() {
        // Tell the physics task to kill itself on the next physics tick
        vsPhysicsTask.tellTaskToKillItself()
    }
}
