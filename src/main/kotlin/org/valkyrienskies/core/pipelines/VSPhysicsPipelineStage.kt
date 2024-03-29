package org.valkyrienskies.core.pipelines

import org.joml.Matrix3d
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.primitives.AABBd
import org.valkyrienskies.core.api.impl.APIForcesApplier
import org.valkyrienskies.core.game.ships.PhysInertia
import org.valkyrienskies.core.game.ships.PhysShip
import org.valkyrienskies.core.game.ships.ShipId
import org.valkyrienskies.core.util.logger
import org.valkyrienskies.physics_api.PhysicsWorldReference
import org.valkyrienskies.physics_api.PoseVel
import org.valkyrienskies.physics_api.RigidBodyInertiaData
import org.valkyrienskies.physics_api.SegmentTracker
import org.valkyrienskies.physics_api.voxel_updates.IVoxelShapeUpdate
import org.valkyrienskies.physics_api.voxel_updates.VoxelRigidBodyShapeUpdates
import org.valkyrienskies.physics_api_krunch.KrunchBootstrap
import org.valkyrienskies.physics_api_krunch.KrunchPhysicsWorldSettings
import java.util.concurrent.ConcurrentLinkedQueue

class VSPhysicsPipelineStage {
    private val gameFramesQueue: ConcurrentLinkedQueue<VSGameFrame> = ConcurrentLinkedQueue()
    private val physicsEngine: PhysicsWorldReference = KrunchBootstrap.createKrunchPhysicsWorld()

    // Map ships ids to rigid bodies, and map rigid bodies to ship ids
    private val shipIdToPhysShip: MutableMap<ShipId, PhysShip> = HashMap()
    private var physTick = 0

    init {
        // Apply physics engine settings
        val settings = KrunchPhysicsWorldSettings()
        // Only use 10 sub-steps
        settings.subSteps = 10
        // Decrease max de-penetration speed so that rigid bodies don't go
        // flying apart when they overlap
        settings.maxDePenetrationSpeed = 10.0
        KrunchBootstrap.setKrunchSettings(physicsEngine, settings)
    }

    /**
     * Push a game frame to the physics engine stage
     */
    fun pushGameFrame(gameFrame: VSGameFrame) {
        if (gameFramesQueue.size >= 10) {
            // throw IllegalStateException("Too many game frames in the game frame queue. Is the physics stage broken?")
            logger.warn("Too many game frames in the game frame queue. Is the physics stage broken?")
            Thread.sleep(1000L)
        }
        gameFramesQueue.add(gameFrame)
    }

    /**
     * Process queued game frames, tick the physics, then create a new physics frame
     */
    fun tickPhysics(gravity: Vector3dc, timeStep: Double, simulatePhysics: Boolean): VSPhysicsFrame {
        // Apply game frames
        while (gameFramesQueue.isNotEmpty()) {
            val gameFrame = gameFramesQueue.remove()
            applyGameFrame(gameFrame)
        }

        // Update the [poseVel] stored in [PhysShip]
        shipIdToPhysShip.values.forEach {
            it.poseVel = it.rigidBodyReference.poseVel
            // TODO: In the future update the segment tracker too, probably do this after we've added portals to Krunch
            // it.segments = it.rigidBodyReference.segments
        }

        // Compute and apply forces/torques for ships
        shipIdToPhysShip.values.forEach {
            val applier = APIForcesApplier(it.rigidBodyReference)
            it.forceInducers.forEach { i -> i.applyForces(applier, it) }
        }

        // Run the physics engine
        physicsEngine.tick(gravity, timeStep, simulatePhysics)

        // Return a new physics frame
        return createPhysicsFrame()
    }

    fun deleteResources() {
        if (physicsEngine.hasBeenDeleted()) throw IllegalStateException("Physics engine has already been deleted!")
        physicsEngine.deletePhysicsWorldResources()
    }

