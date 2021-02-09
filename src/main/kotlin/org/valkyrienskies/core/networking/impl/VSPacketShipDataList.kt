package org.valkyrienskies.core.networking.impl

import com.fasterxml.jackson.module.kotlin.readValue
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import org.valkyrienskies.core.game.ShipData
import org.valkyrienskies.core.networking.IVSPacket
import org.valkyrienskies.core.util.serialization.VSJacksonUtil

class VSPacketShipDataList private constructor(): IVSPacket {

    private lateinit var shipDataList: List<ShipData>

    fun getShipDataList() = shipDataList

    override fun write(byteBuf: ByteBuf) {
        byteBuf.writeInt(shipDataList.size)
        for (shipData in shipDataList) {
            val bytes = VSJacksonUtil.defaultMapper.writeValueAsBytes(shipData)
            byteBuf.writeInt(bytes.size)
            byteBuf.writeBytes(bytes)
        }
    }

    override fun read(byteBuf: ByteBuf) {
        val newShipDataList: MutableList<ShipData> = ArrayList()
        val listSize: Int = byteBuf.readInt()
        for (i in 1 .. listSize) {
            // Read how many bytes this ShipData is
            val bytesSize = byteBuf.readInt()
            // Create an input stream from byteBuf that is bytesSize bytes big
            val bytesAsInputStream = ByteBufInputStream(byteBuf, bytesSize)
            // Convert the input stream to a ShipData
            val shipDataFromBytes = VSJacksonUtil.defaultMapper.readValue<ShipData>(bytesAsInputStream)
            newShipDataList.add(shipDataFromBytes)
        }
        shipDataList = newShipDataList
    }

    companion object {
        fun createVSPacketShipDataList(shipDataCollection: Collection<ShipData>): VSPacketShipDataList {
            val packet = VSPacketShipDataList()
            packet.shipDataList = ArrayList(shipDataCollection)
            return packet
        }

        fun createEmptyVSPacketShipDataList(): VSPacketShipDataList {
            return VSPacketShipDataList()
        }
    }
}