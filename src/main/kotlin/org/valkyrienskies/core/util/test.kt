package org.valkyrienskies.core.util

import org.joml.Matrix3d
import org.joml.Matrix4d
import org.joml.Vector3d
import org.valkyrienskies.core.physics.CuboidRigidBody
import org.valkyrienskies.core.physics.InertiaData
import org.valkyrienskies.core.physics.nodes.EngineNode
import org.valkyrienskies.core.physics.nodes.control.ForceAndTorqueLPControlLoop

fun main() {
    val body = CuboidRigidBody(
        Vector3d(),
        Matrix4d(),
        InertiaData(0.0, Vector3d(), Matrix3d())
    )

    val control = ForceAndTorqueLPControlLoop(
        Vector3d(0.0, 0.0, 0.0),
        Vector3d(0.0, 5.0, 0.0),
        body,
        listOf(
            EngineNode(Vector3d(), Vector3d(1.1, 1.0, 0.0), 10.0)
        )
    )

    control.tick(10)
}