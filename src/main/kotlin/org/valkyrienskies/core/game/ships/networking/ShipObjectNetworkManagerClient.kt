package org.valkyrienskies.core.game.ships.networking

import kotlinx.coroutines.launch
import org.valkyrienskies.core.game.ships.ShipDataCommon
import org.valkyrienskies.core.game.ships.ShipId
import org.valkyrienskies.core.game.ships.ShipObjectClientWorld
import org.valkyrienskies.core.game.ships.ShipTransform
import org.valkyrienskies.core.networking.Packet
import org.valkyrienskies.core.networking.Packets
import org.valkyrienskies.core.networking.RegisteredHandler
import org.valkyrienskies.core.networking.impl.PacketShipDataCreate
import org.valkyrienskies.core.networking.impl.PacketShipRemove
import org.valkyrienskies.core.networking.simple.registerClientHandler
import org.valkyrienskies.core.networking.unregisterAll
import org.valkyrienskies.core.util.readQuatd
import org.valkyrienskies.core.util.readVec3d
import org.valkyrienskies.core.util.serialization.VSJacksonUtil

class ShipObjectNetworkManagerClient(
    private val parent: ShipObjectClientWorld
) {

    private val worldScope get() = parent.coroutineScope

    private val latestReceived = HashMap<ShipId, Int>()

    private lateinit var handlers: List<RegisteredHandler>

    fun registerPacketListeners() {
        handlers = listOf(
            Packets.UDP_SHIP_TRANSFORM.registerClientHandler(this::onShipTransform),
            Packets.TCP_SHIP_DATA_DELTA.registerClientHandler(this::onShipDataDelta),
            PacketShipDataCreate::class.registerClientHandler(this::onShipDataCreate),
            PacketShipRemove::class.registerClientHandler(this::onShipDataRemove)
        )
    }

    fun onDestroy() {
        handlers.unregisterAll()
    }

    private fun onShipDataRemove(packet: PacketShipRemove) = worldScope.launch {
        packet.toRemove.forEach(parent::removeShip)
    }

    private fun onShipDataCreate(packet: PacketShipDataCreate) = worldScope.launch {
        for (ship in packet.toCreate) {
            if (parent.queryableShipData.getById(ship.id) == null) {
                parent.addShip(ship)
            } else {
                // todo: just throw if this is the case, this should never happen
                println("WARN: Received ship create packet for already loaded ship?!")
                // Update the next ship transform
                parent.shipObjects[ship.id]?.updateNextShipTransform(ship.shipTransform)
            }
        }
    }

    private fun onShipDataDelta(packet: Packet) = worldScope.launch {
        val buf = packet.data

        while (buf.isReadable) {
            val shipId = buf.readLong()

            val ship = parent.shipObjects.get(shipId)
            if (ship == null) {
                println("Received ship data delta for ship with unknown ID!!")
                buf.release()
                return@launch
            }
            val shipDataJson = ship.shipDataChannel.decode(buf)

            VSJacksonUtil.deltaMapper
                .readerForUpdating(ship.shipData)
                .readValue<ShipDataCommon>(shipDataJson)
        }
        buf.release()
    }.also { packet.data.retain() }

    private fun onShipTransform(packet: Packet) = worldScope.launch {
        val buf = packet.data
        val tickNum = buf.readInt()

        while (buf.isReadable) {
            val shipId = buf.readLong()
            val latest = latestReceived[shipId] ?: Int.MIN_VALUE
            if (latest >= tickNum) {
                buf.skipBytes((3 + 3 + 4 + 3 + 3 + 3) * 8)
            } else {
                val shipObject = parent.shipObjects[shipId]
                if (shipObject == null) {
                    println("Received ship transform for ship with unknown ID!!")
                    buf.release()
                    return@launch
                }

                val centerOfMass = buf.readVec3d()
                val scaling = buf.readVec3d()
                val rotation = buf.readQuatd()
                val position = buf.readVec3d()
                val linearVelocity = buf.readVec3d()
                val angularVelocity = buf.readVec3d()

                shipObject.updateNextShipTransform(
                    ShipTransform.createFromCoordinatesAndRotationAndScaling(
                        position, centerOfMass, rotation, scaling
                    )
                )

                shipObject.shipData.physicsData.linearVelocity = linearVelocity
                shipObject.shipData.physicsData.angularVelocity = angularVelocity

                latestReceived[shipId] = tickNum
            }
        }
        buf.release()
    }.also { packet.data.retain() }
}
