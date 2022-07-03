package org.valkyrienskies.core.networking

import org.valkyrienskies.core.networking.impl.PacketShipDataCreate
import org.valkyrienskies.core.networking.impl.PacketShipRemove
import org.valkyrienskies.core.networking.simple.register

/**
 * Contains packets used by vs-core.
 */
object Packets {
    /**
     * TCP Packet used as fallback when no UDP channel available
     */
    val TCP_UDP_FALLBACK = VSNetworking.TCP.registerPacket("UDP fallback")
    
    val TCP_SHIP_DATA_DELTA = VSNetworking.TCP.registerPacket("Ship data delta update")

    val UDP_SHIP_TRANSFORM = VSNetworking.UDP.registerPacket("Ship transform update")

    init {
        PacketShipDataCreate::class.register()
        PacketShipRemove::class.register()
    }

    // no-op to force the class to load
    internal fun init() {}
}
