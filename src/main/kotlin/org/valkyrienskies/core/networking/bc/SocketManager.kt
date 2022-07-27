package org.valkyrienskies.core.networking.bc

import com.google.common.collect.HashBiMap
import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.valkyrienskies.core.game.IPlayer
import org.valkyrienskies.core.networking.VSNetworking
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress
import java.util.UUID

class SocketManager(val socket: DatagramSocket, val newConnection: (NetworkPlayer) -> Unit) : AutoCloseable {
    private val thread = Thread(::run, "SocketManager")
    private val connections = mutableMapOf<SocketAddress, NetworkPlayer>()
    private val playerMap = HashBiMap.create<UUID, NetworkPlayer>()
    private val pooled = PooledByteBufAllocator()

    private val sendContext = newSingleThreadContext("SocketManagerSend")
    private val sendPacket = DatagramPacket(ByteArray(0), 0, 0)

    val recvContext = newSingleThreadContext("SocketManagerRecv")

    init {
        thread.start()
    }

    internal fun rawSend(buffer: ByteArray, off: Int, length: Int, player: NetworkPlayer) =
        GlobalScope.launch(sendContext) {
            if (off != 0) throw IllegalArgumentException("offset must be 0")
            if (buffer.size != length) throw IllegalArgumentException("buffer.length must be the same as length")

            sendPacket.data = buffer
            sendPacket.socketAddress = player.address
            socket.send(sendPacket)
        }

    // the resulting buffer should be released after use
    internal fun rawReceive(player: NetworkPlayer): CompletableDeferred<ByteBuf> {
        val r = player.packets.poll()
        if (r == null) {
            if (player.deferred != null) {
                player.deferred!!.completeExceptionally(
                    IllegalStateException("Second receive before this one was completed")
                )
            }

            player.deferred = CompletableDeferred()

            return player.deferred!!
        } else {
            return CompletableDeferred(r)
        }
    }

    private fun run() {
        var recvBuffer = pooled.heapBuffer(VSNetworking.UDP_PACKET_MAX)
        val packet = DatagramPacket(recvBuffer.array(), VSNetworking.UDP_PACKET_MAX)
        VSNetworking.serverUsesUDP = true
        while (!socket.isClosed) {
            socket.receive(packet)

            val player = connections[packet.socketAddress]

            if (player == null) {
                val newPlayer = NetworkPlayer(packet.socketAddress)
                connections[packet.socketAddress] = newPlayer
                newConnection(newPlayer)
            } else {
                recvBuffer = if (player.deferred != null) {
                    player.deferred!!.complete(recvBuffer)
                    recvBuffer.retain()
                    pooled.heapBuffer(VSNetworking.UDP_PACKET_MAX)
                } else {
                    player.packets.add(recvBuffer)
                    recvBuffer.retain()
                    pooled.heapBuffer(VSNetworking.UDP_PACKET_MAX)
                }
            }
        }
        VSNetworking.serverUsesUDP = false
    }

    fun getPlayer(player: IPlayer): NetworkPlayer? = playerMap[player.uuid]

    fun disconnect(player: IPlayer) {
        val p = playerMap[player.uuid]
        if (p != null) {
            connections.remove(p.address)
            playerMap.remove(player.uuid)
            p.isConnected = false
        }
    }

    override fun close() {
        playerMap.values.forEach { it.player?.let { disconnect(it) } }
        socket.close()
    }
}
