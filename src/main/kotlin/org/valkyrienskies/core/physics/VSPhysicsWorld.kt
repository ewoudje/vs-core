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
        KrunchBootstrap.setKrunchSettings(physicsWorld, settingsWrapper)
    }

    fun tick(timeStep: Double) {
        settingsWrapper.setCollisionCompliance(1e-5)
        settingsWrapper.setCollisionRestitutionCompliance(1e-5)
        settingsWrapper.setSolverIterationWeight(.05)
        settingsWrapper.setIterations(2)
        settingsWrapper.setMaxCollisionPoints(4)
        settingsWrapper.setSolverType("jacobi")
        settingsWrapper.setMaxCollisionPointDepth(5e-2)
        val gravity = Vector3d(0.0, -10.0, 0.0)
        physicsWorld.tick(gravity, timeStep)
    }

    /**
     * Thread safe, creates a new rigid body.
     */
    fun createVoxelRigidBody(): VoxelRigidBody {
        val body = physicsWorld.createVoxelRigidBody()
        body.dynamicFrictionCoefficient = 1.0
        body.staticFrictionCoefficient = 1.0
        return body
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
