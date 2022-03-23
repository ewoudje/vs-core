package org.valkyrienskies.core.pipelines

import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3i
import org.valkyrienskies.core.game.ships.ShipData
import org.valkyrienskies.core.game.ships.ShipObjectServerWorld
import org.valkyrienskies.core.game.ships.ShipTransform
import org.valkyrienskies.physics_api.RigidBodyInertiaData
import org.valkyrienskies.physics_api.RigidBodyTransform
import org.valkyrienskies.physics_api.voxel_updates.IVoxelShapeUpdate
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

class VSGamePipelineStage {
    private val shipWorlds: MutableMap<Int, ShipObjectServerWorld> = HashMap()
    private val physicsFramesQueue: ConcurrentLinkedQueue<VSPhysicsFrame> = ConcurrentLinkedQueue()

    /**
     * Push a physics frame to the game stage
     */
    fun pushPhysicsFrame(physicsFrame: VSPhysicsFrame) {
        if (physicsFramesQueue.size >= 100) {
            // throw IllegalStateException("Too many physics frames in the physics frame queue. Is the game stage broken?")
            println("Too many physics frames in the physics frame queue. Is the game stage broken?")
            Thread.sleep(1000L)
        }
        physicsFramesQueue.add(physicsFrame)
    }

    /**
     * Apply queued physics frames to the game
     */
    fun preTickGame() {
        // Set the values of prevTickShipTransform
        shipWorlds.forEach { (_, shipWorld) ->
            shipWorld.shipObjects.forEach { (_, shipObject) ->
                shipObject.shipData.prevTickShipTransform = shipObject.shipData.shipTransform
            }
        }

        // Apply the physics frames
        while (physicsFramesQueue.isNotEmpty()) {
            val physicsFrame = physicsFramesQueue.remove()
            applyPhysicsFrame(physicsFrame)
        }
    }

    /**
     * Create a new game frame to be sent to the physics
     */
    fun postTickGame(): VSGameFrame {
        // Finally, return the game frame
        return createGameFrame()
    }

    private fun applyPhysicsFrame(physicsFrame: VSPhysicsFrame) {
        physicsFrame.shipDataMap.forEach { (uuid, shipInPhysicsFrameData) ->
            val dimension = shipInPhysicsFrameData.dimensionId
            val shipWorld: ShipObjectServerWorld? = shipWorlds[dimension]
            if (shipWorld != null) {
                // Only apply physics updates to ShipObjects. Do not apply them to ShipData without a ShipObject
                val shipData: ShipData? = shipWorld.shipObjects[uuid]?.shipData
                if (shipData != null) {
                    // TODO: Don't apply the transform if we are forcing the ship to move somewhere else
                    val applyTransform = true // For now just set [applyTransform] to always be true
                    if (applyTransform) {
                        val transformFromPhysics = shipInPhysicsFrameData.shipTransform
                        val voxelOffsetFromPhysics = shipInPhysicsFrameData.shipVoxelOffset

                        val deltaVoxelOffset =
                            shipData.inertiaData.getCenterOfMassInShipSpace().sub(voxelOffsetFromPhysics, Vector3d())

                        val shipPosAccountingForVoxelOffsetDifference =
                            transformFromPhysics.position.add(deltaVoxelOffset, Vector3d())
                        val newShipTransform = ShipTransform.createFromCoordinatesAndRotation(
                            shipPosAccountingForVoxelOffsetDifference,
                            shipData.inertiaData.getCenterOfMassInShipSpace(),
                            transformFromPhysics.rotation
                        )

                        shipData.shipTransform = newShipTransform
                    }
                } else {
                    if (shipWorld.groundBodyUUID != uuid)
                        println(
                            "Received physics frame update for ship with uuid: $uuid and dimension $dimension, " +
                                "but a ship with this uuid does not exist!"
                        )
                }
            } else {
                println(
                    "Received physics frame update for ship with uuid: $uuid and dimension $dimension, " +
                        "but a world with this dimension does not exist!"
                )
            }
        }
    }

