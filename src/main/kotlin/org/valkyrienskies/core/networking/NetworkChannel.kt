package org.valkyrienskies.core.networking

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.valkyrienskies.core.game.IPlayer
import org.valkyrienskies.core.hooks.CoreHooks
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
        val handlers = clientHandlers.get(packet.type.id)

        globalClientHandlers.forEach { it.handlePacket(packet) }
        handlers?.forEach { it.handlePacket(packet) }

        if (globalClientHandlers.isEmpty() && (handlers == null || handlers.isEmpty())) {
            println("WARN: received a packet ${packet.type.name} on the client, but no handlers were registered")
        }
    }

    /**
     * To be called by Forge or Fabric networking
     */
    fun onReceiveServer(data: ByteBuf, player: IPlayer) {
        val packet = bytesToPacket(data)
        println("Server received packet of type: ${packet.type}")
        val handlers = serverHandlers.get(packet.type.id)

        globalServerHandlers.forEach { it.handlePacket(packet, player) }
        handlers?.forEach { it.handlePacket(packet, player) }

        if (globalServerHandlers.isEmpty() && (handlers == null || handlers.isEmpty())) {
            println("WARN: received a packet ${packet.type.name} on the server, but no handlers were registered")
        }
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

    fun sendToClients(packet: Packet, vararg players: IPlayer) {
        players.forEach { player -> sendToClient(packet, player) }
    }

    fun sendToAllClients(packet: Packet) {
        val shipWorld = requireNotNull(CoreHooks.currentShipServerWorld) {
            "Tried to send a packet of type ${packet.type} to all clients, but there is no server currently running!"
        }

        shipWorld.players.forEach { player -> sendToClient(packet, player) }
    }

    /**
     * To be implemented by Forge or Fabric networking. Should not be called.
     */
    lateinit var rawSendToServer: (data: ByteBuf) -> Unit

    /**
     * To be implemented by Forge or Fabric networking. Should not be called.
     */
    lateinit var rawSendToClient: (data: ByteBuf, player: IPlayer) -> Unit
}
