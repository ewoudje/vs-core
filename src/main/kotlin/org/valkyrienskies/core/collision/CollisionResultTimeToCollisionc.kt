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
     * Accessing this when [colliding] is true will throw a [CollidingException].
     *
     * Also, access this when [timeToCollision] is [Double.POSITIVE_INFINITY] will throw a [NeverCollidingException].
     *
     * @return the normal with the largest time to collision
     */
    val collisionAxis: Vector3dc

    /**
     * Accessing this when [colliding] is true will throw a [CollidingException].
     *
     * @return the time to collision
     */
    val timeToCollision: Double
}
