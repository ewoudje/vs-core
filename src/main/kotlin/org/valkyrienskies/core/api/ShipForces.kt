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

    fun applyRotDependentForce(force: Vector3dc)

    fun applyInvariantForce(force: Vector3dc)

    fun applyInvariantForceToPos(force: Vector3dc, pos: Vector3dc)

    fun applyRotDependentTorque(torque: Vector3dc)

    fun applyInvariantTorque(torque: Vector3dc)
}
