package org.valkyrienskies.core.game.ships.networking

import io.netty.buffer.ByteBuf
import kotlinx.coroutines.launch
import org.valkyrienskies.core.game.ships.ShipDataCommon
import org.valkyrienskies.core.game.ships.ShipId
import org.valkyrienskies.core.game.ships.ShipObjectClient
import org.valkyrienskies.core.game.ships.ShipObjectClientWorld
import org.valkyrienskies.core.game.ships.ShipTransform
import org.valkyrienskies.core.networking.Packet
import org.valkyrienskies.core.networking.Packets
import org.valkyrienskies.core.networking.RegisteredHandler
import org.valkyrienskies.core.networking.VSCryptUtils
import org.valkyrienskies.core.networking.VSNetworking
import org.valkyrienskies.core.networking.VSNetworking.tryUdpClient
import org.valkyrienskies.core.networking.impl.PacketShipDataCreate
import org.valkyrienskies.core.networking.impl.PacketShipRemove
import org.valkyrienskies.core.networking.simple.registerClientHandler
import org.valkyrienskies.core.networking.unregisterAll
import org.valkyrienskies.core.pipelines.VSNetworkPipelineStage
import org.valkyrienskies.core.util.logger
import org.valkyrienskies.core.util.read3FAsNormQuatd
import org.valkyrienskies.core.util.readVec3d
import org.valkyrienskies.core.util.readVec3fAsDouble
import org.valkyrienskies.core.util.serialization.VSJacksonUtil
import java.net.SocketAddress
import javax.crypto.SecretKey

class ShipObjectNetworkManagerClient(
    private val parent: ShipObjectClientWorld
) {

    private val worldScope get() = parent.coroutineScope

    private lateinit var handlers: List<RegisteredHandler>

    private var secretKey: SecretKey? = null

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
        secretKey = null
    }

    private fun onShipDataRemove(packet: PacketShipRemove) = worldScope.launch {
        packet.toRemove.forEach(parent::removeShip)
    }

    private fun onShipDataCreate(packet: PacketShipDataCreate) = worldScope.launch {
        for (ship in packet.toCreate) {
            if (parent.queryableShipData.getById(ship.id) == null) {
                parent.addShip(ship)
            } else {
                // Update the next ship transform
                parent.shipObjects[ship.id]?.nextShipTransform = ship.shipTransform

                throw logger.throwing(
                    IllegalArgumentException("Received ship create packet for already loaded ship?!")
                )
            }
        }
    }

    private fun onShipDataDelta(packet: Packet) = worldScope.launch {
        val buf = packet.data

        while (buf.isReadable) {
            val shipId = buf.readLong()

            val ship = parent.shipObjects[shipId]
            if (ship == null) {
                logger.warn("Received ship data delta for ship with unknown ID!")
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

    private fun onShipTransform(packet: Packet) {
        val buf = packet.data
        readShipTransform(buf, parent.shipObjects)
    }

    private var serverNoUdp = false
    private var tryConnectIn = 100
    fun tick(server: SocketAddress) {
        if (!VSNetworking.clientUsesUDP && !serverNoUdp) {
            tryConnectIn--
            if (tryConnectIn <= 0) {
                secretKey = VSCryptUtils.generateAES128Key()
                tryUdpClient(server, secretKey!!) { supports: Boolean ->
                    if (!supports) {
                        serverNoUdp = true
                    }
                }
                tryConnectIn = 100
            }
        }
    }

    companion object {
        private val logger by logger()

        // Reads all ship transforms in a buffer and places them in the latestReceived map.
        internal fun readShipTransform(buf: ByteBuf, shipObjects: Map<ShipId, ShipObjectClient>) {
            val tickNum = buf.readInt()
            while (buf.isReadable) {
                val shipId = buf.readLong()
                val ship = shipObjects[shipId]
                if (ship == null) {
                    logger.warn("Received ship data delta for ship with unknown ID!")
                    buf.skipBytes(VSNetworkPipelineStage.TRANSFORM_SIZE - 8)
                } else if (ship.latestNetworkTTick >= tickNum) {
                    // Skip the transform if we already have it
                    buf.skipBytes(VSNetworkPipelineStage.TRANSFORM_SIZE - 8)
                } else {
                    ship.latestNetworkTTick = tickNum

                    val centerOfMass = buf.readVec3d()
                    val scaling = buf.readVec3fAsDouble()
                    val rotation = buf.read3FAsNormQuatd()
                    val position = buf.readVec3d()

                    ship.latestNetworkTransform = ShipTransform.createFromCoordinatesAndRotationAndScaling(
                        position, centerOfMass, rotation, scaling
                    )
                }
            }
        }
    }
}
