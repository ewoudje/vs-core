package org.valkyrienskies.core.game.ships

import com.google.common.collect.MutableClassToInstanceMap
import org.joml.Matrix4dc
import org.valkyrienskies.core.api.Ship
import org.valkyrienskies.core.api.ShipForcesInducer
import org.valkyrienskies.core.api.ShipUser
import org.valkyrienskies.core.api.Ticked
import org.valkyrienskies.core.networking.delta.DeltaEncodedChannelServerTCP
import org.valkyrienskies.core.util.serialization.VSJacksonUtil

class ShipObjectServer(
    override val shipData: ShipData
) : ShipObject(shipData), Ship {

    override val shipToWorld: Matrix4dc
        get() = shipData.shipToWorld
    override val worldToShip: Matrix4dc
        get() = shipData.worldToShip

    internal val shipDataChannel = DeltaEncodedChannelServerTCP(
        jsonDiffDeltaAlgorithm,
        VSJacksonUtil.deltaMapper.valueToTree(shipData)
    )

    // runtime attached data only server-side, cus syncing to clients would be pain
    internal val attachedData = MutableClassToInstanceMap.create<Any>()
    internal val forceInducers = mutableListOf<ShipForcesInducer>()
    internal val toBeTicked = mutableListOf<Ticked>()

    init {
        for (data in shipData.persistentAttachedData) {
            applyAttachmentInterfaces(data.key, data.value)
        }
    }

    override fun <T> setAttachment(clazz: Class<T>, value: T?) {
        if (value == null) {
            val r = attachedData.remove(clazz)
            forceInducers.remove(r)
            toBeTicked.remove(r)
        } else {
            applyAttachmentInterfaces(clazz, value)
            attachedData[clazz] = value
        }
    }

    override fun <T> getAttachment(clazz: Class<T>): T? =
        attachedData[clazz] as T? ?: shipData.getAttachment(clazz) as T?

    override fun <T> saveAttachment(clazz: Class<T>, value: T?) {
        applyAttachmentInterfaces(clazz, value)

        shipData.saveAttachment(clazz, value)
    }

    private fun applyAttachmentInterfaces(clazz: Class<*>, value: Any?) {
        if (ShipForcesInducer::class.java.isAssignableFrom(clazz)) {
            forceInducers.add(value as ShipForcesInducer)
        }

        if (ShipUser::class.java.isAssignableFrom(clazz)) {
            (value as ShipUser).ship = this
        }

        if (Ticked::class.java.isAssignableFrom(clazz)) {
            toBeTicked.add(value as Ticked)
        }
    }

    companion object {
        private const val DEFAULT_CHUNK_WATCH_DISTANCE = 128.0
        private const val DEFAULT_CHUNK_UNWATCH_DISTANCE = 192.0
    }
}
