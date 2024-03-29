package org.valkyrienskies.core.collision

import org.joml.Matrix4dc
import org.joml.Vector3d
import org.joml.primitives.AABBdc
import org.valkyrienskies.core.game.ships.ShipId

/**
 * A [TransformedCuboidPolygon] is a polygon whose shape is a cuboid that been transformed by a 4x4 transform matrix. It is guaranteed to have 8 [points] and 3 [normals].
 */
class TransformedCuboidPolygon private constructor(
    private val _points: List<Vector3d>, private val _normals: List<Vector3d>, _shipFrom: ShipId? = null
) : ConvexPolygon(_points, _normals, _shipFrom) {

    /**
     * Sets this [TransformedCuboidPolygon] to be the shape of [aabb] transformed by [transform].
     *
     * @return this
     */
    fun setFromAABB(aabb: AABBdc, transform: Matrix4dc? = null): TransformedCuboidPolygon {
        _points[0].set(aabb.minX(), aabb.minY(), aabb.minZ())
        _points[1].set(aabb.minX(), aabb.minY(), aabb.maxZ())
        _points[2].set(aabb.minX(), aabb.maxY(), aabb.minZ())
        _points[3].set(aabb.minX(), aabb.maxY(), aabb.maxZ())
        _points[4].set(aabb.maxX(), aabb.minY(), aabb.minZ())
        _points[5].set(aabb.maxX(), aabb.minY(), aabb.maxZ())
        _points[6].set(aabb.maxX(), aabb.maxY(), aabb.minZ())
        _points[7].set(aabb.maxX(), aabb.maxY(), aabb.maxZ())

        _normals[0].set(1.0, 0.0, 0.0)
        _normals[1].set(0.0, 1.0, 0.0)
        _normals[2].set(0.0, 0.0, 1.0)

        if (transform != null) {
            for (point in _points) transform.transformPosition(point)
            for (normal in _normals) transform.transformDirection(normal).normalize()
        }
        return this
    }

    companion object {
        private const val NUMBER_OF_POINTS = 8
        private const val NUMBER_OF_NORMALS = 3

        /**
         * Creates an empty polygon with 8 points and 3 vertices, all of them set to (0,0,0).
         */
        private fun createEmptyRectangularPrismPolygon(shipFrom: ShipId? = null): TransformedCuboidPolygon {
            val points: List<Vector3d> = List(NUMBER_OF_POINTS) { Vector3d() }
            val normals: List<Vector3d> = List(NUMBER_OF_NORMALS) { Vector3d() }

            return TransformedCuboidPolygon(points, normals, shipFrom)
        }

        fun createFromAABB(
            aabb: AABBdc, transform: Matrix4dc? = null, shipFrom: ShipId? = null
        ): TransformedCuboidPolygon {
            return createEmptyRectangularPrismPolygon(shipFrom).setFromAABB(aabb, transform)
        }
    }
}
