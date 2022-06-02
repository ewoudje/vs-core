package org.valkyrienskies.core.collision

import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.primitives.AABBd
import org.joml.primitives.AABBdc
import org.valkyrienskies.core.util.extend
import org.valkyrienskies.core.util.horizontalLength
import org.valkyrienskies.core.util.signedDistanceTo
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.tan

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
        // Let the player climb slopes up to 45 degrees without being slowed down horizontally
        val maxSlopeClimbAngle = 46.0 // Slightly more than 45 to account for numerical error

        // Try moving horizontally
        val horizontalResponse = handleHorizontalCollisions(
            entityBoundingBox, entityVelocity, collidingPolygons, maxSlopeClimbAngle
        )

        // Commit the horizontal movement from horizontalResponse
        /*
        val verticalResponse = handleVerticalCollisions(
            entityBoundingBox.translate(horizontalResponse.x(), 0.0, horizontalResponse.z(), AABBd()),
            Vector3d(0.0, entityVelocity.y(), 0.0),
            collidingPolygons,
            maxSlopeClimbAngle
        )
         */

        return horizontalResponse
    }

    /**
     * [maxSlopeClimbAngle] is the angle of the max slope we can climb, in degrees. It must be between 0 and 90.
     */
    private fun handleHorizontalCollisions(
        entityAABB: AABBdc, initialEntityVelocity: Vector3dc, collidingPolygons: List<ConvexPolygonc>,
        maxSlopeClimbAngle: Double
    ): Vector3dc {
        if (maxSlopeClimbAngle < 0 || maxSlopeClimbAngle > 90)
            throw IllegalArgumentException("Argument maxSlopeClimbAngle must be between 0 and 90 inclusive!")
        val tanMaxSlopeClimbAngle = tan(Math.toRadians(maxSlopeClimbAngle))

        val feetAABB = createFeetAABB(entityAABB)

        val entityPolygon: ConvexPolygonc = TransformedCuboidPolygon.createFromAABB(entityAABB, null)
        val feetPolygon: ConvexPolygonc = TransformedCuboidPolygon.createFromAABB(feetAABB, null)

        // Sort the polygons by the distance of their centers to [feetAABB]
        val polysSorted = collidingPolygons.sortedBy {
            val centerPos = it.computeCenterPos(Vector3d())
            feetAABB.signedDistanceTo(centerPos)
        }

        val newEntityVelocity = Vector3d(initialEntityVelocity)

        polysSorted.forEach { shipPoly ->
            val allNormals = generateAllNormals(shipPoly.normals)

            val timeOfImpactResponse = SATConvexPolygonCollider.timeToCollision(
                entityPolygon,
                shipPoly,
                newEntityVelocity,
                allNormals.iterator()
            )

            // Overriding this to try doesn't handle the case of flying towards a block we are slightly below the top of
            // correctly
            //
            // TODO: Use [timeOfImpactResponse] to correctly handle that case. Also use it to handle stepping.
            if (true || timeOfImpactResponse.initiallyColliding) { // For now just always use this case
                // Initially colliding? Try forcing the y response if it's not too large

                val aabbExpanded = AABBd(entityAABB).extend(newEntityVelocity)
                val aabbExpandedPoly = TransformedCuboidPolygon.createFromAABB(aabbExpanded, null)

                val forcedYColResult = CollisionResult.create()
                SATConvexPolygonCollider.checkIfColliding(
                    aabbExpandedPoly,
                    shipPoly,
                    allNormals.iterator(),
                    forcedYColResult,
                    CollisionRange.create(),
                    CollisionRange.create(),
                    Y_NORMAL
                )

                if (forcedYColResult.colliding) {
                    var usingForcedYCol = false

                    val forcedYResponse: Vector3dc = forcedYColResult.getCollisionResponse(Vector3d())
                    val velWithResponseApplied = Vector3d(newEntityVelocity)
                    applyResponse(velWithResponseApplied, forcedYResponse)

                    val maxAddedYVel = max(
                        0.0,
                        tanMaxSlopeClimbAngle * initialEntityVelocity.horizontalLength()
                    )

                    if (abs(velWithResponseApplied.y()) < 1e-8) {
                        // Force small values of y velocity to be 0 to handle numerical error
                        velWithResponseApplied.y = 0.0
                    }

                    if (velWithResponseApplied.y() >= 0.0) {
                        if (velWithResponseApplied.y() <= maxAddedYVel || velWithResponseApplied.y() <= newEntityVelocity.y()) {
                            // Use forced y vel
                            newEntityVelocity.set(velWithResponseApplied)
                            usingForcedYCol = true
                        }
                    } else {
                        // Push down with up to [maxAddedYVel] vel, or any vel less than the initial entity velocity
                        if (-velWithResponseApplied.y() <= maxAddedYVel || -velWithResponseApplied.y() <= -newEntityVelocity.y()) {
                            // Use forced y vel
                            newEntityVelocity.set(velWithResponseApplied)
                            usingForcedYCol = true
                        }
                    }

                    if (!usingForcedYCol) {
                        // Use the min separating axis
                        val minAxisColResult = CollisionResult.create()
                        SATConvexPolygonCollider.checkIfColliding(
                            aabbExpandedPoly,
                            shipPoly,
                            allNormals.iterator(),
                            minAxisColResult,
                            CollisionRange.create(),
                            CollisionRange.create()
                        )

                        val minAxisColResponse = minAxisColResult.getCollisionResponse(Vector3d())
                        applyResponse(newEntityVelocity, minAxisColResponse)
                    }
                }
            }
        }

        return newEntityVelocity
    }

    /*
    private fun handleVerticalCollisions(
        entityAABB: AABBdc, initialEntityVelocity: Vector3dc, collidingPolygons: List<ConvexPolygonc>,
        maxSlopeClimbAngle: Double
    ): Vector3dc {
        // Sort the polygons by the distance of their centers to [entityAABB]
        val polysSorted = collidingPolygons.sortedBy {
            val centerPos = it.computeCenterPos(Vector3d())
            entityAABB.signedDistanceTo(centerPos)
        }

        val newEntityVelocity = Vector3d(initialEntityVelocity)

        val entityPolygon: ConvexPolygonc = TransformedCuboidPolygon.createFromAABB(entityAABB, null)

        polysSorted.forEach { shipPoly ->
            val allNormals = generateAllNormals(shipPoly.normals)

            val forcedYColResult = CollisionResult.create()
            SATConvexPolygonCollider.checkIfColliding(
                entityPolygon,
                shipPoly,
                newEntityVelocity,
                allNormals.iterator(),
                forcedYColResult,
                CollisionRange.create(),
                CollisionRange.create(),
                Y_NORMAL
            )

            if (forcedYColResult.colliding) {
                val forcedYColResponse = forcedYColResult.getCollisionResponse(Vector3d())

            }
        }

        return initialEntityVelocity
    }
     */

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

    private fun createFeetAABB(aabb: AABBdc): AABBdc {
        return AABBd(
            aabb.minX(),
            aabb.minY(),
            aabb.minZ(),
            aabb.maxX(),
            aabb.minY() + .1 * (aabb.maxY() - aabb.minY()),
            aabb.maxZ()
        )
    }

    private fun generateAllNormals(shipNormals: Iterable<Vector3dc>): List<Vector3dc> {
        val normals = ArrayList<Vector3dc>()
        for (normal in UNIT_NORMALS) normals.add(normal)
        for (normal in shipNormals) {
            normals.add(normal)
            for (unitNormal in UNIT_NORMALS) {
                val crossProduct: Vector3dc = normal.cross(unitNormal, Vector3d()).normalize()
                if (crossProduct.lengthSquared() > 1.0e-6) {
                    normals.add(crossProduct)
                }
            }
        }
        return normals
    }
}
