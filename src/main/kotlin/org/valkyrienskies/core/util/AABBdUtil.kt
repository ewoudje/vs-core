package org.valkyrienskies.core.util

import org.joml.Vector3dc
import org.joml.primitives.AABBd

fun AABBd.expand(expansion: Double): AABBd {
    minX -= expansion
    minY -= expansion
    minZ -= expansion
    maxX += expansion
    maxY += expansion
    maxZ += expansion
    return this
}

fun AABBd.extend(extension: Vector3dc): AABBd {
    if (extension.x() > 0) maxX += extension.x() else minX += extension.x()
    if (extension.y() > 0) maxY += extension.y() else minY += extension.y()
    if (extension.z() > 0) maxZ += extension.z() else minZ += extension.z()
    return this
}