    private fun applyGameFrame(gameFrame: VSGameFrame) {
        // Delete deleted ships
        gameFrame.deletedShips.forEach { deletedShipId ->
            val shipRigidBodyReferenceAndId = shipIdToPhysShip[deletedShipId]
                ?: throw IllegalStateException(
                    "Tried deleting rigid body from ship with UUID $deletedShipId," +
                        " but no rigid body exists for this ship!"
                )

            val shipRigidBodyReference = shipRigidBodyReferenceAndId.rigidBodyReference
            physicsEngine.deleteRigidBody(shipRigidBodyReference.rigidBodyId)
            shipIdToPhysShip.remove(deletedShipId)
        }

        // Create new ships
        gameFrame.newShips.forEach { newShipInGameFrameData ->
            val shipId = newShipInGameFrameData.uuid
            if (shipIdToPhysShip.containsKey(shipId)) {
                throw IllegalStateException(
                    "Tried creating rigid body from ship with UUID $shipId," +
                        " but a rigid body already exists for this ship!"
                )
            }
            val dimension = newShipInGameFrameData.dimension
            val minDefined = newShipInGameFrameData.minDefined
            val maxDefined = newShipInGameFrameData.maxDefined
            val totalVoxelRegion = newShipInGameFrameData.totalVoxelRegion
            val inertiaData = newShipInGameFrameData.inertiaData
            val poseVel = newShipInGameFrameData.poseVel
            val segments = newShipInGameFrameData.segments
            val isStatic = newShipInGameFrameData.isStatic
            val shipVoxelsFullyLoaded = newShipInGameFrameData.shipVoxelsFullyLoaded

            val newRigidBodyReference =
                physicsEngine.createVoxelRigidBody(
                    dimension, minDefined, maxDefined, totalVoxelRegion
                )
            newRigidBodyReference.inertiaData = physInertiaToRigidBodyInertiaData(inertiaData)
            newRigidBodyReference.poseVel = poseVel
            newRigidBodyReference.collisionShapeOffset = newShipInGameFrameData.voxelOffset
            newRigidBodyReference.isStatic = isStatic
            newRigidBodyReference.isVoxelTerrainFullyLoaded = shipVoxelsFullyLoaded
            // TODO: This will need to be changed when we have multiple segments
            newRigidBodyReference.setSegmentDisplacement(0, segments.segments.values.first().segmentDisplacement)

            shipIdToPhysShip[shipId] =
                PhysShip(
                    shipId,
                    newRigidBodyReference,
                    newShipInGameFrameData.forcesInducers,
                    inertiaData,
                    poseVel,
                    segments
                )
        }

        // Update existing ships
        gameFrame.updatedShips.forEach { (shipId, shipUpdate) ->
            val physShip = shipIdToPhysShip[shipId]
                ?: throw IllegalStateException(
                    "Tried updating rigid body from ship with UUID $shipId, but no rigid body exists for this ship!"
                )

            val shipRigidBody = physShip.rigidBodyReference
            val oldPoseVel = shipRigidBody.poseVel

            val oldVoxelOffset = shipRigidBody.collisionShapeOffset
            val newVoxelOffset = shipUpdate.newVoxelOffset
            val deltaVoxelOffset = oldPoseVel.rot.transform(newVoxelOffset.sub(oldVoxelOffset, Vector3d()))
            val isStatic = shipUpdate.isStatic
            val shipVoxelsFullyLoaded = shipUpdate.shipVoxelsFullyLoaded

            val newShipPoseVel = PoseVel(
                oldPoseVel.pos.sub(deltaVoxelOffset, Vector3d()), oldPoseVel.rot, oldPoseVel.vel, oldPoseVel.omega
            )

            physShip._inertia = shipUpdate.inertiaData

            shipRigidBody.collisionShapeOffset = newVoxelOffset
            shipRigidBody.poseVel = newShipPoseVel
            shipRigidBody.inertiaData = physInertiaToRigidBodyInertiaData(shipUpdate.inertiaData)
            shipRigidBody.isStatic = isStatic
            shipRigidBody.isVoxelTerrainFullyLoaded = shipVoxelsFullyLoaded
        }

        // Send voxel updates
        gameFrame.voxelUpdatesMap.forEach { (shipId, voxelUpdatesList) ->
            val shipRigidBodyReferenceAndId = shipIdToPhysShip[shipId]
                ?: throw IllegalStateException(
                    "Tried sending voxel updates to rigid body from ship with UUID $shipId," +
                        " but no rigid body exists for this ship!"
                )

            val shipRigidBodyReference = shipRigidBodyReferenceAndId.rigidBodyReference
            val voxelRigidBodyShapeUpdates =
                VoxelRigidBodyShapeUpdates(shipRigidBodyReference.rigidBodyId, voxelUpdatesList.toTypedArray())
            physicsEngine.queueVoxelShapeUpdates(arrayOf(voxelRigidBodyShapeUpdates))
        }
    }

    private fun createPhysicsFrame(): VSPhysicsFrame {
        val shipDataMap: MutableMap<ShipId, ShipInPhysicsFrameData> = HashMap()
        // For now the physics doesn't send voxel updates, but it will in the future
        val voxelUpdatesMap: Map<ShipId, List<IVoxelShapeUpdate>> = emptyMap()
        shipIdToPhysShip.forEach { (shipId, shipIdAndRigidBodyReference) ->
            val rigidBodyReference = shipIdAndRigidBodyReference.rigidBodyReference
            val inertiaData: RigidBodyInertiaData = rigidBodyReference.inertiaData
            val poseVel: PoseVel = rigidBodyReference.poseVel
            val segments: SegmentTracker = rigidBodyReference.segmentTracker
            val shipVoxelOffset: Vector3dc = rigidBodyReference.collisionShapeOffset
            val aabb = AABBd()
            rigidBodyReference.getAABB(aabb)

            shipDataMap[shipId] =
                ShipInPhysicsFrameData(
                    shipId, inertiaData, poseVel, segments, shipVoxelOffset, aabb
                )
        }
        return VSPhysicsFrame(shipDataMap, voxelUpdatesMap, physTick++)
    }

    companion object {
        private fun physInertiaToRigidBodyInertiaData(inertia: PhysInertia): RigidBodyInertiaData {
            val invMass = 1.0 / inertia.shipMass
            if (!invMass.isFinite())
                throw IllegalStateException("invMass is not finite!")

            val invInertiaMatrix = inertia.momentOfInertiaTensor.invert(Matrix3d())
            if (!invInertiaMatrix.isFinite)
                throw IllegalStateException("invInertiaMatrix is not finite!")

            return RigidBodyInertiaData(invMass, invInertiaMatrix)
        }

        private val logger by logger()
    }
}
