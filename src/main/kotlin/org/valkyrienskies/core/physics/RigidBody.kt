package org.valkyrienskies.core.physics

import org.joml.Matrix4d
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3ic
import org.valkyrienskies.core.util.multiplyTerms
import java.nio.ByteBuffer

// TODO: make 'RigidBodyShape' class for static objects

sealed class RigidBody(
    /**
     * Starts at <0,0,0> relative to the voxel shape.
     * This will be mutated.
     *
     * You may mutate this (request update)
     */
    val transform: Matrix4d,
    /**
     * If inertia data is null, then this rigid body
     * is STATIC and does not move.
     */
    val inertiaData: InertiaData?
) {
    /**
     * You may mutate this (request update)
     */
    val angularVelocity = Vector3d()

    /**
     * You may mutate this (request update)
     */
    val linearVelocity = Vector3d()

    /**
     * Do NOT mutate
     */
    internal val _totalTorque = Vector3d()
    val totalTorque: Vector3dc = _totalTorque

    /**
     * Do NOT mutate
     */
    internal val _totalForce = Vector3d()
    val totalForce: Vector3dc = _totalForce
}

class VoxelRigidBody(
    /**
     * Buffer representing the voxel shape (bit set to 1 is an occupied voxel).
     * It goes 0 -> +x, then 0 -> +y, then 0 -> +z
     *
     * Do NOT mutate this
     */
    val buffer: ByteBuffer,
    /**
     * List of voxel indices to remove on next physics tick. Will be cleared on next physics tick.
     *
     * You may mutate this AND change reference (request update)
     */
    var toRemove: IntArray,
    /**
     * List of voxel indices to add on next physics tick. Will be cleared on next physics tick.
     *
     * You may mutate this AND change reference (request update)
     */
    var toAdd: IntArray,
    /**
     * Dimensions of the voxel shape
     *
     * You may mutate this (request update)
     */
    val dimensions: Vector3ic,
    transform: Matrix4d,
    inertiaData: InertiaData?
) : RigidBody(transform, inertiaData) {
    init {
        require(buffer.isDirect && buffer.capacity() >= ((dimensions.multiplyTerms() + 7) / 8)) {
            "Buffer must be direct and capacity must fit volume / 8"
        }
    }
}

class CuboidRigidBody(
    val halfExtents: Vector3dc,
    transform: Matrix4d,
    inertiaData: InertiaData?
) : RigidBody(transform, inertiaData)

