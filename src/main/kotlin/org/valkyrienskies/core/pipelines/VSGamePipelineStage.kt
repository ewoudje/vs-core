package org.valkyrienskies.core.pipelines

import org.joml.Matrix3d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.primitives.AABBi
import org.valkyrienskies.core.game.ships.ShipData
import org.valkyrienskies.core.game.ships.ShipId
import org.valkyrienskies.core.game.ships.ShipInertiaData
import org.valkyrienskies.core.game.ships.ShipObjectServer
import org.valkyrienskies.core.game.ships.ShipObjectServerWorld
import org.valkyrienskies.core.game.ships.ShipPhysicsData
import org.valkyrienskies.core.game.ships.ShipTransform
import org.valkyrienskies.physics_api.PhysicsWorldReference
import org.valkyrienskies.physics_api.RigidBodyInertiaData
import org.valkyrienskies.physics_api.RigidBodyTransform
import org.valkyrienskies.physics_api.voxel_updates.IVoxelShapeUpdate
import java.util.concurrent.ConcurrentLinkedQueue

class VSGamePipelineStage(val shipWorld: ShipObjectServerWorld) {
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
        shipWorld.shipObjects.forEach { (_, shipObject) ->
            shipObject.shipData.updatePrevTickShipTransform()
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
        physicsFrame.shipDataMap.forEach { (shipId, shipInPhysicsFrameData) ->
            // Only apply physics updates to ShipObjects. Do not apply them to ShipData without a ShipObject
            val shipObject: ShipObjectServer? = shipWorld.shipObjects[shipId]
            val shipData: ShipData? = shipObject?.shipData
            if (shipData != null) {
                // TODO: Don't apply the transform if we are forcing the ship to move somewhere else
                val applyTransform = true // For now just set [applyTransform] to always be true
                if (applyTransform) {
                    val transformFromPhysics = shipInPhysicsFrameData.shipTransform
                    val voxelOffsetFromPhysics = shipInPhysicsFrameData.shipVoxelOffset
                    val voxelOffsetFromGame = getShipVoxelOffset(shipData.inertiaData)

                    val deltaVoxelOffset = transformFromPhysics.rotation.transform(
                        voxelOffsetFromGame.sub(voxelOffsetFromPhysics, Vector3d())
                    )

                    val shipPosAccountingForVoxelOffsetDifference =
                        transformFromPhysics.position.sub(deltaVoxelOffset, Vector3d())
                    val newShipTransform = ShipTransform.createFromCoordinatesAndRotation(
                        shipPosAccountingForVoxelOffsetDifference,
                        shipData.inertiaData.getCenterOfMassInShipSpace().add(.5, .5, .5, Vector3d()),
                        transformFromPhysics.rotation
                    )

                    shipData.physicsData.linearVelocity = shipInPhysicsFrameData.vel
                    shipData.physicsData.angularVelocity = shipInPhysicsFrameData.omega
                    shipData.shipTransform = newShipTransform
                    shipObject.debugShipPhysicsAABB = shipInPhysicsFrameData.aabb
                }
            } else {
                // Check ground rigid body objects
                if (!shipWorld.dimensionToGroundBodyIdImmutable.containsValue(shipId))
                    println(
                        "Received physics frame update for ship with ShipId: $shipId, " +
                            "but a ship with this ShipId does not exist!"
                    )
            }
        }
    }

