package org.valkyrienskies.core.networking.delta

import io.netty.buffer.ByteBuf
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap
import org.valkyrienskies.core.networking.NetworkChannel

/**
 * Manages delta encoded communications to keep a collection of objects with IDs in sync.
 *
 * [T] is the type of object being delta-encoded
 */
class DeltaEncodedChannelServer<T>(
    private val algorithm: DeltaAlgorithm<T>,
    clients: Iterable<NetworkChannel>
) {

    data class Snapshot<T>(val index: Int, val data: T)

    /**
     * Contains the snapshot index -> snapshot
     */
    private val history = Int2ObjectRBTreeMap<T>()

    /**
     * Contains the client -> last acknowledged snapshots
     */
    private val clientHistory = HashMap<NetworkChannel, Snapshot<T>>()

    init {
        // clients.forEach { clientHistory[it] = Snapshot(0) }
    }

    fun receiveAck(client: NetworkChannel, idx: Int, dest: ByteBuf): ByteBuf {
        return dest
    }

    fun initialize(obj: T) {
        history.put(0, obj)
    }

    fun send(newSnapshot: T) {
    }
}
