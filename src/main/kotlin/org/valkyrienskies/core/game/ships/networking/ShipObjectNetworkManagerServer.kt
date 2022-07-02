package org.valkyrienskies.core.game.ships.networking

import com.fasterxml.jackson.databind.JsonNode
import io.netty.buffer.Unpooled
import org.valkyrienskies.core.game.IPlayer
import org.valkyrienskies.core.game.ships.ShipData
import org.valkyrienskies.core.game.ships.ShipObjectServerWorld
import org.valkyrienskies.core.networking.Packets
import org.valkyrienskies.core.networking.impl.PacketShipDataCreate
import org.valkyrienskies.core.networking.simple.sendToClient
import org.valkyrienskies.core.util.serialization.VSJacksonUtil
import org.valkyrienskies.core.util.writeQuatd
import org.valkyrienskies.core.util.writeVec3d

class ShipObjectNetworkManagerServer(
    private val parent: ShipObjectServerWorld
) {

    private val tracker = parent.chunkTracker

    private lateinit var players: Iterable<IPlayer>

    fun tick() {
        this.players = parent.players
        updateShipData()
        updateTracking()

        // todo: send transforms directly from the physics rather than on game tick
        sendTransforms()
    }

    private fun IPlayer.getTrackedShips(): Iterable<ShipData> {
        return tracker.getShipsPlayerIsWatching(this)
    }

    /**
     * Send create and destroy packets for ships that players have started/stopped watching
     */
    private fun updateTracking() {
        tracker.playersToShipsNewlyWatchingMap
            .forEach { (player, ships) -> startTracking(player, ships) }

        tracker.playersToShipsNoLongerWatchingMap
            .forEach { (player, ships) -> endTracking(player, ships) }

        tracker.playersToShipsNewlyWatchingMap.clear()
        tracker.playersToShipsNoLongerWatchingMap.clear()
    }

    private fun endTracking(player: IPlayer, shipsToNotTrack: Iterable<ShipData>) {
        val s = shipsToNotTrack.toList()
        if (s.isNotEmpty()) {
            println("${player.uuid} unwatched ships ${s.map { it.id }}")
        }
    }

    private fun startTracking(player: IPlayer, shipsToTrack: Iterable<ShipData>) {
        val s = shipsToTrack.toList()
        println("${player.uuid} watched ships: ${s.map { it.id }}")
        PacketShipDataCreate(s).sendToClient(player)
    }

    /**
     * Send ShipData deltas to players
     */
    private fun updateShipData() {
        players.forEach { player ->
            val buf = Unpooled.buffer()
            val newlyWatching = tracker.playersToShipsNewlyWatchingMap[player] ?: emptySet()
            val trackedShips = player.getTrackedShips()
                // bad time complexity; sue me
                .filter { tracked -> newlyWatching.none { tracked.id == it.id } }
                .map { parent.shipObjects[it.id]!! }

            trackedShips.forEach { ship ->
                buf.writeLong(ship.shipData.id)
                val json = VSJacksonUtil.deltaMapper.valueToTree<JsonNode>(ship.shipData)
                ship.shipDataChannel.encode(json, buf)
            }

            Packets.TCP_SHIP_DATA_DELTA.sendToClient(buf, player)
        }
    }

    /**
     * Send ship transforms to players
     */
    private fun sendTransforms() {
        players.forEach { player ->
            // Ships the player is tracking
            val trackedShips = player.getTrackedShips().toList()
            // Write ship transforms into a ByteBuf
            val buf = Unpooled.buffer()

            buf.writeInt(parent.tickNumber)

            trackedShips.forEach { ship ->
                val transform = ship.shipTransform
                val physicsData = ship.physicsData

                buf.writeLong(ship.id)
                buf.writeVec3d(transform.shipPositionInShipCoordinates)
                buf.writeVec3d(transform.shipCoordinatesToWorldCoordinatesScaling)
                buf.writeQuatd(transform.shipCoordinatesToWorldCoordinatesRotation)
                buf.writeVec3d(transform.shipPositionInWorldCoordinates)
                buf.writeVec3d(physicsData.linearVelocity)
                buf.writeVec3d(physicsData.angularVelocity)
            }

            // Send it to the player
            Packets.UDP_SHIP_TRANSFORM.sendToClient(buf, player)
        }
    }
}

