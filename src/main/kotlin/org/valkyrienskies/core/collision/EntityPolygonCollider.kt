package org.valkyrienskies.core.collision

import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.primitives.AABBd
import org.joml.primitives.AABBdc
import org.valkyrienskies.core.util.horizontalLengthSq
import java.util.ArrayList

object EntityPolygonCollider {

    private val UNIT_NORMALS =
        arrayOf<Vector3dc>(Vector3d(1.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0), Vector3d(0.0, 0.0, 1.0))

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

        // Compute the collision response assuming the entity can't step
        val collisionResponseAssumingNoStep: Vector3dc =
            adjustMovementComponentWise(entityBoundingBox, originalMovement, collidingPolygons)

        // If [entityStepHeight] is 0 then it can't step
        if (entityStepHeight == 0.0) {
            return collisionResponseAssumingNoStep
        }

        // If the entity is not moving horizontally then it can't step
        if (movement.horizontalLengthSq() < 1e-4) {
            return collisionResponseAssumingNoStep
        }

        // Determine if the entity is standing on the polygons
        val isEntityStandingOnPolygons =
            isEntityStandingOnPolygons(
                TransformedCuboidPolygon.createFromAABB(entityBoundingBox, null), originalMovement, collidingPolygons
            )

        // If the entity is not standing on the polygons then it can't step
        if (!isEntityStandingOnPolygons) {
            return collisionResponseAssumingNoStep
        }

        // Compute the collision response assuming the entity can step
        val collisionResponseAssumingFullStep =
            adjustMovementComponentWise(
                entityBoundingBox,
                Vector3d(originalMovement.x(), entityStepHeight, originalMovement.z()),
                collidingPolygons
            )

        val originalMovementSpeedSq = originalMovement.horizontalLengthSq()

        val collisionResponseAssumingNoStepHorizontalSpeedSq = collisionResponseAssumingNoStep.horizontalLengthSq()

        val collisionResponseAssumingFullStepHorizontalSpeedSq = collisionResponseAssumingFullStep.horizontalLengthSq()

        // Only choose [collisionResponseAssumingFullStep] if it has a larger horizontal speed than [collisionResponseAssumingNoStep]
        if (collisionResponseAssumingFullStepHorizontalSpeedSq >= collisionResponseAssumingNoStepHorizontalSpeedSq &&
            collisionResponseAssumingFullStepHorizontalSpeedSq >= originalMovementSpeedSq
        ) {
            // Now that we've chosen [collisionResponseAssumingFullStep], move the entity downwards such that it is still on the ground
            val entityAfterSteppingFullyPolygon: ConvexPolygonc =
                TransformedCuboidPolygon.createFromAABB(
                    entityBoundingBox.translate(
                        collisionResponseAssumingFullStep.x(), collisionResponseAssumingFullStep.y(),
                        collisionResponseAssumingFullStep.z(), AABBd()
                    ),
                    null
                )
            val fixStepUpResponse = adjustMovement(
                entityAfterSteppingFullyPolygon,
                Vector3d(0.0, originalMovement.y() - collisionResponseAssumingFullStep.y(), 0.0),
                collidingPolygons, UNIT_NORMALS[1]
            )

            return fixStepUpResponse.add(collisionResponseAssumingFullStep, Vector3d())
        } else {
            // [collisionResponseAssumingNoStep] had a larger horizontal speed, so we choose it instead of [collisionResponseAssumingFullStep]
            return collisionResponseAssumingNoStep
        }
    }

    /**
     * @return [entityVelocity] modified such that the entity is colliding with [collidingPolygons], with the Y-axis prioritized
     */
    private fun adjustMovementComponentWise(
        entityBoundingBox: AABBdc, entityVelocity: Vector3dc, collidingPolygons: List<ConvexPolygonc>
    ): Vector3dc {
        val entityPolygon: ConvexPolygonc = TransformedCuboidPolygon.createFromAABB(entityBoundingBox, null)

        // First collide along the y-axis
        val yOnlyResponse = adjustMovement(
            entityPolygon, Vector3d(0.0, entityVelocity.y(), 0.0), collidingPolygons, UNIT_NORMALS[1]
        )

        entityPolygon.points.forEach {
            it as Vector3d
            it.add(yOnlyResponse)
        }

        // Then collide along the x-axis
        val horizontalResponse = adjustMovement(
            entityPolygon, Vector3d(entityVelocity.x(), 0.0, entityVelocity.z()), collidingPolygons
        )

        return Vector3d(horizontalResponse.x(), yOnlyResponse.y() + horizontalResponse.y(), horizontalResponse.z())
    }

    /**
     * @return True if and only if [entityPolygon] is standing on [collidingPolygons].
     */
    private fun isEntityStandingOnPolygons(
        entityPolygon: ConvexPolygonc, entityVelocity: Vector3dc, collidingPolygons: List<ConvexPolygonc>
    ): Boolean {
        val collisionResult = CollisionResult.create()

        // region declare temp objects
        val temp1 = CollisionRange.create()
        val temp2 = CollisionRange.create()
        val temp3 = Vector3d()
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
                entityVelocity,
                normals.iterator(),
                collisionResult,
                temp1,
                temp2
            )
            if (collisionResult.colliding) {
                // Compute the response that pushes the player out of this polygon
                val collisionResponse: Vector3dc = collisionResult.getCollisionResponse(temp3)

                if (Math.toDegrees(collisionResponse.angle(UNIT_NORMALS[1])) < 30) {
                    // If this response is less than 30 degrees from the Y normal then this entity is standing on [collidingPolygons]
                    return true
                }
            }
        }
        return false
    }

    /**
     * @return [entityVelocity] modified such that the entity is colliding with [collidingPolygons]. If [forcedResponseNormal] != null then the collision response will be parallel to [forcedResponseNormal].
     */
    private fun adjustMovement(
        entityPolygon: ConvexPolygonc, entityVelocity: Vector3dc, collidingPolygons: List<ConvexPolygonc>,
        forcedResponseNormal: Vector3dc? = null
    ): Vector3dc {
        val newEntityVelocity = Vector3d(entityVelocity)
        val collisionResult = CollisionResult.create()

        // region declare temp objects
        val temp1 = CollisionRange.create()
        val temp2 = CollisionRange.create()
        val temp3 = Vector3d()
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
                // Compute the response that pushes the entity out of this polygon
                val collisionResponse: Vector3dc =
                    if (forcedResponseNormal != null) {
                        SATConvexPolygonCollider.checkIfColliding(
                            entityPolygon,
                            shipPolygon,
                            newEntityVelocity,
                            normals.iterator(),
                            collisionResult,
                            temp1,
                            temp2,
                            forcedResponseNormal
                        )
                        collisionResult.getCollisionResponse(temp3)
                    } else {
                        collisionResult.getCollisionResponse(temp3)
                    }

                newEntityVelocity.add(collisionResponse)
            }
        }
        return newEntityVelocity
    }
}
