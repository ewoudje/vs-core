package org.valkyrienskies.core.physics.nodes

import org.joml.Vector3dc
import org.valkyrienskies.core.util.*

class EngineNode(
    /**
     * Position of the engine relative to the rigid body. DO NOT MUTATE
     */
    val position: Vector3dc,
    /**
     * Direction of the engine's force. It should be normalized. DO NOT MUTATE
     */
    val direction: Vector3dc,
    /**
     * Max force output of the engine
     */
    val maxForce: Double,
    /**
     * Energy used per force outputted
     */
    val energyPerForce: Double = 1.0
) {

    /**
     * DON'T MUTATE THIS (unless you're a control loop!)
     */
    @Volatile
    var currentForce: Double = 0.0
        internal set(v) {
            assertIsPhysicsThread()
            field = v
        }
}