package org.valkyrienskies.core.collision

import org.joml.Vector3dc

/**
 * Interface to view the result of a collision test on two [ConvexPolygonc].
 */
interface CollisionResultTimeToCollisionc {
    /**
     * @return whether the polygons are colliding or not
     */
    val colliding: Boolean

    /**
     * Accessing this will throw a [NotCollidingException] when [colliding] is false.
     *
     * @return the normal with the smallest overlap
     */
    val collisionAxis: Vector3dc

    /**
     * Accessing this will throw a [NotCollidingException] when [colliding] is false.
     *
     * @return the time to collision
     */
    val timeToCollision: Double
}
