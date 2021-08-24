package org.valkyrienskies.core.physics

import org.joml.Vector3d
import org.valkyrienskies.physics_api.RigidBody
import org.valkyrienskies.physics_api.VoxelRigidBody
import org.valkyrienskies.physics_api_krunch.KrunchBootstrap

class VSPhysicsWorld {
    private val physicsWorld = KrunchBootstrap.createKrunchPhysicsWorld()

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
}
