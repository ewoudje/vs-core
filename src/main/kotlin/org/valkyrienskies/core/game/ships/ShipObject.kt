package org.valkyrienskies.core.game.ships

import com.google.common.collect.MutableClassToInstanceMap

/**
 * A [ShipObject] is essentially a [ShipData] that has been loaded.
 *
 * Its just is to interact with the player. This includes stuff like rendering, colliding with entities, and adding
 * a rigid body to the physics engine.
 */
open class ShipObject(
    shipData: ShipDataCommon
) {
    @Suppress("CanBePrimaryConstructorProperty") // don't want to refer to open val in constructor
    open val shipData: ShipDataCommon = shipData

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
}
