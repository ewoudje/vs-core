package org.valkyrienskies.core.networking

import io.netty.buffer.ByteBuf
import org.valkyrienskies.core.game.IPlayer

data class PacketType(val channel: NetworkChannel, val id: Int, val name: String) {
    fun sendToServer(data: ByteBuf) =
        channel.sendToServer(Packet(this, data))

    fun sendToClient(data: ByteBuf, player: IPlayer) =
        channel.sendToClient(Packet(this, data), player)

    fun registerServerHandler(handler: ServerHandler) =
        channel.registerServerHandler(this, handler)

    fun registerClientHandler(handler: ClientHandler) =
        channel.registerClientHandler(this, handler)
}
