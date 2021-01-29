package org.valkyrienskies.core

import org.joml.Matrix3d
import org.joml.Matrix4d
import org.joml.Vector3d
import org.joml.primitives.AABBd
import org.valkyrienskies.core.datastructures.IBlockPosSet
import org.valkyrienskies.core.datastructures.SmallBlockPosSet
import org.valkyrienskies.core.datastructures.SmallBlockPosSetAABB
import org.valkyrienskies.core.game.ChunkClaim
import org.valkyrienskies.core.game.ShipData
import org.valkyrienskies.core.game.ShipInertiaData
import org.valkyrienskies.core.game.ShipPhysicsData
import org.valkyrienskies.core.game.ShipTransform
import java.util.*
import kotlin.random.Random

/**
 * This singleton generates random objects to be used in tests.
 */
internal object VSRandomUtils {

    /**
     * Use this instead of random.nextDouble() to avoid overflow errors
     */
    @Suppress("WeakerAccess")
    fun randomDoubleNotCloseToLimit(random: Random = Random.Default): Double {
        return random.nextDouble(-1000000.0, 1000000.0)
    }

    /**
     * Use this instead of random.nextInt() to avoid overflow errors
     */
    @Suppress("WeakerAccess")
    fun randomIntegerNotCloseToLimit(random: Random = Random.Default): Int {
        return random.nextInt(-1000000, 1000000)
    }

    @Suppress("WeakerAccess")
    fun randomVector3d(random: Random = Random.Default): Vector3d {
        return Vector3d(randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random))
    }

    @Suppress("WeakerAccess")
    fun randomMatrix3d(random: Random = Random.Default): Matrix3d {
        return Matrix3d(randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random))
    }

    @Suppress("WeakerAccess")
    fun randomMatrix4d(random: Random = Random.Default): Matrix4d {
        return Matrix4d(randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random))
    }

    @Suppress("WeakerAccess")
    fun randomString(random: Random = Random.Default, length: Int): String {
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map{random.nextInt(0, charPool.size)}
            .map(charPool::get)
            .joinToString("")
    }

    @Suppress("WeakerAccess")
    fun randomChunkClaim(random: Random = Random.Default): ChunkClaim {
        return ChunkClaim.getClaim(randomIntegerNotCloseToLimit(random), randomIntegerNotCloseToLimit(random))
    }

    @Suppress("WeakerAccess")
    fun randomShipPhysicsData(random: Random = Random.Default): ShipPhysicsData {
        return ShipPhysicsData(randomVector3d(random), randomVector3d(random))
    }

    @Suppress("WeakerAccess")
    fun randomShipInertiaData(random: Random = Random.Default): ShipInertiaData {
        return ShipInertiaData(randomVector3d(random), randomDoubleNotCloseToLimit(random), randomMatrix3d(random))
    }

    @Suppress("WeakerAccess")
    fun randomShipTransform(random: Random = Random.Default): ShipTransform {
        return ShipTransform(randomMatrix4d(random), randomVector3d(random))
    }

    @Suppress("WeakerAccess")
    fun randomAABBd(random: Random = Random.Default): AABBd {
        return AABBd(randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random), randomDoubleNotCloseToLimit(random)).correctBounds()
    }

    @Suppress("WeakerAccess")
    fun randomBlockPosSetAABB(random: Random = Random.Default, size: Int): SmallBlockPosSetAABB {
        val centerX = randomIntegerNotCloseToLimit(random)
        val centerZ = randomIntegerNotCloseToLimit(random)
        val blockPosSet = SmallBlockPosSetAABB(centerX, 0, centerZ, 4096, 4096, 4096)
        fillBlockPosSet(random, blockPosSet, centerX, centerZ, size)
        return blockPosSet
    }

    @Suppress("WeakerAccess")
    fun randomBlockPosSet(random: Random = Random.Default, size: Int): SmallBlockPosSet {
        val centerX = randomIntegerNotCloseToLimit(random)
        val centerZ = randomIntegerNotCloseToLimit(random)
        val blockPosSet = SmallBlockPosSet(centerX, centerZ)
        fillBlockPosSet(random, blockPosSet, centerX, centerZ, size)
        return blockPosSet
    }

    private fun fillBlockPosSet(random: Random = Random.Default, blockPosSet: IBlockPosSet, centerX: Int, centerZ: Int, size: Int) {
        for (i in 1 until size) {
            val x = random.nextInt(-2048, 2047) + centerX
            val y = random.nextInt(0, 255)
            val z = random.nextInt(-2048, 2047) + centerZ
            blockPosSet.add(x, y, z)
        }
    }

    @Suppress("WeakerAccess")
    fun randomShipData(random: Random = Random.Default): ShipData {
        return ShipData(
            shipUUID = UUID.randomUUID(),
            name = randomString(random, random.nextInt(10)),
            chunkClaim = randomChunkClaim(random),
            physicsData = randomShipPhysicsData(random),
            inertiaData = randomShipInertiaData(random),
            shipTransform = randomShipTransform(random),
            prevTickShipTransform = randomShipTransform(random),
            shipAABB = randomAABBd(random),
            blockPositionSet = randomBlockPosSetAABB(random, random.nextInt(100)),
            forceBlockPositionsSet = randomBlockPosSet(random, random.nextInt(100))
        )
    }

}