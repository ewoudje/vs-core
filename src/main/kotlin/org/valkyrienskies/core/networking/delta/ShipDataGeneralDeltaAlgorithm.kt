package org.valkyrienskies.core.networking.delta

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.flipkart.zjsonpatch.JsonDiff
import com.flipkart.zjsonpatch.JsonPatch
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import org.valkyrienskies.core.game.ships.ShipData
import org.valkyrienskies.core.util.serialization.VSJacksonUtil
import org.valkyrienskies.core.util.serialization.PacketIgnore
import java.io.DataOutput

/**
 * Delta encodes every field of [ShipData] not marked with [PacketIgnore]
 */
object ShipDataGeneralDeltaAlgorithm : DeltaAlgorithm<ShipData> {

    override fun encode(old: ShipData, new: ShipData, dest: ByteBuf): ByteBuf {
        val mapper = VSJacksonUtil.packetMapper

        // Serialize both old and new ShipData into JSON
        val oldJson: JsonNode = mapper.valueToTree(old)
        val newJson: JsonNode = mapper.valueToTree(new)

        // Compute json-patch diff
        val diffJson = JsonDiff.asJson(oldJson, newJson)

        // Write the diff as bytes into [dest]
        mapper.writeValue(ByteBufOutputStream(dest) as DataOutput, diffJson)

        return dest
    }

    override fun apply(old: ShipData, delta: ByteBuf): ShipData {
        val mapper = VSJacksonUtil.packetMapper

        // Get the json-patch diff
        val diffJson: JsonNode = mapper.readValue(ByteBufInputStream(delta))
        // Serialize the old ShipData
        val json: JsonNode = mapper.valueToTree(old)

        // Apply the patch to [json]
        JsonPatch.applyInPlace(json, diffJson)

        // Deserialize the json (with the patch applied) back into ShipData
        return mapper.treeToValue(json)!!
    }
}
