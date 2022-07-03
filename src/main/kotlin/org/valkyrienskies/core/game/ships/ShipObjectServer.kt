package org.valkyrienskies.core.game.ships

import com.google.common.collect.MutableClassToInstanceMap
import org.valkyrienskies.core.networking.delta.DeltaEncodedChannelServerTCP
import org.valkyrienskies.core.util.serialization.VSJacksonUtil

class ShipObjectServer(
    override val shipData: ShipData
) : ShipObject(shipData) {

    internal val shipDataChannel = DeltaEncodedChannelServerTCP(
        jsonDiffDeltaAlgorithm,
        VSJacksonUtil.deltaMapper.valueToTree(shipData)
    )

    // runtime attached data only server-side, cus syncing to clients would be pain
    private val attachedData = MutableClassToInstanceMap.create<Any>()

    // Java friendly
    fun <T> setAttachment(clazz: Class<T>, value: T) {
        attachedData[clazz] = value
    }

    // Kotlin Only Inlining
    inline fun <reified T> setAttachment(value: T) = setAttachment(T::class.java, value)

    // Java friendly
    fun <T> getAttachment(clazz: Class<T>) = attachedData[clazz]

    // Kotlin Only Inlining
    inline fun <reified T> getAttachment() = getAttachment(T::class.java)

    companion object {
        private const val DEFAULT_CHUNK_WATCH_DISTANCE = 128.0
        private const val DEFAULT_CHUNK_UNWATCH_DISTANCE = 192.0
    }
}
