package org.valkyrienskies.core.game

import org.joml.Matrix3dc
import org.joml.Vector3dc

data class ShipInertiaData(val centerOfMassInShipSpace: Vector3dc, val shipMass: Double, val momentOfInertiaTensor: Matrix3dc) {
}