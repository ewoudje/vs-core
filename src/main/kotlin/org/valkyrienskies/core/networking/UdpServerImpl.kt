package org.valkyrienskies.core.networking

import com.google.common.collect.HashBiMap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext
import org.bouncycastle.tls.DTLSServerProtocol
import org.bouncycastle.tls.DTLSTransport
import org.bouncycastle.tls.DefaultTlsServer
import org.bouncycastle.tls.UDPTransport
import org.valkyrienskies.core.game.IPlayer
import org.valkyrienskies.core.networking.impl.PacketRequestUdp
import org.valkyrienskies.core.util.logger
import java.net.DatagramSocket
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class UdpServerImpl(val socket: DatagramSocket, val channel: NetworkChannel) : AutoCloseable {
    @OptIn(DelicateCoroutinesApi::class)
    private val networkPool = newFixedThreadPoolContext(3, "UdpServer")

    // Send data
    @OptIn(DelicateCoroutinesApi::class)
    private val sendContext = newSingleThreadContext("UdpServerSend")
    private val buffer = ByteArray(PACKET_SIZE)

    // Encryption
    private val mtu = 1500
    private val protocol = DTLSServerProtocol()
    private val udpTransport = UDPTransport(socket, mtu)
    private val tls = object : DefaultTlsServer(Encryption.crypto) {}

    // Connection data
    private val connections = HashBiMap.create<DTLSTransport, IPlayer>()
    private val identification = Long2ObjectArrayMap<IPlayer>() // TODO remove them after a while
    private val playerSecrets = HashMap<IPlayer, SecretKey>()

    // Debug data
    private var packetCount = AtomicInteger(0)
    private var lastPacketPrint = System.currentTimeMillis()
    private var failedConnectionsInRow = 0

    private var shutdown = false

    init {
        channel.rawSendToClient = ::sendToClient
        socket.sendBufferSize = PACKET_SIZE * 60
        socket.receiveBufferSize = PACKET_SIZE * 60
        GlobalScope.launch(networkPool) { acceptConnections() }
    }

    private fun sendToClient(buf: ByteBuf, player: IPlayer) {
        val transport = connections.inverse()[player]
        if (transport == null) {
            Packets.TCP_UDP_FALLBACK.sendToClient(buf, player)
        } else GlobalScope.launch(sendContext) {
            packetCount.incrementAndGet()
            if (lastPacketPrint + 1000 < System.currentTimeMillis()) {
                logger.info("Sended $packetCount UDP packets")
                packetCount.set(0)
                lastPacketPrint = System.currentTimeMillis()
            }

            if (buf.hasArray()) {
                transport.send(buf.array(), buf.arrayOffset(), buf.writerIndex())
            } else {
                buf.readBytes(buffer, 0, buf.writerIndex())
                transport.send(buffer, 0, buf.writerIndex())
            }
        }
    }

    // Is blocking ;-;
    private fun acceptConnections() {
        VSNetworking.serverUsesUDP = true

        while (!shutdown) {
            try {
                val transport = protocol.accept(tls, udpTransport) ?: continue
                GlobalScope.launch(networkPool) {
                    newConnection(transport)
                }
            } catch (e: Exception) {
                logger.error("Error in server network connection thread", e)
            }
        }
    }

    private suspend fun newConnection(transport: DTLSTransport) = coroutineScope {
        try {
            val bytes = ByteArray(PACKET_SIZE)
            val buffer = Unpooled.wrappedBuffer(bytes)
            try {
                transport.receive(bytes, 0, 8, 1000)
            } catch (e: TimeoutException) {
                logger.warn("Player timeout while waiting for identification")
                return@coroutineScope
            }
            val player = identification.remove(buffer.readLong()) ?: return@coroutineScope
            connections[transport] = player
            failedConnectionsInRow = 0

            sendToClient(
                Unpooled.buffer(16)
                    .writeLong(player.uuid.leastSignificantBits)
                    .writeLong(player.uuid.mostSignificantBits),
                player
            )

            while (connections.contains(transport)) {
                // We write this in a separate method for suspend reasons
                try {
                    waitForPacket(bytes, buffer, player, transport)
                } catch (e: Exception) {
                    logger.error("Error in server network thread", e)
                }
            }
        } finally {
            transport.close()
        }
    }

    private suspend fun waitForPacket(bytes: ByteArray, buffer: ByteBuf, player: IPlayer, transport: DTLSTransport) =
        coroutineScope {
            buffer.clear()
            transport.receive(bytes, 0, PACKET_SIZE, 1)
            channel.onReceiveServer(buffer, player)
        }

    fun prepareIdentifier(player: IPlayer, packet: PacketRequestUdp): Long? =
        Random.nextLong().apply {
            val secretKeyBytes = packet.secretKeyBytes
            try {
                playerSecrets[player] = SecretKeySpec(secretKeyBytes, "AES")
            } catch (e: Exception) {
                logger.warn("Failed to parse secret from player ${player.uuid}", e)
                return null
            }
            identification.put(this, player)
            if (failedConnectionsInRow == 5) {
                logger.warn("There were 5 cases of failed connections in a row, is the UDP port accessible?")
                logger.info("By disabling UDP this warning shall not be printed anymore.")
            }
            failedConnectionsInRow++
        }

    fun disconnect(player: IPlayer) {
        connections.inverse().remove(player)
        identification.values.remove(player)
        playerSecrets.remove(player)
    }

    override fun close() {
        connections.forEach { (c, p) -> c.close() }
        networkPool.close()
        shutdown = false
    }

    companion object {
        const val PACKET_SIZE = 508 - Encryption.packetOverhead
        private val logger by logger()
    }
}
