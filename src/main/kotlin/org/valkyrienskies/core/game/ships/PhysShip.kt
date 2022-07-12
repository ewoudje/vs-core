package org.valkyrienskies.core.game.ships

import org.joml.Vector3dc
import org.valkyrienskies.core.api.ShipForcesInducer
import org.valkyrienskies.physics_api.RigidBodyReference

data class PhysShip internal constructor(
    val shipId: ShipId,
    // Don't use these outside of vs-core, I beg of thee
    internal val rigidBodyReference: RigidBodyReference,
    internal val forceInducers: List<ShipForcesInducer>,

    val inertia: PhysInertia,
    val velocity: Vector3dc,
    val omega: Vector3dc
)
