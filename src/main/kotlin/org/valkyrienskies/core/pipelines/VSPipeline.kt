package org.valkyrienskies.core.pipelines

import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.game.ships.ShipObjectServerWorld
import kotlin.concurrent.thread

/**
 * A pipeline that moves data between the game, the physics, and the network stages.
 *
 * The Game stage sends [VSGameFrame]s to the Physics stage.
 *
 * The Physics stage sends [VSPhysicsFrame]s to the Game stage and to the Network stage.
 *
 * Game <--> Physics --> Network
 */
class VSPipeline private constructor() {
    private val gameStage = VSGamePipelineStage()
    private val physicsStage = VSPhysicsPipelineStage()
    private val networkStage = VSNetworkPipelineStage()

    private val physicsPipelineBackgroundTask: VSPhysicsPipelineBackgroundTask = VSPhysicsPipelineBackgroundTask(this)

    // The thread the physics engine runs on
    private val physicsThread: Thread = thread(start = true, priority = 8) {
        physicsPipelineBackgroundTask.run()
    }

    private var deleteResources = false

    fun preTickGame() {
        gameStage.preTickGame()
    }

    fun postTickGame() {
        val gameFrame = gameStage.postTickGame()
        physicsStage.pushGameFrame(gameFrame)
    }

    fun tickPhysics(gravity: Vector3dc, timeStep: Double, simulatePhysics: Boolean) {
        if (deleteResources) {
            physicsStage.deleteResources()
            physicsPipelineBackgroundTask.tellTaskToKillItself()
            return
        }
        val physicsFrame = physicsStage.tickPhysics(gravity, timeStep, simulatePhysics)
        gameStage.pushPhysicsFrame(physicsFrame)
        networkStage.pushPhysicsFrame(physicsFrame)
    }

    fun addShipWorld(shipWorld: ShipObjectServerWorld) {
        gameStage.addShipWorld(shipWorld)
    }

    fun removeShipWorld(shipWorld: ShipObjectServerWorld) {
        gameStage.removeShipWorld(shipWorld)
    }

    fun getPhysicsGravity(): Vector3dc {
        return Vector3d(0.0, -10.0, 0.0)
    }

    fun arePhysicsRunning(): Boolean {
        return true
    }

    fun computePhysTps(): Double {
        return physicsPipelineBackgroundTask.computePhysicsTPS()
    }

    companion object {
        private var INSTANCE: VSPipeline? = null

        fun createVSPipeline() {
            if (INSTANCE != null) throw IllegalStateException("INSTANCE is not null!")
            INSTANCE = VSPipeline()
        }

        fun deleteVSPipeline() {
            INSTANCE?.deleteResources = true
            INSTANCE = null
        }

        fun getVSPipeline(): VSPipeline {
            return INSTANCE!!
        }
    }
}
