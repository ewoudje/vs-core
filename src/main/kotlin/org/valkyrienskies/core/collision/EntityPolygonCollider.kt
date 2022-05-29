package org.valkyrienskies.core.collision

import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.primitives.AABBdc
import org.valkyrienskies.core.util.horizontalLength
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sign

object EntityPolygonCollider {

    private val X_NORMAL: Vector3dc = Vector3d(1.0, 0.0, 0.0)
    private val Y_NORMAL: Vector3dc = Vector3d(0.0, 1.0, 0.0)
    private val Z_NORMAL: Vector3dc = Vector3d(0.0, 0.0, 1.0)
    private val UNIT_NORMALS = arrayOf(X_NORMAL, Y_NORMAL, Z_NORMAL)

    /**
     * @return [movement] modified such that the entity is colliding with [collidingPolygons]
     */
    fun adjustEntityMovementForPolygonCollisions(
        movement: Vector3dc,
        entityBoundingBox: AABBdc,
        entityStepHeight: Double,
        collidingPolygons: List<ConvexPolygonc>
    ): Vector3dc {
        val originalMovement: Vector3dc = movement

        return adjustMovementComponentWise(entityBoundingBox, originalMovement, collidingPolygons)
    }

    /**
     * @return [entityVelocity] modified such that the entity is colliding with [collidingPolygons], with the Y-axis prioritized
     */
    private fun adjustMovementComponentWise(
        entityBoundingBox: AABBdc, entityVelocity: Vector3dc, collidingPolygons: List<ConvexPolygonc>
    ): Vector3dc {
        val entityPolygon: ConvexPolygonc = TransformedCuboidPolygon.createFromAABB(entityBoundingBox, null)

        // Try moving horizontally
        val horizontalResponse = handleHorizontalCollisions(
            entityPolygon, entityVelocity, collidingPolygons, 45.0
        )

        entityPolygon.points.forEach {
             it as Vector3d
             it.add(horizontalResponse.x(), 0.0, horizontalResponse.z())
        }

        val newEntityVelocity = Vector3d(0.0, entityVelocity.y(), 0.0)

        // Try moving vertically
        val yOnlyResponse = adjustMovementAlongOneAxis(
            entityPolygon, Vector3d(newEntityVelocity), collidingPolygons, UNIT_NORMALS[1], 45.0
        )

        return Vector3d(horizontalResponse.x() + yOnlyResponse.x(), yOnlyResponse.y(), horizontalResponse.z() + yOnlyResponse.z())
    }

    /**
     * Put the player back on the ground, or at least try to...
     */
    private fun adjustMovementAlongOneAxis(
        entityPolygon: ConvexPolygonc, entityVelocity: Vector3dc, collidingPolygons: List<ConvexPolygonc>,
        forcedResponseNormal: Vector3dc, forceResponseAngle: Double
    ): Vector3dc {
        val newEntityVelocity = Vector3d(entityVelocity)
        val collisionResult = CollisionResult.create()

        // region declare temp objects
        val temp1 = CollisionRange.create()
        val temp2 = CollisionRange.create()
        // endregion

        for (shipPolygon in collidingPolygons) {
            val normals: MutableList<Vector3dc> = ArrayList()

            for (normal in UNIT_NORMALS) normals.add(normal)
            for (normal in shipPolygon.normals) {
                normals.add(normal)
                for (unitNormal in UNIT_NORMALS) {
                    val crossProduct: Vector3dc = normal.cross(unitNormal, Vector3d()).normalize()
                    if (crossProduct.lengthSquared() > 1.0e-6) {
                        normals.add(crossProduct)
                    }
                }
            }

            SATConvexPolygonCollider.checkIfColliding(
                entityPolygon,
                shipPolygon,
                newEntityVelocity,
                normals.iterator(),
                collisionResult,
                temp1,
                temp2
            )
            if (collisionResult.colliding) {
                val forcedCollisionAxisResult = CollisionResult.create()
                // If we can force the response, then force the response
                SATConvexPolygonCollider.checkIfColliding(
                    entityPolygon,
                    shipPolygon,
                    newEntityVelocity,
                    normals.iterator(),
                    forcedCollisionAxisResult,
                    CollisionRange.create(),
                    CollisionRange.create(),
                    forcedResponseNormal
                )

                val forcedUpResponse = forcedCollisionAxisResult.getCollisionResponse(Vector3d())

                applyResponse(newEntityVelocity, forcedUpResponse)
            }
        }

        return newEntityVelocity
    }

    private fun handleHorizontalCollisions(
        entityPolygon: ConvexPolygonc, entityVelocity: Vector3dc, collidingPolygons: List<ConvexPolygonc>,
        maxSlopeClimbAngle: Double
    ): Vector3dc {
        val newEntityVelocity = Vector3d(entityVelocity)
        val collisionResult = CollisionResult.create()

        // The maximum y velocity we can add to climb up slopes
        val maxStep = max(entityVelocity.horizontalLength() * cos(Math.toRadians(maxSlopeClimbAngle)), 1e-2)

        for (shipPolygon in collidingPolygons) {
            val normals: MutableList<Vector3dc> = ArrayList()

            for (normal in UNIT_NORMALS) normals.add(normal)
            for (normal in shipPolygon.normals) {
                normals.add(normal)
                for (unitNormal in UNIT_NORMALS) {
                    val crossProduct: Vector3dc = normal.cross(unitNormal, Vector3d()).normalize()
                    if (crossProduct.lengthSquared() > 1.0e-6) {
                        normals.add(crossProduct)
                    }
                }
            }

            SATConvexPolygonCollider.checkIfColliding(
                entityPolygon,
                shipPolygon,
                newEntityVelocity,
                normals.iterator(),
                collisionResult,
                CollisionRange.create(),
                CollisionRange.create()
            )
            if (collisionResult.colliding) {
                // Compute the response that pushes the entity out of this polygon
                val collisionResponse: Vector3dc = collisionResult.getCollisionResponse(Vector3d())


                val forcedYCollisionResult = CollisionResult.create()
                SATConvexPolygonCollider.checkIfColliding(
                    entityPolygon,
                    shipPolygon,
                    newEntityVelocity,
                    normals.iterator(),
                    forcedYCollisionResult,
                    CollisionRange.create(),
                    CollisionRange.create(),
                    Y_NORMAL
                )

                val forcedYCollisionResponse = forcedYCollisionResult.getCollisionResponse(Vector3d())

                val newNewVel = Vector3d(newEntityVelocity) // The new velocity IF we apply forcedYCollisionResponse
                applyResponse(newNewVel, forcedYCollisionResponse)
                // Only climb slopes if forcedYCollisionResponse.y() is >= than 0
                if (forcedYCollisionResponse.y() >= 0 && newNewVel.y() < maxStep) {
                    applyResponse(newEntityVelocity, forcedYCollisionResponse)
                } else {
                    applyResponse(newEntityVelocity, collisionResponse)
                }
            }
        }
        return newEntityVelocity
    }

    private fun applyResponse(velocity: Vector3d, response: Vector3dc) {
        velocity.x = applyResponseOneAxis(velocity.x(), response.x())
        velocity.y = applyResponseOneAxis(velocity.y(), response.y())
        velocity.z = applyResponseOneAxis(velocity.z(), response.z())
    }

    private fun applyResponseOneAxis(vel: Double, response: Double): Double {
        return if (sign(vel) == sign(response))
            sign(response) * max(abs(vel), abs(response))
        else
            vel + response
    }
}
