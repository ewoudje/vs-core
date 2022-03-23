package org.valkyrienskies.core.pipelines

import java.util.concurrent.ConcurrentLinkedQueue

class VSNetworkPipelineStage {
    private val physicsFramesQueue: ConcurrentLinkedQueue<VSPhysicsFrame> = ConcurrentLinkedQueue()

    /**
     * Push a physics frame to the game stage
     */
    fun pushPhysicsFrame(physicsFrame: VSPhysicsFrame) {
        // TODO: Implement this
        /*
        if (physicsFramesQueue.size >= 100) {
            throw IllegalStateException("Too many physics frames in the physics frame queue. Is the game stage broken?")
        }
        physicsFramesQueue.add(physicsFrame)
         */
    }

    /**
     * Process queued physics frames, tick the game, then create a new game frame
     */
    fun tickNetwork() {
        while (physicsFramesQueue.isNotEmpty()) {
            val physicsFrame = physicsFramesQueue.remove()
            applyPhysicsFrame(physicsFrame)
        }
        TODO("Send network updates to players")
    }

    private fun applyPhysicsFrame(physicsFrame: VSPhysicsFrame) {
        TODO("Not yet implemented")
    }
}
