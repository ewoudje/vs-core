package org.valkyrienskies.core.pipelines

import org.joml.Vector3dc
import org.valkyrienskies.physics_api.PhysicsWorldReference
import org.valkyrienskies.physics_api.RigidBodyReference
import org.valkyrienskies.physics_api_krunch.KrunchBootstrap
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class VSPhysicsPipelineStage {
    private val gameFramesQueue: ConcurrentLinkedQueue<VSGameFrame> = ConcurrentLinkedQueue()
    private val physicsEngine: PhysicsWorldReference = KrunchBootstrap.createKrunchPhysicsWorld()
    // Map ships ids to rigid bodies, and map rigid bodies to ship ids
    private val shipIdToRigidBodyMap: Map<UUID, ShipIdAndRigidBodyReference> = HashMap()

    /**
     * Push a game frame to the physics engine stage
     */
    fun pushGameFrame(gameFrame: VSGameFrame) {
        if (gameFramesQueue.size >= 10) {
            throw IllegalStateException("Too many game frames in the game frame queue. Is the physics stage broken?")
        }
        gameFramesQueue.add(gameFrame)
    }

    /**
     * Process queued game frames, tick the physics, then create a new physics frame
     */
    fun tickPhysics(gravity: Vector3dc, timeStep: Double, simulatePhysics: Boolean): VSPhysicsFrame {
        while (gameFramesQueue.isNotEmpty()) {
            val gameFrame = gameFramesQueue.remove()
            applyGameFrame(gameFrame)
        }
        physicsEngine.tick(gravity, timeStep, simulatePhysics)
        return createPhysicsFrame()
    }

    fun deleteResources() {
        if (physicsEngine.hasBeenDeleted()) throw IllegalStateException("Physics engine has already been deleted!")
        physicsEngine.deletePhysicsWorldResources()
    }

    private fun applyGameFrame(gameFrame: VSGameFrame) {
        TODO("Not yet implemented")
    }

    private fun createPhysicsFrame(): VSPhysicsFrame {
        TODO("Not yet implemented")
    }

}

data class ShipIdAndRigidBodyReference(val shipId: UUID, val rigidBodyReference: RigidBodyReference)
