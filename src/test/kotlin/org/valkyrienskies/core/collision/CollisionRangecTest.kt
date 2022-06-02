package org.valkyrienskies.core.collision

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CollisionRangecTest {

    @Test
    fun testComputeCollisionTime1() {
        val playerCollisionRange = CollisionRange(1.0, 2.0)
        val shipCollisionRange = CollisionRange(3.0, 4.0)

        run {
            val playerVelocity = 1.0
            assertEquals(
                1.0, CollisionRangec.computeCollisionTime(playerCollisionRange, shipCollisionRange, playerVelocity)
            )
        }

        run {
            val playerVelocity = 2.0
            assertEquals(
                0.5, CollisionRangec.computeCollisionTime(playerCollisionRange, shipCollisionRange, playerVelocity)
            )
        }

        run {
            val playerVelocity = -1.0
            assertEquals(
                Double.POSITIVE_INFINITY,
                CollisionRangec.computeCollisionTime(playerCollisionRange, shipCollisionRange, playerVelocity)
            )
        }

        run {
            val playerVelocity = 1e-30
            assertEquals(
                Double.POSITIVE_INFINITY,
                CollisionRangec.computeCollisionTime(playerCollisionRange, shipCollisionRange, playerVelocity)
            )
        }
    }

    @Test
    fun testComputeCollisionTime2() {
        val playerCollisionRange = CollisionRange(1.0, 2.0)
        val shipCollisionRange = CollisionRange(-1.0, 0.0)

        run {
            val playerVelocity = -1.0
            assertEquals(
                1.0, CollisionRangec.computeCollisionTime(playerCollisionRange, shipCollisionRange, playerVelocity)
            )
        }

        run {
            val playerVelocity = -2.0
            assertEquals(
                0.5, CollisionRangec.computeCollisionTime(playerCollisionRange, shipCollisionRange, playerVelocity)
            )
        }

        run {
            val playerVelocity = 1.0
            assertEquals(
                Double.POSITIVE_INFINITY,
                CollisionRangec.computeCollisionTime(playerCollisionRange, shipCollisionRange, playerVelocity)
            )
        }

        run {
            val playerVelocity = -1e-30
            assertEquals(
                Double.POSITIVE_INFINITY,
                CollisionRangec.computeCollisionTime(playerCollisionRange, shipCollisionRange, playerVelocity)
            )
        }
    }

    @Test
    fun testComputeCollisionTime3() {
        val playerCollisionRange = CollisionRange(1.0, 2.0)
        val shipCollisionRange = CollisionRange(1.5, 2.5)

        run {
            val playerVelocity = 1.0
            assertEquals(
                0.0, CollisionRangec.computeCollisionTime(playerCollisionRange, shipCollisionRange, playerVelocity)
            )
        }

        run {
            val playerVelocity = 2.0
            assertEquals(
                0.0, CollisionRangec.computeCollisionTime(playerCollisionRange, shipCollisionRange, playerVelocity)
            )
        }

        run {
            val playerVelocity = -1.0
            assertEquals(
                0.0, CollisionRangec.computeCollisionTime(playerCollisionRange, shipCollisionRange, playerVelocity)
            )
        }

        run {
            val playerVelocity = 1e-30
            assertEquals(
                0.0, CollisionRangec.computeCollisionTime(playerCollisionRange, shipCollisionRange, playerVelocity)
            )
        }
    }
}
