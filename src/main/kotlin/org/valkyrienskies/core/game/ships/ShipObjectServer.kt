package org.valkyrienskies.core.game.ships

import org.valkyrienskies.core.networking.delta.DeltaEncodedChannelServerTCP
import org.valkyrienskies.core.util.serialization.VSJacksonUtil

class ShipObjectServer(
    override val shipData: ShipData
) : ShipObject(shipData) {

    internal val shipDataChannel = DeltaEncodedChannelServerTCP(
        jsonDiffDeltaAlgorithm,
        VSJacksonUtil.deltaMapper.valueToTree(shipData)
    )

    companion object {
        private const val DEFAULT_CHUNK_WATCH_DISTANCE = 128.0
        private const val DEFAULT_CHUNK_UNWATCH_DISTANCE = 192.0
    }
}
