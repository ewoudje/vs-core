package org.valkyrienskies.core.game

import org.joml.Matrix4d
import org.joml.Matrix4dc
import org.joml.Vector3d

class ChunksTransform(
    /**
     * <0,0,0> to global transform
     */
    val originToGlobal: Matrix4d = Matrix4d(),
    val chunks: ChunkClaim
) {
    private val subspaceToOrigin = Vector3d(-chunks.x * 16.0, 0.0, -chunks.z * 16.0)

    private val _subspacetoGlobal = Matrix4d()
    val subspaceToGlobal: Matrix4dc
        get() = originToGlobal.translate(subspaceToOrigin, _subspacetoGlobal)

    private val _globalToSubspace = Matrix4d()
    val globalToSubspace: Matrix4dc
        get() = subspaceToGlobal.invert(_globalToSubspace)
}