    private fun createGameFrame(): VSGameFrame {
        val newShips = ArrayList<NewShipInGameFrameData>() // Ships to be added to the Physics simulation
        val deletedShips = ArrayList<ShipId>() // Ships to be deleted from the Physics simulation
        val updatedShips = HashMap<ShipId, UpdateShipInGameFrameData>() // Map of ship updates
        val gameFrameVoxelUpdatesMap = HashMap<ShipId, List<IVoxelShapeUpdate>>() // Voxel updates applied by this frame

        val newGroundRigidBodyObjects = shipWorld.getNewGroundRigidBodyObjects()
        val newShipObjects = shipWorld.getNewShipObjects()
        val updatedShipObjects = shipWorld.getUpdatedShipObjects()
        val deletedShipObjects = shipWorld.getDeletedShipObjectsIncludingGround()
        val shipVoxelUpdates = shipWorld.getShipToVoxelUpdates()

        newGroundRigidBodyObjects.forEach { newGroundObjectData ->
            val dimensionId = newGroundObjectData.first
            val shipId = newGroundObjectData.second
            val minDefined = Vector3i(Int.MIN_VALUE, 0, Int.MIN_VALUE)
            val maxDefined = Vector3i(Int.MAX_VALUE, 255, Int.MAX_VALUE)
            val totalVoxelRegion = PhysicsWorldReference.INFINITE_VOXEL_REGION
            // Some random inertia values, the ground body is static so these don't matter
            val inertiaData = RigidBodyInertiaData(
                1e-1,
                Matrix3d(
                    1e-1, 0.0, 0.0,
                    0.0, 1e-1, 0.0,
                    0.0, 0.0, 1e-1
                )
            )
            // Set the transform to be the origin with no rotation
            val shipTransform = RigidBodyTransform(Vector3d(), Quaterniond())
            // No voxel offset
            val voxelOffset = Vector3d(.5, .5, .5)
            val isStatic = true
            val isVoxelsFullyLoaded = false
            val newShipInGameFrameData = NewShipInGameFrameData(
                shipId,
                dimensionId,
                minDefined,
                maxDefined,
                totalVoxelRegion,
                inertiaData,
                ShipPhysicsData(Vector3d(), Vector3d()),
                shipTransform,
                voxelOffset,
                isStatic,
                isVoxelsFullyLoaded,
                emptyList()
            )
            newShips.add(newShipInGameFrameData)
        }

        newShipObjects.forEach {
            val uuid = it.shipData.id
            val minDefined = Vector3i()
            val maxDefined = Vector3i()
            it.shipData.shipActiveChunksSet.getMinMaxWorldPos(minDefined, maxDefined)

            val totalVoxelRegion = it.shipData.chunkClaim.getTotalVoxelRegion(AABBi())

            val rigidBodyInertiaData = getRigidBodyInertiaData(it.shipData.inertiaData)

            val shipTransform = RigidBodyTransform(
                it.shipData.shipTransform.shipPositionInWorldCoordinates,
                it.shipData.shipTransform.shipCoordinatesToWorldCoordinatesRotation
            )
            val voxelOffset = getShipVoxelOffset(it.shipData.inertiaData)
            val isStatic = it.shipData.isStatic
            val isVoxelsFullyLoaded = it.shipData.areVoxelsFullyLoaded()
            val newShipInGameFrameData = NewShipInGameFrameData(
                uuid,
                it.shipData.chunkClaimDimension,
                minDefined,
                maxDefined,
                totalVoxelRegion,
                rigidBodyInertiaData,
                it.shipData.physicsData,
                shipTransform,
                voxelOffset,
                isStatic,
                isVoxelsFullyLoaded,
                it.forceInducers
            )
            newShips.add(newShipInGameFrameData)
        }

        updatedShipObjects.forEach {
            val uuid = it.shipData.id
            val newVoxelOffset = getShipVoxelOffset(it.shipData.inertiaData)
            val rigidBodyInertiaData = getRigidBodyInertiaData(it.shipData.inertiaData)
            val isStatic = it.shipData.isStatic
            val isVoxelsFullyLoaded = it.shipData.areVoxelsFullyLoaded()
            val updateShipInGameFrameData = UpdateShipInGameFrameData(
                uuid,
                newVoxelOffset,
                rigidBodyInertiaData,
                it.shipData.physicsData,
                isStatic,
                isVoxelsFullyLoaded
            )
            updatedShips[uuid] = updateShipInGameFrameData
        }

        deletedShips.addAll(deletedShipObjects)

        shipVoxelUpdates.forEach forEachVoxelUpdate@{ (shipId, voxelUpdatesMap) ->
            gameFrameVoxelUpdatesMap[shipId] = voxelUpdatesMap.values.toList()
        }

        shipWorld.clearNewUpdatedDeletedShipObjectsAndVoxelUpdates()
        return VSGameFrame(newShips, deletedShips, updatedShips, gameFrameVoxelUpdatesMap)
    }

    private companion object {
        private fun getShipVoxelOffset(inertiaData: ShipInertiaData): Vector3dc {
            val cm = inertiaData.getCenterOfMassInShipSpace()
            return Vector3d(-cm.x(), -cm.y(), -cm.z())
        }

        private fun getRigidBodyInertiaData(shipInertiaData: ShipInertiaData): RigidBodyInertiaData {
            val invMass = 1.0 / shipInertiaData.getShipMass()
            if (!invMass.isFinite())
                throw IllegalStateException("invMass is not finite!")

            val invInertiaMatrix = shipInertiaData.getMomentOfInertiaTensor().invert(Matrix3d())
            if (!invInertiaMatrix.isFinite)
                throw IllegalStateException("invInertiaMatrix is not finite!")

            return RigidBodyInertiaData(invMass, invInertiaMatrix)
        }
    }
}
