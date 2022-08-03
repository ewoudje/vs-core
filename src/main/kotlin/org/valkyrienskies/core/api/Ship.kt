package org.valkyrienskies.core.api

import org.joml.Matrix4dc
import org.valkyrienskies.core.game.ships.ShipObjectClient
import org.valkyrienskies.core.game.ships.ShipTransform

/**
 * Abstraction of a ship, there are many types such as offline ships
 *  or loaded ships so this is the generic interface for all ships.
 *
 *  But this is for Server side only see [ShipObjectClient] for client side.
 */
interface Ship : ShipProvider {

    val shipTransform: ShipTransform

    val shipToWorld: Matrix4dc get() = shipTransform.shipToWorldMatrix
    val worldToShip: Matrix4dc get() = shipTransform.worldToShipMatrix

    /**
     * Gets from the ship storage the specified class
     *  it tries it first from the non-persistent storage
     *  and afterwards from the persistent storage
     * @param T
     * @param clazz of T
     * @return the data stored inside the ship
     */
    fun <T> getAttachment(clazz: Class<T>): T?

    /**
     * Sets data in the non-persistent storage
     *  if you are using a ship that is not loaded in
     *  it will do nothing
     *
     * @param T
     * @param clazz of T
     * @param value the data that will be stored, if null will be removed
     */
    fun <T> setAttachment(clazz: Class<T>, value: T?)

    override val ship: Ship
        get() = this
}

inline fun <reified T> Ship.getAttachment() = getAttachment(T::class.java)
inline fun <reified T> Ship.setAttachment(value: T?) = setAttachment(T::class.java, value)
