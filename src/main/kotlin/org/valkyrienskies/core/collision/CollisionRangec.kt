package org.valkyrienskies.core.collision

import kotlin.math.abs

/**
 * An immutable view of [CollisionRange].
 */
interface CollisionRangec {
    val min: Double
    val max: Double

    companion object {
        /**
         * @return The offset we must move [collisionRange1] such that it is no longer overlapping with [collisionRange2]. If [collisionRange1] and [collisionRange2] are not overlapping then this returns 0.
         */
        fun computeCollisionResponse(
            collisionRange1: CollisionRangec,
            collisionRange2: CollisionRangec,
            collisionRange1Velocity: Double = 0.0
        ): Double {
            val pushLeft = -collisionRange1.max + collisionRange2.min
            val pushRight = -collisionRange1.min + collisionRange2.max

            var pushLeftWithRespectToVelocity = -collisionRange1.max + collisionRange2.min
            var pushRightWithRespectToVelocity = -collisionRange1.min + collisionRange2.max

            if (collisionRange1Velocity > 0) {
                pushLeftWithRespectToVelocity -= collisionRange1Velocity
            } else {
                pushRightWithRespectToVelocity -= collisionRange1Velocity
            }

            return if (pushRightWithRespectToVelocity <= 0 || pushLeftWithRespectToVelocity >= 0) {
                // Not overlapping
                0.0
            } else if (abs(pushLeft) > abs(pushRight)) {
                // Its more efficient to push [collisionRange1] left
                pushRightWithRespectToVelocity
            } else {
                // Its more efficient to push [collisionRange1] right
                pushLeftWithRespectToVelocity
            }
        }

        /**
         * @return The time when [collisionRange1] will collide with [collisionRange2] given [collisionRange1Velocity].
         *         If they are initially overlapping then we return 0.0, if they will never overlap we return
         *         [Double.POSITIVE_INFINITY]
         */
        fun computeCollisionTime(
            collisionRange1: CollisionRangec,
            collisionRange2: CollisionRangec,
            collisionRange1Velocity: Double
        ): Double {
            val pushLeft = -collisionRange1.max + collisionRange2.min
            val pushRight = -collisionRange1.min + collisionRange2.max

            return if (pushRight <= 0 || pushLeft >= 0) {
                // If vel is too small then assume that it will take infinite time to collide
                if (abs(collisionRange1Velocity) < 1e-8) return Double.POSITIVE_INFINITY

                // Compute time before pushRight is > 0 or pushLeft < 0
                if (collisionRange1Velocity > 0) {
                    if (pushLeft >= 0) pushLeft / collisionRange1Velocity
                    else Double.POSITIVE_INFINITY
                } else {
                    if (pushRight <= 0) pushRight / collisionRange1Velocity
                    else Double.POSITIVE_INFINITY
                }
            } else {
                // Already overlapping, so time of impact is 0
                0.0
            }
        }
    }
}
