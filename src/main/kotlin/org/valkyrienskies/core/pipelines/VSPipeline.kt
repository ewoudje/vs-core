package org.valkyrienskies.core.pipelines

import org.joml.Vector3dc
import org.valkyrienskies.core.game.ships.ShipObjectServerWorld

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

    fun preTickGame() {
        gameStage.preTickGame()
    }

    fun postTickGame() {
        val gameFrame = gameStage.postTickGame()
        physicsStage.pushGameFrame(gameFrame)
    }

    fun tickPhysics(gravity: Vector3dc, timeStep: Double, simulatePhysics: Boolean) {
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

    companion object {
        private var INSTANCE: VSPipeline? = null

        fun createVSPipeline() {
            if (INSTANCE != null) throw IllegalStateException("INSTANCE is not null!")
            INSTANCE = VSPipeline()
        }

        fun deleteVSPipeline() {
            INSTANCE = null
        }

        fun getVSPipeline(): VSPipeline {
            return INSTANCE!!
        }
    }
}
