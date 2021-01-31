package org.valkyrienskies.core.game

import org.joml.Vector3d

/**
 * This class keeps track of a ships linear and angular velocity.
 */
data class ShipPhysicsData(val linearVelocity: Vector3d, val angularVelocity: Vector3d) {
    companion object {
        internal fun newEmptyShipPhysicsData(): ShipPhysicsData {
            return ShipPhysicsData(Vector3d(), Vector3d())
        }
    }
}