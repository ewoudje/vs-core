package org.valkyrienskies.core.physics.bullet.compound

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.*
import com.badlogic.gdx.physics.bullet.dynamics.*
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody.btRigidBodyConstructionInfo
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.core.physics.CuboidRigidBody
import org.valkyrienskies.core.physics.PhysicsEngine
import org.valkyrienskies.core.physics.RigidBody
import org.valkyrienskies.core.physics.VoxelRigidBody
import org.valkyrienskies.core.util.*
import java.nio.ByteBuffer

/**
 * Implementation of PhysicsEngine using Bullet compound shapes and boxes.
 * Not efficient at all, just for POC
 */
class BulletCompoundShapePhysicsEngine : PhysicsEngine {

    companion object {
        fun iterateNonemptyVoxels(buffer: ByteBuffer, dimensions: Vector3ic, func: (Int, Int, Int) -> Unit) {
            iterateVoxels(buffer, dimensions) { x, y, z, exists ->
                if (exists) func(x, y, z)
            }
        }

        fun iterateVoxels(buffer: ByteBuffer, dimensions: Vector3ic, func: (Int, Int, Int, Boolean) -> Unit) {
            val temp = Vector3i()
            buffer.iterateBits { b, i ->
                if (i > dimensions.multiplyTerms()) {
                    return@iterateBits
                }
                val (x, y, z) = unwrapIndex(i, dimensions, temp)

                func(x, y, z, b)
            }
        }

    }

    private val _rigidBodies = HashSet<RigidBody>()
    override val rigidBodies: Set<RigidBody> get() = _rigidBodies

    private val requestingUpdate = HashSet<RigidBody>()

    private val bulletWorld: btDynamicsWorld
    private val constraintSolver: btConstraintSolver
    private val collisionConfig: btCollisionConfiguration
    private val collisionDispatcher: btCollisionDispatcher
    private val broadphase: btDbvtBroadphase

    private val unitBox = btBoxShape(Vector3(0.5f, 0.5f, 0.5f))

    private val temps = VectorTemps()

    init {
        collisionConfig = btDefaultCollisionConfiguration()
        broadphase = btDbvtBroadphase()

        constraintSolver = btSequentialImpulseConstraintSolver()
        collisionDispatcher = btCollisionDispatcher(collisionConfig)
        bulletWorld = btDiscreteDynamicsWorld(
            collisionDispatcher, broadphase,
            constraintSolver, collisionConfig
        )

        bulletWorld.gravity = Vector3(0f, -9.8f, 0f)
        bulletWorld.forceUpdateAllAabbs = false

        btGImpactCollisionAlgorithm.registerAlgorithm(collisionDispatcher)
    }

    override fun applyForce(body: RigidBody, force: Vector3dc, position: Vector3dc) {
        getBullet(body).applyForce(force.toGDX(), position.toGDX())
    }

    override fun addCentralForce(body: RigidBody, force: Vector3dc) {
        getBullet(body).applyCentralForce(force.toGDX())
    }

    override fun requestUpdate(body: RigidBody) {
        requestingUpdate += body
    }

    private fun getBullet(body: RigidBody): btRigidBody {
        return when (body) {
            is VoxelRigidBody -> body.attached.getValue<VoxelRigidBodyData>().bulletBody
            is CuboidRigidBody -> error("Not supported yet")
        }
    }

    override fun addRigidBody(body: RigidBody) {
        _rigidBodies += body

        when (body) {
            is VoxelRigidBody -> addVoxelRigidBody(body)
            is CuboidRigidBody -> error("Not supported yet")
        }
    }

    private fun addVoxelRigidBody(body: VoxelRigidBody) {
        val mass = body.inertiaData?.mass?.toFloat() ?: 0f
        val compoundShape = btCompoundShape()

        iterateNonemptyVoxels(body.buffer, body.dimensions) { x, y, z ->
            compoundShape.addChildShape(
                Matrix4().setTranslation(x.toFloat(), y.toFloat(), z.toFloat()),
                unitBox
            )
        }

        val bulletBody = btRigidBody(
            btRigidBodyConstructionInfo(
                mass, null, compoundShape, compoundShape.getLocalInertia(mass, Vector3())
            )
        )

        val data = VoxelRigidBodyData(bulletBody, compoundShape)
        body.attached.set(data)

        bulletWorld.addRigidBody(bulletBody)
    }

    override fun removeRigidBody(body: RigidBody) {
        when (body) {
            is VoxelRigidBody -> removeVoxelRigidBody(body)
            is CuboidRigidBody -> error("Not supported")
        }
    }

    private fun removeVoxelRigidBody(body: VoxelRigidBody) {
        val data = body.attached.getValue<VoxelRigidBodyData>()

        bulletWorld.removeRigidBody(data.bulletBody)
        data.compoundShape.dispose()
        data.bulletBody.dispose()
    }

    private fun updateVoxelRigidBody(body: VoxelRigidBody) {
        val data = body.attached.getValue<VoxelRigidBodyData>()
        body.toAdd.forEach { index ->
            val (x, y, z) = unwrapIndex(index, body.dimensions, temps.v3i[0])
            val transform = Matrix4().setTranslation(Vector3(x.toFloat(), y.toFloat(), z.toFloat()))
            data.compoundShape.addChildShape(transform, unitBox)
        }

        // Convert toRemove to set for better time complexity
        val toRemove = body.toRemove.toSet()
        for (i in (data.compoundShape.numChildShapes - 1) downTo 0) {
            val transform = data.compoundShape.getChildTransform(i)
            val translation = transform.getTranslation(temps.v3GDX[0]) assignTo temps.v3i[0]
            val voxelIndex = wrapIndex(translation, body.dimensions)
            if (toRemove.contains(voxelIndex)) {
                data.compoundShape.removeChildShapeByIndex(i)
            }
        }
    }

    override fun tick(deltaNs: Long) {
        requestingUpdate.removeAll { body ->
            when (body) {
                is VoxelRigidBody -> updateVoxelRigidBody(body)
                is CuboidRigidBody -> {}
            }

            val bulletBody = getBullet(body)
            bulletBody.centerOfMassTransform
            body.linearVelocity assignTo bulletBody.linearVelocity
            body.angularVelocity assignTo bulletBody.angularVelocity
            body.transform assignTo bulletBody.worldTransform
            true
        }

        bulletWorld.stepSimulation(deltaNs / 1e9f)

        rigidBodies.forEach { body ->
            val bulletBody = getBullet(body)
            bulletBody.linearVelocity assignTo body.linearVelocity
            bulletBody.angularVelocity assignTo body.angularVelocity
            bulletBody.worldTransform assignTo body.transform
            bulletBody.totalTorque assignTo body._totalTorque
        }
    }

    private interface RigidBodyData {
        val bulletBody: btRigidBody
    }

    private class VoxelRigidBodyData(
        override val bulletBody: btRigidBody,
        val compoundShape: btCompoundShape
    ) : RigidBodyData
}

