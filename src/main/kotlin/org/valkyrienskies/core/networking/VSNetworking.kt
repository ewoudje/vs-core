package org.valkyrienskies.core.networking

import org.valkyrienskies.core.config.VSConfigClass

object VSNetworking {
    /**
     * Valkyrien Skies UDP channel
     */
    val UDP = NetworkChannel()

    /**
     * Valkyrien Skies TCP channel
     *
     * Should be initialized by Forge or Fabric (see [NetworkChannel])
     */
    val TCP = NetworkChannel()

    fun init() {
        registerUDP()
        Packets.init()
        VSConfigClass.registerNetworkHandlers()
    }

    private fun registerUDP() {
        // For now, "UDP" just always uses TCP fallback cause lazy

        UDP.rawSendToClient = { data, player ->
            Packets.TCP_UDP_FALLBACK.sendToClient(data, player)
        }

        UDP.rawSendToServer = { data ->
            Packets.TCP_UDP_FALLBACK.sendToServer(data)
        }

        TCP.registerClientHandler(Packets.TCP_UDP_FALLBACK) { packet ->
            UDP.onReceiveClient(packet.data)
        }

        TCP.registerServerHandler(Packets.TCP_UDP_FALLBACK) { packet, player ->
            UDP.onReceiveServer(packet.data, player)
        }
    }
}
