package org.valkyrienskies.core.api

import org.joml.Vector3dc

/**
 * Ship force inducer
 * The values shall be used every phyTick to apply forces to the ship.
 */
interface ShipForcesInducer {

    fun applyForces(forcesApplier: ForcesApplier)
}

/**
 * Ship torque inducer
 * The values shall be used every phyTick to apply torque to the ship.
 */
interface ForcesApplier {

    fun addRotDependentForce(force: Vector3dc)

    fun addInvariantForce(force: Vector3dc)

    fun addInvariantForceToPos(force: Vector3dc, pos: Vector3dc)

    fun addRotDependentTorque(torque: Vector3dc)

    fun addInvariantTorque(torque: Vector3dc)
}
