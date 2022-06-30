package org.valkyrienskies.core.networking

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.valkyrienskies.core.game.IPlayer
import java.util.function.IntFunction

/**
 * Before use: set [rawSendToServer] and [rawSendToClient], and ensure that [onReceiveServer] and [onReceiveClient]
 * are called appropriately when packets are received.
 */
class NetworkChannel {

    private val packetTypes = ArrayList<PacketType>()
    private val serverHandlers = Int2ObjectOpenHashMap<MutableSet<ServerHandler>>()
    private val clientHandlers = Int2ObjectOpenHashMap<MutableSet<ClientHandler>>()
    private val globalServerHandlers = HashSet<ServerHandler>()
    private val globalClientHandlers = HashSet<ClientHandler>()

    /**
     * Allocate a new packet type. This should be always be called in the same order, on startup, on both server and
     * client. Otherwise packet IDs will not be correct.
     */
    fun registerPacket(name: String): PacketType {
        return PacketType(channel = this, id = packetTypes.size, name)
            .also { packetTypes.add(it) }
    }

    fun registerGlobalServerHandler(handler: ServerHandler): RegisteredHandler {
        globalServerHandlers.add(handler)
        return RegisteredHandler { globalServerHandlers.remove(handler) }
    }

    fun registerGlobalClientHandler(handler: ClientHandler): RegisteredHandler {
        globalClientHandlers.add(handler)
        return RegisteredHandler { globalClientHandlers.remove(handler) }
    }

    fun registerServerHandler(packetType: PacketType, handler: ServerHandler): RegisteredHandler {
        serverHandlers.computeIfAbsent(packetType.id, IntFunction { HashSet() }).add(handler)
        return RegisteredHandler { serverHandlers[packetType.id]?.remove(handler) }
    }

    fun registerClientHandler(packetType: PacketType, handler: ClientHandler): RegisteredHandler {
        clientHandlers.computeIfAbsent(packetType.id, IntFunction { HashSet() }).add(handler)
        return RegisteredHandler { clientHandlers[packetType.id]?.remove(handler) }
    }

    /**
     * To be called by Forge or Fabric networking
     */
    fun onReceiveClient(data: ByteBuf) {
        val packet = bytesToPacket(data)
        globalClientHandlers.forEach { it.handlePacket(packet) }
        clientHandlers.get(packet.type.id)?.forEach { it.handlePacket(packet) }
    }

    /**
     * To be called by Forge or Fabric networking
     */
    fun onReceiveServer(data: ByteBuf, player: IPlayer) {
        val packet = bytesToPacket(data)
        globalServerHandlers.forEach { it.handlePacket(packet, player) }
        serverHandlers.get(packet.type.id)?.forEach { it.handlePacket(packet, player) }
    }

    private fun bytesToPacket(data: ByteBuf): Packet {
        val id = data.readInt()
        val type = packetTypes[id]
        return Packet(type, data)
    }

    private fun packetToBytes(packet: Packet): ByteBuf {
        val composite = Unpooled.compositeBuffer(2)
        val index = Unpooled.buffer(4).apply { writeInt(packet.type.id) }
        return composite.addComponents(true, index, packet.data)
    }

    fun sendToServer(packet: Packet) =
        rawSendToServer(packetToBytes(packet))

    fun sendToClient(packet: Packet, player: IPlayer) =
        rawSendToClient(packetToBytes(packet), player)

    /**
     * To be implemented by Forge or Fabric networking. Should not be called.
     */
    lateinit var rawSendToServer: (data: ByteBuf) -> Unit

    /**
     * To be implemented by Forge or Fabric networking. Should not be called.
     */
    lateinit var rawSendToClient: (data: ByteBuf, player: IPlayer) -> Unit
}
