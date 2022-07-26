package org.valkyrienskies.core.networking

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bouncycastle.tls.DTLSServerProtocol
import org.bouncycastle.tls.DefaultTlsServer
import org.valkyrienskies.core.game.IPlayer
import org.valkyrienskies.core.networking.bc.NetworkPlayer
import org.valkyrienskies.core.networking.bc.PlayerTransport
import org.valkyrienskies.core.networking.bc.SocketManager
import org.valkyrienskies.core.util.logger
import java.net.DatagramSocket
import java.util.concurrent.atomic.AtomicInteger

class UdpServerImpl(socket: DatagramSocket, val channel: NetworkChannel) : AutoCloseable {
    private val socketManager: SocketManager = SocketManager(socket, ::newConnection)

    // Encryption
    private val mtu = 1500
    private val protocol = DTLSServerProtocol()
    private val tls = object : DefaultTlsServer(Encryption.crypto) {}

    // Debug data
    private var packetCount = AtomicInteger(0)
    private var lastPacketPrint = System.currentTimeMillis()
    private var failedConnectionsInRow = 0

    private var shutdown = false

    init {
        channel.rawSendToClient = ::sendToClient
    }

    private fun sendToClient(buf: ByteBuf, player: IPlayer) {
        val network = socketManager.getPlayer(player)
        if (network == null) {
            Packets.TCP_UDP_FALLBACK.sendToClient(buf, player)
        } else {
            packetCount.incrementAndGet()
            if (lastPacketPrint + 1000 < System.currentTimeMillis()) {
                logger.info("Sended $packetCount UDP packets")
                packetCount.set(0)
                lastPacketPrint = System.currentTimeMillis()
            }

            if (buf.hasArray()) {
                network.transport!!.send(buf.array())
            } else {
                val array = ByteArray(buf.readableBytes())
                buf.readBytes(array, 0, buf.readableBytes())
                network.transport!!.send(array)
            }
        }
    }

    private fun newConnection(player: NetworkPlayer) = GlobalScope.launch(socketManager.recvContext) {
        var transport: PlayerTransport? = null

        try {
            val array = ByteArray(VSNetworking.UDP_PACKET_SIZE)
            transport = PlayerTransport(socketManager, player) { udp -> protocol.accept(tls, udp) }
            failedConnectionsInRow = 0
            player.transport = transport

            sendToClient(
                Unpooled.buffer(16)
                    .writeLong(player.player!!.uuid.leastSignificantBits)
                    .writeLong(player.player!!.uuid.mostSignificantBits),
                player.player!!
            )

            while (player.isConnected) {
                // We write this in a separate method for suspend reasons
                try {
                    transport.receive(array)
                    val buf = Unpooled.wrappedBuffer(array)
                    channel.onReceiveServer(buf, player.player!!)
                } catch (e: Exception) {
                    logger.error("Error in server network thread", e)
                }
            }
        } finally {
            transport?.close()
        }
    }

    fun disconnect(player: IPlayer) {
        socketManager.disconnect(player)
    }

    override fun close() {
        socketManager.close()
        shutdown = false
    }

    companion object {
        private val logger by logger()
    }
}
