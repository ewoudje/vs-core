package org.valkyrienskies.core.networking

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.apache.logging.log4j.message.StringFormattedMessage
import org.bouncycastle.tls.DTLSClientProtocol
import org.bouncycastle.tls.DTLSTransport
import org.bouncycastle.tls.DefaultTlsClient
import org.bouncycastle.tls.TlsAuthentication
import org.bouncycastle.tls.UDPTransport
import org.valkyrienskies.core.util.logger
import java.net.DatagramSocket
import java.net.SocketAddress

class UdpClientImpl(
    val socket: DatagramSocket,
    val channel: NetworkChannel,
    val server: SocketAddress,
    id: Long
) : AutoCloseable {
    private val thread = Thread { run(id) }

    private val mtu = 1500
    private val protocol = DTLSClientProtocol()
    private val udpTransport = UDPTransport(socket, mtu)
    private lateinit var transport: DTLSTransport
    private val tls = object : DefaultTlsClient(Encryption.crypto) {
        override fun getAuthentication(): TlsAuthentication = Encryption.clientAuth
    }

    private val recvBuffer = ByteArray(VSNetworking.UDP_PACKET_SIZE)
    private val sendBuffer = ByteArray(VSNetworking.UDP_PACKET_SIZE)

    init {
        channel.rawSendToServer = ::sendToServer
        socket.connect(server)

        socket.sendBufferSize = 508 * 60
        socket.receiveBufferSize = 508 * 60

        thread.start()
    }

    private fun sendToServer(buf: ByteBuf) {
        buf.readBytes(sendBuffer, 0, buf.writerIndex())
        transport.send(sendBuffer, 0, buf.writerIndex())
    }

    private fun run(id: Long) {
        transport = protocol.connect(tls, udpTransport)
        val buffer = Unpooled.wrappedBuffer(recvBuffer)

        // Sending connection id
        sendToServer(Unpooled.buffer(8).writeLong(id))

        // Initial confirmation packet
        val Rlength = transport.receive(sendBuffer, 0, VSNetworking.UDP_PACKET_SIZE, 1000)
        if (Rlength != 16) {
            throw IllegalStateException("Invalid confirmation packet")
        }
        // TODO check player uuid is the same

        VSNetworking.clientUsesUDP = true
        var packetCount = 0
        var lastPacketPrint = System.currentTimeMillis()

        while (!socket.isClosed) {
            try {
                buffer.clear()
                val packetLength = transport.receive(recvBuffer, 0, VSNetworking.UDP_PACKET_SIZE, 0)
                packetCount++
                // if (!recvPacket.socketAddress.equals(server)) {
                //    logger.warn("Received packet from non server address: ${recvPacket.socketAddress}")
                //    logger.warn("This is VERY SUSPICIOUS!")
                //    continue
                //}

                if (lastPacketPrint + 1000 < System.currentTimeMillis()) {
                    logger.info("Received $packetCount UDP packets")
                    packetCount = 0
                    lastPacketPrint = System.currentTimeMillis()
                }

                logger.trace { StringFormattedMessage("Client received packet of size ${packetLength}") }
                channel.onReceiveClient(Unpooled.wrappedBuffer(packetLength, buffer))
            } catch (e: Exception) {
                logger.error("Error in client network thread", e)
            }
        }
    }

    companion object {
        private val logger by logger()
    }

    override fun close() {
        transport.close()
    }
}
