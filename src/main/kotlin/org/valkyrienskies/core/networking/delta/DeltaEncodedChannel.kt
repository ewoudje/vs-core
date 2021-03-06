package org.valkyrienskies.core.networking.delta

import io.netty.buffer.ByteBuf

/**
 * Manages delta encoded communications to keep a collection of objects with IDs in sync.
 *
 * [T] is the type of object being delta-encoded
 * [ID] is the type of the identifier of that object
 */
class DeltaEncodedChannel<T, ID>(
    private val algorithm: DeltaAlgorithm<T>
) {

    /**
     * Contains the ID -> previous
     */
    private val history = HashMap<ID, T>()

    fun register(id: ID, obj: T) {
        history[id] = obj
    }

    fun encode(id: ID, obj: T, dest: ByteBuf): ByteBuf {
        algorithm.encode(getPrevious(id), obj, dest)
        history[id] = obj
        return dest
    }

    fun decode(id: ID, delta: ByteBuf): T {
        val result = algorithm.apply(getPrevious(id), delta)
        history[id] = result
        return result
    }

    private fun getPrevious(id: ID): T {
        return requireNotNull(history[id]) { "Object with ID $id not registered" }
    }
}
