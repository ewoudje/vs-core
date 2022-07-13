package org.valkyrienskies.core.pipelines

import io.netty.buffer.Unpooled
import org.valkyrienskies.core.game.ships.ShipObjectServerWorld
import org.valkyrienskies.core.networking.Packets
import org.valkyrienskies.core.util.writeQuatd
import org.valkyrienskies.core.util.writeVec3d

class VSNetworkPipelineStage(private val shipWorld: ShipObjectServerWorld) {

    /**
     * Push a physics frame to the game stage
     */
    fun pushPhysicsFrame(physicsFrame: VSPhysicsFrame) {
        val ships = physicsFrame.shipDataMap

        shipWorld.networkManager.playersToTrackedShips.forEach { (player, trackedShips) ->

            // Write ship transforms into a ByteBuf
            val buf = Unpooled.buffer()

            buf.writeInt(shipWorld.tickNumber)

            trackedShips.forEach { shipData ->
                val physicsFrameData = ships.getValue(shipData.id)
                val transform = VSGamePipelineStage.generateTransformFromPhysicsFrameData(physicsFrameData, shipData)

                buf.writeLong(shipData.id)
                buf.writeVec3d(transform.shipPositionInShipCoordinates)
                buf.writeVec3d(transform.shipCoordinatesToWorldCoordinatesScaling)
                buf.writeQuatd(transform.shipCoordinatesToWorldCoordinatesRotation)
                buf.writeVec3d(transform.shipPositionInWorldCoordinates)
                buf.writeVec3d(physicsFrameData.vel)
                buf.writeVec3d(physicsFrameData.omega)
            }

            // Send it to the player
            Packets.UDP_SHIP_TRANSFORM.sendToClient(buf, player)
        }
    }
}
