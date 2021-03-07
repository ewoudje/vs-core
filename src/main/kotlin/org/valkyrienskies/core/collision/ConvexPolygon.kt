package org.valkyrienskies.core.collision

import org.joml.Matrix4dc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.primitives.AABBdc

/**
 * A convex polygon consists of points and normals.
 *
 * Note that we do not assign a specific normal for each points, because we do not need them for SAT collision.
 */
class ConvexPolygon private constructor(private val _points: List<Vector3dc>, private val _normals: List<Vector3dc>) :
    ConvexPolygonc {
    override val points: Iterable<Vector3dc> get() = _points
    override val normals: Iterable<Vector3dc> get() = _normals

    companion object {
        private val UNIT_NORMALS: List<Vector3dc> =
            listOf(Vector3d(1.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0), Vector3d(1.0, 0.0, 1.0))

        fun createFromPointsAndNormals(points: List<Vector3dc>, normals: List<Vector3dc>): ConvexPolygon {
            return ConvexPolygon(points, normals)
        }

        fun createFromAABB(aabb: AABBdc, transform: Matrix4dc? = null): ConvexPolygon {
            val points: List<Vector3d> = listOf(
                Vector3d(aabb.minX(), aabb.minY(), aabb.minZ()),
                Vector3d(aabb.minX(), aabb.minY(), aabb.maxZ()),
                Vector3d(aabb.minX(), aabb.maxY(), aabb.minZ()),
                Vector3d(aabb.minX(), aabb.maxY(), aabb.maxZ()),
                Vector3d(aabb.maxX(), aabb.minY(), aabb.minZ()),
                Vector3d(aabb.maxX(), aabb.minY(), aabb.maxZ()),
                Vector3d(aabb.maxX(), aabb.maxY(), aabb.minZ()),
                Vector3d(aabb.maxX(), aabb.maxY(), aabb.maxZ())
            )
            if (transform != null) {
                for (point in points) transform.transformPosition(point)
                val normals: MutableList<Vector3dc> = ArrayList()
                for (normal in UNIT_NORMALS) normals.add(transform.transformDirection(normal, Vector3d()))
                return ConvexPolygon(points, normals)
            }
            return ConvexPolygon(points, UNIT_NORMALS)
        }
    }
}
