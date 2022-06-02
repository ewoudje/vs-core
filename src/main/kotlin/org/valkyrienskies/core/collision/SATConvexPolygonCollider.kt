package org.valkyrienskies.core.collision

import org.joml.Vector3dc
import kotlin.math.abs

/**
 * A basic implementation of [ConvexPolygonCollider] using the Separating Axis Theorem algorithm.
 */
object SATConvexPolygonCollider : ConvexPolygonCollider {
    override fun checkIfColliding(
        firstPolygon: ConvexPolygonc,
        secondPolygon: ConvexPolygonc,
        normals: Iterator<Vector3dc>,
        collisionResult: CollisionResult,
        temp1: CollisionRange,
        temp2: CollisionRange,
        forcedResponseNormal: Vector3dc?
    ) {
        var minCollisionDepth = Double.MAX_VALUE
        collisionResult._colliding = true // Initially assume that polygons are collided

        for (normal in normals) {
            // Calculate the overlapping range of the projection of both polygons along the [normal] axis
            val rangeOverlapResponse =
                computeCollisionResponseAlongNormal(
                    firstPolygon, secondPolygon, normal, temp1, temp2
                )

            if (abs(rangeOverlapResponse) < 1.0e-6) {
                // Polygons are separated along [normal], therefore they are NOT colliding
                collisionResult._colliding = false
                return
            } else {
                if (forcedResponseNormal != null) {
                    val dotProduct = forcedResponseNormal.dot(normal)
                    if (abs(dotProduct) < 1e-6) continue // Skip
                    val modifiedRangeOverlapResponse = rangeOverlapResponse / dotProduct

                    // Polygons are colliding along this axis, doesn't guarantee if the polygons are colliding or not
                    val collisionDepth = abs(modifiedRangeOverlapResponse)
                    if (collisionDepth < minCollisionDepth) {
                        minCollisionDepth = collisionDepth
                        collisionResult._collisionAxis.set(forcedResponseNormal)
                        collisionResult._penetrationOffset = modifiedRangeOverlapResponse
                    }
                } else {
                    // Polygons are colliding along this axis, doesn't guarantee if the polygons are colliding or not
                    val collisionDepth = abs(rangeOverlapResponse)
                    if (collisionDepth < minCollisionDepth) {
                        minCollisionDepth = collisionDepth
                        collisionResult._collisionAxis.set(normal)
                        collisionResult._penetrationOffset = rangeOverlapResponse
                    }
                }
            }
        }

        if (minCollisionDepth == Double.MAX_VALUE) collisionResult._colliding = false
    }

    override fun timeToCollision(
        firstPolygon: ConvexPolygonc, secondPolygon: ConvexPolygonc, firstPolygonVelocity: Vector3dc,
        normals: Iterator<Vector3dc>
    ): CollisionResultTimeToCollisionc {
        val temp1 = CollisionRange.create()
        val temp2 = CollisionRange.create()
        val result = CollisionResultTimeToCollision.createEmptyCollisionResultTimeToCollision()

        var maxTimeToCollision = 0.0
        result._initiallyColliding = true // Initially assume that polygons are collided

        for (normal in normals) {
            // Calculate the overlapping range of the projection of both polygons along the [normal] axis
            val timeToImpactResponse =
                computeTimeToCollisionAlongNormal(
                    firstPolygon, secondPolygon, firstPolygonVelocity, normal, temp1, temp2
                )

            if (timeToImpactResponse != 0.0) {
                // Polygons are not colliding along [normal]
                result._initiallyColliding = false
                if (timeToImpactResponse > maxTimeToCollision) {
                    maxTimeToCollision = timeToImpactResponse
                    result._collisionAxis.set(normal)
                    result._timeToCollision = maxTimeToCollision
                    // Stop looping if we will never collide with the other polygon
                    if (timeToImpactResponse == Double.POSITIVE_INFINITY) break
                }
            }
        }

        return result
    }

    fun computeCollisionResponseAlongNormal(
        firstPolygon: ConvexPolygonc,
        secondPolygon: ConvexPolygonc,
        normal: Vector3dc,
        temp1: CollisionRange,
        temp2: CollisionRange
    ): Double {
        // Check if the polygons are separated along the [normal] axis
        val firstCollisionRange: CollisionRangec = firstPolygon.getProjectionAlongAxis(normal, temp1)
        val secondCollisionRange: CollisionRangec = secondPolygon.getProjectionAlongAxis(normal, temp2)

        return CollisionRangec.computeCollisionResponse(
            firstCollisionRange,
            secondCollisionRange
        )
    }

    fun computeTimeToCollisionAlongNormal(
        firstPolygon: ConvexPolygonc,
        secondPolygon: ConvexPolygonc,
        firstPolygonVelocity: Vector3dc,
        normal: Vector3dc,
        temp1: CollisionRange,
        temp2: CollisionRange
    ): Double {
        // Check if the polygons are separated along the [normal] axis
        val firstCollisionRange: CollisionRangec = firstPolygon.getProjectionAlongAxis(normal, temp1)
        val secondCollisionRange: CollisionRangec = secondPolygon.getProjectionAlongAxis(normal, temp2)
        val firstRangeVelocityAlongNormal = firstPolygonVelocity.dot(normal)

        return CollisionRangec.computeCollisionTime(
            firstCollisionRange,
            secondCollisionRange,
            firstRangeVelocityAlongNormal
        )
    }
}
