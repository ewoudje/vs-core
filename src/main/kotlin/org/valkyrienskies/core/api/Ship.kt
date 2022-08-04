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
interface Ship {

    val shipTransform: ShipTransform

    val shipToWorld: Matrix4dc get() = shipTransform.shipToWorldMatrix
    val worldToShip: Matrix4dc get() = shipTransform.worldToShipMatrix
}
