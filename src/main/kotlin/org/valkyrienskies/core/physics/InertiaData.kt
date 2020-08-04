package org.valkyrienskies.core.physics

import org.joml.Matrix3dc
import org.joml.Vector3dc

/**
 * All fields of inertia data can be mutated,
 * but request update on the rigid body which owns it.
 */
data class InertiaData(
    var mass: Double,
    val centerOfMass: Vector3dc,
    val inertia: Matrix3dc
)