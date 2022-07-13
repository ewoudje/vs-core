package org.valkyrienskies.core.networking.impl

import org.valkyrienskies.core.game.ships.ShipDataCommon
import org.valkyrienskies.core.networking.simple.SimplePacket

data class PacketShipDataList(
    val ships: List<ShipDataCommon>
) : SimplePacket
