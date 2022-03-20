package org.valkyrienskies.core.physics

import org.joml.Vector3d
import org.joml.Vector3ic
import org.valkyrienskies.physics_api.PhysicsWorldReference
import org.valkyrienskies.physics_api.RigidBodyReference
import org.valkyrienskies.physics_api.voxel_updates.VoxelRigidBodyShapeUpdates
import org.valkyrienskies.physics_api_krunch.KrunchBootstrap
import org.valkyrienskies.physics_api_krunch.KrunchPhysicsWorldSettings
import org.valkyrienskies.physics_api_krunch.SolverType

// TODO: Probably delete this
class VSPhysicsWorld {
    private val physicsWorld: PhysicsWorldReference = KrunchBootstrap.createKrunchPhysicsWorld()
    private val settingsWrapper = KrunchPhysicsWorldSettings()

    init {
        KrunchBootstrap.setKrunchSettings(physicsWorld, settingsWrapper)
    }

    fun tick(timeStep: Double) {
        // Set compliance to 1e-5 to loosen the collision and restitution constraints
        settingsWrapper.collisionCompliance = 1e-5
        settingsWrapper.collisionRestitutionCompliance = 1e-5
        // Only partially update the constraints
        settingsWrapper.solverIterationWeight = .05
        settingsWrapper.iterations = 2
        // Use 4 collision points to stabilize post shapes
        settingsWrapper.maxCollisionPoints = 4
        settingsWrapper.solverType = SolverType.JACOBI
        // Limit the max depth of collision points we use to prevent explosions
        settingsWrapper.maxCollisionPointDepth = 5e-2
        KrunchBootstrap.setKrunchSettings(physicsWorld, settingsWrapper)

        val gravity = Vector3d(0.0, -10.0, 0.0)
        physicsWorld.tick(gravity, timeStep, true)
    }

    /**
     * Not thread safe, creates a new rigid body.
     */
    fun createVoxelRigidBody(dimensionId: Int, minDefined: Vector3ic, maxDefined: Vector3ic): RigidBodyReference {
        val body = physicsWorld.createVoxelRigidBody(dimensionId, minDefined, maxDefined)
        body.isStatic = true
        body.dynamicFrictionCoefficient = 1.0
        body.staticFrictionCoefficient = 1.0
        return body
    }

    fun queueShapeUpdates(updates: Array<VoxelRigidBodyShapeUpdates>) {
        physicsWorld.queueVoxelShapeUpdates(updates)
    }
}
