package org.valkyrienskies.core.pipelines

import org.valkyrienskies.core.game.ships.ShipData
import org.valkyrienskies.core.game.ships.ShipObjectServerWorld
import org.valkyrienskies.physics_api.voxel_updates.IVoxelShapeUpdate
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class VSGamePipelineStage {
    private val shipWorlds: MutableMap<Int, ShipObjectServerWorld> = HashMap()
    private val physicsFramesQueue: ConcurrentLinkedQueue<VSPhysicsFrame> = ConcurrentLinkedQueue()

    /**
     * Push a physics frame to the game stage
     */
    fun pushPhysicsFrame(physicsFrame: VSPhysicsFrame) {
        if (physicsFramesQueue.size >= 100) {
            throw IllegalStateException("Too many physics frames in the physics frame queue. Is the game stage broken?")
        }
        physicsFramesQueue.add(physicsFrame)
    }

    /**
     * Apply queued physics frames to the game
     */
    fun preTickGame() {
        while (physicsFramesQueue.isNotEmpty()) {
            val physicsFrame = physicsFramesQueue.remove()
            applyPhysicsFrame(physicsFrame)
        }
    }

    /**
     * Create a new game frame to be sent to the physics
     */
    fun postTickGame(): VSGameFrame {
        // Finally, return the game frame
        return createGameFrame()
    }

    private fun applyPhysicsFrame(physicsFrame: VSPhysicsFrame) {
        physicsFrame.shipDataMap.forEach { (uuid, shipInPhysicsFrameData) ->
            val dimension = shipInPhysicsFrameData.dimensionId
            val shipWorld: ShipObjectServerWorld? = shipWorlds[dimension]
            if (shipWorld != null) {
                val shipData: ShipData? = shipWorld.queryableShipData.getShipDataFromUUID(uuid)
                if (shipData != null) {
                    TODO("Apply the transform from the physics to the ship here")
                } else {
                    println("Received physics frame update for ship with uuid: $uuid and dimension $dimension, but a ship with this uuid does not exist!")
                }
            } else {
                println("Received physics frame update for ship with uuid: $uuid and dimension $dimension, but a world with this dimension does not exist!")
            }
        }
    }

    private fun createGameFrame(): VSGameFrame {
        val newShips = ArrayList<NewShipInGameFrameData>() // Ships to be added to the Physics simulation
        val deletedShips = ArrayList<UUID>() // Ships to be deleted from the Physics simulation
        val voxelUpdatesMap = HashMap<UUID, List<IVoxelShapeUpdate>>() // Voxel updates applied by this frame
        val tasks: Queue<() -> Unit> = LinkedList()

        shipWorlds.forEach { (dimension, shipWorld) ->
            shipWorld.shipObjects.forEach { (uuid, shipObject) ->
                TODO("Implement this")
            }
        }
        return VSGameFrame(newShips, deletedShips, voxelUpdatesMap, tasks)
    }

    fun addShipWorld(shipWorld: ShipObjectServerWorld) {
        val dimension = shipWorld.dimension
        if (shipWorlds.containsKey(dimension)) throw IllegalStateException("Ship world with dimension $dimension already exists!")
        shipWorlds[dimension] = shipWorld
    }

    fun removeShipWorld(shipWorld: ShipObjectServerWorld) {
        shipWorlds.remove(shipWorld.dimension)
    }
}
