package org.valkyrienskies.core.collision

import org.joml.Vector3d
import org.joml.Vector3dc

data class CollisionResultTimeToCollision internal constructor(
    internal var _initiallyColliding: Boolean,
    internal var _collisionAxis: Vector3d,
    internal var _timeToCollision: Double
) : CollisionResultTimeToCollisionc {
    override val initiallyColliding: Boolean get() = _initiallyColliding
    override val collisionAxis: Vector3dc
        get() {
            if (initiallyColliding) throw CollidingException("Cannot access collisionAxis because we are colliding!")
            if (_timeToCollision == Double.POSITIVE_INFINITY) throw NeverCollidingException(
                "Cannot access collisionAxis because we will never collide!"
            )
            return _collisionAxis
        }
    override val timeToCollision: Double
        get() {
            if (initiallyColliding) throw CollidingException("Cannot access timeToCollision because we are colliding!")
            return _timeToCollision
        }

    companion object {
        internal fun createEmptyCollisionResultTimeToCollision(): CollisionResultTimeToCollision {
            return CollisionResultTimeToCollision(false, Vector3d(), Double.POSITIVE_INFINITY)
        }
    }
}
