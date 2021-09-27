package org.valkyrienskies.core.physics

import org.joml.Vector3d
import org.valkyrienskies.physics_api.PhysicsWorld
import org.valkyrienskies.physics_api.RigidBody
import org.valkyrienskies.physics_api.VoxelRigidBody
import org.valkyrienskies.physics_api.voxel_updates.VoxelRigidBodyShapeUpdates
import org.valkyrienskies.physics_api_krunch.KrunchBootstrap
import org.valkyrienskies.physics_api_krunch.KrunchPhysicsWorldSettingsWrapper

class VSPhysicsWorld {
    private val physicsWorld: PhysicsWorld
    private val settingsWrapper = KrunchPhysicsWorldSettingsWrapper()

    init {
        physicsWorld = KrunchBootstrap.createKrunchPhysicsWorld()
        // Use 5 iterations to get good results
        settingsWrapper.setIterations(5)
        // Set compliance to 1e-4 to limit the max force of constraints, and prevent the solver from blowing up
        settingsWrapper.setCollisionCompliance(1e-4)
        settingsWrapper.setCollisionRestitutionCompliance(1e-4)
        settingsWrapper.setDynamicFrictionCompliance(1e-4)
        settingsWrapper.setMaxCollisionPoints(4)
        KrunchBootstrap.setKrunchSettings(physicsWorld, settingsWrapper)
    }

    fun tick(timeStep: Double) {
        val gravity = Vector3d(0.0, -10.0, 0.0)
        physicsWorld.tick(gravity, timeStep)
    }

    /**
     * Thread safe, creates a new rigid body.
     */
    fun createVoxelRigidBody(): VoxelRigidBody {
        return physicsWorld.createVoxelRigidBody()
    }

    /**
     * Not thread safe. Don't invoke this while tick() is running.
     */
    fun addRigidBody(rigidBody: RigidBody<*>) {
        physicsWorld.addRigidBody(rigidBody)
    }

    fun queueShapeUpdates(updates: List<VoxelRigidBodyShapeUpdates>) {
        physicsWorld.queueVoxelShapeUpdates(updates)
    }
}
