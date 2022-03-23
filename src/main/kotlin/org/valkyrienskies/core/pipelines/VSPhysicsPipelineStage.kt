package org.valkyrienskies.core.pipelines

import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.physics_api.PhysicsWorldReference
import org.valkyrienskies.physics_api.RigidBodyInertiaData
import org.valkyrienskies.physics_api.RigidBodyReference
import org.valkyrienskies.physics_api.RigidBodyTransform
import org.valkyrienskies.physics_api.voxel_updates.IVoxelShapeUpdate
import org.valkyrienskies.physics_api.voxel_updates.VoxelRigidBodyShapeUpdates
import org.valkyrienskies.physics_api_krunch.KrunchBootstrap
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

class VSPhysicsPipelineStage {
    private val gameFramesQueue: ConcurrentLinkedQueue<VSGameFrame> = ConcurrentLinkedQueue()
    private val physicsEngine: PhysicsWorldReference = KrunchBootstrap.createKrunchPhysicsWorld()

    // Map ships ids to rigid bodies, and map rigid bodies to ship ids
    private val shipIdToRigidBodyMap: MutableMap<UUID, ShipIdAndRigidBodyReference> = HashMap()

    /**
     * Push a game frame to the physics engine stage
     */
    fun pushGameFrame(gameFrame: VSGameFrame) {
        if (gameFramesQueue.size >= 10) {
            // throw IllegalStateException("Too many game frames in the game frame queue. Is the physics stage broken?")
            println("Too many game frames in the game frame queue. Is the physics stage broken?")
            Thread.sleep(1000L)
        }
        gameFramesQueue.add(gameFrame)
    }

    /**
     * Process queued game frames, tick the physics, then create a new physics frame
     */
    fun tickPhysics(gravity: Vector3dc, timeStep: Double, simulatePhysics: Boolean): VSPhysicsFrame {
        while (gameFramesQueue.isNotEmpty()) {
            val gameFrame = gameFramesQueue.remove()
            applyGameFrame(gameFrame)
        }
        physicsEngine.tick(gravity, timeStep, simulatePhysics)
        return createPhysicsFrame()
    }

    fun deleteResources() {
        if (physicsEngine.hasBeenDeleted()) throw IllegalStateException("Physics engine has already been deleted!")
        physicsEngine.deletePhysicsWorldResources()
    }

    private fun applyGameFrame(gameFrame: VSGameFrame) {
        // Delete deleted ships
        gameFrame.deletedShips.forEach { deletedShipId ->
            val shipRigidBodyReferenceAndId = shipIdToRigidBodyMap[deletedShipId]
                ?: throw IllegalStateException(
                    "Tried deleting rigid body from ship with UUID $deletedShipId, but no rigid body exists for this ship!"
                )

            val shipRigidBodyReference = shipRigidBodyReferenceAndId.rigidBodyReference
            physicsEngine.deleteRigidBody(shipRigidBodyReference.rigidBodyId)
            shipIdToRigidBodyMap.remove(deletedShipId)
        }

        // Create new ships
        gameFrame.newShips.forEach { newShipInGameFrameData ->
            val shipId = newShipInGameFrameData.uuid
            if (shipIdToRigidBodyMap.containsKey(shipId)) {
                throw IllegalStateException(
                    "Tried creating rigid body from ship with UUID $shipId, but a rigid body already exists for this ship!"
                )
            }
            val dimension = newShipInGameFrameData.dimensionId
            val minDefined = newShipInGameFrameData.minDefined
            val maxDefined = newShipInGameFrameData.maxDefined
            val inertiaData = newShipInGameFrameData.inertiaData
            val shipTransform = newShipInGameFrameData.shipTransform
            val isStatic = newShipInGameFrameData.isStatic

            val newRigidBodyReference = physicsEngine.createVoxelRigidBody(dimension, minDefined, maxDefined)
            newRigidBodyReference.inertiaData = inertiaData
            newRigidBodyReference.rigidBodyTransform = shipTransform
            newRigidBodyReference.collisionShapeOffset = newShipInGameFrameData.voxelOffset
            newRigidBodyReference.isStatic = isStatic

            shipIdToRigidBodyMap[shipId] = ShipIdAndRigidBodyReference(shipId, newRigidBodyReference)
        }

        // Update existing ships
        gameFrame.updatedShips.forEach { (shipId, shipUpdate) ->
            val shipRigidBodyReferenceAndId = shipIdToRigidBodyMap[shipId]
                ?: throw IllegalStateException(
                    "Tried updating rigid body from ship with UUID $shipId, but no rigid body exists for this ship!"
                )

            val shipRigidBody = shipRigidBodyReferenceAndId.rigidBodyReference

            val oldVoxelOffset = shipRigidBody.collisionShapeOffset
            val newVoxelOffset = shipUpdate.newVoxelOffset
            val deltaVoxelOffset = newVoxelOffset.sub(oldVoxelOffset, Vector3d())

            val oldShipTransform = shipRigidBody.rigidBodyTransform
            val newShipTransform = RigidBodyTransform(
                oldShipTransform.position.sub(deltaVoxelOffset, Vector3d()), oldShipTransform.rotation
            )

            shipRigidBody.collisionShapeOffset = newVoxelOffset
            shipRigidBody.rigidBodyTransform = newShipTransform
        }

        // Send voxel updates
        gameFrame.voxelUpdatesMap.forEach { (shipId, voxelUpdatesList) ->
            val shipRigidBodyReferenceAndId = shipIdToRigidBodyMap[shipId]
                ?: throw IllegalStateException(
                    "Tried sending voxel updates to rigid body from ship with UUID $shipId, but no rigid body exists for this ship!"
                )

            val shipRigidBodyReference = shipRigidBodyReferenceAndId.rigidBodyReference
            val voxelRigidBodyShapeUpdates =
                VoxelRigidBodyShapeUpdates(shipRigidBodyReference.rigidBodyId, voxelUpdatesList.toTypedArray())
            physicsEngine.queueVoxelShapeUpdates(arrayOf(voxelRigidBodyShapeUpdates))
        }
    }

    private fun createPhysicsFrame(): VSPhysicsFrame {
        val shipDataMap: MutableMap<UUID, ShipInPhysicsFrameData> = HashMap()
        // For now the physics doesn't send voxel updates, but it will in the future
        val voxelUpdatesMap: Map<UUID, List<IVoxelShapeUpdate>> = emptyMap()
        shipIdToRigidBodyMap.forEach { (shipId, shipIdAndRigidBodyReference) ->
            val rigidBodyReference = shipIdAndRigidBodyReference.rigidBodyReference

            val uuid: UUID = shipId
            val dimensionId = rigidBodyReference.initialDimension
            val inertiaData: RigidBodyInertiaData = rigidBodyReference.inertiaData
            val shipTransform: RigidBodyTransform = rigidBodyReference.rigidBodyTransform
            val shipVoxelOffset: Vector3dc = rigidBodyReference.collisionShapeOffset
            val vel: Vector3dc = rigidBodyReference.velocity
            val omega: Vector3dc = rigidBodyReference.omega

            shipDataMap[uuid] =
                ShipInPhysicsFrameData(uuid, dimensionId, inertiaData, shipTransform, shipVoxelOffset, vel, omega)
        }
        return VSPhysicsFrame(shipDataMap, voxelUpdatesMap)
    }
}

private data class ShipIdAndRigidBodyReference(val shipId: UUID, val rigidBodyReference: RigidBodyReference)
