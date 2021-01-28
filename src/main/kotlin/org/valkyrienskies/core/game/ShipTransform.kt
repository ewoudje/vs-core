package org.valkyrienskies.core.game

import org.joml.Matrix4dc
import org.joml.Vector3dc

data class ShipTransform(val shipSpaceToWorldSpace: Matrix4dc, val shipPositionInWorld: Vector3dc) {

}