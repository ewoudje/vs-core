package org.valkyrienskies.core.game.ships

import org.joml.Quaterniondc
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ShipForcesInducer
import org.valkyrienskies.physics_api.RigidBodyReference

data class PhysShip internal constructor(
    val shipId: ShipId,
    // Don't use these outside of vs-core, I beg of thee
    internal val rigidBodyReference: RigidBodyReference,
    internal val forceInducers: List<ShipForcesInducer>,
    internal var _inertia: PhysInertia,

    // TODO transformation matrix
    val position: Vector3dc,
    val rotation: Quaterniondc,

    val velocity: Vector3dc,
    val omega: Vector3dc
) {
    val inertia: PhysInertia
        get() = _inertia
}
