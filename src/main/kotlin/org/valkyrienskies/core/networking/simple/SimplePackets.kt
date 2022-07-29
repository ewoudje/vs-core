@file:JvmName("SimplePackets")

package org.valkyrienskies.core.networking.simple

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.valkyrienskies.core.game.IPlayer
import org.valkyrienskies.core.networking.PacketType
import org.valkyrienskies.core.networking.RegisteredHandler
import org.valkyrienskies.core.networking.VSNetworking
import org.valkyrienskies.core.util.serialization.VSJacksonUtil
import org.valkyrienskies.core.util.serialization.readValue
import kotlin.reflect.KClass

private val classToPacket = HashMap<Class<out SimplePacket>, SimplePacketInfo>()

private data class SimplePacketInfo(
    val type: PacketType,
    val serverHandlers: MutableList<(SimplePacket, IPlayer) -> Unit> = mutableListOf(),
    val clientHandlers: MutableList<(SimplePacket) -> Unit> = mutableListOf()
)

private fun Class<out SimplePacket>.getPacketType(): PacketType {
    return getSimplePacketInfo().type
}

private fun Class<out SimplePacket>.getSimplePacketInfo(): SimplePacketInfo {
    return requireNotNull(classToPacket[this]) { "SimplePacket ($this) not registered" }
}

fun SimplePacket.serialize(): ByteBuf {
    return Unpooled.wrappedBuffer(VSJacksonUtil.packetMapper.writeValueAsBytes(this))
}

fun <T : SimplePacket> KClass<T>.deserialize(buf: ByteBuf): T {
    return VSJacksonUtil.packetMapper.readValue(buf.duplicate(), this.java)
}

fun <T : SimplePacket> KClass<T>.registerServerHandler(handler: (T, IPlayer) -> Unit): RegisteredHandler {
    @Suppress("UNCHECKED_CAST")
    this.java.getSimplePacketInfo().serverHandlers.add(handler as (SimplePacket, IPlayer) -> Unit)

    return RegisteredHandler { this.java.getSimplePacketInfo().serverHandlers.remove(handler) }
}

fun <T : SimplePacket> KClass<T>.registerClientHandler(handler: (T) -> Unit): RegisteredHandler {
    @Suppress("UNCHECKED_CAST")
    this.java.getSimplePacketInfo().clientHandlers.add(handler as (SimplePacket) -> Unit)

    return RegisteredHandler { this.java.getSimplePacketInfo().clientHandlers.remove(handler) }
}

fun SimplePacket.sendToServer() {
    this::class.java.getPacketType().sendToServer(this.serialize())
}

fun SimplePacket.sendToClient(player: IPlayer) {
    this::class.java.getPacketType().sendToClient(this.serialize(), player)
}

fun SimplePacket.sendToClients(vararg players: IPlayer) {
    require(players.isNotEmpty())

    this::class.java.getPacketType().sendToClients(this.serialize(), *players)
}

fun SimplePacket.sendToAllClients() {
    this::class.java.getPacketType().sendToAllClients(this.serialize())
}

fun KClass<out SimplePacket>.register(name: String = "SimplePacket - ${this.java}") {
    check(this.isData) { "SimplePacket (${this.java}) must be a data class!" }

    val packetType = VSNetworking.TCP.registerPacket(name)
    val packetInfo = SimplePacketInfo(packetType)
    classToPacket[this.java] = packetInfo

    packetType.registerClientHandler { packet ->
        val data = this.deserialize(packet.data)
        packetInfo.clientHandlers.forEach { it(data) }
    }

    packetType.registerServerHandler { packet, player ->
        val data = this.deserialize(packet.data)
        packetInfo.serverHandlers.forEach { it(data, player) }
    }

    registerClientHandler(SimplePacket::receivedByClient)
    registerServerHandler(SimplePacket::receivedByServer)
}