    private fun createGameFrame(): VSGameFrame {
        val newShips = ArrayList<NewShipInGameFrameData>() // Ships to be added to the Physics simulation
        val deletedShips = ArrayList<UUID>() // Ships to be deleted from the Physics simulation
        val updatedShips = HashMap<UUID, UpdateShipInGameFrameData>() // Map of ship updates
        val gameFrameVoxelUpdatesMap = HashMap<UUID, List<IVoxelShapeUpdate>>() // Voxel updates applied by this frame

        shipWorlds.forEach { (dimension, shipWorld) ->
            val newGroundRigidBodyObjects = shipWorld.getNewGroundRigidBodyObjects()
            val newShipObjects = shipWorld.getNewShipObjects()
            val updatedShipObjects = shipWorld.getUpdatedShipObjects()
            val deletedShipObjects = shipWorld.getDeletedShipObjects()
            val shipVoxelUpdates = shipWorld.getShipToVoxelUpdates()

            newGroundRigidBodyObjects.forEach {
                val uuid = it
                val minDefined = Vector3i(Int.MIN_VALUE, 0, Int.MIN_VALUE)
                val maxDefined = Vector3i(Int.MAX_VALUE, 255, Int.MAX_VALUE)
                // Some random inertia values, the ground body is static so these don't matter
                val inertiaData = RigidBodyInertiaData(10.0, Vector3d(10.0))
                // Set the transform to be the origin with no rotation
                val shipTransform = RigidBodyTransform(Vector3d(), Quaterniond())
                // No voxel offset
                val voxelOffset = Vector3d()
                val newShipInGameFrameData = NewShipInGameFrameData(
                    uuid,
                    dimension,
                    minDefined,
                    maxDefined,
                    inertiaData,
                    shipTransform,
                    voxelOffset,
                    true
                )
                newShips.add(newShipInGameFrameData)
            }

            newShipObjects.forEach {
                val uuid = it.shipData.shipUUID
                val minDefined = Vector3i()
                val maxDefined = Vector3i()
                it.shipData.shipActiveChunksSet.getMinMaxWorldPos(minDefined, maxDefined)

                val inertiaTensorMatrix = it.shipData.inertiaData.getMomentOfInertiaTensor()
                // For now, just put the diagonal of the inertia tensor in
                // TODO: Make Krunch take in an inertia matrix
                val inertiaTensorDiagonal = Vector3d(
                    inertiaTensorMatrix.get(0, 0),
                    inertiaTensorMatrix.get(1, 1),
                    inertiaTensorMatrix.get(2, 2)
                )

                val inertiaData = RigidBodyInertiaData(it.shipData.inertiaData.getShipMass(), inertiaTensorDiagonal)
                val shipTransform = RigidBodyTransform(
                    it.shipData.shipTransform.shipPositionInWorldCoordinates,
                    it.shipData.shipTransform.shipCoordinatesToWorldCoordinatesRotation
                )
                val voxelOffset = it.shipData.inertiaData.getCenterOfMassInShipSpace()
                val newShipInGameFrameData = NewShipInGameFrameData(
                    uuid,
                    dimension,
                    minDefined,
                    maxDefined,
                    inertiaData,
                    shipTransform,
                    voxelOffset,
                    false
                )
                newShips.add(newShipInGameFrameData)
            }

            updatedShipObjects.forEach {
                val uuid = it.shipData.shipUUID
                val newVoxelOffset = it.shipData.inertiaData.getCenterOfMassInShipSpace()
                val updateShipInGameFrameData = UpdateShipInGameFrameData(uuid, newVoxelOffset)
                updatedShips[uuid] = updateShipInGameFrameData
            }

            deletedShips.addAll(deletedShipObjects)

            shipVoxelUpdates.forEach { (shipData, voxelUpdatesMap) ->
                val uuid: UUID = shipData?.shipUUID ?: shipWorld.groundBodyUUID
                gameFrameVoxelUpdatesMap[uuid] = voxelUpdatesMap.values.toList()
            }

            shipWorld.clearNewUpdatedDeletedShipObjectsAndVoxelUpdates()
        }
        return VSGameFrame(newShips, deletedShips, updatedShips, gameFrameVoxelUpdatesMap)
    }

    fun addShipWorld(shipWorld: ShipObjectServerWorld) {
        val dimension = shipWorld.dimension
        if (shipWorlds.containsKey(dimension))
            throw IllegalStateException("Ship world with dimension $dimension already exists!")
        shipWorlds[dimension] = shipWorld
    }

    fun removeShipWorld(shipWorld: ShipObjectServerWorld) {
        shipWorlds.remove(shipWorld.dimension)
    }
}
