package org.valkyrienskies.core.networking.bc

import io.netty.buffer.ByteBuf
import org.bouncycastle.tls.DatagramTransport
import org.valkyrienskies.core.networking.VSNetworking

class PlayerTransport(
    val manager: SocketManager, val player: NetworkPlayer,
    apiWrap: (DatagramTransport) -> DatagramTransport
) : AutoCloseable {

    // region suspend cached wrapping
    var cachedBuffer: ByteBuf? = null
    val udpTransport = object : DatagramTransport {
        override fun getReceiveLimit(): Int = VSNetworking.UDP_PACKET_MAX

        override fun receive(buf: ByteArray, off: Int, len: Int, waitMillis: Int): Int {
            val length = cachedBuffer!!.readableBytes()
            if (length > len)
                throw IllegalStateException("Cached buffer is larger than requested buffer")

            cachedBuffer!!.readBytes(buf, off, length)
            return length
        }

        override fun getSendLimit(): Int = VSNetworking.UDP_PACKET_MAX

        override fun send(buf: ByteArray, off: Int, len: Int) {
            manager.rawSend(buf, off, len, player)
        }

        override fun close() {}
    }
    val apiTransport = apiWrap(udpTransport)
    // endregion

    suspend fun receive(array: ByteArray) {
        cachedBuffer = manager.rawReceive(player).await()

        // The normally blocking call
        apiTransport.receive(array, 0, array.size, 0)
        cachedBuffer!!.release()
    }

    fun send(array: ByteArray) {
        apiTransport.send(array, 0, array.size)
    }

    override fun close() {
        apiTransport.close()
    }
}
