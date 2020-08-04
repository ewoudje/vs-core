package org.valkyrienskies.core.physics

import org.joml.Vector3dc

interface PhysicsEngine {
    fun applyForce(body: RigidBody, force: Vector3dc, position: Vector3dc)
    fun addCentralForce(body: RigidBody, force: Vector3dc)
    /**
     * Call this if you want the physics engine to
     * check the parameters of the rigid body again next tick, like if you changed
     * the inertia data. Note that the physics engine may update WITHOUT you ever
     * having set this to true. This is just a hint.
     */
    fun requestUpdate(body: RigidBody)

    fun addRigidBody(body: RigidBody)
    fun removeRigidBody(body: RigidBody)

    val rigidBodies: Set<RigidBody>

    fun tick(deltaNs: Long)
    fun unload() {}
}
