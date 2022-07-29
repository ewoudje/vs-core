package org.valkyrienskies.core.game.ships

import com.fasterxml.jackson.databind.JsonNode
import org.joml.primitives.AABBd
import org.joml.primitives.AABBdc
import org.valkyrienskies.core.networking.delta.DeltaEncodedChannelClientTCP
import org.valkyrienskies.core.util.serialization.VSJacksonUtil
import org.valkyrienskies.core.util.toAABBd

class ShipObjectClient(
    shipData: ShipDataCommon,
    shipDataJson: JsonNode = VSJacksonUtil.defaultMapper.valueToTree(shipData)
) : ShipObject(shipData) {
    // The last ship transform sent by the sever
    internal var nextShipTransform: ShipTransform

    // The transform used when rendering the ship
    var renderTransform: ShipTransform
        private set

    var renderAABB: AABBdc
        private set

    internal var latestNetworkTransform: ShipTransform = shipData.shipTransform
    internal var latestNetworkTTick = Int.MIN_VALUE

    val shipDataChannel = DeltaEncodedChannelClientTCP(jsonDiffDeltaAlgorithm, shipDataJson)

    init {
        nextShipTransform = shipData.shipTransform
        renderTransform = shipData.shipTransform
        renderAABB = shipData.shipTransform.createEmptyAABB()
    }

    fun tickUpdateShipTransform() {
        this.nextShipTransform = latestNetworkTransform
        shipData.updatePrevTickShipTransform()
        shipData.shipTransform = ShipTransform.createFromSlerp(shipData.shipTransform, nextShipTransform, EMA_ALPHA)
    }

    fun updateRenderShipTransform(partialTicks: Double) {
        renderTransform =
            ShipTransform.createFromSlerp(shipData.prevTickShipTransform, shipData.shipTransform, partialTicks)
        renderAABB = shipData.shipVoxelAABB?.toAABBd(AABBd())?.transform(renderTransform.shipToWorldMatrix, AABBd())
            ?: renderTransform.createEmptyAABB()
    }

    companion object {
        // Higher values will converge to the transforms sent by the server faster, but lower values make movement
        // smoother. Ideally this should be configurable, but leave it constant for now.
        private const val EMA_ALPHA = 0.7
    }
}
