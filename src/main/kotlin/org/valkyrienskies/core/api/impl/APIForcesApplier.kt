package org.valkyrienskies.core.api.impl

import org.joml.Vector3dc
import org.valkyrienskies.core.api.ForcesApplier
import org.valkyrienskies.physics_api.RigidBodyReference

@JvmInline
value class APIForcesApplier(val rigidBody: RigidBodyReference) : ForcesApplier {

    override fun addRotDependentForce(force: Vector3dc) {
        rigidBody.addRotDependentForceToNextPhysTick(force)
    }

    override fun addInvariantForce(force: Vector3dc) {
        rigidBody.addInvariantForceToNextPhysTick(force)
    }

    override fun addInvariantForceToPos(force: Vector3dc, pos: Vector3dc) {
        rigidBody.addInvariantForceAtPosToNextPhysTick(pos, force)
    }

    override fun addRotDependentTorque(torque: Vector3dc) {
        rigidBody.addRotDependentTorqueToNextPhysTick(torque)
    }

    override fun addInvariantTorque(torque: Vector3dc) {
        rigidBody.addInvariantTorqueToNextPhysTick(torque)
    }
}
